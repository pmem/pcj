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
  (JNIEnv *env, jobject obj, jstring path, jlong size)
{
    const char* native_path = env->GetStringUTFChars(path, 0);
    get_or_create_pool(native_path, (size_t)size);
    env->ReleaseStringUTFChars(path, native_path);
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

JNIEXPORT jlong JNICALL Java_lib_xpersistent_XHeap_nativeAllocate
  (JNIEnv *env, jobject obj, jlong size)
{
    TOID(char) bytes = TOID_NULL(char);
    bytes = TX_ZALLOC(char, size);
    if (pmemobj_tx_errno() == 0) return bytes.oid.off;
    else return -1;
}

JNIEXPORT jlong JNICALL Java_lib_xpersistent_XHeap_nativeAllocateAtomic
  (JNIEnv *env, jobject obj, jlong size)
{
    TOID(char) bytes = TOID_NULL(char);
    POBJ_ZALLOC(pool, &bytes, char, size);
    if (TOID_IS_NULL(bytes)) return -1;
    else return bytes.oid.off;
}

JNIEXPORT jlong JNICALL Java_lib_xpersistent_XHeap_nativeAllocateObject
  (JNIEnv *env, jobject obj, jlong size)
{
    TOID(object) bytes = TOID_NULL(object);
    POBJ_ZALLOC(pool, &bytes, object, size);
    if (TOID_IS_NULL(bytes)) return -1;
    else return bytes.oid.off;
}

JNIEXPORT jint JNICALL Java_lib_xpersistent_XHeap_nativeFree
  (JNIEnv *env, jobject obj, jlong region_offset)
{
    PMEMoid oid = {get_uuid_lo(), (uint64_t)region_offset};
    TOID(char) bytes;
    TOID_ASSIGN(bytes, oid);

    TX_FREE(bytes);
    return pmemobj_tx_errno() == 0 ? 0 : -1;
}

JNIEXPORT void JNICALL Java_lib_xpersistent_XHeap_nativeMemoryRegionMemcpy
  (JNIEnv *env, jobject obj, jlong src_region, jlong src_offset, jlong dest_region, jlong dest_offset, jlong length)
{
    PMEMoid src_oid = {get_uuid_lo(), (uint64_t)src_region};
    PMEMoid dest_oid = {get_uuid_lo(), (uint64_t)dest_region};

    void* src = (void*)((uint64_t)pmemobj_direct(src_oid)+(uint64_t)src_offset);
    void* dest = (void*)((uint64_t)pmemobj_direct(dest_oid)+(uint64_t)dest_offset);

    TX_BEGIN(pool) {
        TX_MEMCPY(dest, src, (uint64_t)length);
    } TX_ONABORT {
        throw_persistence_exception(env, "Failed to memcpy! ");
    } TX_END
}

JNIEXPORT void JNICALL Java_lib_xpersistent_XHeap_nativeToByteArrayMemcpy
  (JNIEnv *env, jobject obj, jlong src_region, jlong src_offset, jbyteArray dest_array, jint dest_offset, jint length)
{
    PMEMoid src_oid = {get_uuid_lo(), (uint64_t)src_region};
    jbyte* src = (jbyte*)((void*)((uint64_t)pmemobj_direct(src_oid)+(uint64_t)src_offset));
    env->SetByteArrayRegion(dest_array, dest_offset, length, src);
    // for (int i = 0; i < length; i++) {
    //     printf("reading byte %d at offset %lu\n", *(char*)(src+i), src_offset+i);
    //     fflush(stdout);
    // }
}

JNIEXPORT void JNICALL Java_lib_xpersistent_XHeap_nativeFromByteArrayMemcpy
  (JNIEnv *env, jobject obj, jbyteArray src_array, jint src_offset, jlong dest_region, jlong dest_offset, jint length)
{
    PMEMoid dest_oid = {get_uuid_lo(), (uint64_t)dest_region};
    jbyte* dest = (jbyte*)((void*)((uint64_t)pmemobj_direct(dest_oid)+(uint64_t)dest_offset));

    jboolean is_copy;
    jbyte* bytes = env->GetByteArrayElements(src_array, &is_copy);

    TX_BEGIN(pool) {
        TX_MEMCPY((void*)dest, (void*)(bytes+src_offset), length);
    } TX_ONABORT {
        throw_persistence_exception(env, "Failed to memcpy! ");
    } TX_FINALLY {
        if (is_copy) env->ReleaseByteArrayElements(src_array, bytes, 0);
    } TX_END
    // for (int i = 0; i < length; i++) {
    //     printf("wrote byte %d to offset %lu\n", *(char*)(dest+i), dest_offset+i);
    //     fflush(stdout);
    // }
}

JNIEXPORT void JNICALL Java_lib_xpersistent_XHeap_nativeCopyBytesToAddress
  (JNIEnv *env, jobject obj, jbyteArray src_array, jint src_offset, jlong dest_address, jint length)
{
    // fprintf(stdout, "nativeCopyBytesToAddress\n"); fflush(stdout);
    jboolean is_copy;
    jbyte* bytes = env->GetByteArrayElements(src_array, &is_copy);

    TX_MEMCPY((void*)dest_address, (void*)(bytes + src_offset), length);
    if (is_copy) env->ReleaseByteArrayElements(src_array, bytes, 0);
}


JNIEXPORT jlong JNICALL Java_lib_xpersistent_XHeap_nativeDebugPool
  (JNIEnv *env, jobject obj, jboolean verbose)
{
    TOID(object) mr_toid;
    uint64_t mr_count = 0;

    POBJ_FOREACH_TYPE(pool, mr_toid) {
        if (verbose == JNI_TRUE) {
            printf("MemoryRegion found at address %lu\n", mr_toid.oid.off);
            fflush(stdout);
        }
        mr_count++;
    }

    if (verbose == JNI_TRUE) {
        printf("Total number of MemoryRegions: %d\n", mr_count);
        fflush(stdout);
    }

    return mr_count;
}
