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
    TOID(struct aggregate) aggr;
    TOID(struct persistent_long) lng;
    int map_count = 0, buf_count = 0, arr_count = 0, aggr_count = 0, lng_count=0;

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
                    printf("ByteArray found at offset %lu, header at offset %lu, content %s, refCount %d, vm_offsets %lu\n", oid.off, D_RO(arr)->header.oid.off, (char*)(pmemobj_direct(D_RO(arr)->bytes)), D_RO(D_RO(arr)->header)->refCount, (hm_tx_get(pool, vm_offsets, arr.oid.off)).value);
                    fflush(stdout);
                }
                arr_count++;
                break;
            case AGGREGATE_TYPE_OFFSET:
                if (verbosity > 0) {
                    TOID_ASSIGN(aggr, oid);
                    printf("Aggregate found at offset %lu, header at offset %lu, class %s, refCount %d, vm_offsets %lu\n", oid.off, D_RO(aggr)->header.oid.off, (char*)(pmemobj_direct(D_RO(D_RO(aggr)->header)->class_name.oid)), D_RO(D_RO(aggr)->header)->refCount, (hm_tx_get(pool, vm_offsets, aggr.oid.off)).value);
                    fflush(stdout);
                }
                aggr_count++;
                break;
            case PERSISTENT_LONG_TYPE_OFFSET:
                if (verbosity > 0) {
                    TOID_ASSIGN(lng, oid);
                    printf("Long found at offset %lu, header at offset %lu, content %lld, refCount %d, vm_offsets %lu\n", oid.off, D_RO(lng)->header.oid.off, D_RO(lng)->value, D_RO(D_RO(lng)->header)->refCount, (hm_tx_get(pool, vm_offsets, lng.oid.off)).value);
                    fflush(stdout);
                }
                lng_count++;
                break;
            default:
                printf("Offset %lu is of type %d\n", oid.off, D_RO(*hdr)->type);
                fflush(stdout);
        }

        oid.off = D_RO(*hdr)->prev_obj_offset;
    }
    printf("==========================================================\n");
    printf("Total number of maps: %d, total number of buffers: %d, total number of arrays: %d, total number of aggregates: %d\n", map_count, buf_count, arr_count, aggr_count);
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
    TOID(struct header) *hdr = (TOID(struct header)*)(pmemobj_direct(oid));
    pmemobj_mutex_unlock(pool, &D_RW(*hdr)->obj_mutex);
    return 0;
}

void clear_locks(TOID(struct locks_entry) locks)
{
    TX_BEGIN(pool) {
        hm_tx_foreach(pool, D_RW(locks)->locks, unlock_from_locks, NULL);
        hm_tx_delete(pool, &D_RW(locks)->locks);
        POBJ_LIST_REMOVE_FREE(pool, &D_RW(root)->locks, locks, list);
    } TX_ONABORT {
        printf("Failed to clear locks!\n");
        exit(-1);
    } TX_END
}

static jobject construct_object(JNIEnv *env, PMEMoid oid, char *className)
{
    jclass objClass = env->FindClass(className);
    if (objClass == NULL) {
        printf("create_object: can't find class %s!\n", className);
        exit(-1);
    }

    jobject obj = env->AllocObject(objClass);

    jfieldID offset_fid = env->GetFieldID(objClass, "offset", "J");
    env->SetLongField(obj, offset_fid, oid.off);

    jclass odClass = env->FindClass("lib/persistent/ObjectDirectory");
    if (odClass == NULL) {
        printf("create_object: can't find class ObjectDirectory!\n");
        exit(-1);
    }

    jmethodID mid = env->GetStaticMethodID(odClass, "registerObject", "(Llib/persistent/Persistent;)V");
    if (mid == 0) {
        printf("create_object: can't find registerObject function on ObjectDirectory!\n");
        exit(-1);
    }
    env->CallVoidMethod(odClass, mid, obj);

    return obj;
}

jobject create_object(JNIEnv *env, PMEMoid oid)
{
    if (OID_IS_NULL(oid)) return NULL;

    TOID(struct header) *hdr = (TOID(struct header)*)(pmemobj_direct(oid));
    //printf("create_object: type is %d on offset %lu, hdr is at %lu\n", D_RO(*hdr)->type, oid.off, (*hdr).oid.off);
    //fflush(stdout);

    char class_name[128];
    if (TOID_IS_NULL(D_RO(*hdr)->class_name)) {
        printf("create_object: cannot create object with type %d!\n", D_RO(*hdr)->type);
        exit(-1);
    } else {
        strcpy(class_name, (char*)(pmemobj_direct(D_RO(*hdr)->class_name.oid)));
    }
    return construct_object(env, oid, class_name);
}

