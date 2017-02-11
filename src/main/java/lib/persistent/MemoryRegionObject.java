/* Copyright (C) 2017  Intel Corporation
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General public synchronized License
 * version 2 only, as published by the Free Software Foundation.
 * This file has been designated as subject to the "Classpath"
 * exception as provided in the LICENSE file that accompanied
 * this code.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General public synchronized License version 2 for more details (a copy
 * is included in the LICENSE file that accompanied this code).
 *
 * You should have received a copy of the GNU General public synchronized License
 * version 2 along with this program; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA  02110-1301, USA.
 */

package lib.persistent;

import java.util.Map;
import java.util.TreeMap;
import java.lang.UnsupportedOperationException;

public class MemoryRegionObject implements PersistentMemoryRegion, Persistent {

    static {
        System.loadLibrary("Persistent");
        Util.openPool();
    }

    public int MODE_TRANSACTIONAL = 1;
    public int MODE_MANUAL = 0;

    private long offset;
    private TreeMap<Long, Long> addressRanges;

    public MemoryRegionObject(long size) {
        this(size, 1);
    }

    public MemoryRegionObject(long size, int consistency) {
        synchronized(MemoryRegionObject.class) {
            this.offset = nativeGetMemoryRegion(size, consistency);
            this.addressRanges = new TreeMap<Long, Long>();
        }
    }

    public synchronized int consistency() {
        return nativeGetConsistency(this.offset);
    }

    public synchronized boolean isDirty() {
        return (nativeGetDirty(this.offset) == 1);
    }

    public synchronized void flush() {
        if (addressRanges == null) {
            addressRanges = new TreeMap<Long, Long>();
        }
        for (Map.Entry<Long, Long> e : addressRanges.entrySet()) {
            nativeFlush(this.offset, e.getKey(), e.getValue());
        }
        addressRanges.clear();
        nativeSetDirty(this.offset, 0);
    }

    public synchronized long addr() {
        throw new UnsupportedOperationException();
    }

    public synchronized long getOffset() {
        return this.offset;
    }

    synchronized long size() {
        return nativeGetSize(this.offset);
    }

    private synchronized void addToMemoryRanges(long offset, long size) {
        if (addressRanges == null) addressRanges = new TreeMap<Long, Long>();
        for (Map.Entry<Long, Long> e : addressRanges.entrySet()) {
            long start = (long)e.getKey();
            long end = start + (long)e.getValue();
            if (offset >= start && offset <= end) {
                e.setValue(offset + size - start);
                return;
            }
        }
        addressRanges.put(offset, size);
    }

    public synchronized void checkAccess(int mode) throws IllegalAccessException {}
    public synchronized void checkAlive() {}
    public synchronized void checkBounds(long offset) throws IndexOutOfBoundsException {
        if (offset < 0 || offset >= size()) {
            throw new IndexOutOfBoundsException("offset=0x" + Long.toHexString(offset) + " length=0x" + Long.toHexString(size()));
        }
    }

    private synchronized void checkRange(long offset, long length) {
        checkBounds(offset);
        checkBounds(offset + length - 1);
    }

    public synchronized long getBits(long offset, long size, boolean isSigned) {
        if (size < 0 || size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid size: " + size);
        }

        checkRange(offset, size);
        long bits = nativeGetLong(this.offset, offset, (int)size);

        switch ((int)size) {
        case 1:
            return isSigned ? bits : Byte.toUnsignedLong((byte)bits);
        case 2:
            return isSigned ? bits : Short.toUnsignedLong((short)bits);
        case 4:
            return isSigned ? bits : Integer.toUnsignedLong((int)bits);
        case 8:
            return bits;

        default:
            throw new IllegalArgumentException("Invalid size: " + size);
        }
    }

    public synchronized void putBits(long offset, long size, long value) {
        if (size < 0 || size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid size: " + size);
        }

        if (size != 1 && size != 2 && size != 4 && size != 8) {
            throw new IllegalArgumentException("Invalid size: " + size);
        }

        checkRange(offset, size);
        if (consistency() == MODE_MANUAL) {
            nativeSetDirty(this.offset, 1);
            nativePutLong(this.offset, offset, value, (int)size);
            addToMemoryRanges(offset, size);
        } else {
            nativePutLong(this.offset, offset, value, (int)size);
        }
    }

    public synchronized byte getByte(long offset) {
        return (byte)getBits(offset, 1, true);
    }
    public synchronized void putByte(long offset, byte value) {
        putBits(offset, 1, value);
    }

    public synchronized short getShort(long offset) {
        return (short)getBits(offset, 2, true);
    }
    public synchronized void putShort(long offset, short value) {
        putBits(offset, 2, value);
    }

    public synchronized int getInt(long offset) {
        return (int)getBits(offset, 4, true);
    }
    public synchronized void putInt(long offset, int value) {
        putBits(offset, 4, value);
    }

    public synchronized long getLong(long offset) {
        return getBits(offset, 8, true);
    }
    public synchronized void putLong(long offset, long value) {
        putBits(offset, 8, value);
    }

    public synchronized long getAddress(long offset) {
        return this.offset + offset;
    }
    public synchronized void putAddress(long offset, long value) {
        putLong(offset, value);
    }

    private synchronized native long nativeGetMemoryRegion(long size, int consistency);
    private synchronized native int nativeGetDirty(long offset);
    private synchronized native int nativeGetConsistency(long offset);
    private synchronized native long nativeGetSize(long offset);
    private synchronized native void nativeSetDirty(long offset, int dirty);
    private synchronized native void nativeSetConsistency(long offset, int consistency);
    private synchronized native long nativeGetLong(long regionOffset, long offset, int size);
    private synchronized native void nativePutLong(long regionOffset, long offset, long value, int size);
    private synchronized native void nativeFlush(long regionOffset, long offset, long size);
    private synchronized native void nativeFree(long offset);
}
