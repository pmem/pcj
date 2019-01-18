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

import lib.util.persistent.PersistentHeap;
import lib.util.persistent.MemoryRegion;
import lib.util.persistent.PersistenceException;
import lib.util.persistent.spi.PersistentMemoryProvider;
import lib.util.persistent.Root;
import lib.util.persistent.CycleCollector;
import lib.util.persistent.Transaction;
import lib.util.persistent.ObjectDirectory;
import lib.util.persistent.Util;

import java.util.Properties;
import java.io.FileInputStream;
import sun.misc.Unsafe;

public class XHeap implements PersistentHeap {
    static {
        System.loadLibrary("Persistent");
        try {
            java.lang.reflect.Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (Unsafe)f.get(null);
        }
        catch (Exception e) {
            throw new RuntimeException("Unable to initialize UNSAFE.");
        }
    }

    static Unsafe UNSAFE;
    private Root root;
    private boolean open;
    private boolean debug;

    public XHeap() {
        this.debug = false;
        //open();
    }

    public synchronized void open() {
        if (open) return;
        this.open = true;

        String path;
        long size;

        try {
            FileInputStream propInput = new FileInputStream("config.properties");
            Properties prop = new Properties();
            prop.load(propInput);

            path = prop.getProperty("path");
            size = Long.parseLong(prop.getProperty("size"));
        } catch (Exception e) {
            System.out.println("Could not properly load config.properties file; using default values for pool path (/mnt/mem/persistent_heap) and size (2GB).");
            path = "/mnt/mem/persistent_heap";
            size = 2L*1024L*1024L*1024L;
        }

        System.out.print("Opening heap... ");
        nativeOpenHeap(path, size);
        lib.util.persistent.ClassInfo.init();
        System.out.print("Cleaning up heap... ");
        cleanHeap();
        System.out.println("Heap opened.");
    }

    public synchronized void close() {
        if (!open) return;
        this.open = false;
        nativeCloseHeap();
    }

    public MemoryRegion allocateRegion(long size) {
        if (!open) open();
        return Transaction.run(() -> {
            long addr = nativeAllocate(size);
            // System.out.println("allocateRegion -> " + addr);
            if (addr == -1) throw new PersistenceException("Failed to allocate region of size " + size);
            return new UncheckedPersistentMemoryRegion(addr);
        });
    }

    public MemoryRegion allocateRegionAtomic(long size) {
        if (!open) open();
        long addr = nativeAllocateAtomic(size);
        if (addr == -1) throw new PersistenceException("Failed to allocate region of size " + size);
        return new UncheckedPersistentMemoryRegion(addr);
    }

    public MemoryRegion allocateObjectRegion(long size) {
        if (!open) open();
        long addr = nativeAllocateObject(size);
        // System.out.println("allocateObjectRegion -> " + addr);
        if (addr == -1) throw new PersistenceException("Failed to allocate object region of size " + size);
        return new UncheckedPersistentMemoryRegion(addr);
    }

    public void freeRegion(MemoryRegion region) {
        if (!open) open();
        Transaction.run(() -> {
            if (nativeFree(region.addr()) != 0) throw new PersistenceException("Failed to free region");
        });
    }

    public synchronized Root getRoot() {
        if (root == null) {
            if (!open) open();
            if (root == null) root = new XRoot(this);
            ObjectDirectory.initialize();
        }
        return root;
    }

    public void memcpy(MemoryRegion srcRegion, long srcOffset, MemoryRegion destRegion, long destOffset, long length) {
        nativeMemoryRegionMemcpy(srcRegion.addr(), srcOffset, destRegion.addr(), destOffset, length);
    }

    public void memcpy(MemoryRegion srcRegion, long srcOffset, byte[] destArray, int destOffset, int length) {
        long srcAddress = ((UncheckedPersistentMemoryRegion)srcRegion).directAddress + srcOffset;
        long destAddressOffset = UNSAFE.ARRAY_BYTE_BASE_OFFSET + UNSAFE.ARRAY_BYTE_INDEX_SCALE * destOffset;
        UNSAFE.copyMemory(null, srcAddress, destArray, destAddressOffset, length);
    }

    public void memcpy(byte[] srcArray, int srcOffset, MemoryRegion destRegion, long destOffset, int length) {
        nativeFromByteArrayMemcpy(srcArray, srcOffset, destRegion.addr(), destOffset, length);
    }

    // must be called from within a transaction
    public void copyBytesToRegion(byte[] bytes, int startIndex, MemoryRegion destRegion, long destOffset, int length) {
        UncheckedPersistentMemoryRegion xregion = (UncheckedPersistentMemoryRegion)destRegion;
        long srcAddress = XHeap.UNSAFE.ARRAY_BYTE_BASE_OFFSET + XHeap.UNSAFE.ARRAY_BYTE_INDEX_SCALE * startIndex;
        long destAddress = xregion.directAddress + destOffset;
        xregion.addToTransaction(destAddress, length);
        XHeap.UNSAFE.copyMemory(bytes, srcAddress, null, destAddress, length);
    }

    public void setDebugMode(boolean debug) {
        this.debug = debug;
    }
    public boolean getDebugMode() { return this.debug; }

    public long debug() {
        return debug(false);
    }

    public long debug(boolean verbose) {
        return nativeDebugPool(verbose);
    }

    private void cleanHeap() {
        XRoot rt = (XRoot)(getRoot());
        rt.clean();
        //CycleCollector.collect();
    }

    private synchronized native void nativeOpenHeap(String path, long size);
    private synchronized native void nativeCloseHeap();
    private native long nativeAllocate(long size);
    private native long nativeAllocateAtomic(long size);
    private native long nativeAllocateObject(long size);
    private native int nativeFree(long addr);
    private synchronized native void nativeMemoryRegionMemcpy(long srcRegion, long srcOffset, long destRegion, long destOffset, long length);
    private synchronized native void nativeToByteArrayMemcpy(long srcRegion, long srcOffset, byte[] destArray, int destOffset, int length);
    private native void nativeFromByteArrayMemcpy(byte[] srcArray, int srcOffset, long destRegion, long destOffset, int length);
    private synchronized native long nativeDebugPool(boolean verbose);

    private native void nativeCopyBytesToAddress(byte[] srcArray, int srcOffset, long address, int length);
}