int call_comparator(PMEMoid first_oid, PMEMoid second_oid)
{
    TOID(struct aggregate) first, second;
    TOID_ASSIGN(first, first_oid);
    TOID_ASSIGN(second, second_oid);

    JNIEnv *env;
    if ((jvm->AttachCurrentThread((void**)&env, NULL)) != JNI_OK) {
        printf("Failed to reattach env to JVM!\n");
        exit(-1);
    }

    char* first_class = (char*)(pmemobj_direct(D_RO(D_RO(first)->header)->class_name.oid));
    char* second_class = (char*)(pmemobj_direct(D_RO(D_RO(second)->header)->class_name.oid));

    jobject first_obj, second_obj;
    TX_BEGIN(pool) {
        inc_ref(first_oid, 1, 0);
        first_obj = construct_object(env, first_oid, first_class);
    } TX_ONABORT {
        printf("Failed in creating objects to compare!\n");
        exit(-1);
    } TX_END
    TX_BEGIN(pool) {
        inc_ref(second_oid, 1, 0);
        second_obj = construct_object(env, second_oid, second_class);
    } TX_ONABORT {
        printf("Failed in creating objects to compare!\n");
        exit(-1);
    } TX_END

    jclass cls = env->GetObjectClass(first_obj);
    jmethodID mid = env->GetMethodID(cls, "compareTo", "(Ljava/lang/Object;)I");
    if (mid == 0) {
        char signature[256];
        strcpy(signature, "(L");
        strcpy(signature, second_class);
        strcpy(signature, ";)I");
        mid = env->GetMethodID(cls, "compareTo", signature);
        if (mid == 0) {
            printf("Cannot find comparator on class %s!\n", first_class);
        }
    }

    return env->CallIntMethod(first_obj, mid, second_obj);
}

void get_class_name(JNIEnv *env, char* arr, jobject obj)
{
    jclass cls = env->GetObjectClass(obj);
    jmethodID mid = env->GetMethodID(cls, "getClass", "()Ljava/lang/Class;");
    jobject clsObj = env->CallObjectMethod(obj, mid);

    cls = env->GetObjectClass(clsObj);
    mid = env->GetMethodID(cls, "getName", "()Ljava/lang/String;");
    jstring strObj = (jstring)env->CallObjectMethod(clsObj, mid);

    int len = env->GetStringUTFLength(strObj);
    env->GetStringUTFRegion(strObj, 0, len, arr);
    fix_class_name(arr);
}

void fix_class_name(char *str)
{
    for (int i = 0; i < strlen(str); i++) {
        char c = str[i];
        if (c == '.') {
            str[i] = '/';
        }
    }
}

void free_header(PMEMoid oid)
{
    TOID(struct header) *hdr = (TOID(struct header)*)(pmemobj_direct(oid));

    TX_BEGIN(pool) {
        TX_ADD(*hdr);

        if (!TOID_IS_NULL(D_RO(*hdr)->class_name)) {
            int class_name_len = strlen((char*)(pmemobj_direct(D_RO(*hdr)->class_name.oid)));
            TX_MEMSET(pmemobj_direct(D_RO(*hdr)->class_name.oid), 0, class_name_len);
        }

        TX_MEMSET(pmemobj_direct((*hdr).oid), 0, sizeof(struct header));
        TX_FREE(*hdr);
    } TX_ONABORT {
        printf("Failed to free header for object at offset %lu!\n", oid.off);
        exit(-1);
    } TX_END

}
void lock_objs(TOID(struct hashmap_tx) locks, PMEMoid obj, int ref_count_change)
{
    TOID(struct header) *hdr = (TOID(struct header)*)(pmemobj_direct(obj));

    if (add_to_locks(obj, locks) == 0) {
        lock(obj);
    }

    if (D_RO(*hdr)->type != AGGREGATE_TYPE_OFFSET) {
        PMEMoid child;
        for (uint64_t i = 0; i < D_RO(*hdr)->fieldCount; i++) {
            child = *(PMEMoid*)((uint64_t)pmemobj_direct(obj) + sizeof(TOID(struct header)) + i*sizeof(PMEMoid));
            lock_objs(locks, child, (D_RO(*hdr)->refCount + ref_count_change) == 0 ? -1 : 0 );
        }
    } else {
        TOID(struct aggregate_field) child;
        for (uint64_t i = 0; i < D_RO(*hdr)->fieldCount; i++) {
            child = *(TOID(struct aggregate_field)*)((uint64_t)pmemobj_direct(obj) + sizeof(TOID(struct header)) + i*sizeof(TOID(struct aggregate_field)));
            lock_objs(locks, D_RO(child)->field, (D_RO(*hdr)->refCount + ref_count_change) == 0 ? -1 : 0 );
        }
    }

    if (!(OID_IS_NULL(obj)) && !(TOID_IS_NULL(*hdr))) {
        if (D_RO(*hdr)->refCount + ref_count_change == 0) {
            hm_tx_remove(pool, locks, obj.off);
        }
    }
}
