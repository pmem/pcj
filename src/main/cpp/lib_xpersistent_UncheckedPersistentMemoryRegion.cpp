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

#include "lib_xpersistent_UncheckedPersistentMemoryRegion.h"
#include "persistent_heap.h"
#include "util.h"


JNIEXPORT void JNICALL Java_lib_xpersistent_UncheckedPersistentMemoryRegion_nativePutLong
  (JNIEnv *env, jobject obj, jlong region_offset, jlong offset, jlong value, jint size)
{
    PMEMoid oid = {get_uuid_lo(), (uint64_t)region_offset};

    void* dest = (void*)((uint64_t)pmemobj_direct(oid)+(uint64_t)offset);
    void* src = &value;
    TX_BEGIN(pool) {
        TX_MEMCPY(dest, src, size);
    } TX_ONABORT {
        throw_persistence_exception(env, "Failed to put into MemoryRegion! ");
    } TX_END

    //printf("Putting long into region at %lu at offset %lu, address %p, value %lu\n", region_offset, offset, src, value);
    //fflush(stdout);
}


JNIEXPORT jlong JNICALL Java_lib_xpersistent_UncheckedPersistentMemoryRegion_getDirectAddress
  (JNIEnv *env, jobject obj, jlong region_offset)
{
    PMEMoid oid = {get_uuid_lo(), (uint64_t)region_offset};
    return (long)pmemobj_direct(oid);
}

