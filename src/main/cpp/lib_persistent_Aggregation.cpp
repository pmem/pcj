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

#include "lib_persistent_Aggregation.h"
#include "util.h"
#include "persistent_structs.h"
#include "persistent_heap.h"
#include "aggregate.h"

JNIEXPORT jlong JNICALL Java_lib_persistent_Aggregation_nativeAggregate
  (JNIEnv *env, jclass klass, jstring class_name, jlong field_count)
{
    char str[128];
    int len = env->GetStringUTFLength(class_name);
    env->GetStringUTFRegion(class_name, 0, len, str);
    fix_class_name(str);

    TOID(struct aggregate) aggregate;
    TOID(struct header) header;
    TX_BEGIN(pool) {
        aggregate = TX_ZALLOC(struct aggregate, sizeof(struct aggregate) + (uint64_t)field_count * sizeof(TOID(struct aggregate_field)));
        header = TX_ZNEW(struct header);
        D_RW(aggregate)->header = header;

        D_RW(D_RW(aggregate)->header)->refCount = 1;
        D_RW(D_RW(aggregate)->header)->type = TOID_TYPE_NUM(struct aggregate);
        D_RW(D_RW(aggregate)->header)->fieldCount = (uint64_t)field_count;
        D_RW(D_RW(aggregate)->header)->color = BLACK;

        TOID(char) name = TX_ZALLOC(char, len);
        TX_MEMCPY(pmemobj_direct(name.oid), str, len);
        D_RW(D_RW(aggregate)->header)->class_name = name;

        //printf("now class name is %s\n", (char*)(pmemobj_direct(D_RO(D_RO(aggregate)->header)->class_name.oid)));
        //fflush(stdout);

        for (uint64_t i = 0; i < (uint64_t)field_count; i++) {
            TOID(struct aggregate_field) aggr_field = TOID_NULL(struct aggregate_field);
            aggr_field = TX_ZNEW(struct aggregate_field);
            D_RW(aggregate)->fields[i] = aggr_field;
        }

        add_to_obj_list(aggregate.oid);
    } TX_ONABORT {
        printf("Aggregating object failed!\n");
        exit(-1);
    } TX_END

    //printf("Just created aggregate of class %s at offset %lu\n", str, aggregate.oid.off);
    //fflush(stdout);
    return aggregate.oid.off;
}

JNIEXPORT void JNICALL Java_lib_persistent_Aggregation_nativeSetField
  (JNIEnv *env, jclass klass, jlong aggr_offset, jlong field_index, jlong field_offset)
{
    TOID(struct aggregate) aggr;
    PMEMoid aggr_oid = {get_uuid_lo(), aggr_offset};
    TOID_ASSIGN(aggr, aggr_oid);

    TOID(struct aggregate_field) field;

    PMEMoid field_oid = OID_NULL;
    if (field_offset != 0) {
        field_oid.pool_uuid_lo = get_uuid_lo();
        field_oid.off = field_offset;
    }

    TX_BEGIN(pool) {
        TX_ADD_FIELD(D_RO(aggr)->fields[field_index], field);
        field = D_RO(aggr)->fields[field_index];

        PMEMoid original_oid = D_RO(field)->field;
        D_RW(field)->field = field_oid;

        if (!OID_IS_NULL(field_oid)) inc_ref(field_oid, 1, 0);
        if (!OID_IS_NULL(original_oid)) dec_ref(original_oid, 1, 0);
    } TX_ONABORT {
        printf("Failed to set field #%lu for object at offset %lu!\n", field_index, aggr_offset);
        exit(-1);
    } TX_END
}

JNIEXPORT jobject JNICALL Java_lib_persistent_Aggregation_nativeGetField
  (JNIEnv *env, jclass klass, jlong aggr_offset, jlong field_index)
{
    TOID(struct aggregate) aggr;
    PMEMoid aggr_oid = {get_uuid_lo(), aggr_offset};
    TOID_ASSIGN(aggr, aggr_oid);

    jobject obj = NULL;
    TOID(struct aggregate_field) field = D_RO(aggr)->fields[field_index];
    TX_BEGIN(pool) {
        if (!OID_IS_NULL(D_RO(field)->field))
            inc_ref(D_RO(field)->field, 1, 0);
        obj = create_object(env, D_RO(field)->field);
    } TX_ONABORT {
        printf("Failed to get field %lu for object at offset %lu!\n", field_index, aggr_offset);
    } TX_END

    return obj;
}

JNIEXPORT jlong JNICALL Java_lib_persistent_Aggregation_nativeGetFieldCount
  (JNIEnv *env , jclass klass, jlong offset)
{
    TOID(struct aggregate) aggr;
    PMEMoid oid = {get_uuid_lo(), offset};
    TOID_ASSIGN(aggr, oid);

    return D_RO(D_RO(aggr)->header)->fieldCount;
}

void free_aggregate(PMEMoid oid, int already_locked)
{
    TOID(struct aggregate) aggregate;
    TOID_ASSIGN(aggregate, oid);

    TX_BEGIN(pool) {
        TX_ADD(aggregate);

        uint64_t field_count = D_RO(D_RO(aggregate)->header)->fieldCount;
        for (uint64_t i = 0; i < field_count; i++) {
            if (!OID_IS_NULL(D_RO(D_RO(aggregate)->fields[i])->field))
                dec_ref(D_RO(D_RO(aggregate)->fields[i])->field, 1, already_locked);
            TX_MEMSET(pmemobj_direct(D_RO(aggregate)->fields[i].oid), 0, sizeof(struct aggregate_field));
            TX_FREE(D_RW(aggregate)->fields[i]);
        }

        free_header(oid);

        TX_MEMSET(pmemobj_direct(oid), 0, sizeof(struct aggregate) + field_count * sizeof(TOID(struct aggregate_field)));
        TX_FREE(aggregate);
    } TX_ONABORT {
        printf("Freeing aggregate at offset %lu failed!\n", oid.off);
        exit(-1);
    } TX_END
}
