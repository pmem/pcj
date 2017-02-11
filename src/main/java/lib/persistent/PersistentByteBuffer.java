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

package lib.persistent;

import java.nio.*;
import java.lang.IllegalArgumentException;
import java.lang.IndexOutOfBoundsException;
import java.lang.UnsupportedOperationException;

public class PersistentByteBuffer implements Comparable, Persistent {

    private long offset;

    static {
        System.loadLibrary("Persistent");
        Util.openPool();
    }

    public synchronized static PersistentByteBuffer allocate(int size) {
        if (size < 0)
            throw new IllegalArgumentException("Capacity of PersistentByteBuffer must be non-negative");
        return new PersistentByteBuffer(size);
    }

    synchronized static PersistentByteBuffer fromOffset(long offset) {
        return new PersistentByteBuffer(offset);
    }

    synchronized static boolean isValidOffset(long offset) {
        return nativeCheckByteBufferExists(offset);
    }

    public synchronized long getOffset() {
        return this.offset;
    }

    // package private
    PersistentByteBuffer(int size) {
        synchronized (PersistentByteBuffer.class) {
            this.offset = nativeReserveByteBufferMemory(size);
            ObjectDirectory.registerObject(this);
        }
    }

    // package private
    PersistentByteBuffer(long offset) {
        synchronized (PersistentByteBuffer.class) {
            this.offset = offset;
            ObjectDirectory.registerObject(this);
        }
    }

    public synchronized byte[] array() {
        throw new UnsupportedOperationException("No backing array for PersistentByteBuffer");
    }

    public synchronized int arrayOffset() {
        throw new UnsupportedOperationException("No backing array for PersistentByteBuffer");
    }

    public synchronized ByteBuffer asReadOnlyBuffer() {
        ByteBuffer buf = nativeCreateByteBuffer(this.offset);
        return buf.asReadOnlyBuffer();
    }

    public synchronized int capacity() {
        return nativeRetrieveByteBufferState(offset)[3];    // 4th element of return is capacity
    }

