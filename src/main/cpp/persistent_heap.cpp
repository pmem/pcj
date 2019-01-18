/* Copyright (C) 2017  Intel Corporation
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
#include <pthread.h>

PMEMobjpool *pool = NULL;
TOID(struct root_struct) root;

static uint64_t uuid_lo;
static int pool_refs = 0;

PMEMobjpool* get_or_create_pool(const char* path, size_t size)
{
    pool_refs++;
    if (pool != NULL) {
        return pool;
    }

    pool = pmemobj_open(path, POBJ_LAYOUT_NAME(persistent_heap));
    if (pool == NULL) {
        pool = pmemobj_create(path, POBJ_LAYOUT_NAME(persistent_heap),
                              size, S_IRUSR | S_IWUSR);
    }

    if (pool == NULL) {
        printf("Encountered error opening pool. Please check if %s exists and accessible.\n", path);
        exit(-1);
    }

    TOID(char) arr;
    TX_BEGIN (pool) {
        arr = TX_ALLOC(char, 1);
        uuid_lo = arr.oid.pool_uuid_lo;
        TX_FREE(arr);
        TX_MEMSET(pmemobj_direct(arr.oid), 0, 1);
    } TX_ONABORT {
        printf("Encountered error opening pool.\n");
        fflush(stdout);
        exit(-1);
    } TX_END

    root = POBJ_ROOT(pool, struct root_struct);

    return pool;
}

void close_pool() {
    pool_refs--;
    if (pool_refs == 0) {
        pmemobj_close(pool);
        pool = NULL;
    }
}

void create_root(uint64_t root_size)
{
    TX_BEGIN(pool) {
        TX_ADD_FIELD(root, root_memory_region);
        D_RW(root)->root_memory_region = TX_ZALLOC(char, root_size);
    } TX_ONABORT {
        printf("Failed to create root object.\n");
        exit(-1);
    } TX_END
}

TOID(struct root_struct) get_root()
{
    return root;
}

int check_root_exists()
{
    return (TOID_IS_NULL(D_RO(root)->root_memory_region) ? 0 : 1);
}

uint64_t get_uuid_lo()
{
    return uuid_lo;
}
