/* Copyright (C) 2016  Intel Corporation
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 only, as published by the Free Software Foundation.
 * This file has been designated as subject to the "Classpath"
 * exception as provided in the LICENSE file that accompanied
 * this code.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License version 2 for more details (a copy
 * is included in the LICENSE file that accompanied this code).
 *
 * You should have received a copy of the GNU General Public License
 * version 2 along with this program; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA  02110-1301, USA.
 */

#include "persistent_heap.h"
#include "util.h"
#include "persistent_structs.h"
#include "persistent_byte_buffer.h"
#include "persistent_sorted_map.h"

PMEMobjpool *pool = NULL;
TOID(struct root_struct) root;
TOID(struct hashmap_tx) vm_offsets;

static uint64_t uuid_lo;
static int pool_refs = 0;
//static const size_t PM_POOL_SIZE = 1 * 1024 * 1024 * 1024;    // default 1GB
static const size_t PM_POOL_SIZE = 1 * 1024 * 1024 * 1024 + (1 * 1024 * 1024 * 1024 - 1);    // max size 2GB-1
static const char PATH[50] = "/mnt/mem/persistent_pool";

PMEMobjpool* get_or_create_pool()
{
    pool_refs++;
    if (pool != NULL) {
        return pool;
    }
    if (access(PATH, F_OK)) {
        pool = pmemobj_create(PATH, POBJ_LAYOUT_NAME(persistent_structures),
                              PM_POOL_SIZE, S_IRUSR | S_IWUSR);
    } else {
        pool = pmemobj_open(PATH, POBJ_LAYOUT_NAME(persistent_structures));
    }

    TOID(char) arr;
    TX_BEGIN (pool) {
        arr = TX_ALLOC(char, 1);
        uuid_lo = arr.oid.pool_uuid_lo;
        TX_FREE(arr);
        TX_MEMSET(pmemobj_direct(arr.oid), 0, 1);
    } TX_ONABORT {
        printf("Encountered error opening pool!\n");
        exit(-1);
    } TX_END

    printf("Opening heap... ");
    fflush(stdout);
    root = POBJ_ROOT(pool, struct root_struct);
    create_root(root);
    vm_offsets = D_RO(root)->vm_offsets;

    //walk_through_vm_offsets();
    printf("Cleaning up heap... ");
    fflush(stdout);
    hm_tx_foreach(pool, vm_offsets, decrement_from_obj_ref_counts, NULL);
    hm_tx_clear(pool, vm_offsets);
    //walk_through_vm_offsets();
    //collect();
    //print_all_objects();

    printf("Done.\n");
    fflush(stdout);
    return pool;
}

TOID(struct root_struct) get_root()
{
    return root;
}

uint64_t get_uuid_lo()
{
    return uuid_lo;
}

int add_to_vm_offsets(uint64_t offset)
{
    uint64_t old_count;
    NEPMEMoid ret = hm_tx_get(pool, vm_offsets, offset);
    if (NEOID_IS_NULL(ret)) {
        old_count = 0;
    } else {
        old_count = ret.value;
    }
    //printf("Adding: Offset %lu had old count of %lu\n", offset, old_count);
    fflush(stdout);
    ret = hm_tx_insert(pool, vm_offsets, offset, old_count + 1);
    if (NEOID_IS_ERR(ret)) {
        printf("Encountered error adding a new reference to offset %lu\n", offset);
        exit(-1);
    } else {
        //printf("Added a new reference to offset %lu to vm_offsets, new count is %d\n", offset, (hm_tx_get(pool, vm_offsets, offset)).value);
        fflush(stdout);
    }
    return 0;
}

int remove_from_vm_offsets(uint64_t offset)
{
    //printf("Removing: Offset %lu\n", offset);
    fflush(stdout);
    NEPMEMoid ret = hm_tx_remove(pool, vm_offsets, offset);
    if (NEOID_IS_ERR(ret) || NEOID_IS_NULL(ret)) {
        printf("Encountered error removing all references to offset %lu\n", offset);
        exit(-1);
    }
    return 0;
}

