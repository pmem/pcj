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

package lib.xpersistent;

import lib.util.persistent.MemoryRegion;
import java.util.concurrent.locks.ReentrantLock;

public class UncheckedPersistentMemoryRegion implements MemoryRegion {
    private long addr;
    private final ReentrantLock lock = new ReentrantLock();

    static {
        System.loadLibrary("Persistent");
    }

    public UncheckedPersistentMemoryRegion(long addr) {
        this.addr = addr;
    }

    public long addr() {
        return this.addr;
    }

    public void checkAccess(int mode) throws IllegalAccessException {}
    public void checkAlive() {}
    public void checkBounds(long offset) throws IndexOutOfBoundsException {}

    public long getBits(long offset, long size, boolean isSigned) {
        if (size < 0 || size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid size: " + size);
        }

        long bits = 0;
        bits = nativeGetLong(this.addr, offset, (int)size);

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

    public void putBits(long offset, long size, long value) {
        if (size < 0 || size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid size: " + size);
        }
        if (size != 1 && size != 2 && size != 4 && size != 8) {
            throw new IllegalArgumentException("Invalid size: " + size);
        }
        nativePutLong(this.addr, offset, value, (int)size);
    }

    public byte getByte(long offset) {
        return (byte)getBits(offset, 1, true);
    }
    public void putByte(long offset, byte value) {
        putBits(offset, 1, value);
    }

    public short getShort(long offset) {
        return (short)getBits(offset, 2, true);
    }
    public void putShort(long offset, short value) {
        putBits(offset, 2, value);
    }

    public int getInt(long offset) {
        return (int)getBits(offset, 4, true);
    }
    public void putInt(long offset, int value) {
        putBits(offset, 4, value);
    }

    public long getLong(long offset) {
        return getBits(offset, 8, true);
    }
    public void putLong(long offset, long value) {
        putBits(offset, 8, value);
    }

    public long getAddress(long offset) {
        return this.addr + offset;
    }
    public void putAddress(long offset, long value) {
        putLong(offset, value);
    }

    private native long nativeGetLong(long regionOffset, long offset, int size);
    private native void nativePutLong(long regionOffset, long offset, long value, int size);
}
