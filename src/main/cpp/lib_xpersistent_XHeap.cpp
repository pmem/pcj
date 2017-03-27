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

#include "lib_xpersistent_XHeap.h"
#include "persistent_heap.h"
#include "util.h"

JavaVM *jvm = NULL;

JNIEXPORT void JNICALL Java_lib_xpersistent_XHeap_nativeOpenHeap
  (JNIEnv *env, jobject obj)
{
    get_or_create_pool();
    jint rs = env->GetJavaVM(&jvm);
    if (rs != JNI_OK) {
        printf("Failed to get JVM from env!\n");
        exit(-1);
    }
}

JNIEXPORT void JNICALL Java_lib_xpersistent_XHeap_nativeCloseHeap
  (JNIEnv *env, jobject obj)
{
    close_pool();
}

JNIEXPORT jlong JNICALL Java_lib_xpersistent_XHeap_nativeGetMemoryRegion
  (JNIEnv *env, jobject obj, jlong size)
{
    TOID(char) bytes = TOID_NULL(char);

    TX_BEGIN(pool) {
        bytes = TX_ZALLOC(char, size);
    } TX_ONABORT {
        throw_persistence_exception(env, "Failed to allocate MemoryRegion! ");
    } TX_END

    return bytes.oid.off;
}

JNIEXPORT void JNICALL Java_lib_xpersistent_XHeap_nativeFree
  (JNIEnv *env, jobject obj, jlong region_offset)
{
    PMEMoid oid = {get_uuid_lo(), region_offset};
    TOID(char) bytes;
    TOID_ASSIGN(bytes, oid);

    TX_BEGIN(pool) {
        TX_FREE(bytes);
    } TX_ONABORT {
        throw_persistence_exception(env, "Failed to free! ");
    } TX_END
}

JNIEXPORT void JNICALL Java_lib_xpersistent_XHeap_nativeMemoryRegionMemcpy
  (JNIEnv *env, jobject obj, jlong src_region, jlong src_offset, jlong dest_region, jlong dest_offset, jlong length)
{
    PMEMoid src_oid = {get_uuid_lo(), src_region};
    PMEMoid dest_oid = {get_uuid_lo(), dest_region};

    void* src = (void*)((uint64_t)pmemobj_direct(src_oid)+(uint64_t)src_offset);
    void* dest = (void*)((uint64_t)pmemobj_direct(dest_oid)+(uint64_t)dest_offset);

    TX_BEGIN(pool) {
        TX_MEMCPY(dest, src, (uint64_t)length);
    } TX_ONABORT {
        throw_persistence_exception(env, "Failed to memcpy! ");
    } TX_END
}

JNIEXPORT void JNICALL Java_lib_xpersistent_XHeap_nativeDebugPool
  (JNIEnv *env, jobject obj)
{
    TOID(char) mr_toid;
    int mr_count = 0;

    POBJ_FOREACH_TYPE(pool, mr_toid) {
        printf("MemoryRegion found at address %lu\n", mr_toid.oid.off);
        fflush(stdout);
        mr_count++;
    }

    printf("Total number of MemoryRegions: %d\n", mr_count);
    fflush(stdout);
}
