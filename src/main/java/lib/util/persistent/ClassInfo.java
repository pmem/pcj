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
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

public class ClassInfo {
    private static Map<String, ClassInfo> classInfo = new ConcurrentHashMap<>();
    private static Map<Long, ClassInfo> reverseClassInfo = new ConcurrentHashMap<>();
    protected static AtomicReference<ClassInfo> lastClassInfo = new AtomicReference<>(null);
    private static final PersistentHeap heap = PersistentMemoryProvider.getDefaultProvider().getHeap();
    // field offsets and allocation size
    private static final int CLASS_NAME = 0;  
    private static final int NEXT_CLASS_INFO = 8; 
    private static final int ALLOCATION_SIZE = 16;
    public static final String TYPE_FIELD_NAME = "TYPE";
    private static boolean initialized = false;


    private final MemoryRegion region;
    private String className;
    private Class<?> cls;
    private Constructor reconstructor;
    private PersistentType type;

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
        // trace(true, "ClassInfo reconstructor(%s)", region); 
        this.region = region;
        this.className = className();
        initReconstructor(className);
        initType();
    }

    public static boolean isInitialized() {return initialized;}

    public static ClassInfo getClassInfo(String className) {
        // System.out.println("getClassInfo(" + className + ")");
        ClassInfo ci = classInfo.get(className);
        if (ci == null) {
            synchronized(ClassInfo.class) {
                Transaction.runOuter(() -> {
                    ClassInfo nci = new ClassInfo(className);
                    if (lastClassInfo.get() != null) {
                        lastClassInfo.get().setNextClassInfoAddr(nci.getRegion().addr());
                    }
                    classInfo.put(className, nci);
                    reverseClassInfo.put(nci.getRegion().addr(), nci);
                    lastClassInfo.set(nci);
                }); 
            }
        }
        return ci == null ? classInfo.get(className) : ci;
    }

    public static ClassInfo getClassInfo(Class<?> cls) {
        return getClassInfo(cls.getName());
    }

    public static synchronized ClassInfo getClassInfo(long addr) {
        return reverseClassInfo.get(addr);
    }

    public synchronized static void init() {
        // System.out.println("ClassInfo.init() enter");
        // rebuild classInfo map
        XRoot root = (XRoot)heap.getRoot();
        ClassInfo ci = null;
        long rootClassInfoAddr = root.getRootClassInfoAddr();
        if (rootClassInfoAddr == 0) {
            ci = new ClassInfo("lib.util.persistent.PersistentString");
            rootClassInfoAddr = ci.getRegion().addr();
            root.setRootClassInfoAddr(rootClassInfoAddr);
        }
        else ci = new ClassInfo(new UncheckedPersistentMemoryRegion(rootClassInfoAddr));
        lastClassInfo.set(ci);
        classInfo.put(ci.className(), ci);
        reverseClassInfo.put(ci.getRegion().addr(), ci);
        long nextAddr = ci.getNextClassInfoAddr();
        while(nextAddr != 0) {
            ClassInfo old = ci;
            ci = new ClassInfo(new UncheckedPersistentMemoryRegion(nextAddr));
            // trace(true, "%s (%d) has next -> %s (%d)", old, old.getRegion().addr(), ci, ci.getRegion().addr());
            classInfo.put(ci.className(), ci);
            reverseClassInfo.put(ci.getRegion().addr(), ci);
            lastClassInfo.set(ci);
            nextAddr = ci.getNextClassInfoAddr();
        }
        initialized = true;        
        // System.out.println("ClassInfo.init() exit");
    }        


    void initReconstructor(String className) {
        try {
            this.cls = Class.forName(className);
            Constructor ctor = cls.getDeclaredConstructor(ObjectPointer.class);
            ctor.setAccessible(true);
            this.reconstructor = ctor;
        }
        catch (ClassNotFoundException cnf) {throw new RuntimeException("Exception during initReconstructor: " + cnf.getMessage());}
        catch (NoSuchMethodException nsm) {throw new RuntimeException("Exception during initReconstructor: " + nsm.getMessage());}
    }

    public Constructor getReconstructor() {
        if (reconstructor == null) initReconstructor(className);
        return reconstructor;
    }

    void initType() {
        Field typeField = getTypeField(className);
        try {
            type = (PersistentType)typeField.get(null);
        }
        catch (IllegalAccessException e) {throw new RuntimeException("illegal access accessing persistent type field ");}
    }

    public static synchronized PersistentType getType(Class<?> cls) {
        if (cls == AnyPersistent.class) return null;        
        if (initialized) return getClassInfo(cls).getType();
        else {
            try {
                Field typeField = getTypeField(cls.getName());
                return (PersistentType)typeField.get(null);
            }
            catch (IllegalAccessException e) {throw new RuntimeException("illegal access accessing persistent type field ");}
        }
    }

    private static Field getTypeField(String className) {
        try {
            Class<?> cls = Class.forName(className);
            Field field = cls.getDeclaredField(TYPE_FIELD_NAME);
            field.setAccessible(true);
            return field;
        }
        catch (NoSuchFieldException nsf) {throw new RuntimeException("Exception during initTypeField: " + nsf.getMessage());}
        catch (ClassNotFoundException cnf) {throw new RuntimeException("Exception during initTypeField: " + cnf.getMessage());}
    }

    public synchronized PersistentType getType() {
        if (type == null) initType();
        return type;
    }

    public MemoryRegion getRegion() {return region;}

    public String className() {
        if (className != null) return className;
        className = new RawString(new UncheckedPersistentMemoryRegion(region.getLong(CLASS_NAME))).toString();
        return className;
    }

    public Class<?> cls() {return cls;}

    public long getClassNameAddr() {return region.getLong(CLASS_NAME);}

    public long getNextClassInfoAddr() {return region.getLong(NEXT_CLASS_INFO);}

    public void setNextClassInfoAddr(long addr) {
        Transaction.run(() -> {
            region.putLong(NEXT_CLASS_INFO, addr);
        });
    }

    private int allocationSize() {return ALLOCATION_SIZE;}

    public String toString() {
        return String.format("ClassInfo {\n  className = %s\n  cls = %s\n  rctor = %s\n  type = %s\n}", className, cls, reconstructor, type); 
    }
}