    public synchronized String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(getClass().getName());
        sb.append("[pos=");
        sb.append(position());
        sb.append(" lim=");
        sb.append(limit());
        sb.append(" cap=");
        sb.append(capacity());
        sb.append("]");
        return sb.toString();
    }

    public synchronized ByteOrder order() {
        return ByteOrder.BIG_ENDIAN;
    }

    public synchronized boolean isDirect() {
        return true;
    }

    public synchronized boolean hasArray() {
        return false;
    }

    public synchronized boolean isReadOnly() {
        return false;
    }

    public synchronized PersistentByteBuffer put(byte[] src, int arrOffset, int length) {
        if (length > remaining())
            throw new BufferOverflowException();
        if ((arrOffset < 0 || arrOffset > src.length) ||
            (length < 0 || length > (src.length - arrOffset)))
            throw new IndexOutOfBoundsException();
        nativePut(offset, src, arrOffset, length);
        return this;
    }

    public synchronized PersistentByteBuffer put(byte b) {
        byte[] src = {b};
        return put(src, 0, 1);
    }

    public synchronized PersistentByteBuffer put(PersistentByteBuffer src) {
        if (this == src)
            throw new IllegalArgumentException();
        if (src.remaining() > this.remaining())
            throw new BufferOverflowException();
        nativePutByteBuffer(offset, src.getOffset());
        return this;
    }

    public synchronized PersistentByteBuffer put(int index, byte b) {
        byte[] value = new byte[1];
        value[0] = b;
        return putAbsolute(index, value, 1);
    }

    private synchronized PersistentByteBuffer putAbsolute(int index, byte[] value, int length) {
        if (index < 0 || !(index < (limit() - (length - 1))))
            throw new IndexOutOfBoundsException();
        nativePutAbsolute(offset, value, index, length);
        return this;
    }

    public synchronized PersistentByteBuffer put(byte[] value) {
        return put(value, 0, value.length);
    }

    public synchronized PersistentByteBuffer putChar(int index, char value) {
        return putShort(index, (short)value);
    }

    public synchronized PersistentByteBuffer putChar(char value) {
        return putShort((short)value);
    }

    public synchronized PersistentByteBuffer putDouble(int index, double value) {
        return putLong(index, Double.doubleToLongBits(value));
    }

    public synchronized PersistentByteBuffer putDouble(double value) {
        return putLong(Double.doubleToLongBits(value));
    }

    public synchronized PersistentByteBuffer putFloat(int index, float value) {
        return putInt(index, Float.floatToIntBits(value));
    }

    public synchronized PersistentByteBuffer putFloat(float value) {
        return putInt(Float.floatToIntBits(value));
    }

    public synchronized PersistentByteBuffer putInt(int index, int value) {
        byte[] result = new byte[4];
        for (int i = 3; i >= 0; i--) {
            result[i] = (byte)(value & 0xff);
            value >>= 8;
        }
        return putAbsolute(index, result, 4);
    }

    public synchronized PersistentByteBuffer putInt(int value) {
        byte[] result = new byte[4];
        for (int i = 3; i >= 0; i--) {
            result[i] = (byte)(value & 0xff);
            value >>= 8;
        }
        return put(result);
    }

    public synchronized PersistentByteBuffer putShort(int index, short value) {
        byte[] result = new byte[2];
        result[1] = (byte)(value & 0xff);
        result[0] = (byte)((value >> 8) & 0xff);
        return putAbsolute(index, result, 2);
    }

    public synchronized PersistentByteBuffer putShort(short value) {
        byte[] result = new byte[2];
        result[1] = (byte)(value & 0xff);
        result[0] = (byte)((value >> 8) & 0xff);
        return put(result);
    }

    public synchronized PersistentByteBuffer putLong(int index, long value) {
        byte[] result = new byte[8];
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte)(value & 0xff);
            value >>= 8;
        }
        return putAbsolute(index, result, 8);
    }

    public synchronized PersistentByteBuffer putLong(long value) {
        byte[] result = new byte[8];
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte)(value & 0xff);
            value >>= 8;
        }
        return put(result);
    }

    public synchronized PersistentByteBuffer rewind() {
        int[] bufferState = {0, limit(), -1, capacity()};

        nativePersistByteBufferState(offset, bufferState);
        return this;
    }

    public synchronized int limit() {
        return nativeRetrieveByteBufferState(offset)[1];    // 2nd return element is limit
    }

    public synchronized PersistentByteBuffer limit(int newLimit) {
        if (newLimit < 0 || newLimit > capacity()) {
            throw new IllegalArgumentException("Limit must be non-negative and no larger than the capacity");
        }
        int[] bufferState = {position(), newLimit, getMark(), capacity()};
        nativePersistByteBufferState(offset, bufferState);
        return this;
    }

    public synchronized int position() {
        return nativeRetrieveByteBufferState(offset)[0];    // 1st return element is limit
    }

    public synchronized PersistentByteBuffer position(int newPosition) {
        if (newPosition < 0 || newPosition > limit()) {
            throw new IllegalArgumentException("Position must be non-negative and no larger than the current limit");
        }
        int[] bufferState = {newPosition, limit(), getMark(), capacity()};
        nativePersistByteBufferState(offset, bufferState);
        return this;
    }

    public synchronized PersistentByteBuffer mark() {
        int[] bufferState = {position(), limit(), position(), capacity()};
        nativePersistByteBufferState(offset, bufferState);
        return this;
    }

    // This does not exist in the public synchronized API; private synchronized use only; needed because there is no
    // way to extract mark from the embedded ByteBuffer object
    private synchronized int getMark() {
        return nativeRetrieveByteBufferState(offset)[2];   // 3rd return element is mark
    }

    public synchronized int remaining() {
        return nativeRemaining(offset);
    }

    public synchronized boolean hasRemaining() {
        return remaining() != 0;
    }

    public synchronized PersistentByteBuffer reset() {
        if (getMark() < 0)
            throw new InvalidMarkException();
        nativeReset(offset);
        return this;
    }

    public synchronized PersistentByteBuffer get(byte[] dst, int arrOffset, int length) {
        if (length > remaining())
            throw new BufferUnderflowException();
        if ((arrOffset < 0 || arrOffset > dst.length) ||
            (length < 0 || length > (dst.length - arrOffset)))
            throw new IndexOutOfBoundsException();
        nativeGet(offset, dst, arrOffset, length);
        return this;
    }

    public synchronized byte get() {
        byte[] ret = new byte[1];
        get(ret, 0, 1);
        return ret[0];
    }

    public synchronized PersistentByteBuffer get(byte[] dst) {
        return get(dst, 0, dst.length);
    }

    public synchronized byte get(int index) {
        return getAbsolute(index, 1)[0];
    }

    private synchronized byte[] getAbsolute(int index, int length) {
        if (index < 0 || !(index < (limit() - (length - 1))))
            throw new IndexOutOfBoundsException();
        byte[] value = new byte[length];
        nativeGetAbsolute(offset, value, index, length);
        return value;
    }

    public synchronized char getChar(int index) {
        return (char)(getShort(index));
    }

    public synchronized char getChar() {
        return (char)(getShort());
    }

    public synchronized double getDouble(int index) {
        return Double.longBitsToDouble(getLong(index));
    }

    public synchronized double getDouble() {
        return Double.longBitsToDouble(getLong());
    }

    public synchronized float getFloat(int index) {
        return Float.intBitsToFloat(getInt(index));
    }

    public synchronized float getFloat() {
        return Float.intBitsToFloat(getInt());
    }

    public synchronized int getInt(int index) {
        byte[] intBytes = getAbsolute(index, Integer.BYTES);

        int ret = 0;
        for (int i = 0; i < Integer.BYTES; i++) {
            ret = (ret << 8) + (intBytes[i] & 0xff);
        }
        return ret;
    }

    public synchronized int getInt() {
        byte[] intBytes = new byte[Integer.BYTES];
        get(intBytes);

        int ret = 0;
        for (int i = 0; i < Integer.BYTES; i++) {
            ret = (ret << 8) + (intBytes[i] & 0xff);
        }
        return ret;
    }

    public synchronized short getShort(int index) {
        byte[] shortBytes = getAbsolute(index, Short.BYTES);

        short ret = 0;
        for (int i = 0; i < Short.BYTES; i++) {
            ret = (short)((ret << 8) + (shortBytes[i] & 0xff));
        }
        return ret;
    }

    public synchronized short getShort() {
        byte[] shortBytes = new byte[Short.BYTES];
        get(shortBytes);

        short ret = 0;
        for (int i = 0; i < Short.BYTES; i++) {
            ret = (short)((ret << 8) + (shortBytes[i] & 0xff));
        }
        return ret;
    }

    public synchronized long getLong(int index) {
        byte[] longBytes = getAbsolute(index, Long.BYTES);

        long ret = 0;
        for (int i = 0; i < Long.BYTES; i++) {
            ret = (ret << 8) + (longBytes[i] & 0xff);
        }
        return ret;
    }

    public synchronized long getLong() {
        byte[] longBytes = new byte[Long.BYTES];
        get(longBytes, 0, Long.BYTES);

        long ret = 0;
        for (int i = 0; i < Long.BYTES; i++) {
            ret = (ret << 8) + (longBytes[i] & 0xff);
        }
        return ret;
    }

    // Per JavaDoc on Buffer$Clear: does not actually erase content, merely resets position and limit
    public synchronized PersistentByteBuffer clear() {
        int[] bufferState = {0, capacity(), getMark(), capacity()};
        nativePersistByteBufferState(offset, bufferState);
        return this;
    }

    public synchronized PersistentByteBuffer flip() {
        int[] bufferState = {0, position(), -1, capacity()};
        nativePersistByteBufferState(offset, bufferState);
        return this;
    }

    public synchronized PersistentByteBuffer duplicate() {
        long duplicateOffset = nativeDuplicate(offset);
        return new PersistentByteBuffer(duplicateOffset);
    }

    public synchronized PersistentByteBuffer slice() {
        long sliceOffset = nativeSlice(offset);
        int sliceSize = nativeRemaining(sliceOffset);
        return new PersistentByteBuffer(sliceOffset);
    }

    public synchronized boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof PersistentByteBuffer))
            return false;
        PersistentByteBuffer that = (PersistentByteBuffer)o;
        if (this.remaining() != that.remaining())
            return false;
        if (compareTo(o) != 0)
            return false;
        return true;
    }

    @Override
    public synchronized int compareTo(Object o) {
        if (!(o instanceof PersistentByteBuffer))
            throw new ClassCastException(o.getClass().getName() + " cannot be cast to PersistentByteBuffer");
        PersistentByteBuffer that = (PersistentByteBuffer)o;
        return nativeCompareTo(this.offset, that.getOffset());
    }

    @Override
    public synchronized int hashCode() {
        int h = 1;
        int p = position();
        for (int i = limit() - 1; i >= p; i--)
            h = 31 * h + (int)get(i);
        return h;
    }

    private synchronized native long nativeReserveByteBufferMemory(int size);
    private synchronized native long nativeGetByteBufferAddress(ByteBuffer bb);
    private synchronized native ByteBuffer nativeCreateByteBuffer(long offset);
    private synchronized native int nativePut(long offset, byte[] src, int arrOffset, int length);
    private synchronized native int nativePutAbsolute(long offset, byte[] src, int index, int length);
    private synchronized native int nativePutByteBuffer(long thisOffset, long thatOffset);
    private synchronized native int nativeGet(long offset, byte[] dst, int arrOffset, int length);
    private synchronized native int nativeGetAbsolute(long offset, byte[] dst, int index, int length);
    private synchronized native void nativePersistByteBufferState(long offset, int[] bufferState);
    private synchronized static native int[] nativeRetrieveByteBufferState(long offset);
    private synchronized static native boolean nativeCheckByteBufferExists(long offset);
    private synchronized native long nativeDuplicate(long offset);
    private synchronized native long nativeSlice(long offset);
    private synchronized native int nativeRemaining(long offset);
    private synchronized native int nativeReset(long offset);
    private synchronized native int nativeCompareTo(long thisOffset, long thatOffset);
}
