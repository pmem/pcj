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

#include "util.h"
#include "persistent_structs.h"
#include "persistent_heap.h"

void print_all_objects(int verbosity)
{
    TOID(struct rbtree_map) map;
    TOID(struct persistent_byte_buffer) buf;
    TOID(struct persistent_byte_array) arr;
    int map_count = 0, buf_count = 0, arr_count = 0;;

    /*POBJ_FOREACH_TYPE(pool, map) {
        if (verbosity > 0) {
            printf("Map found at offset %lu", map.oid.off);
            fflush(stdout);
            printf(", header at offset %lu, refCount %d, vm_offsets %lu\n", D_RO(map)->header.oid.off, D_RO(D_RO(map)->header)->refCount, (hm_tx_get(pool, vm_offsets, map.oid.off)).value);
            fflush(stdout);
        }
        map_count++;
    }

    POBJ_FOREACH_TYPE(pool, buf) {
        if (verbosity > 0) {
            printf("Buffer found at offset %lu", buf.oid.off);
            fflush(stdout);
            printf(", header at offset %lu, array offset %lu, refCount %d, vmOffsets %lu\n", D_RO(buf)->header.oid.off, (D_RO(buf)->arr).off, D_RO(D_RO(buf)->header)->refCount, (hm_tx_get(pool, vm_offsets, buf.oid.off)).value);
            fflush(stdout);
        }
        buf_count++;
    }

    POBJ_FOREACH_TYPE(pool, arr) {
        if (verbosity > 0) {
            printf("Array found at offset %lu", arr.oid.off);
            fflush(stdout);
            printf(", header at offset %lu, content %s, refCount %d\n", D_RO(arr)->header.oid.off, (char*)(pmemobj_direct(D_RO(arr)->bytes)), D_RO(D_RO(arr)->header)->refCount);
            fflush(stdout);
        }
        arr_count++;
    }*/


    PMEMoid oid = {get_uuid_lo(), D_RO(root)->newest_obj_off};
    while (!OID_IS_NULL(oid)) {
        TOID(struct header) *hdr = (TOID(struct header)*)(pmemobj_direct(oid));

        switch(D_RO(*hdr)->type) {
            case RBTREE_MAP_TYPE_OFFSET:
                if (verbosity > 0) {
                    TOID_ASSIGN(map, oid);
                    printf("Map found at offset %lu, header at offset %lu, refCount %d, vm_offsets %lu\n", map.oid.off, D_RO(map)->header.oid.off, D_RO(D_RO(map)->header)->refCount, (hm_tx_get(pool, vm_offsets, map.oid.off)).value);
                    fflush(stdout);
                }
                map_count++;
                break;
            case PERSISTENT_BYTE_BUFFER_TYPE_OFFSET:
                if (verbosity > 0) {
                    TOID_ASSIGN(buf, oid);
                    printf("Buffer found at offset %lu, header at offset %lu, array offset %lu, refCount %d, vmOffsets %lu\n", buf.oid.off, D_RO(buf)->header.oid.off, (D_RO(buf)->arr).off, D_RO(D_RO(buf)->header)->refCount, (hm_tx_get(pool, vm_offsets, buf.oid.off)).value);
                    fflush(stdout);
                }
                buf_count++;
                break;
            case PERSISTENT_BYTE_ARRAY_TYPE_OFFSET:
                if (verbosity > 0) {
                    TOID_ASSIGN(arr, oid);
                    printf("Array found at offset %lu, header at offset %lu, content %s, refCount %d\n", oid.off, D_RO(arr)->header.oid.off, (char*)(pmemobj_direct(D_RO(arr)->bytes)), D_RO(D_RO(arr)->header)->refCount);
                    fflush(stdout);
                }
                arr_count++;
                break;
            default:
                printf("Offset %lu is of type %d\n", oid.off, D_RO(*hdr)->type);
                fflush(stdout);
        }

        oid.off = D_RO(*hdr)->prev_obj_offset;
    }
    printf("==========================================================\n");
    printf("Total number of maps: %d, total number of buffers: %d, total number of arrays: %d\n", map_count, buf_count, arr_count);
    printf("==========================================================\n");
    fflush(stdout);
}

