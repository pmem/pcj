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

#include "lib_persistent_PersistentLong.h"
#include "util.h"
#include "persistent_structs.h"
#include "persistent_long.h"
#include "persistent_byte_array.h"
#include "persistent_heap.h"

JNIEXPORT jlong JNICALL Java_lib_persistent_PersistentLong_nativeSetLong
  (JNIEnv *env, jobject obj, jlong val)
{
    TOID(struct persistent_long) pl;
    TOID(struct header) pl_header;
    char class_name[128];
    get_class_name(env, class_name, obj);

    TX_BEGIN(pool) {
        pl = TX_ZNEW(struct persistent_long);
        pl_header = TX_ZNEW(struct header);

        D_RW(pl)->header = pl_header;
        D_RW(pl)->value = val;
        D_RW(D_RW(pl)->header)->refCount = 1;
        D_RW(D_RW(pl)->header)->type = TOID_TYPE_NUM(struct persistent_long);
        D_RW(D_RW(pl)->header)->fieldCount = PERSISTENT_LONG_FIELD_COUNT;
        D_RW(D_RW(pl)->header)->color = BLACK;

        TOID(char) pl_class_name = TX_ZALLOC(char, strlen(class_name));
        TX_MEMCPY(pmemobj_direct(pl_class_name.oid), class_name, strlen(class_name));
        D_RW(D_RW(pl)->header)->class_name = pl_class_name;
        add_to_obj_list(pl.oid);
    } TX_ONABORT {
        throw_persistent_object_exception(env, "NativeSetLong: ");
    } TX_END
    return pl.oid.off;
}

JNIEXPORT jlong JNICALL Java_lib_persistent_PersistentLong_nativeGetLong
  (JNIEnv *env, jobject object, jlong offset)
{
    TOID(struct persistent_long) pl;
    OFFSET_TO_TOID(pl, offset);
    return(D_RO(pl)->value);
}

int compare_persistent_longs(long this_offset, long that_offset)
{
    TOID(struct persistent_long) this_long, that_long;
    PMEMoid this_pl_oid = {get_uuid_lo(), (uint64_t)this_offset};
    PMEMoid that_pl_oid = {get_uuid_lo(), (uint64_t)that_offset};
    TOID_ASSIGN(this_long, this_pl_oid);
    TOID_ASSIGN(that_long, that_pl_oid);
    long int this_val = D_RO(this_long)->value, that_val = D_RO(that_long)->value;

    return this_val - that_val;
}

void free_long(TOID(struct persistent_long) lng)
{
    TX_BEGIN(pool) {
        TX_ADD(lng);
        TX_ADD_FIELD(lng, header);

        free_header(lng.oid);

        TX_MEMSET(pmemobj_direct(lng.oid), 0, sizeof(struct persistent_long));
        TX_FREE(lng);
    } TX_ONABORT {
        printf("Freeing long failed at offset %lu failed!\n", lng.oid.off);
        exit(-1);
    } TX_END
}
