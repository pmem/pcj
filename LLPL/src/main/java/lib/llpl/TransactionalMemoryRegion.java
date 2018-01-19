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

public class TransactionalMemoryRegion extends AbstractMemoryRegion<Transactional> {
    private static final long METADATA_SIZE = 8;

    TransactionalMemoryRegion(Heap h, long x, boolean isAddr) {
        super(h, x, isAddr);
    }

    @Override
    public void putBits(long offset, long size, long value) {
        if (size < 0 || size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid size: " + size);
        }

        if (size != 1 && size != 2 && size != 4 && size != 8) {
            throw new IllegalArgumentException("Invalid size: " + size);
        }

        if (nativePutBits(this.addr, offset + baseOffset(), value, (int)size) < 0) {
            throw new PersistenceException("Failed to put bits into region!");
        }
    }

    @Override
    public void flush() { throw new UnsupportedOperationException(); }

    @Override
    public boolean isFlushed() { throw new UnsupportedOperationException(); }

    @Override
    public void putByte(long offset, byte value) {
        nativePutBits(this.addr, offset + baseOffset(), value, Byte.BYTES);
    }

    @Override
    public void putShort(long offset, short value) {
        nativePutBits(this.addr, offset + baseOffset(), value, Short.BYTES);
    }

    @Override
    public void putInt(long offset, int value) {
        nativePutBits(this.addr, offset + baseOffset(), value, Integer.BYTES);
    }

    @Override
    public void putLong(long offset, long value) {
        nativePutBits(this.addr, offset + baseOffset(), value, Long.BYTES);
    }

    @Override
    public void copyFromMemory(MemoryRegion<?> srcRegion, long srcOffset, long dstOffset, long length) {
        if (nativeMemoryRegionMemcpyTransactional(srcRegion.addr(), srcRegion.baseOffset() + srcOffset, addr(), baseOffset() + dstOffset, length) != 0)
            throw new PersistenceException("Failed to copy from MemoryRegion!");
    }

    @Override
    public void copyFromArray(byte[] srcArray, int srcOffset, long dstOffset, int length) {
        nativeFromByteArrayMemcpyTransactional(srcArray, srcOffset, addr(), baseOffset() + dstOffset, length);
    }

    @Override
    public void setMemory(byte val, long offset, long length) {
        nativeMemoryRegionMemsetTransactional(addr(), baseOffset() + offset, val, length);
    }

    @Override
    public long baseOffset() { return METADATA_SIZE; }

    private native int nativePutBits(long regionOffset, long offset, long value, int size);
    private native int nativeMemoryRegionMemcpyTransactional(long srcRegion, long srcOffset, long dstRegion, long dstOffset, long length);
    private native int nativeFromByteArrayMemcpyTransactional(byte[] srcArray, int srcOffset, long dstRegion, long dstOffset, int length);
    private native int nativeMemoryRegionMemsetTransactional(long region, long offset, int val, long length);
}
