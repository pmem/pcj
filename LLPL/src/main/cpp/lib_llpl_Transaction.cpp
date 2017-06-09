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

#include "lib_llpl_Transaction.h"
#include "persistent_heap.h"

JNIEXPORT void JNICALL Java_lib_llpl_Transaction_nativeStartTransaction
  (JNIEnv *env, jobject obj)
{
    int ret = pmemobj_tx_begin(pool, NULL, TX_PARAM_NONE);
    if (ret) {
        pmemobj_tx_end();
        printf("Error starting transaction!\n");
        exit(-1);
    }
}

JNIEXPORT void JNICALL Java_lib_llpl_Transaction_nativeEndTransaction
  (JNIEnv *env, jobject obj)
{
    pmemobj_tx_commit();
    pmemobj_tx_end();
}

JNIEXPORT void JNICALL Java_lib_llpl_Transaction_nativeAbortTransaction
  (JNIEnv *env, jobject obj)
{
    pmemobj_tx_abort(0);
    pmemobj_tx_end();
}
