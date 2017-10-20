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

package lib.util.persistent;

import lib.util.persistent.types.Types;
import lib.util.persistent.types.PersistentType;
import lib.util.persistent.spi.PersistentMemoryProvider;
import lib.xpersistent.UncheckedPersistentMemoryRegion;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import lib.xpersistent.XRoot;
import static lib.util.persistent.Trace.*;
import java.util.concurrent.atomic.AtomicReference;

public class ClassInfo {
    private static Map<String, ClassInfo> classInfo;
    private static Map<Long, ClassInfo> reverseClassInfo;
    protected static AtomicReference<ClassInfo> lastClassInfo = new AtomicReference<>(null);
    private static final PersistentHeap heap = PersistentMemoryProvider.getDefaultProvider().getHeap();
    // field offsets and allocation size
    private static final int CLASS_NAME = 0;  
    private static final int NEXT_CLASS_INFO = 8; 
    private static final int ALLOCATION_SIZE = 16;

    private final MemoryRegion region;
    private String className;

    // constructor
    public ClassInfo(String className) {
        this.className = className;
        final Box<MemoryRegion> box = new Box<>();
        Transaction.run(() -> {
            MemoryRegion r = box.set(heap.allocateRegion(allocationSize()));
            RawString rs = new RawString(className);
            r.putLong(CLASS_NAME, rs.getRegion().addr());
            r.putLong(NEXT_CLASS_INFO, 0);
        });
        this.region = box.get();
    }

    // reconstuctor
    public ClassInfo(MemoryRegion region) {
        this.region = region;
        this.className = className();
    }

    public static synchronized ClassInfo getClassInfo(String className) {
        ClassInfo ci = classInfo.get(className);
        // System.out.println("hit on " + className + ", classInfo = " + classInfo);
        if (ci == null) {
            // trace(true, "----------------------------------------------------------- miss on " + className);
            Transaction.runOuter(() -> {
                ClassInfo nci = new ClassInfo(className);
                if (lastClassInfo.get() != null) {
                    lastClassInfo.get().setNextClassInfoAddr(nci.getRegion().addr());
                    // trace(true, "linked %s (%d) -> %s (%d)", lastClassInfo, lastClassInfo.get().getRegion().addr(), nci, nci.getRegion().addr());
                    // assert(lastClassInfo.get().getNextClassInfoAddr() == nci.getRegion().addr());
                }
                classInfo.put(className, nci);
                reverseClassInfo.put(nci.getRegion().addr(), nci);
                lastClassInfo.set(nci);
                // trace(true, "set lastClassInfo to %s", nci);
                // trace(true, "classInfo.size() = %d", classInfo.size());
            }); 
        }
        return ci == null ? classInfo.get(className) : ci;
    }

    public static synchronized ClassInfo getClassInfo(long addr) {
        // System.out.println("getClassInfo for addr " + addr);
        return reverseClassInfo.get(addr);
    }

    public synchronized static void init() {
        // System.out.println("ClassInfo.init() enter");
        // rebuild classInfo map
        classInfo = new ConcurrentHashMap<>();
        reverseClassInfo = new ConcurrentHashMap<>();
        XRoot root = (XRoot)heap.getRoot();
        ClassInfo ci = null;
        long rootClassInfoAddr = root.getRootClassInfoAddr();
        // System.out.println("root address = " + rootClassInfoAddr);
        if (rootClassInfoAddr == 0) {
            ci = new ClassInfo("lib.util.persistent.PersistentString");
            rootClassInfoAddr = ci.getRegion().addr();
            // trace(true, "root address now = " + rootClassInfoAddr);
            root.setRootClassInfoAddr(rootClassInfoAddr);
        }
        else ci = new ClassInfo(new UncheckedPersistentMemoryRegion(rootClassInfoAddr));
        lastClassInfo.set(ci);
        classInfo.put(ci.className(), ci);
        reverseClassInfo.put(ci.getRegion().addr(), ci);
        long nextAddr = ci.getNextClassInfoAddr();
        // trace(true, "nextAddr = " + nextAddr);
        while(nextAddr != 0) {
            ClassInfo old = ci;
            ci = new ClassInfo(new UncheckedPersistentMemoryRegion(nextAddr));
            // trace(true, "%s (%d) has next -> %s (%d)", old, old.getRegion().addr(), ci, ci.getRegion().addr());
            classInfo.put(ci.className(), ci);
            reverseClassInfo.put(ci.getRegion().addr(), ci);
            lastClassInfo.set(ci);
            nextAddr = ci.getNextClassInfoAddr();
        }
        // trace(true, "%s (%d) had nextClassInfo of zero", ci, ci.getRegion().addr());
        // System.out.println("ClassInfo map size = " + classInfo.size());
        // System.out.println("ClassInfo.init() exit");
    }        

    public MemoryRegion getRegion() {return region;}

    public String className() {
        if (className != null) return className;
        className = new RawString(new UncheckedPersistentMemoryRegion(region.getLong(CLASS_NAME))).toString();
        return className;
    }

    public long getClassNameAddr() {
        return region.getLong(CLASS_NAME);
    }


    public synchronized long getNextClassInfoAddr() {
        return region.getLong(NEXT_CLASS_INFO);
    }

    public synchronized void setNextClassInfoAddr(long addr) {
        Transaction.run(() -> {
            region.putLong(NEXT_CLASS_INFO, addr);
        });
    }

    private int allocationSize() {return ALLOCATION_SIZE;}

    public String toString() {
        return "ClassInfo(" + className + ")";
    }

}