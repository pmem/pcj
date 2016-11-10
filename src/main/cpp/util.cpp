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

    PMEMoid oid = {get_uuid_lo(), D_RO(root)->newest_obj_off};
    while (!OID_IS_NULL(oid)) {
        TOID(struct header) *hdr = (TOID(struct header)*)(pmemobj_direct(oid));

        switch(D_RO(*hdr)->type) {
            case RBTREE_MAP_TYPE_OFFSET:
                if (verbosity > 0) {
                    TOID_ASSIGN(map, oid);
                    printf("Map found at offset %lu, refCount %d, vm_offsets %lu\n", map.oid.off, D_RO(D_RO(map)->header)->refCount, (hm_tx_get(pool, vm_offsets, map.oid.off)).value);
                    fflush(stdout);
                }
                map_count++;
                break;
            case PERSISTENT_BYTE_BUFFER_TYPE_OFFSET:
                if (verbosity > 0) {
                    TOID_ASSIGN(buf, oid);
                    printf("Buffer found at offset %lu, array offset %lu, refCount %d, vmOffsets %lu\n", buf.oid.off, (D_RO(buf)->arr).off, D_RO(D_RO(buf)->header)->refCount, (hm_tx_get(pool, vm_offsets, buf.oid.off)).value);
                    fflush(stdout);
                }
                buf_count++;
                break;
            case PERSISTENT_BYTE_ARRAY_TYPE_OFFSET:
                if (verbosity > 0) {
                    TOID_ASSIGN(arr, oid);
                    printf("Array found at offset %lu, content %s, refCount %d\n", oid.off, (char*)(pmemobj_direct(D_RO(arr)->bytes)), D_RO(D_RO(arr)->header)->refCount);
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
        fflush(stdout);
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
        fflush(stdout);
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
