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

#include "lib_persistent_PersistentByteBuffer.h"
#include "util.h"
#include "persistent_structs.h"
#include "persistent_byte_buffer.h"
#include "persistent_heap.h"

JNIEXPORT jlong JNICALL Java_lib_persistent_PersistentByteBuffer_nativeReserveByteBufferMemory
  (JNIEnv *env, jobject obj, jint capacity)
{
    TOID(struct persistent_byte_buffer) pbb;
    TOID(struct persistent_byte_array) arr;
    TOID(struct header) pbb_header, arr_header;
    TOID(char) bytes;

    char class_name[128];
    get_class_name(env, class_name, obj);

    TX_BEGIN (pool) {
        pbb = TX_ZNEW(struct persistent_byte_buffer);
        pbb_header = TX_ZNEW(struct header);
        arr = TX_ZNEW(struct persistent_byte_array);
        arr_header = TX_ZNEW(struct header);

        D_RW(pbb)->header = pbb_header;
        D_RW(arr)->header = arr_header;
        if (capacity == 0) {
            bytes = TOID_NULL(char);
        } else {
            bytes = TX_ZALLOC(char, capacity);
        }

        D_RW(arr)->bytes = bytes.oid;
        D_RW(arr)->length = capacity;

        D_RW(pbb)->capacity = capacity;
        D_RW(pbb)->arr = arr.oid;

        D_RW(pbb)->position = 0;
        D_RW(pbb)->limit = capacity;
        D_RW(pbb)->mark = -1;
        D_RW(pbb)->start = 0;

        D_RW(D_RW(pbb)->header)->refCount = 1;
        D_RW(D_RW(pbb)->header)->type = TOID_TYPE_NUM(struct persistent_byte_buffer);
        D_RW(D_RW(pbb)->header)->fieldCount = PERSISTENT_BYTE_BUFFER_FIELD_COUNT;
        D_RW(D_RW(pbb)->header)->color = BLACK;

        TOID(char) pbb_class_name = TX_ZALLOC(char, strlen(class_name));
        TX_MEMCPY(pmemobj_direct(pbb_class_name.oid), class_name, strlen(class_name));
        D_RW(D_RW(pbb)->header)->class_name = pbb_class_name;

        D_RW(D_RW(arr)->header)->refCount = 1;
        D_RW(D_RW(arr)->header)->type = TOID_TYPE_NUM(struct persistent_byte_array);
        D_RW(D_RW(arr)->header)->fieldCount = PERSISTENT_BYTE_ARRAY_FIELD_COUNT;
        D_RW(D_RW(arr)->header)->color = BLACK;
        D_RW(D_RW(arr)->header)->class_name = TOID_NULL(char);

        add_to_obj_list(pbb.oid);
        add_to_obj_list(arr.oid);
    } TX_ONABORT {
        throw_persistent_object_exception(env, "ReserveByteBufferMemory: ");
    } TX_END

    //printf("created pbb at offset %lu, its header is at offset %lu\n", pbb.oid.off, D_RO(pbb)->header.oid.off);
    //printf("created pba at offset %lu, its header is at offset %lu\n", arr.oid.off, D_RO(arr)->header.oid.off);
    //fflush(stdout);
    return pbb.oid.off;
}

JNIEXPORT jlong JNICALL Java_lib_persistent_PersistentByteBuffer_nativeGetByteBufferAddress
  (JNIEnv *env, jobject obj, jobject buf)
{
    return (jlong)(env)->GetDirectBufferAddress(buf);
}

JNIEXPORT jobject JNICALL Java_lib_persistent_PersistentByteBuffer_nativeCreateByteBuffer
  (JNIEnv *env, jobject obj, jlong offset)
{
    TOID(struct persistent_byte_buffer) pbb;
    OFFSET_TO_TOID(pbb, offset);

    TOID(struct persistent_byte_array) arr;
    TOID_ASSIGN(arr, D_RO(pbb)->arr);
    PMEMoid bytes = D_RO(arr)->bytes;

    return (env)->NewDirectByteBuffer(pmemobj_direct(bytes), D_RO(pbb)->capacity);
}

