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
import java.util.Iterator;

public class FlushableMemoryRegion extends AbstractMemoryRegion {
    static final long FLUSH_FLAG_OFFSET = 0;    // store the flushed flag as an int at the 0th offset, real data starts at offset 4
    static final long FLUSH_FLAG_SIZE = 4;       // flushed flag is an int
    static final long DATA_OFFSET = 4;          // see above
    static final int FLUSHED = 0;               // need to maintain 0 for default (flushed)
    static final int DIRTY = 1;                 // so "flushed" flag is really "dirty" flag
    static final int FLUSH_GRANULARITY = 64;    // flushing is aligned to 64 byte cache lines

    private ConcurrentHashMap<Long, Long> addressRanges;
    private long volatileAddress;

    FlushableMemoryRegion(long addr) {
        super(addr);
        this.addressRanges = new ConcurrentHashMap<Long, Long>();
        this.volatileAddress = nativeGetVolatileAddress(this.addr);
    }

    public boolean isFlushed() {
        return nativeGetBits(this.addr, FLUSH_FLAG_OFFSET, (int)FLUSH_FLAG_SIZE) == FLUSHED ? true : false;
    }

    public void flush() {
        if (addressRanges == null) {
            addressRanges = new ConcurrentHashMap<Long, Long>();
        }
        addressRanges.forEach((Long address, Long nulls) -> {
            nativeFlush(this.addr, address, FLUSH_GRANULARITY);
        });
        addressRanges = new ConcurrentHashMap<Long, Long>();
        if (nativeSetFlushed(this.addr, FLUSHED) < 0) {
            throw new PersistenceException("Failed to mark region as flushed!");
        }
    }

    private void addToMemoryRanges(long offset, long size) {
        if (addressRanges == null) addressRanges = new ConcurrentHashMap<Long, Long>();
        long start = this.volatileAddress + offset;
        long end = this.volatileAddress + offset + size - 1;
        long startFlushAddr = (start / FLUSH_GRANULARITY) * FLUSH_GRANULARITY;
        long endFlushAddr = (end / FLUSH_GRANULARITY) * FLUSH_GRANULARITY;
        addressRanges.put(startFlushAddr, 0L);
        if (startFlushAddr != endFlushAddr) addressRanges.put(endFlushAddr, 0L);
    }

    @Override
    public long getBits(long offset, long size, boolean isSigned) {
        if (size < 0 || size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid size: " + size);
        }

        long realOffset = offset + DATA_OFFSET;
        long bits = nativeGetBits(this.addr, realOffset, (int)size);

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

    @Override
    public void putBits(long offset, long size, long value) {
        if (size < 0 || size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid size: " + size);
        }

        if (size != 1 && size != 2 && size != 4 && size != 8) {
            throw new IllegalArgumentException("Invalid size: " + size);
        }

        if (isFlushed()) {
            if (nativeSetFlushed(this.addr, DIRTY) < 0) {
                throw new PersistenceException("Failed to mark region as dirty!");
            }
        }
        long realOffset = offset + DATA_OFFSET;
        nativePutBits(this.addr, realOffset, value, (int)size);
        addToMemoryRanges(realOffset, size);
    }

    @Override
    public long getAddress(long offset) {
        return this.addr + offset + DATA_OFFSET;
    }

    protected native void nativeFlush(long regionOffset, long offset, long size);
    protected native void nativePutBits(long regionOffset, long offset, long value, int size);
    protected synchronized native int nativeSetFlushed(long regionOffset, int value);
    protected native long nativeGetVolatileAddress(long regionOffset);
}
