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

class RawMemoryRegion extends AbstractMemoryRegion<Raw> {
    static final long METADATA_SIZE = 8;

    RawMemoryRegion(long x, boolean isAddr) {
        super(x, isAddr);
    }

    @Override
    public void flush() { throw new UnsupportedOperationException(); }

    @Override
    public boolean isFlushed() { throw new UnsupportedOperationException(); }

    @Override
    public void copyFromMemory(MemoryRegion<?> srcRegion, long srcOffset, long dstOffset, long length) {
        nativeMemoryRegionMemcpyRaw(srcRegion.addr(), srcRegion.baseOffset() + srcOffset, addr(), baseOffset() + dstOffset, length);
    }

    @Override
    public void copyFromArray(byte[] srcArray, int srcOffset, long dstOffset, int length) {
        nativeFromByteArrayMemcpyRaw(srcArray, srcOffset, addr(), baseOffset() + dstOffset, length);
    }

    @Override
    public void setMemory(byte val, long offset, long length) {
        nativeMemoryRegionMemsetRaw(addr(), baseOffset() + offset, val, length);
    }

    @Override
    public long baseOffset() { return METADATA_SIZE; }

    private native int nativePutBits(long regionOffset, long offset, long value, int size);
}