JNIEXPORT jint JNICALL Java_lib_persistent_PersistentByteBuffer_nativePut
  (JNIEnv *env, jobject obj, jlong offset, jbyteArray value, jint arrOffset, jint length)
{
    TOID(struct persistent_byte_buffer) pbb;
    OFFSET_TO_TOID(pbb, offset);

    put_common(env, obj, pbb, value, arrOffset, 0, length, 1);
    return 0;
}

JNIEXPORT jint JNICALL Java_lib_persistent_PersistentByteBuffer_nativePutAbsolute
  (JNIEnv *env, jobject obj, jlong offset, jbyteArray value, jint index, int length)
{
    TOID(struct persistent_byte_buffer) pbb;
    OFFSET_TO_TOID(pbb, offset);

    put_common(env, obj, pbb, value, 0, index, length, 0);
    return 0;
}

void put_common
  (JNIEnv *env, jobject obj, TOID(struct persistent_byte_buffer) pbb, jbyteArray value, int arrOffset, int index, int length, int from_position)
{
    int position = D_RO(pbb)->position;
    int start = D_RO(pbb)->start;
    int start_position = (from_position == 1) ? position : index;

    jboolean is_copy;
    jbyte* bytes = (env)->GetByteArrayElements(value, &is_copy);

    TOID(struct persistent_byte_array) arr;
    TOID_ASSIGN(arr, D_RO(pbb)->arr);
    PMEMoid bytes_oid = D_RO(arr)->bytes;
    void* dest = pmemobj_direct(bytes_oid);

    TX_BEGIN(pool) {
        if (from_position == 1) {
            TX_ADD_FIELD(pbb, position);
            D_RW(pbb)->position = position + length;
        }

        TX_MEMCPY((void*)(((char*)dest)+start+start_position), bytes+arrOffset, length);
    } TX_ONABORT {
        throw_persistent_object_exception(env, "NativePut: ");
    } TX_FINALLY {
        if (is_copy) {
            (env)->ReleaseByteArrayElements(value, bytes, 0);
        }
    } TX_END
}

JNIEXPORT jint JNICALL Java_lib_persistent_PersistentByteBuffer_nativePutByteBuffer
  (JNIEnv *env, jobject obj, jlong this_offset, jlong that_offset)
{
    TOID(struct persistent_byte_buffer) this_pbb, that_pbb;

    PMEMoid this_bb_oid = {get_uuid_lo(), (uint64_t)this_offset};
    PMEMoid that_bb_oid = {get_uuid_lo(), (uint64_t)that_offset};
    TOID_ASSIGN(this_pbb, this_bb_oid);
    TOID_ASSIGN(that_pbb, that_bb_oid);

    int this_start = D_RO(this_pbb)->start;
    int that_start = D_RO(that_pbb)->start;

    int that_remaining = calculate_remaining(that_pbb);

    TOID(struct persistent_byte_array) this_arr, that_arr;
    TOID_ASSIGN(this_arr, D_RO(this_pbb)->arr);
    TOID_ASSIGN(that_arr, D_RO(that_pbb)->arr);
    PMEMoid this_bytes = D_RO(this_arr)->bytes, that_bytes = D_RO(that_arr)->bytes;

    void* dest = (void*)((long)pmemobj_direct(this_bytes) + this_start + D_RO(this_pbb)->position);
    void* src  = (void*)((long)pmemobj_direct(that_bytes) + that_start + D_RO(that_pbb)->position);

    TX_BEGIN(pool) {
        TX_ADD_FIELD(this_pbb, position);
        TX_ADD_FIELD(that_pbb, position);

        TX_MEMCPY((void*)((char*)dest), (void*)((char*)src), that_remaining);
        D_RW(this_pbb)->position += that_remaining;
        D_RW(that_pbb)->position += that_remaining;
    } TX_ONABORT {
        throw_persistent_object_exception(env, "NativePutByteBuffer: ");
    } TX_END

    return 0;
}

JNIEXPORT jint JNICALL Java_lib_persistent_PersistentByteBuffer_nativeGet
  (JNIEnv *env, jobject obj, jlong offset, jbyteArray dest, jint arrOffset, jint length)
{
    TOID(struct persistent_byte_buffer) pbb;
    OFFSET_TO_TOID(pbb, offset);

    get_common(env, obj, pbb, dest, arrOffset, 0, length, 1);
    return 0;
}