int decrement_from_vm_offsets(uint64_t offset)
{
    uint64_t old_count;
    NEPMEMoid ret = hm_tx_get(pool, vm_offsets, offset);
    if (NEOID_IS_NULL(ret)) {
        printf("Trying to decrement a reference from offset %lu when there are none!\n", offset);
        exit(-1);
    } else {
        old_count = ret.value;
    }
    //printf("Decrementing: Offset %lu had old count of %lu\n", offset, old_count);
    if (old_count == 1) {
        ret = hm_tx_remove(pool, vm_offsets, offset);
        if (NEOID_IS_ERR(ret)) {
            printf("Encountered error removing the last reference to offset %lu\n", offset);
            exit(-1);
        } else {
            //printf("Removed all references to offset %lu in vm_offsets\n", offset);
            fflush(stdout);
        }
    } else {
        ret = hm_tx_insert(pool, vm_offsets, offset, old_count - 1);
        if (NEOID_IS_ERR(ret)) {
            printf("Encountered error decrementing one reference to offset %lu\n", offset);
            exit(-1);
        } else {
            //printf("Removed a reference to offset %lu to vm_offsets, new count is %d\n", offset, (hm_tx_get(pool, vm_offsets, offset)).value);
            fflush(stdout);
        }
    }
    return 0;
}

int decrement_from_obj_ref_counts(uint64_t key, uint64_t value, void* arg)
{
    PMEMoid oid = {get_uuid_lo(), key};
    dec_ref(oid, value);
    return 0;
}

int walk_through_vm_offsets()
{
    hm_tx_cmd(pool, vm_offsets, HASHMAP_CMD_DEBUG, 0);
}

int inc_ref(PMEMoid oid, int amount)
{
    TOID(struct header) *hdr = (TOID(struct header)*)(pmemobj_direct(oid));
    //printf("incref: refcount is %d on offset %lu\n", D_RO(*hdr)->refCount, oid.off);
    fflush(stdout);

    int ret = 0;
    TX_BEGIN_LOCK(pool, TX_LOCK_MUTEX, &D_RW(*hdr)->obj_mutex, TX_LOCK_NONE) {
        TX_ADD_FIELD(*hdr, refCount);
        TX_ADD_FIELD(*hdr, color);

        D_RW(*hdr)->refCount += amount;
        D_RW(*hdr)->color = BLACK;
    } TX_ONABORT {
        printf("IncRef failed on offset %lu, exiting now\n", oid.off);
        exit(-1);
        //ret = 1;
    } TX_END

    return ret;
}

int dec_ref(PMEMoid oid, int amount)
{
    TOID(struct header) *hdr = (TOID(struct header)*)(pmemobj_direct(oid));
    //printf("decref: refcount is %d on offset %lu\n", D_RO(*hdr)->refCount, oid.off);
    fflush(stdout);

    int ret = 0;
    TX_BEGIN_LOCK(pool, TX_LOCK_MUTEX, &D_RW(*hdr)->obj_mutex, TX_LOCK_NONE) {
        TX_ADD_FIELD(*hdr, refCount);
        D_RW(*hdr)->refCount -= amount;

        if (D_RO(*hdr)->refCount == 0) {
            delete_struct(oid);
        } else {
            add_candidate(oid);
        }
    } TX_ONABORT {
        printf("DecRef failed on offset %lu, exiting now\n", oid.off);
        exit(-1);
        //ret = 1;
    } TX_END

    return ret;
}

