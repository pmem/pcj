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

#include "lib_persistent_Util.h"
#include "persistent_structs.h"
#include "persistent_heap.h"
#include "util.h"
#include "persistent_tree_map.h"
#include "aggregate.h"

JavaVM *jvm = NULL;

JNIEXPORT void JNICALL Java_lib_persistent_Util_nativeOpenPool
  (JNIEnv *env, jclass klass)
{
    get_or_create_pool();
    jint rs = env->GetJavaVM(&jvm);
    if (rs != JNI_OK) {
        printf("Failed to get JVM from env!\n");
        exit(-1);
    }
}

JNIEXPORT void JNICALL Java_lib_persistent_Util_nativeClosePool
  (JNIEnv *env, jclass klass)
{
    close_pool();
}

JNIEXPORT jlong JNICALL Java_lib_persistent_Util_nativeGetRoot
  (JNIEnv *env, jclass klass)
{
    TX_BEGIN(pool) {
        create_root(root);
        inc_ref((D_RO(root)->root_map).oid, 1, 0);
    } TX_ONABORT {
        throw_persistent_object_exception(env, "Failed to create new root!");
    } TX_END
    return (D_RO(root)->root_map).oid.off;
}

JNIEXPORT void JNICALL Java_lib_persistent_Util_nativeDebugPool
  (JNIEnv *env, jclass klass, jint verbosity)
{
    print_all_objects(verbosity);
}

JNIEXPORT void JNICALL Java_lib_persistent_Util_nativeRegisterOffset
  (JNIEnv *env, jclass klass, jlong offset)
{
    //printf("registering offset %llu to vm_offsets\n", offset);
    //fflush(stdout);
    add_to_vm_offsets(offset);
}

JNIEXPORT void JNICALL Java_lib_persistent_Util_nativeDeregisterOffset
  (JNIEnv *env, jclass klass, jlong offset)
{
    //printf("deregistering offset %llu from vm_offsets\n", offset);
    //fflush(stdout);
    decrement_from_vm_offsets(offset);
}

JNIEXPORT void JNICALL Java_lib_persistent_Util_nativeIncRef
  (JNIEnv *env, jclass klass, jlong offset)
{
    PMEMoid oid = {get_uuid_lo(), (uint64_t)offset};
    TX_BEGIN(pool) {
        inc_ref(oid, 1, 0);
    } TX_ONABORT {
        throw_persistent_object_exception(env, "NativeIncRef: ");
    } TX_END
}

JNIEXPORT void JNICALL Java_lib_persistent_Util_nativeDecRef
  (JNIEnv *env, jclass klass, jlong offset)
{
    //printf("calling PBB nativeDecRef\n");
    //fflush(stdout);

    PMEMoid oid = {get_uuid_lo(), (uint64_t)offset};
    TX_BEGIN(pool) {
        dec_ref(oid, 1, 0);
    } TX_ONABORT {
        throw_persistent_object_exception(env, "NativeDecRef: ");
    } TX_END
}
