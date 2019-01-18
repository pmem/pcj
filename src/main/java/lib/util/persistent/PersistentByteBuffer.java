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

package lib.util.persistent;

import lib.util.persistent.types.*;
import lib.xpersistent.XHeap;
import lib.util.persistent.spi.*;
import java.nio.*;

public final class PersistentByteBuffer extends PersistentObject implements Comparable<PersistentByteBuffer> {
    private static final IntField POSITION = new IntField();
    private static final IntField LIMIT = new IntField();
    private static final IntField MARK = new IntField();
    private static final IntField CAPACITY = new IntField();
    private static final IntField START = new IntField();
    private static final FinalObjectField<PersistentByteArray> BYTES = new FinalObjectField<>(PersistentByteArray.class);
    static final ObjectType<PersistentByteBuffer> TYPE = ObjectType.withFields(PersistentByteBuffer.class, POSITION, LIMIT, MARK, CAPACITY, START, BYTES);

    public static PersistentByteBuffer allocate(int size) {
        if (size < 0) throw new IllegalArgumentException("Capacity of PersistentByteBuffer must be non-negative");
        return new PersistentByteBuffer(size);
    }

    PersistentByteBuffer(int size, PersistentByteArray bytes) {
        super(TYPE, (PersistentByteBuffer obj) -> {
            obj.initObjectField(BYTES, bytes);
        });
        position(0);
        limit(size);
        setMark(-1);
        start(0);
        capacity(size);
    }

    PersistentByteBuffer(int size) {
        this(size, new PersistentByteArray(size));
    }

    protected PersistentByteBuffer(ObjectPointer<? extends PersistentByteBuffer> p) { super(p); }

    public byte[] array() { return bytes().toArray(); }
    public int arrayOffset() { return 0; }

    public PersistentByteBuffer asReadOnlyBuffer() { return null; /* NIY */ }

    public String toString() {
        return Util.synchronizedBlock(this, () -> {
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
        });
    }

    public ByteOrder order() { return ByteOrder.BIG_ENDIAN; }
    public boolean isDirect() { return false; }
    public boolean hasArray() { return true; }
    public boolean isReadOnly() { return false; }

    public PersistentByteBuffer position(int position) { setIntField(POSITION, position); return this; }
    public int position() { return getIntField(POSITION); }

    public PersistentByteBuffer limit(int limit) { setIntField(LIMIT, limit); return this; }
    public int limit() { return getIntField(LIMIT); }

    public PersistentByteBuffer mark() { setMark(position()); return this; }
    private int getMark() { return getIntField(MARK); }
    private void setMark(int mark) { setIntField(MARK, mark); }

    public int capacity() { return getIntField(CAPACITY); }
    private void capacity(int capacity) { setIntField(CAPACITY, capacity); }

    private int start() { return getIntField(START); }
    private void start(int start) { setIntField(START, start); }

    private PersistentByteArray bytes() { return (PersistentByteArray)getObjectField(BYTES); }

    public boolean hasRemaining() { return remaining() != 0; }
    public int remaining() {
        return Util.synchronizedBlock(this, () -> (limit() - position()));
    }

    public PersistentByteBuffer put(byte[] src, int arrOffset, int length) {
        Transaction.run(() -> {
            if (length > remaining()) throw new BufferOverflowException();
            if ((arrOffset < 0 || arrOffset > src.length) ||
                (length < 0 || length > (src.length - arrOffset)))
                throw new IndexOutOfBoundsException();
            PersistentArrays.fromByteArray(src, arrOffset, bytes(), start() + position(), length);
            position(position() + length);
        }/*, this, bytes()*/);
        return this;
    }

    public PersistentByteBuffer put(byte b) {
        Transaction.run(() -> {
            if (1 > remaining()) throw new BufferOverflowException();
            bytes().set(start() + position(), b);
            position(position() + 1);
        }/*, this, bytes()*/);
        return this;
    }

