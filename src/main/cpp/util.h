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

#include <jni.h>
#include <libpmemobj.h>
#include "persistent_structs.h"

void print_all_objects(int verbosity);
void call_JNI_method(JNIEnv *env, jobject obj, const char* method_name, const char* method_sig);
void call_JNI_method_with_int(JNIEnv *env, jobject obj, const char* method_name, const char* method_sig, int arg);
void call_JNI_method_with_string(JNIEnv *env, jobject obj, const char* method_name, const char* method_sig, const char* arg);
void call_JNI_static_method_with_string(JNIEnv *env, jclass klass, const char* method_name, const char* method_sig, const char* arg);
jmethodID find_JNI_method(JNIEnv *env, jobject obj, const char* method_name, const char* method_sig);
void throw_persistent_object_exception(JNIEnv *env, const char* arg);
int create_locks_entry(PMEMobjpool *pop, void *ptr, void *arg);
void lock(PMEMoid oid);
int add_to_locks(PMEMoid oid, TOID(struct hashmap_tx) locks);
int unlock_from_locks(uint64_t key, uint64_t value, void* arg);
void clear_locks(TOID(struct locks_entry) locks);
