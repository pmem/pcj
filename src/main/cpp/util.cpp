/* Copyright (C) 2016-17  Intel Corporation
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

#include "util.h"
#include "persistent_heap.h"

void call_JNI_method
  (JNIEnv *env, jobject obj, const char* method_name, const char* method_sig)
{
    jmethodID mid = find_JNI_method(env, obj, method_name, method_sig);
    if (mid != 0)
        (env)->CallVoidMethod(obj, mid);
}

void call_JNI_static_method_with_string
  (JNIEnv *env, jclass klass, const char* method_name, const char* method_sig, const char* arg)
{
    jmethodID mid = (env)->GetStaticMethodID(klass, method_name, method_sig);

    if (mid == 0) {
        //printf("Can't find method %s\n", method_name);
        //fflush(stdout);
        return;
    }

    (env)->CallStaticVoidMethod(klass, mid, arg);
}

void call_JNI_method_with_int
  (JNIEnv *env, jobject obj, const char* method_name, const char* method_sig, int arg)
{
    jmethodID mid = find_JNI_method(env, obj, method_name, method_sig);
    if (mid != 0)
        (env)->CallVoidMethod(obj, mid, arg);
}

void call_JNI_method_with_string
  (JNIEnv *env, jobject obj, const char* method_name, const char* method_sig, const char* arg)
{
    jmethodID mid = find_JNI_method(env, obj, method_name, method_sig);
    if (mid != 0) {
        jstring str = (env)->NewStringUTF(arg);
        (env)->CallVoidMethod(obj, mid, str);
    }
}

jmethodID find_JNI_method
  (JNIEnv *env, jobject obj, const char* method_name, const char* method_sig)
{
    jclass cls = (env)->GetObjectClass(obj);
    jmethodID mid = (env)->GetMethodID(cls, method_name, method_sig);

    if (mid == 0) {
        //printf("Can't find method %s\n", method_name);
        //fflush(stdout);
    }

    return mid;
}

void throw_persistence_exception(JNIEnv *env, const char* arg)
{
    char className[50] = "lib/util/persistent/PersistenceException";
    jclass exClass = env->FindClass(className);

    if (exClass == NULL) {
        printf("Can't find PersistenceException class!\n");
        fflush(stdout);
    }

    char errmsg[250];
    strcpy(errmsg, arg);
    strcat(errmsg, pmemobj_errormsg());
    env->ThrowNew(exClass, errmsg);
}
