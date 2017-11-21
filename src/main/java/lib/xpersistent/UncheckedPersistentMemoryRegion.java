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

public class UncheckedPersistentMemoryRegion implements MemoryRegion {
    private long addr;
    long directAddress;

    static {
        System.loadLibrary("Persistent");
    }

    public UncheckedPersistentMemoryRegion(long addr) {
        this.addr = addr;
        this.directAddress = getDirectAddress(addr);
    }

    public long addr() {
        return this.addr;
    }

    public void addr(long addr) { 
        this.addr = addr; 
        this.directAddress = getDirectAddress(addr);
    }

    public void checkAccess(int mode) throws IllegalAccessException {}
    public void checkAlive() {}
    public void checkBounds(long offset) throws IndexOutOfBoundsException {}

    private void putBits(long offset, long size, long value) {
        nativePutLong(this.addr, offset, value, (int)size);
    }

    public byte getByte(long offset) {
        return XHeap.UNSAFE.getByte(directAddress + offset);
    }

    public short getShort(long offset) {
        return XHeap.UNSAFE.getShort(directAddress + offset);
    }

    public int getInt(long offset) {
        return XHeap.UNSAFE.getInt(directAddress + offset);
    }

    public long getLong(long offset) {
        return XHeap.UNSAFE.getLong(directAddress + offset);
    }

    public long getAddress(long offset) {
        return getLong(offset);
    }

    public void putByte(long offset, byte value) {
        putBits(offset, 1, value);
    }

    public void putShort(long offset, short value) {
        putBits(offset, 2, value);
    }

    public void putInt(long offset, int value) {
        putBits(offset, 4, value);
    }

    public void putLong(long offset, long value) {
        putBits(offset, 8, value);
    }

    public void putAddress(long offset, long value) {
        putLong(offset, value);
    }

    private native long getDirectAddress(long regionOffset);
    private native void nativePutLong(long regionOffset, long offset, long value, int size);
}
