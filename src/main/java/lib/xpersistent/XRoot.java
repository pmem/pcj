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

import lib.util.persistent.*;
import lib.util.persistent.types.*;
import lib.util.persistent.spi.*;
import java.util.HashSet;
import lib.util.persistent.PersistentLong;

public final class XRoot implements Root {
    private final MemoryRegion region;
    private final XHeap heap;
    private final long ROOT_SIZE = 32;   // 4 objects, each represented by an 8-byte pointer

    private HashSet<Long> candidatesSet;

    private final PersistentHashMap<PersistentString, PersistentObject> objectDirectory;
    private long vmOffsets;     // represented as the raw offset to vmOffsets hashmap
    private long prevVMOffsets; // VMOffsets hashmap of the previous VM iteration
    private long allObjects;    // represented as the raw offset to allObjects hashmap
    private long candidates;    // represented as the raw offset to candidates hashmap

    private PersistentLong registrationLock;
    private PersistentLong candidatesLock;

    @SuppressWarnings("unchecked")
    public XRoot(XHeap heap) {
        this.heap = heap;
        if (nativeRootExists()) {
            region = new UncheckedPersistentMemoryRegion(nativeGetRootOffset());
            objectDirectory = PersistentObject.fromPointer(new ObjectPointer<PersistentHashMap>(PersistentHashMap.TYPE, new UncheckedPersistentMemoryRegion(region.getLong(0))));
            this.prevVMOffsets = region.getLong(8);
            this.vmOffsets = nativeAllocateHashmap();
            region.putLong(8, this.vmOffsets);
            allObjects = region.getLong(16);
            candidates = region.getLong(24);
        } else {
            region = new UncheckedPersistentMemoryRegion(nativeCreateRoot(ROOT_SIZE));
            MemoryRegion objectDirectoryRegion = heap.allocateRegion(PersistentHashMap.TYPE.getAllocationSize());
            objectDirectory = PersistentObject.fromPointer(new ObjectPointer<>(PersistentHashMap.TYPE, objectDirectoryRegion));
            region.putLong(0, objectDirectoryRegion.addr());
            this.vmOffsets = nativeAllocateHashmap();
            this.prevVMOffsets = 0;
            region.putLong(8, this.vmOffsets);
            this.allObjects = nativeAllocateHashmap();
            region.putLong(16, this.allObjects);
            this.candidates = nativeAllocateHashmap();
            region.putLong(24, this.candidates);
        }
    }

    public void init() {
        registrationLock = new PersistentLong(333333333);
        candidatesLock = new PersistentLong(444444444);
    }        

    public PersistentHashMap<PersistentString, PersistentObject> getObjectDirectory() { return objectDirectory; }

    synchronized static native boolean nativeRootExists();
    synchronized static native long nativeGetRootOffset();
    synchronized static native long nativeCreateRoot(long size);
    synchronized static native long nativeAllocateHashmap();
    synchronized native long nativeHashmapPut(long mapOffset, long key, long value);
    synchronized native long nativeHashmapGet(long mapOffset, long key);
    synchronized native long nativeHashmapRemove(long mapOffset, long key);
    synchronized native void nativeHashmapDebug(long mapOffset);
    synchronized native void nativeHashmapClear(long mapOffset);
    synchronized native void nativePrintAllObjects(long allObjects, long vmOffsets);
    synchronized native void nativeCleanVMOffsets(long vmOffsets, long allObjects);
    synchronized native void nativeImportCandidates(long candidatesOffset, XRoot candidates);

    public void addToAllObjects(long addr) {
        nativeHashmapPut(this.allObjects, addr, 0);
    }

    public void removeFromAllObjects(long addr) {
        nativeHashmapRemove(this.allObjects, addr);
    }

    public void printAllObjects() {
        nativePrintAllObjects(this.allObjects, this.vmOffsets);
    }

    public void addToCandidates(long addr) {
        Transaction.run(() -> {
            nativeHashmapPut(this.candidates, addr, 0);
        }, candidatesLock);
    }

    public void removeFromCandidates(long addr) {
        nativeHashmapRemove(this.candidates, addr);
    }

    void cleanVMOffsets() {
        if (prevVMOffsets != 0)
            nativeCleanVMOffsets(prevVMOffsets, allObjects);
    }

    public void registerObject(long addr) {
        Transaction.run(() -> {
            nativeHashmapPut(vmOffsets, addr, (nativeHashmapGet(vmOffsets, addr)) + 1);
        }, registrationLock);
    }

    public void deregisterObject(long addr) {
        Transaction.run(() -> {
            long value = nativeHashmapGet(vmOffsets, addr);
            long newValue = value - 1;
            if (newValue <= 0) {
                nativeHashmapRemove(vmOffsets, addr);
            } else {
                nativeHashmapPut(vmOffsets, addr, newValue);
            }
        }, registrationLock);
    }

    public HashSet<Long> importCandidates() {
        this.candidatesSet = new HashSet<>();
        nativeImportCandidates(this.candidates, this);
        return this.candidatesSet;
    }

    void addToCandidatesFromNative(long addr) {
        Transaction.run(() -> {
            this.candidatesSet.add(addr);
        });
    }

    public void clearCandidates() {
        Transaction.run(() -> {
            nativeHashmapClear(this.candidates);
        });
    }
}
