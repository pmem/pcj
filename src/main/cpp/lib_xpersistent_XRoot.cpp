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

#include "lib_xpersistent_XRoot.h"
#include "persistent_heap.h"
#include "hashmap_tx.h"
#include "util.h"

#define HEADER_REFCOUNT_OFFSET 12

static JNIEnv *env_global = NULL;
static jclass class_global = NULL;
static jmethodID mid = 0;

JNIEXPORT jlong JNICALL Java_lib_xpersistent_XRoot_nativeGetRootOffset
  (JNIEnv *env, jclass klass)
{
    return D_RO(get_root())->root_memory_region.oid.off;
}

JNIEXPORT jboolean JNICALL Java_lib_xpersistent_XRoot_nativeRootExists
  (JNIEnv *env, jclass klass)
{
    return (check_root_exists() ? JNI_TRUE : JNI_FALSE);
}

JNIEXPORT jlong JNICALL Java_lib_xpersistent_XRoot_nativeCreateRoot
  (JNIEnv *env, jclass klass, jlong root_size)
{
    create_root(root_size);
    return D_RO(get_root())->root_memory_region.oid.off;
}

JNIEXPORT jlong JNICALL Java_lib_xpersistent_XRoot_nativeAllocateHashmap
  (JNIEnv *env, jclass klass)
{
    TOID(struct hashmap_tx) hm;
    if (hm_tx_new(pool, &hm, 10000, 0, NULL) != 0) {
        throw_persistence_exception(env, "Failed to create hashmap! ");
        return 0;
    }
    return hm.oid.off;
}

JNIEXPORT jlong JNICALL Java_lib_xpersistent_XRoot_nativeHashmapPut
  (JNIEnv *env, jobject obj, jlong map_offset, jlong key, jlong value)
{
    TOID(struct hashmap_tx) hm;
    PMEMoid oid = {get_uuid_lo(), map_offset};
    TOID_ASSIGN(hm, oid);

    NEPMEMoid ret = hm_tx_insert(pool, hm, (uint64_t)key, (uint64_t)value);
    if (NEOID_IS_ERR(ret)) {
        throw_persistence_exception(env, "Failed to insert into hashmap! ");
        return 0;
    }
    return ret.value;
}

JNIEXPORT jlong JNICALL Java_lib_xpersistent_XRoot_nativeHashmapGet
  (JNIEnv *env, jobject obj, jlong map_offset, jlong key)
{
    TOID(struct hashmap_tx) hm;
    PMEMoid oid = {get_uuid_lo(), map_offset};
    TOID_ASSIGN(hm, oid);

    NEPMEMoid ret = hm_tx_get(pool, hm, (uint64_t)key);
    if (NEOID_IS_ERR(ret)) {
        throw_persistence_exception(env, "Failed to get from hashmap! ");
        return 0;
    }
    return ret.value;
}

JNIEXPORT jlong JNICALL Java_lib_xpersistent_XRoot_nativeHashmapRemove
  (JNIEnv *env, jobject obj, jlong map_offset, jlong key)
{
    TOID(struct hashmap_tx) hm;
    PMEMoid oid = {get_uuid_lo(), map_offset};
    TOID_ASSIGN(hm, oid);

    NEPMEMoid ret = hm_tx_remove(pool, hm, (uint64_t)key);
    if (NEOID_IS_ERR(ret)) {
        throw_persistence_exception(env, "Failed to remove from hashmap! ");
        return 0;
    }
    return ret.value;
}

static int print_all_entries(uint64_t key, uint64_t value, void *arg)
{
    TOID(struct hashmap_tx) vm_offsets = *(TOID(struct hashmap_tx)*)arg;
    PMEMoid oid = {get_uuid_lo(), key};
    uint32_t ref_count = *(uint32_t*)((uint64_t)pmemobj_direct(oid) + (uint64_t)HEADER_REFCOUNT_OFFSET);
    printf("Object at %lu has a refCount of %d; its vmOffsets value is %d\n", key, ref_count, hm_tx_get(pool, vm_offsets, key).value);
    fflush(stdout);
    return 0;
}