JNIEXPORT jint JNICALL Java_lib_persistent_PersistentByteBuffer_nativeGetAbsolute
  (JNIEnv *env, jobject obj, jlong offset, jbyteArray dest, jint index, int length)
{
    TOID(struct persistent_byte_buffer) pbb;
    OFFSET_TO_TOID(pbb, offset);

    get_common(env, obj, pbb, dest, 0, index, length, 0);
    return 0;
}

void get_common
  (JNIEnv *env, jobject obj, TOID(struct persistent_byte_buffer) pbb, jbyteArray dest, int arrOffset, int index, int length, int from_position)
{
    int position = D_RO(pbb)->position;
    int start = D_RO(pbb)->start;
    int start_position = (from_position == 1) ? position : index;

    TOID(struct persistent_byte_array) arr;
    TOID_ASSIGN(arr, D_RO(pbb)->arr);
    PMEMoid bytes_oid = D_RO(arr)->bytes;
    char* src = (char*)(pmemobj_direct(bytes_oid));
    jbyte* bytes = (jbyte*)(src+start+start_position);

    TX_BEGIN(pool) {
        if (from_position == 1) {
            TX_ADD_FIELD(pbb, position);
            D_RW(pbb)->position = D_RO(pbb)->position+length;
        }

        (env)->SetByteArrayRegion(dest, arrOffset, length, bytes);
    } TX_ONABORT {
        throw_persistent_object_exception(env, "NativeGet: ");
    } TX_END
}

JNIEXPORT void JNICALL Java_lib_persistent_PersistentByteBuffer_nativePersistByteBufferState
  (JNIEnv *env, jobject obj, jlong offset, jintArray params)
{
    TOID(struct persistent_byte_buffer) pbb;
    OFFSET_TO_TOID(pbb, offset);

    jboolean is_copy_param;
    jint* param_array = (env)->GetIntArrayElements(params, &is_copy_param);

    TX_BEGIN(pool) {
        TX_ADD_FIELD(pbb, position);
        TX_ADD_FIELD(pbb, limit);
        TX_ADD_FIELD(pbb, mark);

        D_RW(pbb)->position = param_array[0];
        D_RW(pbb)->limit = param_array[1];
        D_RW(pbb)->mark = param_array[2];
    } TX_ONABORT {
        throw_persistent_object_exception(env, "NativePersistByteBufferState: ");
    } TX_FINALLY {
        if (is_copy_param) {
            (env)->ReleaseIntArrayElements(params, param_array, 0);
        }
    } TX_END
}

JNIEXPORT jintArray JNICALL Java_lib_persistent_PersistentByteBuffer_nativeRetrieveByteBufferState
  (JNIEnv *env, jclass klass, jlong offset)
{
    TOID(struct persistent_byte_buffer) pbb;
    OFFSET_TO_TOID(pbb, offset);

    jint fill[] = {D_RO(pbb)->position, D_RO(pbb)->limit, D_RO(pbb)->mark, D_RO(pbb)->capacity};
    jintArray result = (env)->NewIntArray(4);

    (env)->SetIntArrayRegion(result, 0, 4, fill);
    return result;
}

