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

#include "lib_persistent_PersistentTreeMap.h"
#include "util.h"
#include "persistent_structs.h"
#include "persistent_heap.h"
#include "persistent_byte_buffer.h"
#include "persistent_tree_map.h"

JNIEXPORT jlong JNICALL Java_lib_persistent_PersistentTreeMap_nativeCreateTreeMap
  (JNIEnv *env, jobject obj)
{
    TOID(struct rbtree_map) rbtree;

    char class_name[128];
    get_class_name(env, class_name, obj);

    TX_BEGIN(pool) {
        if (rbtree_map_new(pool, &rbtree, class_name, NULL) != 0) {
            pmemobj_tx_abort(0);
        }
        //printf("new PSM created at offset %lu with header at %lu\n", rbtree.oid.off, D_RO(rbtree)->header.oid.off);
        //fflush(stdout);
        add_to_obj_list(rbtree.oid);
    } TX_ONABORT {
        throw_persistent_object_exception(env, "NativeCreateTreeMap: Failed to create new backing tree structure; ");
        rbtree = TOID_NULL(struct rbtree_map);
    } TX_END

    return rbtree.oid.off;
}

JNIEXPORT jboolean JNICALL Java_lib_persistent_PersistentTreeMap_nativeCheckTreeMapExists
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

JNIEXPORT jobject JNICALL Java_lib_persistent_PersistentTreeMap_nativePut
  (JNIEnv *env, jobject obj, jlong map_offset, jlong key_offset, jlong value_offset)
{
    PMEMoid map = {get_uuid_lo(), (uint64_t)map_offset};
    PMEMoid key_buf = {get_uuid_lo(), (uint64_t)key_offset};
    PMEMoid value_buf = {get_uuid_lo(), (uint64_t)value_offset};

    TOID(struct rbtree_map) rbtree;
    TOID_ASSIGN(rbtree, map);

    jobject jobj = NULL;
    TX_BEGIN(pool) {
        EPMEMoid item = put_common(rbtree, key_buf, value_buf, TOID_NULL(struct hashmap_tx), 0, 0);
        if (EOID_IS_ERR(item)) {
            pmemobj_tx_abort(0);
        }
        jobj = create_object(env, item.oid);
    } TX_ONABORT {
        throw_persistent_object_exception(env, "NativePut: Failed to insert key/value pair; ");
    } TX_END

    return jobj;
}

JNIEXPORT jlong JNICALL Java_lib_persistent_PersistentTreeMap_nativeGet
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

JNIEXPORT jobject JNICALL Java_lib_persistent_PersistentTreeMap_nativeRemove
  (JNIEnv *env, jobject obj, jlong map_offset, jlong key_offset)
{
    PMEMoid map = {get_uuid_lo(), (uint64_t)map_offset};
    PMEMoid key_buf = {get_uuid_lo(), (uint64_t)key_offset};

    TOID(struct rbtree_map) rbtree;
    TOID_ASSIGN(rbtree, map);

    TOID(struct tree_map_node) node = rbtree_map_get(pool, rbtree, key_buf);
    jobject jobj = NULL;
    TX_BEGIN(pool) {
        EPMEMoid item = remove_common(rbtree, key_buf, node, TOID_NULL(struct hashmap_tx), 0, 0);
        if (EOID_IS_ERR(item)) {
            pmemobj_tx_abort(0);
        }
        jobj = create_object(env, item.oid);
    } TX_ONABORT {
        throw_persistent_object_exception(env, "NativePut: Failed to insert key/value pair; ");
    } TX_END

    return jobj;
}

JNIEXPORT jint JNICALL Java_lib_persistent_PersistentTreeMap_nativeSize
  (JNIEnv *env, jobject obj, jlong map_offset)
{
    PMEMoid map = {get_uuid_lo(), (uint64_t)map_offset};

    TOID(struct rbtree_map) rbtree;
    TOID_ASSIGN(rbtree, map);

    return rbtree_map_size(pool, rbtree);
}

JNIEXPORT jlong JNICALL Java_lib_persistent_PersistentTreeMap_nativeGetFirstNode
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

JNIEXPORT jlong JNICALL Java_lib_persistent_PersistentTreeMap_nativeGetLastNode
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

