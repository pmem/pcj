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
        // System.out.println("directAddress = " + directAddress);
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

    public byte[] getBytes(long offset, int length) {
        checkAddress();
        byte[] bytes = new byte[length];
        long srcAddress = directAddress + offset;
        long destOffset = XHeap.UNSAFE.ARRAY_BYTE_BASE_OFFSET;// + XHeap.UNSAFE.ARRAY_BYTE_INDEX_SCALE * 0;
        XHeap.UNSAFE.copyMemory(null, srcAddress, bytes, destOffset, length);
        return bytes;
    }


    // transational

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


    // durable

    public void putDurableByte(long offset, byte value) {
        checkAddress();
        nativePutDurableByte(directAddress + offset, value);
    }

    public void putDurableShort(long offset, short value) {
        checkAddress();
        nativePutDurableShort(directAddress + offset, value);
    }

    public void putDurableInt(long offset, int value) {
        checkAddress();
        nativePutDurableInt(directAddress + offset, value);
    }

    public void putDurableLong(long offset, long value) {
        checkAddress();
        nativePutDurableLong(directAddress + offset, value);
    }


    // raw

    public void putRawByte(long offset, byte value) {
        checkAddress();
        XHeap.UNSAFE.putByte(directAddress + offset, value);
    }

    public void putRawShort(long offset, short value) {
        checkAddress();
        XHeap.UNSAFE.putShort(directAddress + offset, value);
    }

    public void putRawInt(long offset, int value) {
        checkAddress();
        XHeap.UNSAFE.putInt(directAddress + offset, value);
    }

    public void putRawLong(long offset, long value) {
        checkAddress();
        XHeap.UNSAFE.putLong(directAddress + offset, value);
    }

    public void putRawBytes(long offset, byte[] bytes) {
        checkAddress();
        long srcOffset = XHeap.UNSAFE.ARRAY_BYTE_BASE_OFFSET + XHeap.UNSAFE.ARRAY_BYTE_INDEX_SCALE * 0;
        long destAddress = directAddress + offset;
        XHeap.UNSAFE.copyMemory(bytes, srcOffset, null, destAddress, bytes.length);
    }

    public void flush(long offset, long size) {
        checkAddress();
        nativeFlush(directAddress + offset, size);
    }

    public void flush(long size) {
        flush(0, size);
    }

    public String toString() {return "UncheckedPersistentMemoryRegion(" + addr + ")";}

    // transactional
    private native void nativePutByte(long address, byte value);
    private native void nativePutShort(long address, short value);
    private native void nativePutInt(long address, int value);
    private native void nativePutLong(long address, long value);

    // durable
    private native void nativePutDurableByte(long address, byte value);
    private native void nativePutDurableShort(long address, short value);
    private native void nativePutDurableInt(long address, int value);
    private native void nativePutDurableLong(long address, long value);

    private native void nativeFlush(long address, long size);
    native void addToTransaction(long address, long size);
    private native long getDirectAddress(long regionOffset);
}
