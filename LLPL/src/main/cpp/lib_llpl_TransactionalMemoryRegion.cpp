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

#include "lib_llpl_TransactionalMemoryRegion.h"
#include "persistent_heap.h"

JNIEXPORT jint JNICALL Java_lib_llpl_TransactionalMemoryRegion_nativePutBits
  (JNIEnv *env, jobject obj, jlong region_offset, jlong offset, jlong value, jint size)
{
    PMEMoid oid = {get_uuid_lo(), region_offset};
    void* dest = (void*)((uint64_t)pmemobj_direct(oid)+(uint64_t)offset);
    void* src = &value;

    int ret = 0;
    TX_BEGIN(pool) {
        TX_MEMCPY(dest, src, size);
    } TX_ONABORT {
        ret = -1;
    } TX_END

    return ret;
}

JNIEXPORT jint JNICALL Java_lib_llpl_TransactionalMemoryRegion_nativeMemoryRegionMemcpyTransactional
  (JNIEnv *env, jobject obj, jlong src_region, jlong src_offset, jlong dest_region, jlong dest_offset, jlong length)
{
    PMEMoid src_oid = {get_uuid_lo(), (uint64_t)src_region};
    PMEMoid dest_oid = {get_uuid_lo(), (uint64_t)dest_region};

    void* src = (void*)((uint64_t)pmemobj_direct(src_oid)+(uint64_t)src_offset);
    void* dest = (void*)((uint64_t)pmemobj_direct(dest_oid)+(uint64_t)dest_offset);

    int ret = 0;
    TX_BEGIN(pool) {
        TX_MEMCPY(dest, src, (uint64_t)length);
    } TX_ONABORT {
        ret = -1;
    } TX_END

    return ret;
}

JNIEXPORT jint JNICALL Java_lib_llpl_TransactionalMemoryRegion_nativeFromByteArrayMemcpyTransactional
  (JNIEnv *env, jobject obj, jbyteArray src_array, jint src_offset, jlong dest_region, jlong dest_offset, jint length)
{
    PMEMoid dest_oid = {get_uuid_lo(), (uint64_t)dest_region};
    jbyte* dest = (jbyte*)((void*)((uint64_t)pmemobj_direct(dest_oid)+(uint64_t)dest_offset));

    jboolean is_copy;
    jbyte* bytes = env->GetByteArrayElements(src_array, &is_copy);

    int ret = 0;
    TX_BEGIN(pool) {
        TX_MEMCPY((void*)dest, (void*)(bytes+src_offset), length);
    } TX_ONABORT {
        ret = -1;
    } TX_FINALLY {
        if (is_copy) env->ReleaseByteArrayElements(src_array, bytes, 0);
    } TX_END

    return ret;
}
JNIEXPORT jint JNICALL Java_lib_llpl_TransactionalMemoryRegion_nativeMemoryRegionMemsetTransactional
  (JNIEnv *env, jobject obj, jlong region, jlong offset, jint val, jlong length)
{
    PMEMoid region_oid = {get_uuid_lo(), (uint64_t)region};
    void* dest = (void*)((uint64_t)pmemobj_direct(region_oid)+(uint64_t)offset);

    int ret = 0;
    TX_BEGIN(pool) {
        TX_MEMSET(dest, val, (size_t)length);
    } TX_ONABORT {
        ret = -1;
    } TX_END

    return ret;
}