int delete_struct(PMEMoid oid)
{
    TOID(struct header) *hdr = (TOID(struct header)*)(pmemobj_direct(oid));
    //printf("delete_struct: type is %d on offset %lu, hdr is at %lu\n", D_RO(*hdr)->type, oid.off, (*hdr).oid.off);
    fflush(stdout);

    int ret = 0;
    TX_BEGIN(pool) {
        delete_from_obj_list(oid);
        TX_ADD_FIELD(*hdr, color);
        D_RW(*hdr)->color = BLACK;

        TOID(struct rbtree_map) map;
        TOID(struct persistent_byte_buffer) buf;
        TOID(struct persistent_byte_array) arr;
        switch(D_RO(*hdr)->type) {
            case RBTREE_MAP_TYPE_OFFSET:
                //printf("Deleting PSM at offset %lu\n", oid.off);
                fflush(stdout);
                TOID_ASSIGN(map, oid);
                rbtree_map_delete(pool, &map);
                break;
            case PERSISTENT_BYTE_BUFFER_TYPE_OFFSET:
                //printf("Deleting PBB at offset %lu\n", oid.off);
                fflush(stdout);
                TOID_ASSIGN(buf, oid);
                free_buffer(buf);
                break;
            case PERSISTENT_BYTE_ARRAY_TYPE_OFFSET:
                //printf("Deleting PBA at offset %lu\n", oid.off);
                fflush(stdout);
                TOID_ASSIGN(arr, oid);
                free_array(arr);
                break;
            default:
                printf("Received unknown type: %d\n", D_RO(*hdr)->type);
                fflush(stdout);
                exit(-1);
        }
    } TX_ONABORT {
        printf("Failed in delete_struct on offset %lu of type %d, exiting now\n", oid.off, D_RO(*hdr)->type);
        exit(-1);
        //ret = 1;
    } TX_END

    return ret;
}

void add_candidate(PMEMoid oid)
{
    TOID(struct header) *hdr = (TOID(struct header)*)(pmemobj_direct(oid));
    ref_color color = D_RO(*hdr)->color;

    if (color != PURPLE) {
        TX_BEGIN(pool) {
            TX_ADD_FIELD(*hdr, color);
            TX_ADD_FIELD(*hdr, is_candidate);

            D_RW(*hdr)->color = PURPLE;
            D_RW(*hdr)->is_candidate = 1;
        } TX_ONABORT {
            printf("Adding candidate on offset %lu failed!\n", oid.off);
            exit(-1);
        } TX_END
    }
}

void collect()
{
    TX_BEGIN(pool) {
        mark_candidates();
        scan_candidates();
        collect_candidates();
    } TX_ONABORT {
        printf("Collecting cyclic reference counts failed!\n");
        exit(-1);
    } TX_END
}

void mark_candidates()
{
    PMEMoid oid = {get_uuid_lo(), D_RO(root)->newest_obj_off};

    TX_BEGIN(pool) {
        while (!OID_IS_NULL(oid)) {
            TOID(struct header) *hdr = (TOID(struct header)*)(pmemobj_direct(oid));

            if (D_RO(*hdr)->is_candidate == 1) {
                if (D_RO(*hdr)->color == PURPLE) {
                    mark_gray(oid);
                } else {
                    TX_ADD_FIELD(*hdr, is_candidate);
                    D_RW(*hdr)->is_candidate = 0;

                    if (D_RO(*hdr)->color == BLACK && D_RO(*hdr)->refCount == 0) {
                        delete_struct(oid);
                    }
                }
            }
            oid.off = D_RO(*hdr)->prev_obj_offset;
        }
    } TX_ONABORT {
        printf("Marking candidates failed!\n");
        exit(-1);
    } TX_END
}

void scan_candidates()
{
    PMEMoid oid = {get_uuid_lo(), D_RO(root)->newest_obj_off};

    TX_BEGIN(pool) {
        while (!OID_IS_NULL(oid)) {
            TOID(struct header) *hdr = (TOID(struct header)*)(pmemobj_direct(oid));
            if (D_RO(*hdr)->is_candidate == 1) {
                scan(oid);
            }
            oid.off = D_RO(*hdr)->prev_obj_offset;
        }
    } TX_ONABORT {
        printf("Scanning candidates failed!\n");
        exit(-1);
    } TX_END
}