void call_JNI_method
  (JNIEnv *env, jobject obj, const char* method_name, const char* method_sig)
{
    jmethodID mid = find_JNI_method(env, obj, method_name, method_sig);
    if (mid != 0)
        (env)->CallVoidMethod(obj, mid);
}

void call_JNI_static_method_with_string
  (JNIEnv *env, jclass klass, const char* method_name, const char* method_sig, const char* arg)
{
    jmethodID mid = (env)->GetStaticMethodID(klass, method_name, method_sig);

    if (mid == 0) {
        //printf("Can't find method %s\n", method_name);
        //fflush(stdout);
        return;
    }

    (env)->CallStaticVoidMethod(klass, mid, arg);
}

void call_JNI_method_with_int
  (JNIEnv *env, jobject obj, const char* method_name, const char* method_sig, int arg)
{
    jmethodID mid = find_JNI_method(env, obj, method_name, method_sig);
    if (mid != 0)
        (env)->CallVoidMethod(obj, mid, arg);
}

void call_JNI_method_with_string
  (JNIEnv *env, jobject obj, const char* method_name, const char* method_sig, const char* arg)
{
    jmethodID mid = find_JNI_method(env, obj, method_name, method_sig);
    if (mid != 0) {
        jstring str = (env)->NewStringUTF(arg);
        (env)->CallVoidMethod(obj, mid, str);
    }
}

jmethodID find_JNI_method
  (JNIEnv *env, jobject obj, const char* method_name, const char* method_sig)
{
    jclass cls = (env)->GetObjectClass(obj);
    jmethodID mid = (env)->GetMethodID(cls, method_name, method_sig);

    if (mid == 0) {
        //printf("Can't find method %s\n", method_name);
        //fflush(stdout);
    }

    return mid;
}

void throw_persistent_object_exception(JNIEnv *env, const char* arg)
{
    char className[50] = "lib/persistent/PersistentObjectException";
    jclass exClass = env->FindClass(className);

    if (exClass == NULL) {
        printf("Can't find PersistentObjectException class!\n");
        fflush(stdout);
    }

    char errmsg[250];
    strcpy(errmsg, arg);
    strcat(errmsg, pmemobj_errormsg());
    env->ThrowNew(exClass, errmsg);
}

int create_locks_entry(PMEMobjpool *pop, void *ptr, void *arg)
{
    int ret = 0;

    struct locks_entry *e = (struct locks_entry *)ptr;
    TOID(struct hashmap_tx) locks;

    hm_tx_new(pop, &locks, *(int*)arg, 0, NULL);
    e->locks = locks;
    memset(&e->list, 0, sizeof(e->list));
    pmemobj_persist(pop, e, sizeof(*e));

    return ret;
}

void lock(PMEMoid oid)
{
    TOID(struct header) *hdr = (TOID(struct header)*)(pmemobj_direct(oid));
    PMEMmutex *obj_lock = &D_RW(*hdr)->obj_mutex;
    pmemobj_mutex_lock(pool, obj_lock);
}

int add_to_locks(PMEMoid oid, TOID(struct hashmap_tx) locks)
{
    if (NEOID_IS_NULL(hm_tx_get(pool, locks, oid.off))) {
        hm_tx_insert(pool, locks, oid.off, 1);
        return 0;
    }
    return 1;
}

int unlock_from_locks(uint64_t key, uint64_t value, void* arg)
{
    PMEMoid oid = {get_uuid_lo(), key};
    TOID(struct persistent_byte_buffer) buf;
    TOID(struct persistent_byte_array) arr;
    TOID_ASSIGN(buf, oid);
    if (TOID_VALID(buf)) {
        pmemobj_mutex_unlock(pool, &D_RW(D_RW(buf)->header)->obj_mutex);
    }
    TOID_ASSIGN(arr, oid);
    if (TOID_VALID(arr)) {
        pmemobj_mutex_unlock(pool, &D_RW(D_RW(arr)->header)->obj_mutex);
    }
    return 0;
}

void clear_locks(TOID(struct locks_entry) locks)
{
    TX_BEGIN(pool) {
        hm_tx_foreach(pool, D_RW(locks)->locks, unlock_from_locks, NULL);
        hm_tx_delete(pool, &D_RW(locks)->locks);
        POBJ_LIST_REMOVE_FREE(pool, &D_RW(root)->locks, locks, list);
    } TX_END
}
