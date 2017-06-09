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

public class Heap {
    static {
        System.loadLibrary("MemoryRegion");
    }

    private static HashMap<String, Heap> heaps = new HashMap<>();

    public synchronized static Heap getHeap(String name) {
        Heap heap;
        if (heaps.get(name) == null) {
            heap = new Heap(name);
            heaps.put(name, heap);
        } else {
            heap = heaps.get(name);
        }
        return heap;
    }

    private boolean open;
    private String name;

    private Heap(String name) {
        if (open) return;
        this.name = name;
        this.open = true;
        nativeOpenHeap(name);
    }

    public FlushableMemoryRegion allocateFlushableMemoryRegion(long size) {
        long addr = nativeGetMemoryRegion(size + FlushableMemoryRegion.FLUSH_FLAG_SIZE);
        if (addr == 0) throw new PersistenceException("Failed to create FlushableMemoryRegion of size " + size + "!");
        return new FlushableMemoryRegion(addr);
    }

    public TransactionalMemoryRegion allocateTransactionalMemoryRegion(long size) {
        long addr = nativeGetMemoryRegion(size);
        if (addr == 0) throw new PersistenceException("Failed to create TransactionalMemoryRegion of size " + size + "!");
        return new TransactionalMemoryRegion(addr);
    }

    public RawMemoryRegion allocateRawMemoryRegion(long size) {
        long addr = nativeGetMemoryRegion(size);
        if (addr == 0) throw new PersistenceException("Failed to create RawMemoryRegion of size " + size + "!");
        return new RawMemoryRegion(addr);
    }

    public FlushableMemoryRegion flushableRegionFromAddress(long addr) {
        return new FlushableMemoryRegion(addr);
    }

    public TransactionalMemoryRegion transactionalRegionFromAddress(long addr) {
        return new TransactionalMemoryRegion(addr);
    }

    public RawMemoryRegion rawRegionFromAddress(long addr) {
        return new RawMemoryRegion(addr);
    }

    public void freeRegion(MemoryRegion region) {
        if (nativeFree(region.addr()) < 0) {
            throw new PersistenceException("Failed to free region!");
        }
    }

    public long getRoot() {
        return nativeGetRoot();
    }

    public void setRoot(long val) {
        if (nativeSetRoot(val) != 0) {
            throw new PersistenceException("Failed to set root to " + val + "!");
        }
    }

    private synchronized native void nativeOpenHeap(String name);
    private synchronized native long nativeGetMemoryRegion(long size);
    private synchronized native int nativeSetRoot(long val);
    private synchronized native long nativeGetRoot();
    private synchronized native int nativeFree(long addr);
}
