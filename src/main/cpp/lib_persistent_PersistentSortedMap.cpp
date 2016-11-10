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

#include "lib_persistent_PersistentSortedMap.h"
#include "util.h"
#include "persistent_structs.h"
#include "persistent_heap.h"
#include "persistent_byte_buffer.h"
#include "persistent_sorted_map.h"

JNIEXPORT void JNICALL Java_lib_persistent_PersistentSortedMap_nativeOpenPool
  (JNIEnv *env, jclass klass)
{
    get_or_create_pool();
}

JNIEXPORT jlong JNICALL Java_lib_persistent_PersistentSortedMap_nativeCreateSortedMap
  (JNIEnv *env, jclass klass)
{
    TOID(struct rbtree_map) rbtree;
    if (rbtree_map_new(pool, &rbtree, NULL) != 0) {
        throw_persistent_object_exception(env, "NativeCreateSortedMap: Failed to create new backing tree structure; ");
    }
    return rbtree.oid.off;
}

JNIEXPORT jboolean JNICALL Java_lib_persistent_PersistentSortedMap_nativeCheckSortedMapExists
  (JNIEnv *env, jclass klass, jlong offset)
{
    PMEMoid map = {get_uuid_lo(), (uint64_t)offset};
    return check_map_existence(map) == 0 ? JNI_TRUE : JNI_FALSE;
}

// Returns 0 if map is valid, 1 otherwise
int check_map_existence(PMEMoid map)
{
    TOID(struct rbtree_map) iter;
    POBJ_FOREACH_TYPE(pool, iter) {
        if (map.off == iter.oid.off) {
            return 0;
        }
    }
    return 1;
}

JNIEXPORT jlong JNICALL Java_lib_persistent_PersistentSortedMap_nativePut
  (JNIEnv *env, jobject obj, jlong map_offset, jlong key_offset, jlong value_offset)
{
    PMEMoid map = {get_uuid_lo(), (uint64_t)map_offset};
    PMEMoid key_buf = {get_uuid_lo(), (uint64_t)key_offset};
    PMEMoid value_buf = {get_uuid_lo(), (uint64_t)value_offset};

    TOID(struct rbtree_map) rbtree;
    TOID_ASSIGN(rbtree, map);

    TOID(struct persistent_byte_buffer) key, value;
    TOID_ASSIGN(key, key_buf);
    TOID_ASSIGN(value, value_buf);

    if (TOID_IS_NULL(rbtree_map_get(pool, rbtree, key_buf))) {
        //printf("put: incRef of key at offset %d\n", key.oid.off);
        fflush(stdout);
        inc_ref(key.oid, 1);
    }
    //printf("put: incRef of value at offset %d\n", value.oid.off);
    fflush(stdout);
    if (!TOID_IS_NULL(value)) inc_ref(value.oid, 1);
    EPMEMoid item = rbtree_map_insert(pool, rbtree, key_buf, value_buf);
    if (EOID_IS_ERR(item)) {
        throw_persistent_object_exception(env, "NativePut: Failed to insert key/value pair; ");
    }
    return item.oid.off;
}

JNIEXPORT jlong JNICALL Java_lib_persistent_PersistentSortedMap_nativeGet
  (JNIEnv *env, jobject obj, jlong map_offset, jlong key_offset)
{
    PMEMoid map = {get_uuid_lo(), (uint64_t)map_offset};
    PMEMoid key_buf = {get_uuid_lo(), (uint64_t)key_offset};

    TOID(struct rbtree_map) rbtree;
    TOID_ASSIGN(rbtree, map);

    TOID(struct tree_map_node) node = rbtree_map_get(pool, rbtree, key_buf);
    if (TOID_IS_NULL(node)) {
        return 0;
    } else {
        return node.oid.off;
    }
}

