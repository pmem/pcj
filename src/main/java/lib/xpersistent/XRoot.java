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
import lib.xpersistent.PersistentConcurrentHashMapInternal.EntryIterator;
import lib.util.persistent.spi.*;
import java.util.HashSet;
import lib.util.persistent.PersistentLong;

public final class XRoot implements Root {
    private final MemoryRegion region;
    private final XHeap heap;
    private final long ROOT_SIZE = 32;   // 4 objects, each represented by an 8-byte pointer

    private HashSet<Long> candidatesSet;

    private final PersistentHashMap<PersistentString, AnyPersistent> objectDirectory;

    PersistentConcurrentHashMapInternal vmOffsets;
    PersistentConcurrentHashMapInternal prevVMOffsets;
    PersistentConcurrentHashMapInternal allObjects;
    PersistentConcurrentHashMapInternal candidates;

    @SuppressWarnings("unchecked")
    public XRoot(XHeap heap) {
        this.heap = heap;
        if (nativeRootExists()) {
            region = new UncheckedPersistentMemoryRegion(nativeGetRootOffset());
            objectDirectory = PersistentObject.fromPointer(new ObjectPointer<PersistentHashMap>(PersistentHashMap.TYPE, new UncheckedPersistentMemoryRegion(region.getLong(0))));
            this.prevVMOffsets = new PersistentConcurrentHashMapInternal(region.getLong(8));
            this.vmOffsets = new PersistentConcurrentHashMapInternal();
            region.putLong(8, this.vmOffsets.addr());
            allObjects = new PersistentConcurrentHashMapInternal(region.getLong(16), true);
            candidates = new PersistentConcurrentHashMapInternal(region.getLong(24), true);
        } else {
            region = new UncheckedPersistentMemoryRegion(nativeCreateRoot(ROOT_SIZE));
            MemoryRegion objectDirectoryRegion = heap.allocateRegion(PersistentHashMap.TYPE.getAllocationSize());
            objectDirectory = PersistentObject.fromPointer(new ObjectPointer<>(PersistentHashMap.TYPE, objectDirectoryRegion));
            region.putLong(0, objectDirectoryRegion.addr());
            this.vmOffsets = new PersistentConcurrentHashMapInternal();
            this.prevVMOffsets = null;
            region.putLong(8, this.vmOffsets.addr());
            this.allObjects = new PersistentConcurrentHashMapInternal();
            region.putLong(16, this.allObjects.addr());
            this.candidates = new PersistentConcurrentHashMapInternal();
            region.putLong(24, this.candidates.addr());
        }
    }

    public PersistentHashMap<PersistentString, AnyPersistent> getObjectDirectory() { return objectDirectory; }

    synchronized static native boolean nativeRootExists();
    synchronized static native long nativeGetRootOffset();
    synchronized static native long nativeCreateRoot(long size);

    public void addToAllObjects(long addr) {
        // allObjects.put(addr, 0);
    }

    public void removeFromAllObjects(long addr) {
        // allObjects.remove(addr);
    }

    public void printAllObjects() {
        // nativePrintAllObjects(this.allObjects, this.vmOffsets);
    }

    public void addToCandidates(long addr) {
        candidates.put(addr, 0);
    }

    public void removeFromCandidates(long addr) {
        candidates.remove(addr);
    }

    void cleanVMOffsets() {
        if (prevVMOffsets != null) {
            PersistentConcurrentHashMapInternal.EntryIterator iter = prevVMOffsets.iter();
            while (iter.hasNext()) {
                PersistentConcurrentHashMapInternal.NodeLL node = iter.next();
                AnyPersistent.deleteResidualReferences(node.getKey(), (int)node.getValue());
            }
            prevVMOffsets.delete();
        }
    }

    public void registerObject(long addr) {
        vmOffsets.increment(addr);
    }

    public void deregisterObject(long addr) {
        vmOffsets.decrement(addr);
    }

    public PersistentConcurrentHashMapInternal getCandidates() {
        PersistentConcurrentHashMapInternal oldCandidates = this.candidates;
        this.candidates = new PersistentConcurrentHashMapInternal();
        this.region.putLong(24, this.candidates.addr());
        return oldCandidates;
    }

    public void debugVMOffsets() {
        vmOffsets.debugFromHead();
    }
}
