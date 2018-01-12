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

package lib.llpl;

abstract class AbstractMemoryRegion<K extends MemoryRegion.Kind> implements MemoryRegion<K>, Comparable<AbstractMemoryRegion<K>> {
    static final long SIZE_OFFSET = 0;

    protected long size;
    protected long addr;
    protected long directAddress;

    static {
        System.loadLibrary("llpl");
    }

    AbstractMemoryRegion(long x, boolean isAddr) {
        if (isAddr) {
            this.addr = x;
            this.size = nativeGetBits(this.addr, SIZE_OFFSET, Long.BYTES);
        } else {
            this.addr = nativeGetMemoryRegion(x + baseOffset());
            this.size = x;
            if (this.addr == 0) throw new PersistenceException("Failed to allocate MemoryRegion of size " + x + "!");
            if (nativeSetSize(this.addr, SIZE_OFFSET, this.size) != 0) throw new PersistenceException("Failed to allocate MemoryRegion of size " + x + "!");
        }
        this.directAddress = nativeGetDirectAddress(this.addr) + baseOffset();
    }

    public long addr() {
        return this.addr;
    }

    public long size() {
        return this.size;
    }

    void addr(long addr) {
        this.addr = addr;
        this.directAddress = nativeGetDirectAddress(addr) + baseOffset();
        if (addr != 0) this.size = nativeGetBits(this.addr, SIZE_OFFSET, Long.BYTES);
    }

    public void checkAccess(int mode) {}
    public void checkAlive() {
        if (this.addr == 0)
            throw new IllegalStateException("Region is not alive!");
    }
    public void checkBounds(long offset) throws IndexOutOfBoundsException {
        if (offset < 0 || offset >= this.size) throw new IndexOutOfBoundsException();
    }


    public long getBits(long offset, long size, boolean isSigned) {
        if (size < 0 || size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid size: " + size);
        }

        long bits;
        switch ((int)size) {
        case 1:
            bits = getByte(offset);
            return isSigned ? bits : Byte.toUnsignedLong((byte)bits);
        case 2:
            bits = getShort(offset);
            return isSigned ? bits : Short.toUnsignedLong((short)bits);
        case 4:
            bits = getInt(offset);
            return isSigned ? bits : Integer.toUnsignedLong((int)bits);
        case 8:
            return getLong(offset);

        default:
            throw new IllegalArgumentException("Invalid size: " + size);
        }
    }

    public void putBits(long offset, long size, long value) {
        if (size < 0 || size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid size: " + size);
        }

        if (size != 1 && size != 2 && size != 4 && size != 8) {
            throw new IllegalArgumentException("Invalid size: " + size);
        }

        switch ((int)size) {
            case 1: putByte(offset, (byte)value); break;
            case 2: putShort(offset, (short)value); break;
            case 4: putInt(offset, (int)value); break;
            case 8: putLong(offset, value); break;
        }
    }

    public byte getByte(long offset) {
        return Heap.UNSAFE.getByte(directAddress + offset);
    }
    public void putByte(long offset, byte value) {
        Heap.UNSAFE.putByte(directAddress + offset, value);
    }

    public short getShort(long offset) {
        return Heap.UNSAFE.getShort(directAddress + offset);
    }
    public void putShort(long offset, short value) {
        Heap.UNSAFE.putShort(directAddress + offset, value);
    }

    public int getInt(long offset) {
        return Heap.UNSAFE.getInt(directAddress + offset);
    }
    public void putInt(long offset, int value) {
        Heap.UNSAFE.putInt(directAddress + offset, value);
    }

    public long getLong(long offset) {
        return Heap.UNSAFE.getLong(directAddress + offset);
    }
    public void putLong(long offset, long value) {
        Heap.UNSAFE.putLong(directAddress + offset, value);
    }

    public long getAddress(long offset) {
        return getLong(offset);
    }
    public void putAddress(long offset, long value) {
        putLong(offset, value);
    }

    @Override
    public int hashCode() {
        return Long.hashCode(this.addr());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AbstractMemoryRegion)) return false;
        return this.addr() == ((AbstractMemoryRegion)o).addr();
    }

    @Override
    public int compareTo(AbstractMemoryRegion that) {
        long diff = this.addr() - that.addr();
        if (diff < 0) return -1;
        else if (diff > 0) return 1;
        else return 0;
    }

    private native long nativeGetMemoryRegion(long size);
    protected native long nativeGetBits(long regionOffset, long offset, int size);
    protected native long nativeGetDirectAddress(long regionOffset);
    protected native int nativeMemoryRegionMemcpyRaw(long srcRegion, long srcOffset, long dstRegion, long dstOffset, long length);
    protected native int nativeFromByteArrayMemcpyRaw(byte[] srcArray, int srcOffset, long dstRegion, long dstOffset, int length);
    protected native int nativeMemoryRegionMemsetRaw(long region, long offset, int val, long length);
    protected native int nativeSetSize(long region, long offset, long size);
}
