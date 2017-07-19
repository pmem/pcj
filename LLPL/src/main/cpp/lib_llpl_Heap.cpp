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

#include "lib_llpl_Heap.h"
#include "persistent_heap.h"

JNIEXPORT void JNICALL Java_lib_llpl_Heap_nativeOpenHeap
  (JNIEnv *env, jobject obj, jstring name)
{
    const char* native_string = env->GetStringUTFChars(name, 0);
    get_or_create_pool(native_string);
    env->ReleaseStringUTFChars(name, native_string);
}

JNIEXPORT jlong JNICALL Java_lib_llpl_Heap_nativeGetMemoryRegion
  (JNIEnv *env, jobject obj, jlong size)
{
    TOID(char) bytes = TOID_NULL(char);

    jlong ret = 0;
    TX_BEGIN(pool) {
        bytes = TX_ZALLOC(char, (size_t)size);
        ret = bytes.oid.off;
    } TX_END

    return ret;
}

JNIEXPORT jint JNICALL Java_lib_llpl_Heap_nativeSetRoot
  (JNIEnv *env, jobject obj, jlong val)
{
    int ret = 0;
    TX_BEGIN(pool) {
        D_RW(root)->root_val = (uint64_t)val;
    } TX_ONABORT {
        ret = -1;
    } TX_END

    return ret;
}

JNIEXPORT jlong JNICALL Java_lib_llpl_Heap_nativeGetRoot
  (JNIEnv *env, jobject obj)
{
    return D_RO(root)->root_val;
}

JNIEXPORT jint JNICALL Java_lib_llpl_Heap_nativeFree
  (JNIEnv *env, jobject obj, jlong region_offset)
{
    PMEMoid oid = {get_uuid_lo(), region_offset};
    TOID(char) bytes;
    TOID_ASSIGN(bytes, oid);

    int ret = 0;
    TX_BEGIN(pool) {
        TX_FREE(bytes);
    } TX_ONABORT {
        ret = -1;
    } TX_END

    return ret;
}

JNIEXPORT void JNICALL Java_lib_llpl_Heap_nativeFlush
  (JNIEnv *env, jobject obj, jlong addr, jlong size)
{
    pmemobj_persist(pool, (void*)((uint64_t)addr), (size_t)size);
}