JNIEXPORT void JNICALL Java_lib_xpersistent_XRoot_nativeHashmapClear
  (JNIEnv *env, jobject obj, jlong map_offset)
{
    TOID(struct hashmap_tx) hm;
    PMEMoid oid = {get_uuid_lo(), map_offset};
    TOID_ASSIGN(hm, oid);

    hm_tx_clear(pool, hm);
}

JNIEXPORT void JNICALL Java_lib_xpersistent_XRoot_nativePrintAllObjects
  (JNIEnv *env, jobject obj, jlong all_objects_offset, jlong vm_offsets_offset)
{
    TOID(struct hashmap_tx) vm_offsets, all_objects;
    PMEMoid vm_oid = {get_uuid_lo(), vm_offsets_offset};
    PMEMoid obj_oid = {get_uuid_lo(), all_objects_offset};
    TOID_ASSIGN(vm_offsets, vm_oid);
    TOID_ASSIGN(all_objects, obj_oid);

    hm_tx_foreach(pool, all_objects, print_all_entries, (void*)&vm_offsets);
}

static int decrement_from_vm_offsets(uint64_t addr, uint64_t count, void* arg)
{
    TOID(struct hashmap_tx) all_objects = *(TOID(struct hashmap_tx)*)arg;
    env_global->CallVoidMethod(class_global, mid, (jlong)addr, (jint) count);
    return 0;
}

JNIEXPORT void JNICALL Java_lib_xpersistent_XRoot_nativeCleanVMOffsets
  (JNIEnv *env, jobject obj, jlong vm_offsets_offset, jlong all_objects_offset)
{
    TOID(struct hashmap_tx) vm_offsets, all_objects;
    PMEMoid vm_oid = {get_uuid_lo(), vm_offsets_offset};
    PMEMoid obj_oid = {get_uuid_lo(), all_objects_offset};
    TOID_ASSIGN(vm_offsets, vm_oid);
    TOID_ASSIGN(all_objects, obj_oid);

    class_global = env->FindClass("lib/util/persistent/PersistentObject");
    if (class_global == NULL) {
        throw_persistence_exception(env, "Cannot find PersistentObject class! ");
        return;
    }

    mid = env->GetStaticMethodID(class_global, "deleteResidualReferences", "(JI)V");
    if (mid == 0) {
        throw_persistence_exception(env, "Cannot find deleteResidualReferences method! ");
        return;
    }

    env_global = env;
    hm_tx_foreach(pool, vm_offsets, decrement_from_vm_offsets, (void*)&all_objects);
    hm_tx_delete(pool, &vm_offsets);
}

static int import_candidates(uint64_t key, uint64_t value, void* arg)
{
    jobject root = *(jobject*)(arg);
    env_global->CallVoidMethod(root, mid, (jlong)key);
    return 0;
}

JNIEXPORT void JNICALL Java_lib_xpersistent_XRoot_nativeImportCandidates
  (JNIEnv *env, jobject obj, jlong candidates_offset, jobject root)
{
    TOID(struct hashmap_tx) candidates_toid;
    PMEMoid candidates_oid = {get_uuid_lo(), candidates_offset};
    TOID_ASSIGN(candidates_toid, candidates_oid);

    class_global = env->FindClass("lib/xpersistent/XRoot");
    if (class_global == NULL) {
        throw_persistence_exception(env, "Cannot find XRoot class! ");
        return;
    }

    mid = env->GetMethodID(class_global, "addToCandidatesFromNative", "(J)V");
    if (mid == 0) {
        throw_persistence_exception(env, "Cannot find addToCandidatesFromnative method on XRoot! ");
        return;
    }

    env_global = env;
    hm_tx_foreach(pool, candidates_toid, import_candidates, (void*)&root);
    hm_tx_clear(pool, candidates_toid);
}