    public PersistentByteBuffer put(PersistentByteBuffer src) {
        if (this == src) throw new IllegalArgumentException();
        Transaction.run(() -> {
            if (src.remaining() > this.remaining()) throw new BufferOverflowException();
            PersistentArrays.toPersistentByteArray(src.bytes(), src.start() + src.position(), bytes(), start() + position(), src.remaining());
            position(position() + src.remaining());
            src.position(src.position() + src.remaining());
        }/*, src, src.bytes(), this, bytes()*/);
        return this;
    }

    public PersistentByteBuffer put(java.nio.ByteBuffer src) {
        Transaction.run(() -> {
            if (src.remaining() > this.remaining()) throw new BufferOverflowException();
            byte[] srcBytes = new byte[src.remaining()];
            src.get(srcBytes);    // also increments src's position
            PersistentArrays.fromByteArray(srcBytes, 0, bytes(), start() + position(), srcBytes.length);
            position(position() + srcBytes.length);
        }/*, this, bytes()*/);
        return this;
    }

    private PersistentByteBuffer putAbsolute(int index, byte[] src, int length) {
        Transaction.run(() -> {
            if (index < 0 || !(index < (limit() - (length - 1)))) throw new IndexOutOfBoundsException();
            PersistentArrays.fromByteArray(src, 0, bytes(), start() + index, length);
        }/*, this, bytes()*/);
        // does not modify position in the absolute case
        return this;
    }

    public PersistentByteBuffer put(int index, byte b) {
        Transaction.run(() -> {
            if (index < 0 || index >= limit()) throw new IndexOutOfBoundsException();
            bytes().set(start() + index, b);
        }/*, this, bytes()*/);
        return this;
    }

    public PersistentByteBuffer put(byte[] value) {
        return put(value, 0, value.length);
    }

    public PersistentByteBuffer putChar(int index, char value) {
        return putShort(index, (short)value);
    }

    public PersistentByteBuffer putChar(char value) {
        return putShort((short)value);
    }

    public PersistentByteBuffer putDouble(int index, double value) {
        return putLong(index, Double.doubleToLongBits(value));
    }

    public PersistentByteBuffer putDouble(double value) {
        return putLong(Double.doubleToLongBits(value));
    }

    public PersistentByteBuffer putFloat(int index, float value) {
        return putInt(index, Float.floatToIntBits(value));
    }

    public PersistentByteBuffer putFloat(float value) {
        return putInt(Float.floatToIntBits(value));
    }

    public PersistentByteBuffer putInt(int index, int value) {
        byte[] result = new byte[4];
        for (int i = 3; i >= 0; i--) {
            result[i] = (byte)(value & 0xff);
            value >>= 8;
        }
        return putAbsolute(index, result, 4);
    }

    public PersistentByteBuffer putInt(int value) {
        byte[] result = new byte[4];
        for (int i = 3; i >= 0; i--) {
            result[i] = (byte)(value & 0xff);
            value >>= 8;
        }
        return put(result);
    }

    public PersistentByteBuffer putShort(int index, short value) {
        byte[] result = new byte[2];
        result[1] = (byte)(value & 0xff);
        result[0] = (byte)((value >> 8) & 0xff);
        return putAbsolute(index, result, 2);
    }

    public PersistentByteBuffer putShort(short value) {
        byte[] result = new byte[2];
        result[1] = (byte)(value & 0xff);
        result[0] = (byte)((value >> 8) & 0xff);
        return put(result);
    }