JNIEXPORT jlong JNICALL Java_lib_persistent_PersistentSortedMap_nativeRemove
  (JNIEnv *env, jobject obj, jlong map_offset, jlong key_offset)
{
    PMEMoid map = {get_uuid_lo(), (uint64_t)map_offset};

    PMEMoid key_buf = {get_uuid_lo(), (uint64_t)key_offset};

    TOID(struct rbtree_map) rbtree;
    TOID_ASSIGN(rbtree, map);

    TOID(struct persistent_byte_buffer) key, map_key, value;
    TOID_ASSIGN(key, key_buf);

    EPMEMoid item = EOID_NULL;
    TX_BEGIN(pool) {
        TOID(struct tree_map_node) node = rbtree_map_get(pool, rbtree, key_buf);
        if (!(TOID_IS_NULL(node))) {
            TOID_ASSIGN(map_key, D_RO(node)->key);
            //printf("remove: supplying key at offset %d\n", key.oid.off);
            fflush(stdout);
            item = rbtree_map_remove(pool, rbtree, key_buf);
            if (!(EOID_IS_ERR(item))) {
                //printf("remove: decRef of key at offset %d\n", map_key.oid.off);
                fflush(stdout);
                dec_ref(map_key.oid, 1);
                TOID_ASSIGN(value, item.oid);
                if (!TOID_IS_NULL(value)) {
                    inc_ref(value.oid, 1);
                    dec_ref(value.oid, 1);
                }
            } else {
                throw_persistent_object_exception(env, "NativeRemove: Failed to remove key/value pair; ");
            }
        }
    } TX_END
    return item.oid.off;
}

JNIEXPORT jint JNICALL Java_lib_persistent_PersistentSortedMap_nativeSize
  (JNIEnv *env, jobject obj, jlong map_offset)
{
    PMEMoid map = {get_uuid_lo(), (uint64_t)map_offset};

    TOID(struct rbtree_map) rbtree;
    TOID_ASSIGN(rbtree, map);

    return rbtree_map_size(pool, rbtree);
}

JNIEXPORT jlong JNICALL Java_lib_persistent_PersistentSortedMap_nativeGetFirstNode
  (JNIEnv *env, jobject obj, jlong map_offset)
{
    PMEMoid map = {get_uuid_lo(), (uint64_t)map_offset};

    TOID(struct rbtree_map) rbtree;
    TOID_ASSIGN(rbtree, map);

    TOID(struct tree_map_node) node = rbtree_map_first_element(rbtree);

    if (TOID_IS_NULL(node)) {
        return 0;
    } else {
        return node.oid.off;
    }
}

JNIEXPORT jlong JNICALL Java_lib_persistent_PersistentSortedMap_nativeGetLastNode
  (JNIEnv *env, jobject obj, jlong map_offset)
{
    PMEMoid map = {get_uuid_lo(), (uint64_t)map_offset};

    TOID(struct rbtree_map) rbtree;
    TOID_ASSIGN(rbtree, map);

    TOID(struct tree_map_node) node = rbtree_map_last_element(rbtree);

    if (TOID_IS_NULL(node)) {
        return 0;
    } else {
        return node.oid.off;
    }
}

JNIEXPORT jlong JNICALL Java_lib_persistent_PersistentSortedMap_nativeGetSuccessor
  (JNIEnv *env, jobject obj, jlong map_offset, jlong key_offset)
{
    PMEMoid map = {get_uuid_lo(), (uint64_t)map_offset};

    TOID(struct rbtree_map) rbtree;
    TOID_ASSIGN(rbtree, map);

    jlong node_offset = Java_lib_persistent_PersistentSortedMap_nativeGet(env, obj, map_offset, key_offset);

    PMEMoid node_pmemoid = {get_uuid_lo(), (uint64_t)node_offset};

    TOID(struct tree_map_node) node;
    TOID_ASSIGN(node, node_pmemoid);

    TOID(struct tree_map_node) successor = rbtree_map_successor(rbtree, node);

    if (TOID_IS_NULL(successor)) {
        return 0;
    } else {
        return successor.oid.off;
    }
}

