/* Copyright (C) 2017  Intel Corporation
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General public synchronized License
 * version 2 only, as published by the Free Software Foundation.
 * This file has been designated as subject to the "Classpath"
 * exception as provided in the LICENSE file that accompanied
 * this code.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General public synchronized License version 2 for more details (a copy
 * is included in the LICENSE file that accompanied this code).
 *
 * You should have received a copy of the GNU General public synchronized License
 * version 2 along with this program; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA  02110-1301, USA.
 */

#include "lib_persistent_PersistentMemoryRegion.h"
#include "persistent_heap.h"
#include "util.h"

#define TRANSACTIONAL 1
#define MANUAL 0

static void init_header(TOID(struct header) hdr, char* class_name)
{
    D_RW(hdr)->refCount = 1;
    D_RW(hdr)->type = TOID_TYPE_NUM(struct persistent_byte_array);
    D_RW(hdr)->fieldCount = PERSISTENT_BYTE_ARRAY_FIELD_COUNT;
    D_RW(hdr)->color = BLACK;

    TOID(char) region_class_name = TX_ZALLOC(char, strlen(class_name));
    TX_MEMCPY(pmemobj_direct(region_class_name.oid), class_name, strlen(class_name));
    D_RW(hdr)->class_name = region_class_name;
}

JNIEXPORT jlong JNICALL Java_lib_persistent_PersistentMemoryRegion_nativeGetMemoryRegion
  (JNIEnv *env, jobject obj, jlong size, jint consistency)
{
    TOID(struct persistent_byte_array) region;
    TOID(struct header) hdr;
    TOID(char) bytes;

    char class_name[128];
    get_class_name(env, class_name, obj);

    /*if (consistency == MANUAL) {
        POBJ_ZNEW(pool, &region, struct persistent_byte_array);
        POBJ_ZNEW(pool, &hdr, struct header);
        POBJ_ZALLOC(pool, &bytes, char, size);

        init_header(hdr, class_name);
        D_RW(region)->header = hdr;
        D_RW(region)->length = (uint64_t)size;
        D_RW(region)->consistency = consistency;
        D_RW(region)->dirty = 0;
        D_RW(region)->bytes = bytes.oid;

        pmemobj_persist(pool, D_RW(hdr), sizeof(*D_RW(hdr)));
        pmemobj_persist(pool, D_RW(bytes), sizeof(*D_RW(bytes)));
        pmemobj_persist(pool, D_RW(region), sizeof(*D_RW(region)));
    } else {*/
        TX_BEGIN(pool) {
            region = TX_ZNEW(struct persistent_byte_array);
            hdr = TX_ZNEW(struct header);
            bytes = TX_ZALLOC(char, size);

            init_header(hdr, class_name);
            D_RW(region)->header = hdr;
            D_RW(region)->length = (uint64_t)size;
            D_RW(region)->consistency = consistency;
            D_RW(region)->dirty = 0;
            D_RW(region)->bytes = bytes.oid;
        } TX_ONABORT {
            printf("Failed to allocate MemoryRegion of size %lu!\n", size);
            exit(-1);
        } TX_END
    //}

    return region.oid.off;
}

JNIEXPORT jint JNICALL Java_lib_persistent_PersistentMemoryRegion_nativeGetDirty
  (JNIEnv *env, jobject obj, jlong offset)
{
    PMEMoid oid = {get_uuid_lo(), offset};
    TOID(struct persistent_byte_array) region;
    TOID_ASSIGN(region, oid);
    return D_RO(region)->dirty;
}

JNIEXPORT jint JNICALL Java_lib_persistent_PersistentMemoryRegion_nativeGetConsistency
  (JNIEnv *env, jobject obj, jlong offset)
{
    PMEMoid oid = {get_uuid_lo(), offset};
    TOID(struct persistent_byte_array) region;
    TOID_ASSIGN(region, oid);
    return D_RO(region)->consistency;
}

JNIEXPORT void JNICALL Java_lib_persistent_PersistentMemoryRegion_nativeSetDirty
  (JNIEnv *env, jobject obj, jlong offset, jint dirty)
{
    PMEMoid oid = {get_uuid_lo(), offset};
    TOID(struct persistent_byte_array) region;
    TOID_ASSIGN(region, oid);
    TX_BEGIN(pool) {
        TX_ADD_FIELD(region, dirty);
        D_RW(region)->dirty = dirty;
    } TX_ONABORT {
        printf("Failed to set dirty bit to %d for region at offset %lu!\n", dirty, offset);
        exit(-1);
    } TX_END
}

JNIEXPORT void JNICALL Java_lib_persistent_PersistentMemoryRegion_nativeSetConsistency
  (JNIEnv *env, jobject obj, jlong offset, jint consistency)
{
    PMEMoid oid = {get_uuid_lo(), offset};
    TOID(struct persistent_byte_array) region;
    TOID_ASSIGN(region, oid);
    TX_BEGIN(pool) {
        TX_ADD_FIELD(region, consistency);
        D_RW(region)->consistency = consistency;
    } TX_ONABORT {
        printf("Failed to set consistency bit to %d for region at offset %lu!\n", consistency, offset);
        exit(-1);
    } TX_END
}

JNIEXPORT jlong JNICALL Java_lib_persistent_PersistentMemoryRegion_nativeGetLong
  (JNIEnv *env, jobject obj, jlong region_offset, jlong offset, jint size)
{
    PMEMoid oid = {get_uuid_lo(), region_offset};
    TOID(struct persistent_byte_array) region;
    TOID_ASSIGN(region, oid);

    jlong ret = 0;
    char* valChar;
    short* valShort;
    int* valInt;
    long* valLong;
    void* src = (void*)((uint64_t)pmemobj_direct(D_RO(region)->bytes)+(uint64_t)offset);
    switch(size) {
        case 1:
            valChar = (char*)(src);
            ret = *valChar;
            break;
        case 2:
            valShort = (short*)(src);
            ret = *valShort;
            break;
        case 4:
            valInt = (int*)(src);
            ret = *valInt;
            break;
        case 8:
            valLong = (long*)(src);
            ret = *valLong;
            break;
        default:
            printf("Asked to get a bad size: %d\n", size);
            exit(-1);
    }

    return ret;
}

JNIEXPORT void JNICALL Java_lib_persistent_PersistentMemoryRegion_nativePutLong
  (JNIEnv *env, jobject obj, jlong region_offset, jlong offset, jlong value, jint size)
{
    PMEMoid oid = {get_uuid_lo(), region_offset};
    TOID(struct persistent_byte_array) region;
    TOID_ASSIGN(region, oid);

    void* dest = (void*)((uint64_t)pmemobj_direct(D_RO(region)->bytes)+(uint64_t)offset);
    void* src = &value;
    if (D_RO(region)->consistency == TRANSACTIONAL) {
        TX_BEGIN(pool) {
            TX_MEMCPY(dest, src, size);
        } TX_ONABORT {
            printf("Error in putting a long at region_offset %lu, location %lu!\n", region_offset, offset);
            exit(-1);
        } TX_END
    } else {
        memcpy(dest, src, size);
    }
}

JNIEXPORT void JNICALL Java_lib_persistent_PersistentMemoryRegion_nativeFlush
  (JNIEnv *env, jobject obj, jlong region_offset, jlong offset, jlong size)
{
    PMEMoid oid = {get_uuid_lo(), region_offset};
    TOID(struct persistent_byte_array) region;
    TOID_ASSIGN(region, oid);

    pmemobj_persist(pool, (void*)((uint64_t)pmemobj_direct(D_RO(region)->bytes)+(uint64_t)offset), (size_t)size);
}