    public PersistentByteBuffer putLong(int index, long value) {
        byte[] result = new byte[8];
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte)(value & 0xff);
            value >>= 8;
        }
        return putAbsolute(index, result, 8);
    }

    public PersistentByteBuffer putLong(long value) {
        byte[] result = new byte[8];
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte)(value & 0xff);
            value >>= 8;
        }
        return put(result);
    }

    public PersistentByteBuffer rewind() {
        Transaction.run(() -> {
            position(0);
            setMark(-1);
        }/*, this*/);
        return this;
    }

    public PersistentByteBuffer reset() {
        Transaction.run(() -> {
            if (getMark() < 0) throw new InvalidMarkException();
            position(getMark());
        }/*, this*/);
        return this;
    }

    public PersistentByteBuffer get(byte[] dst, int arrOffset, int length) {
        Transaction.run(() -> {
            if (length > remaining()) throw new BufferUnderflowException();
            if ((arrOffset < 0 || arrOffset > dst.length) ||
                (length < 0 || length > (dst.length - arrOffset)))
                throw new IndexOutOfBoundsException();
            PersistentArrays.toByteArray(bytes(), start() + position(), dst, arrOffset, length);
            position(position() + length);
        }/*, this, bytes()*/);
        return this;
    }

    public byte get() {
        byte[] ret = new byte[1];
        get(ret, 0, 1);
        return ret[0];
    }

    public PersistentByteBuffer get(byte[] dst) {
        return get(dst, 0, dst.length);
    }

    public byte get(int index) {
        return getAbsolute(index, 1)[0];
    }

    private byte[] getAbsolute(int index, int length) {
        return Util.synchronizedBlock(this, () -> {
            return Util.synchronizedBlock(bytes(), () -> {
                if (index < 0 || !(index < (limit() - (length - 1))))
                    throw new IndexOutOfBoundsException();

                byte[] value = new byte[length];
                PersistentArrays.toByteArray(bytes(), start() + index, value, 0, length);
                // does not modify position in the absolute case
                return value;
            });
        });
    }

    public char getChar(int index) {
        return (char)(getShort(index));
    }

    public char getChar() {
        return (char)(getShort());
    }

    public double getDouble(int index) {
        return Double.longBitsToDouble(getLong(index));
    }

    public double getDouble() {
        return Double.longBitsToDouble(getLong());
    }

    public float getFloat(int index) {
        return Float.intBitsToFloat(getInt(index));
    }

    public float getFloat() {
        return Float.intBitsToFloat(getInt());
    }

    public int getInt(int index) {
        byte[] intBytes = getAbsolute(index, Integer.BYTES);

        int ret = 0;
        for (int i = 0; i < Integer.BYTES; i++) {
            ret = (ret << 8) + (intBytes[i] & 0xff);
        }
        return ret;
    }

    public int getInt() {
        byte[] intBytes = new byte[Integer.BYTES];
        get(intBytes);

        int ret = 0;
        for (int i = 0; i < Integer.BYTES; i++) {
            ret = (ret << 8) + (intBytes[i] & 0xff);
        }
        return ret;
    }

    public short getShort(int index) {
        byte[] shortBytes = getAbsolute(index, Short.BYTES);

        short ret = 0;
        for (int i = 0; i < Short.BYTES; i++) {
            ret = (short)((ret << 8) + (shortBytes[i] & 0xff));
        }
        return ret;
    }

    public short getShort() {
        byte[] shortBytes = new byte[Short.BYTES];
        get(shortBytes);

        short ret = 0;
        for (int i = 0; i < Short.BYTES; i++) {
            ret = (short)((ret << 8) + (shortBytes[i] & 0xff));
        }
        return ret;
    }

    public long getLong(int index) {
        byte[] longBytes = getAbsolute(index, Long.BYTES);

        long ret = 0;
        for (int i = 0; i < Long.BYTES; i++) {
            ret = (ret << 8) + (longBytes[i] & 0xff);
        }
        return ret;
    }

    public long getLong() {
        byte[] longBytes = new byte[Long.BYTES];
        get(longBytes, 0, Long.BYTES);

        long ret = 0;
        for (int i = 0; i < Long.BYTES; i++) {
            ret = (ret << 8) + (longBytes[i] & 0xff);
        }
        return ret;
    }

    public PersistentByteBuffer clear() {
        Transaction.run(() -> {
            position(0);
            limit(capacity());
        }/*, this*/);
        return this;
    }

    public PersistentByteBuffer flip() {
        Transaction.run(() -> {
            limit(position());
            position(0);
            setMark(-1);
        }/*, this*/);
        return this;
    }

    public PersistentByteBuffer duplicate() {
        Box<PersistentByteBuffer> ret = new Box<>();
        Transaction.run(() -> {
            ret.set(new PersistentByteBuffer(capacity(), bytes()));
            ret.get().position(position());
            ret.get().limit(limit());
            ret.get().setMark(getMark());
            ret.get().start(start());
        }/*, this, bytes()*/);
        return ret.get();
    }

    public PersistentByteBuffer slice() {
        Box<PersistentByteBuffer> ret = new Box<>();
        Transaction.run(() -> {
            ret.set(new PersistentByteBuffer(remaining(), bytes()));
            ret.get().position(0);
            ret.get().limit(remaining());
            ret.get().setMark(-1);
            ret.get().start(position());
        }/*, this, bytes()*/);
        return ret.get();
    }

    public static PersistentByteBuffer copyWrap(byte[] array) {
        return PersistentByteBuffer.copyWrap(array, 0, array.length);
    }

    public static PersistentByteBuffer copyWrap(byte[] array, int offset, int length) {
        PersistentByteArray pba = new PersistentByteArray(array);
        Box<PersistentByteBuffer> ret = new Box<>();
        Transaction.run(() -> {
            ret.set(new PersistentByteBuffer(array.length, pba));
            ret.get().position(offset);
            ret.get().limit(offset + length);
            ret.get().setMark(-1);
            ret.get().start(0);
        });
        return ret.get();
    }

    public static PersistentByteBuffer wrap(PersistentByteArray array) {
        return PersistentByteBuffer.wrap(array, 0, array.length());
    }

    public static PersistentByteBuffer wrap(PersistentByteArray array, int offset, int length) {
        Box<PersistentByteBuffer> ret = new Box<>();
        Transaction.run(() -> {
            ret.set(new PersistentByteBuffer(array.length(), array));
            ret.get().position(offset);
            ret.get().limit(offset + length);
            ret.get().setMark(-1);
            ret.get().start(0);
        });
        return ret.get();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o instanceof PersistentByteBuffer) {
            PersistentByteBuffer that = (PersistentByteBuffer)o;
            return Util.synchronizedBlock(this, () -> {
                return Util.synchronizedBlock(that, () -> {
                    if (this.remaining() != that.remaining())
                        return false;
                    if (compareTo(that) != 0)
                        return false;
                    return true;
                });
            });
        } else return false;
    }

    @Override
    public int compareTo(PersistentByteBuffer that) {
        return Util.synchronizedBlock(this, () -> {
            return Util.synchronizedBlock(this.bytes(), () -> {
                return Util.synchronizedBlock(that, () -> {
                    return Util.synchronizedBlock(that.bytes(), () -> {
                        int thisRemaining = this.remaining();
                        int thatRemaining = that.remaining();
                        int iterations = this.position() + (thisRemaining < thatRemaining ? thisRemaining : thatRemaining);
                        int i, j;

                        for (i = this.position(), j = that.position(); i < iterations; i++, j++) {
                            byte thisByte = this.get(i);
                            byte thatByte = that.get(i);
                            int cmp = (int)(thisByte - thatByte);
                            if (cmp != 0) return cmp;
                        }
                        return thisRemaining - thatRemaining;
                    });
                });
            });
        });
    }

    // public int compareTo(java.nio.ByteBuffer that) {
    //     return Util.synchronizedBlock(this, () -> {
    //         return Util.synchronizedBlock(this.bytes(), () -> {
    //             int thisRemaining = this.remaining();
    //             int thatRemaining = that.remaining();
    //             int iterations = this.position() + (thisRemaining < thatRemaining ? thisRemaining : thatRemaining);
    //             int i, j;

    //             for (i = this.position(), j = that.position(); i < iterations; i++, j++) {
    //                 byte thisByte = this.get(i);
    //                 byte thatByte = that.get(i);
    //                 int cmp = (int)(thisByte - thatByte);
    //                 if (cmp != 0) return cmp;
    //             }
    //             return thisRemaining - thatRemaining;
    //         });
    //     });
    // }

    @Override
    public int hashCode() {
        return Util.synchronizedBlock(this, () -> {
            return Util.synchronizedBlock(bytes(), () -> {
                int h = 1;
                int p = position();
                for (int i = limit() - 1; i >= p; i--)
                    h = 31 * h + (int)get(i);
                return h;
            });
        });
    }
}
