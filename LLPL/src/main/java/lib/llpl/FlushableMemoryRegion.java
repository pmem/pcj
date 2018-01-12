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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.print.DocFlavor.BYTE_ARRAY;

import java.util.Iterator;

class FlushableMemoryRegion extends AbstractMemoryRegion<Flushable> {
    static final long FLUSH_FLAG_OFFSET = 8;    // store the flushed flag as an int at the 8th offset, real data starts at offset 12
    static final long FLUSH_FLAG_LENGTH = 4;    // flushed flag is an int
    static final long METADATA_SIZE = 12;
    static final int FLUSHED = 0;               // need to maintain 0 for default (flushed)
    static final int DIRTY = 1;                 // so "flushed" flag is really "dirty" flag
    static final int FLUSH_GRANULARITY = 64;    // flushing is aligned to 64 byte cache lines

    private ConcurrentHashMap<Long, Long> addressRanges;

    FlushableMemoryRegion(long x, boolean isAddr) {
        super(x, isAddr);
        this.addressRanges = new ConcurrentHashMap<Long, Long>();
    }

    private void addToMemoryRanges(long offset, long size) {
        if (addressRanges == null) addressRanges = new ConcurrentHashMap<Long, Long>();
        long start = this.directAddress + offset;
        long end = this.directAddress + offset + size - 1;
        long startFlushAddr = (start / FLUSH_GRANULARITY) * FLUSH_GRANULARITY;
        long endFlushAddr = (end / FLUSH_GRANULARITY) * FLUSH_GRANULARITY;
        addressRanges.put(startFlushAddr, 0L);
        if (startFlushAddr != endFlushAddr) addressRanges.put(endFlushAddr, 0L);
    }

    void markDirty() {
        if (isFlushed()) {
            if (nativeSetFlushed(this.addr, FLUSH_FLAG_OFFSET, DIRTY) < 0) {
                throw new PersistenceException("Failed to mark region as dirty!");
            }
        }
    }

    @Override
    public long getAddress(long offset) {
        return getLong(offset);
    }

    @Override
    public boolean isFlushed() {
        return nativeGetBits(this.addr, FLUSH_FLAG_OFFSET, (int)FLUSH_FLAG_LENGTH) == FLUSHED ? true : false;
    }

    @Override
    public void flush() {
        if (addressRanges == null) {
            addressRanges = new ConcurrentHashMap<Long, Long>();
        }
        addressRanges.forEach((Long address, Long nulls) -> {
            nativeFlush(this.addr, address, FLUSH_GRANULARITY);
        });
        addressRanges = new ConcurrentHashMap<Long, Long>();
        if (nativeSetFlushed(this.addr, FLUSH_FLAG_OFFSET, FLUSHED) < 0) {
            throw new PersistenceException("Failed to mark region as flushed!");
        }
    }

    @Override
    public void putByte(long offset, byte value) {
        markDirty();
        Heap.UNSAFE.putByte(directAddress + offset, value);
        addToMemoryRanges(offset, Byte.BYTES);
    }

    @Override
    public void putShort(long offset, short value) {
        markDirty();
        Heap.UNSAFE.putShort(directAddress + offset, value);
        addToMemoryRanges(offset, Short.BYTES);
    }

    @Override
    public void putInt(long offset, int value) {
        markDirty();
        Heap.UNSAFE.putInt(directAddress + offset, value);
        addToMemoryRanges(offset, Integer.BYTES);
    }

    @Override
    public void putLong(long offset, long value) {
        markDirty();
        Heap.UNSAFE.putLong(directAddress + offset, value);
        addToMemoryRanges(offset, Long.BYTES);
    }

    @Override
    public void copyFromMemory(MemoryRegion<?> srcRegion, long srcOffset, long dstOffset, long length) {
        markDirty();
        nativeMemoryRegionMemcpyRaw(srcRegion.addr(), srcRegion.baseOffset() + srcOffset, addr(), baseOffset() + dstOffset, length);
        addToMemoryRanges(dstOffset, length);
    }

    @Override
    public void copyFromArray(byte[] srcArray, int srcOffset, long dstOffset, int length) {
        markDirty();
        nativeFromByteArrayMemcpyRaw(srcArray, srcOffset, addr(), baseOffset() + dstOffset, length);
        addToMemoryRanges(dstOffset, length);
    }

    @Override
    public void setMemory(byte val, long offset, long length) {
        markDirty();
        nativeMemoryRegionMemsetRaw(addr(), baseOffset() + offset, val, length);
        addToMemoryRanges(offset, length);
    }

    @Override
    public long baseOffset() { return METADATA_SIZE; }

    protected native void nativeFlush(long regionOffset, long offset, long size);
    protected native void nativePutBits(long regionOffset, long offset, long value, int size);
    protected synchronized native int nativeSetFlushed(long regionOffset, long flushOffset, int value);
}
