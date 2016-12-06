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

#include "lib_vmem_XMemory.h"
#include <libpmemobj.h>
#include <errno.h>
#include <unistd.h>
#include <fcntl.h>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <jni.h>

struct byte_buffer;

POBJ_LAYOUT_BEGIN(volatile_heap);
POBJ_LAYOUT_END(volatile_heap);

TOID_DECLARE(struct byte_buffer, 1000);
TOID_DECLARE(char, 1001);

struct byte_buffer {
    int capacity;
    TOID(char) bytes;
};

PMEMobjpool* pool = NULL;
uint64_t uuid_lo = 0;

JNIEXPORT jint JNICALL Java_lib_vmem_XMemory_initialize
  (JNIEnv *env, jclass klass, jstring path, jlong mem_size)
{
    const char* filepath = env->GetStringUTFChars(path, NULL);

    if (access(filepath, F_OK) == 0) {
        unlink(filepath);
    }

    pool = pmemobj_create(filepath, POBJ_LAYOUT_NAME(volatile_heap), mem_size, S_IRUSR | S_IWUSR);
    if (pool == NULL)
        return -1;

    TOID(char) arr;
    POBJ_ZALLOC(pool, &arr, char, 1);
    uuid_lo = arr.oid.pool_uuid_lo;
    POBJ_FREE(&arr);

    return 0;
}

JNIEXPORT jlong JNICALL Java_lib_vmem_XMemory_nativeReserveByteBufferMemory
  (JNIEnv *env, jobject obj, jint size)
{
    TOID(struct byte_buffer) buf;
    POBJ_ZNEW(pool, &buf, struct byte_buffer);
    if (size == 0)
        D_RW(buf)->bytes = TOID_NULL(char);
    else
        POBJ_ZALLOC(pool, &(D_RO(buf)->bytes), char, size);
    D_RW(buf)->capacity = size;
    return buf.oid.off;
}

JNIEXPORT jobject JNICALL Java_lib_vmem_XMemory_nativeCreateByteBuffer
  (JNIEnv *env, jobject obj, jlong offset, jint size)
{
    PMEMoid buf_oid = {uuid_lo, offset};
    TOID(struct byte_buffer) buf;
    TOID_ASSIGN(buf, buf_oid);

    return (env)->NewDirectByteBuffer(pmemobj_direct(D_RO(buf)->bytes.oid), size);
}

JNIEXPORT void JNICALL Java_lib_vmem_XMemory_nativeFree
  (JNIEnv *env, jclass klass, jlong offset)
{
    PMEMoid buf_oid = {uuid_lo, offset};
    TOID(struct byte_buffer) buf;
    TOID_ASSIGN(buf, buf_oid);

    POBJ_FREE(&(D_RO(buf)->bytes));
    POBJ_FREE(&buf);
}
