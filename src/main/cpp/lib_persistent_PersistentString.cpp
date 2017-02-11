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

#include "lib_persistent_PersistentString.h"
#include "util.h"
#include "persistent_structs.h"
#include "persistent_byte_array.h"
#include "persistent_heap.h"

JNIEXPORT jlong JNICALL Java_lib_persistent_PersistentString_nativeReserveByteArrayMemory
  (JNIEnv *env, jobject obj, jint capacity)
{
    TOID(struct persistent_byte_array) arr;
    TOID(struct header) arr_header;
    TOID(char) bytes;

    char class_name[128];
    get_class_name(env, class_name, obj);

    TX_BEGIN (pool) {
        arr = TX_ZNEW(struct persistent_byte_array);
        arr_header = TX_ZNEW(struct header);

        D_RW(arr)->header = arr_header;
        if (capacity == 0) {
            bytes = TOID_NULL(char);
        } else {
            bytes = TX_ZALLOC(char, capacity);
        }

        D_RW(arr)->bytes = bytes.oid;
        D_RW(arr)->length = capacity;

        D_RW(D_RW(arr)->header)->refCount = 1;
        D_RW(D_RW(arr)->header)->type = TOID_TYPE_NUM(struct persistent_byte_array);
        D_RW(D_RW(arr)->header)->fieldCount = PERSISTENT_BYTE_ARRAY_FIELD_COUNT;
        D_RW(D_RW(arr)->header)->color = BLACK;

        TOID(char) arr_class_name = TX_ZALLOC(char, strlen(class_name));
        TX_MEMCPY(pmemobj_direct(arr_class_name.oid), class_name, strlen(class_name));
        D_RW(D_RW(arr)->header)->class_name = arr_class_name;

        add_to_obj_list(arr.oid);
    } TX_ONABORT {
        throw_persistent_object_exception(env, "ReserveByteArrayMemory: ");
    } TX_END

    return arr.oid.off;
}

JNIEXPORT jint JNICALL Java_lib_persistent_PersistentString_nativePut
  (JNIEnv *env, jobject obj, jlong offset, jbyteArray value, jint arrOffset, jint length)
{
    TOID(struct persistent_byte_array) arr;
    OFFSET_TO_TOID(arr, offset);

    put_common(env, obj, arr, value, arrOffset, 0, length, 1);
    return 0;
}

JNIEXPORT jint JNICALL Java_lib_persistent_PersistentString_nativeGet
  (JNIEnv *env, jobject obj, jlong offset, jbyteArray dest, jint arrOffset, jint length)
{
    TOID(struct persistent_byte_array) arr;
    OFFSET_TO_TOID(arr, offset);

    get_common(env, obj, arr, dest, arrOffset, 0, length, 1);
    return 0;
}

JNIEXPORT jint JNICALL Java_lib_persistent_PersistentString_nativeGetLength
  (JNIEnv *env, jobject object, jlong offset)
{
    TOID(struct persistent_byte_array) arr;
    OFFSET_TO_TOID(arr, offset);
    return (D_RO(arr)->length);
}

/*JNIEXPORT jboolean JNICALL Java_lib_persistent_PersistentString_nativeCheckByteArrayExists
  (JNIEnv *env, jclass klass, jlong offset)
{
    PMEMoid ba_oid = {get_uuid_lo(), (uint64_t)offset};
    return check_arr_existence(ba_oid) == 0 ? JNI_TRUE : JNI_FALSE;
}*/

