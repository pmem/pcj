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

import java.util.HashMap;
import java.io.File;

import sun.misc.Unsafe;

public class Heap {
    static {
        System.loadLibrary("llpl");
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
    private static HashMap<String, Heap> heaps = new HashMap<>();

    public synchronized static Heap getHeap(String path, long size) {
        Heap heap;
        if (heaps.get(path) == null) {
            heap = new Heap(path, size);
            heaps.put(path, heap);
        } else {
            heap = heaps.get(path);
        }
        return heap;
    }

    private boolean open;
    private String path;

    private Heap(String path, long size) {
        if (open) return;
        this.path = path;
        this.open = true;
        nativeOpenHeap(path, size);
    }

    public static boolean exists(String path) {
        if (heaps.get(path) != null) return true;
        else return new File(path).exists();
    }

    @SuppressWarnings("unchecked")
    public <K extends MemoryRegion.Kind> MemoryRegion<K> allocateMemoryRegion(Class<K> kind, long size) {
        if (kind == Raw.class)
            return (MemoryRegion<K>)new RawMemoryRegion(this, size, false);
        else if (kind == Flushable.class)
            return (MemoryRegion<K>)new FlushableMemoryRegion(this, size, false);
        else if (kind == Transactional.class)
            return (MemoryRegion<K>)new TransactionalMemoryRegion(this, size, false);
        else throw new IllegalArgumentException("Kind must be one of Raw, Flushable, or Transactional!");
    }

    @SuppressWarnings("unchecked")
    public <K extends MemoryRegion.Kind> MemoryRegion<K> memoryRegionFromAddress(Class<K> kind, long addr) {
        if (kind == Raw.class)
            return (MemoryRegion<K>)new RawMemoryRegion(this, addr, true);
        else if (kind == Flushable.class)
            return (MemoryRegion<K>)new FlushableMemoryRegion(this, addr, true);
        else if (kind == Transactional.class)
            return (MemoryRegion<K>)new TransactionalMemoryRegion(this, addr, true);
        else throw new IllegalArgumentException("Kind must be one of Raw, Flushable, or Transactional!");
    }

    @SuppressWarnings("unchecked")
    public <K extends MemoryRegion.Kind> MemoryRegion<K> reallocateMemoryRegion(Class<K> kind, MemoryRegion<K> region, long newSize) {
        if (newSize == 0) {
            freeMemoryRegion(region);
            return null;
        }

        try {
            MemoryRegion<?>[] ret = new MemoryRegion[1];
            Transaction.run(() -> {
                ret[0] = allocateMemoryRegion(kind, newSize);
                ret[0].copyFromMemory(region, 0, 0, Math.min(newSize, region.size()));
                if (region != null) freeMemoryRegion(region);
            });
            return (MemoryRegion<K>)ret[0];
        } catch (Exception e) {
            throw new PersistenceException("Failed to reallocate MemoryRegion of size " + newSize + "!");
        }
    }

    public void freeMemoryRegion(MemoryRegion<?> region) {
        if (nativeFree(region.addr()) < 0) {
            throw new PersistenceException("Failed to free region!");
        }
        if (region instanceof AbstractMemoryRegion)
            ((AbstractMemoryRegion)region).addr(0);
    }

    public long getRoot() {
        return nativeGetRoot();
    }

    public void setRoot(long val) {
        if (nativeSetRoot(val) != 0) {
            throw new PersistenceException("Failed to set root to " + val + "!");
        }
    }

    public void copyMemory(MemoryRegion<?> srcRegion, long srcOffset, MemoryRegion<?> dstRegion, long dstOffset, long length) {
        dstRegion.copyFromMemory(srcRegion, srcOffset, dstOffset, length);
    }

    public void copyToArray(MemoryRegion<?> srcRegion, long srcOffset, byte[] dstArray, int dstOffset, int length) {
        long srcAddress = ((AbstractMemoryRegion)srcRegion).directAddress + srcOffset;
        long dstAddressOffset = UNSAFE.ARRAY_BYTE_BASE_OFFSET + UNSAFE.ARRAY_BYTE_INDEX_SCALE * dstOffset;
        UNSAFE.copyMemory(null, srcAddress, dstArray, dstAddressOffset, length);
    }

    public void copyFromArray(byte[] srcArray, int srcOffset, MemoryRegion<?> dstRegion, long dstOffset, int length) {
        dstRegion.copyFromArray(srcArray, srcOffset, dstOffset, length);
    }

    public void setMemory(MemoryRegion<?> region, byte val, long offset, long length) {
        region.setMemory(val, offset, length);
    }

    synchronized native long nativeAllocate(long size);
    private synchronized native void nativeOpenHeap(String path, long size);
    private synchronized native int nativeSetRoot(long val);
    private synchronized native int nativeRealloc(long offset, long newSize);
    private synchronized native int nativeFree(long addr);
    private native long nativeGetRoot();
}