void collect_candidates()
{
    PMEMoid oid = {get_uuid_lo(), D_RO(root)->newest_obj_off};

    TX_BEGIN(pool) {
        while (!OID_IS_NULL(oid)) {
            TOID(struct header) *hdr = (TOID(struct header)*)(pmemobj_direct(oid));
            if (D_RO(*hdr)->is_candidate == 1) {
                TX_ADD_FIELD(*hdr, is_candidate);
                D_RW(*hdr)->is_candidate = 0;
                collect_white(oid);
            }
            oid.off = D_RO(*hdr)->prev_obj_offset;
        }
    } TX_ONABORT {
        printf("Collecting candidates failed!\n");
        exit(-1);
    } TX_END
}

void mark_gray(PMEMoid oid)
{
    TOID(struct header) *hdr = (TOID(struct header)*)(pmemobj_direct(oid));

    TX_BEGIN(pool) {
        if (D_RO(*hdr)->color != GRAY) {
            TX_ADD_FIELD(*hdr, color);
            D_RW(*hdr)->color = GRAY;

            PMEMoid child;
            for (int i = 0; i < D_RO(*hdr)->fieldCount; i++) {
                child = *(PMEMoid*)((long)pmemobj_direct(oid) + sizeof(TOID(struct header)) + i*sizeof(PMEMoid));

                TOID(struct header) *child_hdr = (TOID(struct header)*)(pmemobj_direct(child));
                TX_ADD_FIELD(*child_hdr, refCount);
                D_RW(*child_hdr)->refCount--;

                mark_gray(child);
            }
        }
    } TX_ONABORT {
        printf("Mark Gray failed!\n");
        exit(-1);
    } TX_END
}

void scan(PMEMoid oid)
{
    TOID(struct header) *hdr = (TOID(struct header)*)(pmemobj_direct(oid));

    TX_BEGIN(pool) {
        if (D_RO(*hdr)->color == GRAY) {
            if (D_RO(*hdr)->refCount > 0) {
                scan_black(oid);
            } else {
                TX_ADD_FIELD(*hdr, color);
                D_RW(*hdr)->color = WHITE;

                PMEMoid child;
                for (int i = 0; i < D_RO(*hdr)->fieldCount; i++) {
                    child = *(PMEMoid*)((long)pmemobj_direct(oid) + sizeof(TOID(struct header)) + i*sizeof(PMEMoid));
                    scan(child);
                }
            }
        }
    } TX_ONABORT {
        printf("Scanning failed!\n");
        exit(-1);
    } TX_END
}

void scan_black(PMEMoid oid)
{
    TOID(struct header) *hdr = (TOID(struct header)*)(pmemobj_direct(oid));

    TX_BEGIN(pool) {
        TX_ADD_FIELD(*hdr, color);
        D_RW(*hdr)->color = BLACK;

        PMEMoid child;
        for (int i = 0; i < D_RO(*hdr)->fieldCount; i++) {
            child = *(PMEMoid*)((long)pmemobj_direct(oid) + sizeof(TOID(struct header)) + i*sizeof(PMEMoid));

            TOID(struct header) *child_hdr = (TOID(struct header)*)(pmemobj_direct(child));
            TX_ADD_FIELD(*child_hdr, refCount);
            D_RW(*child_hdr)->refCount++;

            if (D_RO(*child_hdr)->color != BLACK) {
                scan_black(child);
            }
        }
    } TX_ONABORT {
        printf("Scan black failed!\n");
        exit(-1);
    } TX_END
}

