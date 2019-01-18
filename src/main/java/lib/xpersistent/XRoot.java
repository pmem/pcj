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
import java.util.ArrayList;
import lib.util.persistent.PersistentLong;

public final class XRoot implements Root {
    private static final int OBJECT_DIRECTORY_OFFSET = 0;
    private static final int CLASS_INFO_OFFSET = 8;
    private static final long ROOT_SIZE = 16;   // 2 objects, each represented by an 8-byte pointer

    private final MemoryRegion region;
    private final PersistentHashMap<PersistentString, AnyPersistent> objectDirectory;
    private long rootClassInfoAddr;

    @SuppressWarnings("unchecked")
    XRoot(XHeap heap) {
        if (nativeRootExists()) {
            region = new UncheckedPersistentMemoryRegion(nativeGetRootOffset());
            objectDirectory = PersistentObject.fromPointer(new ObjectPointer<PersistentHashMap>(PersistentHashMap.TYPE, new UncheckedPersistentMemoryRegion(region.getLong(OBJECT_DIRECTORY_OFFSET))));
            rootClassInfoAddr = region.getLong(CLASS_INFO_OFFSET);
        } else {
            region = new UncheckedPersistentMemoryRegion(nativeCreateRoot(ROOT_SIZE));
            MemoryRegion objectDirectoryRegion = heap.allocateRegion(PersistentHashMap.TYPE.allocationSize());
            // TODO: HACK to make object directory have non-zero refCount
            objectDirectoryRegion.putDurableInt(8, 1);
            objectDirectory = PersistentObject.fromPointer(new ObjectPointer<>(PersistentHashMap.TYPE, objectDirectoryRegion));
            Transaction.run(() -> {
                region.putLong(OBJECT_DIRECTORY_OFFSET, objectDirectoryRegion.addr());
                this.rootClassInfoAddr = 0;
                region.putLong(CLASS_INFO_OFFSET, rootClassInfoAddr);
            });
        }
    }

    public PersistentHashMap<PersistentString, AnyPersistent> getObjectDirectory() { return objectDirectory; }

    void clean() {
        nativeCleanHeap();
    }

    // TODO: should not be public
    public long getRootClassInfoAddr() {return rootClassInfoAddr;}

    // TODO: should not be public
    public void setRootClassInfoAddr(long addr) {
        rootClassInfoAddr = addr;
        Transaction.run(() -> { region.putLong(CLASS_INFO_OFFSET, rootClassInfoAddr); });
    }

    synchronized static native boolean nativeRootExists();
    synchronized static native long nativeGetRootOffset();
    synchronized static native long nativeCreateRoot(long size);
    native void nativeCleanHeap();
}
