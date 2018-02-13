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
    private static final int OBJECT_DIRECTORY_OFFSET = 0;
    private static final int PREV_VMOFFSETS_OFFSET = 8;
    private static final int NEW_VMOFFSETS_OFFSET = 16;
    private static final int ALL_OBJECTS_OFFSET = 24;
    private static final int CANDIDATES_OFFSET = 32;
    private static final int OLD_CANDIDATES_OFFSET = 40;
    private static final int CLASS_INFO_OFFSET = 48;
    private static final long ROOT_SIZE = 56;   // 7 objects, each represented by an 8-byte pointer

    private final MemoryRegion region;
    private final XHeap heap;

    private HashSet<Long> candidatesSet;

    private final PersistentHashMap<PersistentString, AnyPersistent> objectDirectory;

    PersistentConcurrentHashMapInternal vmOffsets;
    PersistentConcurrentHashMapInternal prevVMOffsets;
    //PersistentConcurrentHashMapInternal allObjects;
    PersistentConcurrentHashMapInternal candidates;
    long rootClassInfoAddr;

    @SuppressWarnings("unchecked")
    public XRoot(XHeap heap) {
        this.heap = heap;
        if (nativeRootExists()) {
            region = new UncheckedPersistentMemoryRegion(nativeGetRootOffset());
            objectDirectory = PersistentObject.fromPointer(new ObjectPointer<PersistentHashMap>(PersistentHashMap.TYPE, new UncheckedPersistentMemoryRegion(region.getLong(OBJECT_DIRECTORY_OFFSET))));
            this.prevVMOffsets = new PersistentConcurrentHashMapInternal(region.getLong(PREV_VMOFFSETS_OFFSET));
            long newVMOffsetsAddr = region.getLong(NEW_VMOFFSETS_OFFSET);
            this.vmOffsets = newVMOffsetsAddr == 0 ? new PersistentConcurrentHashMapInternal() : new PersistentConcurrentHashMapInternal(newVMOffsetsAddr);
            //allObjects = new PersistentConcurrentHashMapInternal(region.getLong(ALL_OBJECTS_OFFSET), true);
            candidates = new PersistentConcurrentHashMapInternal(region.getLong(CANDIDATES_OFFSET), true);
            if (region.getLong(OLD_CANDIDATES_OFFSET) != 0) {
                PersistentConcurrentHashMapInternal oldCandidates = new PersistentConcurrentHashMapInternal(region.getLong(OLD_CANDIDATES_OFFSET));
                oldCandidates.delete();
                Transaction.run(() -> { region.putLong(OLD_CANDIDATES_OFFSET, 0); });
            }
            rootClassInfoAddr = region.getLong(CLASS_INFO_OFFSET);
        } else {
            region = new UncheckedPersistentMemoryRegion(nativeCreateRoot(ROOT_SIZE));
            MemoryRegion objectDirectoryRegion = heap.allocateRegion(PersistentHashMap.TYPE.getAllocationSize());
            objectDirectory = PersistentObject.fromPointer(new ObjectPointer<>(PersistentHashMap.TYPE, objectDirectoryRegion));
            Transaction.run(() -> {
                region.putLong(OBJECT_DIRECTORY_OFFSET, objectDirectoryRegion.addr());
                this.vmOffsets = new PersistentConcurrentHashMapInternal();
                region.putLong(NEW_VMOFFSETS_OFFSET, this.vmOffsets.addr());
                this.prevVMOffsets = new PersistentConcurrentHashMapInternal();
                region.putLong(PREV_VMOFFSETS_OFFSET, this.prevVMOffsets.addr());
                //this.allObjects = new PersistentConcurrentHashMapInternal();
                //region.putLong(ALL_OBJECTS_OFFSET, this.allObjects.addr());
                this.candidates = new PersistentConcurrentHashMapInternal();
                region.putLong(CANDIDATES_OFFSET, this.candidates.addr());
                region.putLong(OLD_CANDIDATES_OFFSET, 0);
                this.rootClassInfoAddr = 0;
                region.putLong(CLASS_INFO_OFFSET, rootClassInfoAddr);
            });
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
        PersistentConcurrentHashMapInternal.EntryIterator newIter = vmOffsets.iter();
        while (newIter.hasNext()) {
            PersistentConcurrentHashMapInternal.NodeLL node = newIter.next();
            Transaction.run(() -> {
                long oldCount = prevVMOffsets.get(node.getKey());
                long newCount = (oldCount == -1L) ? node.getValue() : (oldCount + node.getValue());
                prevVMOffsets.put(node.getKey(), newCount);
                newIter.remove();
            });
        }

        vmOffsets = Transaction.run(() -> {
            vmOffsets.delete();
            PersistentConcurrentHashMapInternal newVMOffsets = new PersistentConcurrentHashMapInternal();
            region.putLong(NEW_VMOFFSETS_OFFSET, newVMOffsets.addr());
            return newVMOffsets;
        });

        if (prevVMOffsets != null) {
            PersistentConcurrentHashMapInternal.EntryIterator iter = prevVMOffsets.iter();
            while (iter.hasNext()) {
                PersistentConcurrentHashMapInternal.NodeLL node = iter.next();
                Transaction.run(() -> {
                    AnyPersistent.deleteResidualReferences(node.getKey(), (int)node.getValue());
                    iter.remove();
                });
            }
        }
        prevVMOffsets.delete(false);
        Transaction.run(() -> {
            prevVMOffsets.deleteHead();
            region.putLong(PREV_VMOFFSETS_OFFSET, vmOffsets.addr());
            region.putLong(NEW_VMOFFSETS_OFFSET, 0);
        });
    }

    public void registerObject(long addr) {
        vmOffsets.increment(addr);
    }

    public void deregisterObject(long addr) {
        vmOffsets.decrement(addr);
    }

    public PersistentConcurrentHashMapInternal getCandidates() {
        PersistentConcurrentHashMapInternal oldCandidates = this.candidates;
        Transaction.run(() -> {
            this.candidates = new PersistentConcurrentHashMapInternal();
            this.region.putLong(CANDIDATES_OFFSET, this.candidates.addr());
            this.region.putLong(OLD_CANDIDATES_OFFSET, oldCandidates.addr());
        });
        return oldCandidates;
    }

    public void deleteOldCandidates() {
        PersistentConcurrentHashMapInternal oldCandidates = new PersistentConcurrentHashMapInternal(this.region.getLong(OLD_CANDIDATES_OFFSET));
        oldCandidates.delete();
        Transaction.run(() -> { this.region.putLong(OLD_CANDIDATES_OFFSET, 0); });
    }

    public long getRootClassInfoAddr() {return rootClassInfoAddr;}
    public void setRootClassInfoAddr(long addr) {
        rootClassInfoAddr = addr;
        Transaction.run(() -> { region.putLong(CLASS_INFO_OFFSET, rootClassInfoAddr); });
    }

    public void debugVMOffsets() {
        vmOffsets.debugFromHead();
    }
}