JNIEXPORT jlong JNICALL Java_lib_persistent_PersistentTreeMap_nativeGetSuccessor
  (JNIEnv *env, jobject obj, jlong map_offset, jlong key_offset)
{
    PMEMoid map = {get_uuid_lo(), (uint64_t)map_offset};

    TOID(struct rbtree_map) rbtree;
    TOID_ASSIGN(rbtree, map);

    jlong node_offset = Java_lib_persistent_PersistentTreeMap_nativeGet(env, obj, map_offset, key_offset);

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

JNIEXPORT jlong JNICALL Java_lib_persistent_PersistentTreeMap_nativeGetPredecessor
  (JNIEnv *env, jobject obj, jlong map_offset, jlong key_offset)
{
    PMEMoid map = {get_uuid_lo(), (uint64_t)map_offset};

    TOID(struct rbtree_map) rbtree;
    TOID_ASSIGN(rbtree, map);

    jlong node_offset = Java_lib_persistent_PersistentTreeMap_nativeGet(env, obj, map_offset, key_offset);

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

JNIEXPORT jobject JNICALL Java_lib_persistent_PersistentTreeMap_nativeGetNodeKey
  (JNIEnv *env, jobject obj, jlong map_offset, jlong node_offset)
{
    PMEMoid node_pmemoid = {get_uuid_lo(), (uint64_t)node_offset};

    TOID(struct tree_map_node) node;
    TOID_ASSIGN(node, node_pmemoid);

    PMEMoid key = D_RO(node)->key;

    jobject jobj = NULL;
    TX_BEGIN(pool) {
        inc_ref(key, 1, 0);
        jobj = create_object(env, key);
    } TX_ONABORT {
        throw_persistent_object_exception(env, "NativeGetNodeKey");
    } TX_END

    return jobj;
}

JNIEXPORT jobject JNICALL Java_lib_persistent_PersistentTreeMap_nativeGetNodeValue
  (JNIEnv *env, jobject obj, jlong map_offset, jlong node_offset)
{
    PMEMoid node_pmemoid = {get_uuid_lo(), (uint64_t)node_offset};

    TOID(struct tree_map_node) node;
    TOID_ASSIGN(node, node_pmemoid);

    PMEMoid value = D_RO(node)->value;
    //printf("in getNodeValue, value's offset is %lu\n", value.off);

    TOID(struct persistent_byte_buffer) value_toid;
    TOID_ASSIGN(value_toid, value);

    jobject jobj = NULL;
    TX_BEGIN(pool) {
        if (!OID_IS_NULL(value)) {
            //printf("in getNodeValue, value's header's offset is %lu\n", D_RO(value_toid)->header.oid.off);
            //fflush(stdout);
            inc_ref(value, 1, 0);
        }
        jobj = create_object(env, value);
    } TX_ONABORT {
        throw_persistent_object_exception(env, "NativeGetNodeValue");
    } TX_END

    return jobj;
}

JNIEXPORT void JNICALL Java_lib_persistent_PersistentTreeMap_nativeClear
  (JNIEnv *env, jobject obj, jlong map_offset)
{
    PMEMoid map = {get_uuid_lo(), (uint64_t)map_offset};

    TOID(struct rbtree_map) rbtree;
    TOID_ASSIGN(rbtree, map);

    if (rbtree_map_clear(pool, rbtree) != 0) {
        throw_persistent_object_exception(env, "NativeClear: Failed to clear map properly; ");
    }
}

JNIEXPORT void JNICALL Java_lib_persistent_PersistentTreeMap_nativePutAll
  (JNIEnv *env, jobject obj, jlong map_offset, jlongArray keys, jlongArray values, jint size)
{
    jboolean is_copy_keys, is_copy_values;
    jlong* keys_array = (env)->GetLongArrayElements(keys, &is_copy_keys);
    jlong* values_array = (env)->GetLongArrayElements(values, &is_copy_values);

    PMEMoid map = {get_uuid_lo(), (uint64_t)map_offset};
    TOID(struct rbtree_map) rbtree;
    TOID_ASSIGN(rbtree, map);

    int i;
    TX_BEGIN(pool) {
        PMEMoid locks_entry_oid = POBJ_LIST_INSERT_NEW_HEAD(pool, &D_RW(root)->locks, list, sizeof(struct locks_entry), create_locks_entry, (void*)(&size));
        TOID(struct locks_entry) locks;
        TOID_ASSIGN(locks, locks_entry_oid);

        for (i = 0; i < size; i++) {
            PMEMoid key_buf = {get_uuid_lo(), (uint64_t)keys_array[i]};
            PMEMoid value_buf = {get_uuid_lo(), (uint64_t)values_array[i]};

            EPMEMoid item = put_common(rbtree, key_buf, value_buf, D_RO(locks)->locks, 1, 1);
            if (EOID_IS_ERR(item)) {
                throw_persistent_object_exception(env, "NativePutAll: Failed to insert key/value pair; ");
            }
        }

        clear_locks(locks);
    } TX_ONABORT {
        throw_persistent_object_exception(env, "NativePutAll: Failed to insert all key/valur pairs; ");
    } TX_FINALLY {
        if (is_copy_keys) (env)->ReleaseLongArrayElements(keys, keys_array, 0);
        if (is_copy_values) (env)->ReleaseLongArrayElements(values, values_array, 0);
    } TX_END
}

JNIEXPORT void JNICALL Java_lib_persistent_PersistentTreeMap_nativeRemoveAll
  (JNIEnv *env, jobject obj, jlong map_offset, jlongArray keys, jint size)
{
    jboolean is_copy_keys;
    jlong* keys_array = (env)->GetLongArrayElements(keys, &is_copy_keys);

    PMEMoid map = {get_uuid_lo(), (uint64_t)map_offset};
    TOID(struct rbtree_map) rbtree;
    TOID_ASSIGN(rbtree, map);

    int i;
    TX_BEGIN(pool) {
        PMEMoid locks_entry_oid = POBJ_LIST_INSERT_NEW_HEAD(pool, &D_RW(root)->locks, list, sizeof(struct locks_entry), create_locks_entry, (void*)(&size));
        TOID(struct locks_entry) locks;
        TOID_ASSIGN(locks, locks_entry_oid);

        for (i = 0; i < size; i++) {
            PMEMoid key_buf = {get_uuid_lo(), (uint64_t)keys_array[i]};
            TOID(struct tree_map_node) node = rbtree_map_get(pool, rbtree, key_buf);
            EPMEMoid item = remove_common(rbtree, key_buf, node, D_RO(locks)->locks, 1, 1);
            if (EOID_IS_ERR(item)) {
                throw_persistent_object_exception(env, "NativeRemoveAll: Failed to remove all key/valur pairs; ");
            }
        }

        clear_locks(locks);
    } TX_ONABORT {
        throw_persistent_object_exception(env, "NativeRemoveAll: Failed to remove all key/valur pairs; ");
    } TX_FINALLY {
        if (is_copy_keys) (env)->ReleaseLongArrayElements(keys, keys_array, 0);
    } TX_END
}

EPMEMoid put_common(TOID(struct rbtree_map) map, PMEMoid key, PMEMoid value,
                    TOID(struct hashmap_tx) locks, int lock_key, int lock_value)
{
    EPMEMoid item = EOID_NULL;
    TX_BEGIN(pool) {
        //printf("put: inserting key at %lu and value at %lu into map at offset %lu\n", key.off, value.off, map.oid.off);
        //fflush(stdout);
        if (TOID_IS_NULL(rbtree_map_get(pool, map, key))) {
            //printf("put: incRef of key at offset %d\n", key.off);
            //fflush(stdout);
            if (lock_key) {
                lock_objs(locks, key, 1);
            }
            inc_ref(key, 1, lock_key);
        }
        //printf("put: incRef of value at offset %d\n", value.off);
        //fflush(stdout);
        if (!OID_IS_NULL(value)) {
            if (lock_value) {
                lock_objs(locks, value, 1);
            }
            inc_ref(value, 1, lock_value);
        }
        item = rbtree_map_insert(pool, map, key, value);
    } TX_ONABORT {
        printf("NativePut failed!\n");
        exit(-1);
    } TX_END

    return item;
}

EPMEMoid remove_common(TOID(struct rbtree_map) map, PMEMoid key, TOID(struct tree_map_node) node,
                       TOID(struct hashmap_tx) locks, int lock_key, int lock_value)
{
    EPMEMoid item = EOID_NULL;
    TX_BEGIN(pool) {
        if (!(TOID_IS_NULL(node))) {
            //printf("remove: supplying key at offset %lu\n", key.off);
            //fflush(stdout);
            PMEMoid map_key = D_RO(node)->key;
            item = rbtree_map_remove(pool, map, key);
            if (!(EOID_IS_ERR(item))) {
                //printf("remove: decRef of key at offset %lu\n", map_key.off);
                //fflush(stdout);
                if (lock_key) {
                    lock_objs(locks, map_key, -1);
                }
                dec_ref(map_key, 1, lock_key);
                if (!OID_IS_NULL(item.oid)) {
                    if (lock_value) {
                        lock_objs(locks, item.oid, 0);
                    }
                    inc_ref(item.oid, 1, lock_value);
                    dec_ref(item.oid, 1, lock_value);
                }
            }
        }
    } TX_ONABORT {
        printf("NativeRemove failed!\n");
        exit(-1);
    } TX_END

    return item;
}
