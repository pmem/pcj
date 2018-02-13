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

    // TODO: should not be public
    public UncheckedPersistentMemoryRegion(long addr) {
        this.addr = addr;
        this.directAddress = getDirectAddress(addr);
    }

    // TODO: should not be public
    public void clear() {
        addr = 0;
        directAddress = 0;
    }

    // TODO: should not be public
    public long addr() {
        return this.addr;
    }

    // TODO: should not be public
    public void addr(long addr) {
        this.addr = addr;
        this.directAddress = getDirectAddress(addr);
    }

    public void checkAccess(int mode) throws IllegalAccessException {}
    public void checkAlive() {}
    public void checkBounds(long offset) throws IndexOutOfBoundsException {}

    private void checkAddress() {
        if (directAddress != 0) return;
        throw new IllegalArgumentException();
    }

    public byte getByte(long offset) {
        checkAddress();
        return XHeap.UNSAFE.getByte(directAddress + offset);
    }

    public short getShort(long offset) {
        checkAddress();
        return XHeap.UNSAFE.getShort(directAddress + offset);
    }

    public int getInt(long offset) {
        checkAddress();
		return XHeap.UNSAFE.getInt(directAddress + offset);
    }

    public long getLong(long offset) {
        checkAddress();
		return XHeap.UNSAFE.getLong(directAddress + offset);
    }

    public long getAddress(long offset) {
        return getLong(offset);
    }

    public void putByte(long offset, byte value) {
        checkAddress();
        nativePutByte(directAddress + offset, value);
    }

    public void putShort(long offset, short value) {
        checkAddress();
        nativePutShort(directAddress + offset, value);
    }

    public void putInt(long offset, int value) {
        checkAddress();
        nativePutInt(directAddress + offset, value);
    }

    public void putLong(long offset, long value) {
        checkAddress();
        nativePutLong(directAddress + offset, value);
    }

    public void putAddress(long offset, long value) {
        putLong(offset, value);
    }

    private native long getDirectAddress(long regionOffset);
    private native void nativePutByte(long address, byte value);
    private native void nativePutShort(long address, short value);
    private native void nativePutInt(long address, int value);
    private native void nativePutLong(long address, long value);
}
