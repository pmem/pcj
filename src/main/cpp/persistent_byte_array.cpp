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

#include "persistent_byte_array.h"
#include "lib_persistent_PersistentString.h"
#include "util.h"
#include "persistent_structs.h"
#include "persistent_heap.h"

void put_common
  (JNIEnv *env, jobject obj, TOID(struct persistent_byte_array) arr, jbyteArray value, int arrOffset, int index, int length, int from_position)
{

    jboolean is_copy;
    jbyte* bytes = (env)->GetByteArrayElements(value, &is_copy);

    PMEMoid bytes_oid = D_RO(arr)->bytes;
    void* dest = pmemobj_direct(bytes_oid);
	
    if (!length > 0)
	return;
    TX_BEGIN(pool) {

        TX_MEMCPY((void*)(((char*)dest)), bytes+arrOffset, length);

    } TX_ONABORT {
        throw_persistent_object_exception(env, "NativePut: ");
    } TX_FINALLY {
        if (is_copy) {
            (env)->ReleaseByteArrayElements(value, bytes, 0);
        }
    } TX_END
}
void get_common
  (JNIEnv *env, jobject obj, TOID(struct persistent_byte_array) arr, jbyteArray dest, int arrOffset, int index, int length, int from_position)
{

    PMEMoid bytes_oid = D_RO(arr)->bytes;
    char* src = (char*)(pmemobj_direct(bytes_oid));
    jbyte* bytes = (jbyte*)(src);

    TX_BEGIN(pool) {

        (env)->SetByteArrayRegion(dest, arrOffset, length, bytes);
    } TX_ONABORT {
        throw_persistent_object_exception(env, "NativeGet: ");
    } TX_END
}

int compare_persistent_bytearrays(long this_offset, long that_offset)
{
    TOID(struct persistent_byte_array) this_arr, that_arr;
    PMEMoid this_ba_oid = {get_uuid_lo(), (uint64_t)this_offset};
    PMEMoid that_ba_oid = {get_uuid_lo(), (uint64_t)that_offset};
    TOID_ASSIGN(this_arr, this_ba_oid);
    TOID_ASSIGN(that_arr, that_ba_oid);
    PMEMoid this_bytes = D_RO(this_arr)->bytes, that_bytes = D_RO(that_arr)->bytes;

    int this_size = D_RO(this_arr)->length;
    int that_size = D_RO(that_arr)->length;

    int iterations = (this_size < that_size ? this_size : that_size);
    int i, j;
    for (i = j = 0; i < iterations; i++, j++) {
        char* this_byte = (char*)(pmemobj_direct(this_bytes)) + i;
        char* that_byte = (char*)(pmemobj_direct(that_bytes)) + j;
        int cmp = *this_byte - *that_byte;
        if (cmp != 0)
            return cmp;
    }

    return this_size - that_size;
}