JNIEXPORT jboolean JNICALL Java_lib_persistent_PersistentByteBuffer_nativeCheckByteBufferExists
  (JNIEnv *env, jclass klass, jlong offset)
{
    PMEMoid bb_oid = {get_uuid_lo(), (uint64_t)offset};
    return check_buf_existence(bb_oid) == 0 ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jlong JNICALL Java_lib_persistent_PersistentByteBuffer_nativeDuplicate
  (JNIEnv *env, jobject obj, jlong offset)
{
    TOID(struct persistent_byte_buffer) new_pbb, old_pbb;
    TOID(struct header) header;
    OFFSET_TO_TOID(old_pbb, offset);

    TX_BEGIN (pool) {
        new_pbb = TX_ZNEW(struct persistent_byte_buffer);
        header = TX_ZNEW(struct header);

        D_RW(new_pbb)->header = header;
        D_RW(new_pbb)->capacity = D_RO(old_pbb)->capacity;
        D_RW(new_pbb)->arr = D_RO(old_pbb)->arr;
        D_RW(new_pbb)->position = D_RO(old_pbb)->position;
        D_RW(new_pbb)->limit = D_RO(old_pbb)->limit;
        D_RW(new_pbb)->mark = D_RO(old_pbb)->mark;
        D_RW(new_pbb)->start = D_RO(old_pbb)->start;

        D_RW(D_RW(new_pbb)->header)->refCount = 1;
        D_RW(D_RW(new_pbb)->header)->type = TOID_TYPE_NUM(struct persistent_byte_buffer);
        D_RW(D_RW(new_pbb)->header)->fieldCount = PERSISTENT_BYTE_BUFFER_FIELD_COUNT;
        D_RW(D_RW(new_pbb)->header)->color = BLACK;

        inc_ref(D_RW(new_pbb)->arr, 1, 0);
        add_to_obj_list(new_pbb.oid);
        //printf("duplicated new_pbb at offset %lu, its header is at offset %lu\n", new_pbb.oid.off, D_RO(new_pbb)->header.oid.off);
        //fflush(stdout);
    } TX_ONABORT {
        throw_persistent_object_exception(env, "NativeDuplicate: ");
    } TX_END

    return new_pbb.oid.off;
}

JNIEXPORT jlong JNICALL Java_lib_persistent_PersistentByteBuffer_nativeSlice
  (JNIEnv *env, jobject obj, jlong offset)
{
    TOID(struct persistent_byte_buffer) new_pbb, old_pbb;
    TOID(struct header) header;
    OFFSET_TO_TOID(old_pbb, offset);

    TX_BEGIN (pool) {
        new_pbb = TX_ZNEW(struct persistent_byte_buffer);
        header = TX_ZNEW(struct header);

        D_RW(new_pbb)->header = header;
        D_RW(new_pbb)->capacity = calculate_remaining(old_pbb);
        D_RW(new_pbb)->arr = D_RO(old_pbb)->arr;
        D_RW(new_pbb)->position = 0;
        D_RW(new_pbb)->limit = D_RO(new_pbb)->capacity;
        D_RW(new_pbb)->mark = -1;
        D_RW(new_pbb)->start = D_RO(old_pbb)->position;

        D_RW(D_RW(new_pbb)->header)->refCount = 1;
        D_RW(D_RW(new_pbb)->header)->type = TOID_TYPE_NUM(struct persistent_byte_buffer);
        D_RW(D_RW(new_pbb)->header)->fieldCount = PERSISTENT_BYTE_BUFFER_FIELD_COUNT;
        D_RW(D_RW(new_pbb)->header)->color = BLACK;

        inc_ref(D_RW(new_pbb)->arr, 1, 0);
        add_to_obj_list(new_pbb.oid);
        //printf("sliced new_pbb at offset %lu, its header is at offset %lu\n", new_pbb.oid.off, D_RO(new_pbb)->header.oid.off);
        //fflush(stdout);
    } TX_ONABORT {
        throw_persistent_object_exception(env, "NativeSlice: ");
    } TX_END

    return new_pbb.oid.off;
}

JNIEXPORT jint JNICALL Java_lib_persistent_PersistentByteBuffer_nativeRemaining
  (JNIEnv *env, jobject obj, jlong offset)
{
    TOID(struct persistent_byte_buffer) pbb;
    OFFSET_TO_TOID(pbb, offset);

    return calculate_remaining(pbb);
}

JNIEXPORT jint JNICALL Java_lib_persistent_PersistentByteBuffer_nativeReset
  (JNIEnv *env, jobject obj, jlong offset)
{
    TOID(struct persistent_byte_buffer) pbb;
    OFFSET_TO_TOID(pbb, offset);

    TX_BEGIN (pool) {
        // Per JavaDoc on Buffer$reset(), reset() does not change mark, only position
        TX_ADD_FIELD(pbb, position);

        D_RW(pbb)->position = D_RO(pbb)->mark;
    } TX_ONABORT {
        throw_persistent_object_exception(env, "NativeReset: ");
    } TX_END

    return 0;
}

JNIEXPORT jint JNICALL Java_lib_persistent_PersistentByteBuffer_nativeCompareTo
  (JNIEnv *env, jobject obj, jlong this_offset, jlong that_offset)
{
    return compare_persistent_bytebuffers(this_offset, that_offset);
}

void free_buffer(TOID(struct persistent_byte_buffer) pbb, int already_locked)
{
    TX_BEGIN(pool) {
        TX_ADD(pbb);
        TX_ADD_FIELD(pbb, arr);
        TX_ADD_FIELD(pbb, header);

        if (!OID_IS_NULL(D_RO(pbb)->arr)) {
            dec_ref(D_RO(pbb)->arr, 1, already_locked);
        }

        free_header(pbb.oid);

        TX_MEMSET(pmemobj_direct(pbb.oid), 0, sizeof(struct persistent_byte_buffer));
        TX_FREE(pbb);
    } TX_ONABORT {
        printf("Freeing buffer at offset %lu failed!\n", pbb.oid.off);
        exit(-1);
    } TX_END
}

void free_array(TOID(struct persistent_byte_array) arr)
{
    TX_BEGIN(pool) {
        TX_ADD(arr);
        TX_ADD_FIELD(arr, header);
        TX_ADD_FIELD(arr, bytes);

        free_header(arr.oid);

        pmemobj_memset_persist(pool, pmemobj_direct(D_RO(arr)->bytes), 0, D_RO(arr)->length);
        pmemobj_tx_free(D_RO(arr)->bytes);

        TX_MEMSET(pmemobj_direct(arr.oid), 0, sizeof(struct persistent_byte_array));
        TX_FREE(arr);
    } TX_ONABORT {
        printf("Freeing array at offset %lu failed!\n", arr.oid.off);
        exit(-1);
    } TX_END

}

int compare_persistent_bytebuffers(long this_offset, long that_offset)
{
    TOID(struct persistent_byte_buffer) this_pbb, that_pbb;

    PMEMoid this_bb_oid = {get_uuid_lo(), (uint64_t)this_offset};
    PMEMoid that_bb_oid = {get_uuid_lo(), (uint64_t)that_offset};
    TOID_ASSIGN(this_pbb, this_bb_oid);
    TOID_ASSIGN(that_pbb, that_bb_oid);

    TOID(struct persistent_byte_array) this_arr, that_arr;
    TOID_ASSIGN(this_arr, D_RO(this_pbb)->arr);
    TOID_ASSIGN(that_arr, D_RO(that_pbb)->arr);
    PMEMoid this_bytes = D_RO(this_arr)->bytes, that_bytes = D_RO(that_arr)->bytes;

    int this_remaining = calculate_remaining(this_pbb);
    int that_remaining = calculate_remaining(that_pbb);
    void* dest = (void*)((long)pmemobj_direct(this_bytes) + D_RO(this_pbb)->position);
    void* src =  (void*)((long)pmemobj_direct(that_bytes) + D_RO(that_pbb)->position);

    int iterations = D_RO(this_pbb)->position + (this_remaining < that_remaining ? this_remaining : that_remaining);
    int i, j;
    for (i = D_RO(this_pbb)->position, j = D_RO(that_pbb)->position; i < iterations; i++, j++) {
        char* this_byte = (char*)(pmemobj_direct(this_bytes)) + i;
        char* that_byte = (char*)(pmemobj_direct(that_bytes)) + j;
        int cmp = *this_byte - *that_byte;
        if (cmp != 0)
            return cmp;
    }

    return this_remaining - that_remaining;
}

int calculate_remaining(TOID(struct persistent_byte_buffer) pbb)
{
    return D_RO(pbb)->limit - D_RO(pbb)->position;
}

int check_buf_existence(PMEMoid bb_oid)
{
    return search_through_pool(bb_oid.off);
}

int search_through_pool(long offset)
{
    TOID(struct persistent_byte_buffer) buf;
    POBJ_FOREACH_TYPE(pool, buf) {
        if (buf.oid.off == offset) {
            return 0;
        }
    }
    return 1;
}

void get_num_bufs()
{
    TOID(struct persistent_byte_buffer) buf;
    int count = 0;
    POBJ_FOREACH_TYPE(pool, buf) {
        count++;
    }
    printf("There are %d buffers in pool\n", count);
    fflush(stdout);
}