JNIEXPORT jlong JNICALL Java_lib_persistent_PersistentSortedMap_nativeGetPredecessor
  (JNIEnv *env, jobject obj, jlong map_offset, jlong key_offset)
{
    PMEMoid map = {get_uuid_lo(), (uint64_t)map_offset};

    TOID(struct rbtree_map) rbtree;
    TOID_ASSIGN(rbtree, map);

    jlong node_offset = Java_lib_persistent_PersistentSortedMap_nativeGet(env, obj, map_offset, key_offset);

    PMEMoid node_pmemoid = {get_uuid_lo(), (uint64_t)node_offset};

    TOID(struct tree_map_node) node;
    TOID_ASSIGN(node, node_pmemoid);

    TOID(struct tree_map_node) predecessor = rbtree_map_predecessor(rbtree, node);

    if (TOID_IS_NULL(predecessor)) {
        return 0;
    } else {
        return predecessor.oid.off;
    }
}

JNIEXPORT jlong JNICALL Java_lib_persistent_PersistentSortedMap_nativeGetNodeKey
  (JNIEnv *env, jobject obj, jlong map_offset, jlong node_offset)
{
    PMEMoid node_pmemoid = {get_uuid_lo(), (uint64_t)node_offset};

    TOID(struct tree_map_node) node;
    TOID_ASSIGN(node, node_pmemoid);

    PMEMoid key = D_RO(node)->key;

    inc_ref(key, 1);
    return key.off;
}

JNIEXPORT jlong JNICALL Java_lib_persistent_PersistentSortedMap_nativeGetNodeValue
  (JNIEnv *env, jobject obj, jlong map_offset, jlong node_offset)
{
    PMEMoid node_pmemoid = {get_uuid_lo(), (uint64_t)node_offset};

    TOID(struct tree_map_node) node;
    TOID_ASSIGN(node, node_pmemoid);

    PMEMoid value = D_RO(node)->value;
    if (!OID_IS_NULL(value)) inc_ref(value, 1);
    return value.off;
}

JNIEXPORT void JNICALL Java_lib_persistent_PersistentSortedMap_nativeClear
  (JNIEnv *env, jobject obj, jlong map_offset)
{
    PMEMoid map = {get_uuid_lo(), (uint64_t)map_offset};

    TOID(struct rbtree_map) rbtree;
    TOID_ASSIGN(rbtree, map);

    if (rbtree_map_clear(pool, rbtree) != 0) {
        throw_persistent_object_exception(env, "NativeClear: Failed to clear map properly; ");
    }
}

JNIEXPORT void JNICALL Java_lib_persistent_PersistentSortedMap_nativePutAll
  (JNIEnv *env, jobject obj, jlong map_offset, jlongArray keys, jlongArray values, jint size)
{
    jboolean is_copy_keys, is_copy_values;
    jlong* keys_array = (env)->GetLongArrayElements(keys, &is_copy_keys);
    jlong* values_array = (env)->GetLongArrayElements(values, &is_copy_values);

    int i;
    TX_BEGIN(pool) {
        for (i = 0; i < size; i++) {
            Java_lib_persistent_PersistentSortedMap_nativePut(env, obj, map_offset, keys_array[i], values_array[i]);
        }
    } TX_ONABORT {
        throw_persistent_object_exception(env, "NativePutAll: Failed to insert all key/valur pairs; ");
    } TX_FINALLY {
        if (is_copy_keys) (env)->ReleaseLongArrayElements(keys, keys_array, 0);
        if (is_copy_values) (env)->ReleaseLongArrayElements(values, values_array, 0);
    } TX_END
}

JNIEXPORT void JNICALL Java_lib_persistent_PersistentSortedMap_nativeRemoveAll
  (JNIEnv *env, jobject obj, jlong map_offset, jlongArray keys, jint size)
{
    jboolean is_copy_keys;
    jlong* keys_array = (env)->GetLongArrayElements(keys, &is_copy_keys);

    int i;
    TX_BEGIN(pool) {
        for (i = 0; i < size; i++) {
            Java_lib_persistent_PersistentSortedMap_nativeRemove(env, obj, map_offset, keys_array[i]);
        }
    } TX_ONABORT {
        throw_persistent_object_exception(env, "NativePutAll: Failed to remove all key/valur pairs; ");
    } TX_FINALLY {
        if (is_copy_keys) (env)->ReleaseLongArrayElements(keys, keys_array, 0);
    } TX_END
}