void collect_white(PMEMoid oid)
{
    TOID(struct header) *hdr = (TOID(struct header)*)(pmemobj_direct(oid));

    TX_BEGIN(pool) {
        if (D_RO(*hdr)->color == WHITE) {
            TX_ADD_FIELD(*hdr, color);
            D_RW(*hdr)->color = BLACK;

            PMEMoid child;
            for (int i = 0; i < D_RO(*hdr)->fieldCount; i++) {
                child = *(PMEMoid*)((long)pmemobj_direct(oid) + sizeof(TOID(struct header)) + i*sizeof(PMEMoid));
                collect_white(child);
            }
            delete_struct(oid);
        }
    } TX_ONABORT {
        printf("Collect white failed!\n");
        exit(-1);
    } TX_END
}

int add_to_obj_list(PMEMoid oid)
{
    PMEMoid cur_list_head = {get_uuid_lo(), D_RO(root)->newest_obj_off};
    TOID(struct header) *this_hdr = (TOID(struct header)*)(pmemobj_direct(oid));

    int ret = 0;
    TX_BEGIN_LOCK(pool, TX_LOCK_MUTEX, &D_RW(root)->obj_list_mutex, TX_LOCK_NONE) {
        TX_ADD_FIELD(*this_hdr, prev_obj_offset);
        TX_ADD_FIELD(*this_hdr, next_obj_offset);
        TX_ADD_FIELD(root, newest_obj_off);

        if (!OID_IS_NULL(cur_list_head)) {
            TOID(struct header) *cur_list_head_hdr = (TOID(struct header)*)(pmemobj_direct(cur_list_head));
            TX_ADD_FIELD(*cur_list_head_hdr, next_obj_offset);
            D_RW(*cur_list_head_hdr)->next_obj_offset = oid.off;
        }
        D_RW(*this_hdr)->prev_obj_offset = D_RO(root)->newest_obj_off;
        D_RW(*this_hdr)->next_obj_offset = 0;
        D_RW(root)->newest_obj_off = oid.off;
    } TX_ONABORT {
        printf("Adding obj at offset %lu to obj list failed!\n", oid.off);
        exit(-1);
        //ret = 1;
    } TX_END

    return ret;
}

int delete_from_obj_list(PMEMoid oid)
{
    TOID(struct header) *this_hdr, prev_hdr, next_hdr;
    this_hdr = (TOID(struct header)*)(pmemobj_direct(oid));

    int ret = 0;
    TX_BEGIN_LOCK(pool, TX_LOCK_MUTEX, &D_RW(root)->obj_list_mutex, TX_LOCK_NONE) {
        PMEMoid prev = {get_uuid_lo(), D_RO(*this_hdr)->prev_obj_offset};
        PMEMoid next = {get_uuid_lo(), D_RO(*this_hdr)->next_obj_offset};

        if (!OID_IS_NULL(prev)) {
            prev_hdr = *(TOID(struct header)*)(pmemobj_direct(prev));
        } else {
            prev_hdr = TOID_NULL(struct header);
        }

        if (!OID_IS_NULL(next)) {
            next_hdr = *(TOID(struct header)*)(pmemobj_direct(next));
        } else {
            next_hdr = TOID_NULL(struct header);
        }

        if (!TOID_IS_NULL(prev_hdr)) {
            TX_ADD_FIELD(prev_hdr, next_obj_offset);
            D_RW(prev_hdr)->next_obj_offset = next.off;
            if (!TOID_IS_NULL(next_hdr)) {
                TX_ADD_FIELD(next_hdr, prev_obj_offset);
                D_RW(next_hdr)->prev_obj_offset = prev.off;
            }
        } else {
            if (!TOID_IS_NULL(next_hdr)) {
                TX_ADD_FIELD(next_hdr, prev_obj_offset);
                D_RW(next_hdr)->prev_obj_offset = prev.off;
            }
        }

        if (TOID_IS_NULL(next_hdr)) {
            TX_ADD_FIELD(root, newest_obj_off);
            D_RW(root)->newest_obj_off = prev.off;
        }
    } TX_ONABORT {
        printf("Deleting obj at offset %lu from obj list failed!\n", oid.off);
        exit(-1);
        //ret = 1;
    } TX_END

    return ret;
}
