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
import lib.util.persistent.types.ObjectType;
import lib.util.persistent.types.ValueType;
import lib.util.persistent.types.ValueBasedObjectType;
import lib.util.persistent.types.ArrayType;
import lib.util.persistent.types.CarriedType;
import lib.util.persistent.types.ByteField;
import lib.util.persistent.types.ShortField;
import lib.util.persistent.types.IntField;
import lib.util.persistent.types.LongField;
import lib.util.persistent.types.FloatField;
import lib.util.persistent.types.DoubleField;
import lib.util.persistent.types.CharField;
import lib.util.persistent.types.BooleanField;
import lib.util.persistent.types.ObjectField;
import lib.util.persistent.types.ValueField;
import lib.util.persistent.types.PersistentField;
import lib.util.persistent.spi.PersistentMemoryProvider;
import java.util.List;
import java.util.ArrayList;
import java.util.Deque;
import java.util.ArrayDeque;
import java.lang.reflect.Constructor;
import java.util.Iterator;
import lib.xpersistent.XHeap;
import lib.xpersistent.XRoot;
import lib.xpersistent.UncheckedPersistentMemoryRegion;
import java.util.Random;
import static lib.util.persistent.Trace.*;
import java.util.concurrent.TimeoutException;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Supplier;
import java.util.function.Consumer;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import static lib.util.persistent.ObjectCache.Ref;

import sun.misc.Unsafe;

@SuppressWarnings("sunapi")
public abstract class AnyPersistent {
    protected static final PersistentHeap heap = PersistentMemoryProvider.getDefaultProvider().getHeap();
    protected static final int[] timeoutArray = new int[256];
    protected static final int TIMEOUT_MASK = timeoutArray.length - 1;
    protected static int timeoutCursor = 0;
    protected static Random random = new Random(System.nanoTime());
    public static Unsafe UNSAFE;

    final ObjectType<? extends AnyPersistent> type;
    final MemoryRegion region;
    protected ReentrantLock lock;

    static {
        try {
            java.lang.reflect.Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (Unsafe)f.get(null);
        }
        catch (Exception e) {
            throw new RuntimeException("Unable to initialize UNSAFE.");
        }
        int maxTimeout = Config.MONITOR_ENTER_TIMEOUT;
        for (int i = 0; i < timeoutArray.length; i++) {
            timeoutArray[i] = random.nextInt(maxTimeout);
        }
    }

    public AnyPersistent(ObjectType<? extends AnyPersistent> type) {
        // would like to group the allocation transaction and initialization transaction
        // can't just put one here because this constructor call must be first line
        // Transaction.run(() -> {
            this(type, type.isValueBased() ? new VolatileMemoryRegion(type.getAllocationSize()) : heap.allocateRegion(type.getAllocationSize()));
            // trace(true, "AnyPersistent ctor from %s, allocationSize = %d", type, type.getAllocationSize());
        // });
    }

    @SuppressWarnings("unchecked")
    <T extends AnyPersistent> AnyPersistent(ObjectType<T> type, MemoryRegion region) {
        // trace(true, region.addr(), "creating object of type %s", type.getName());
        this.lock = new ReentrantLock(true);
        if (Config.ENABLE_MEMORY_STATS) Stats.current.memory.constructions++;
        this.type = type;
        this.region = region;
        if (!type.isValueBased()) {
            initHeader(ClassInfo.getClassInfo(type.getName()));
            registerAllocation(this);
            //  ObjectCache.add(this);
            // if (((XHeap)heap).getDebugMode() == true) {
            //     ((XRoot)(heap.getRoot())).addToAllObjects(region.addr());
            // }

            Ref<T> ref = new Ref<>((T)this);
            if(!Transaction.addNewObject(this, ref)) ObjectCache.add(region.addr(), ref);
        }
    }

    protected AnyPersistent(ObjectPointer<? extends AnyPersistent> p) {
        //trace(true, p.region().addr(), "recreating object of type %s", p.type().getName());
        lock = new ReentrantLock(true);
        if (Config.ENABLE_MEMORY_STATS) Stats.current.memory.reconstructions++;
        // this.pointer = p;
        if (p != null) {
            this.type = p.type();
            this.region = p.region();
        }
        else {
            this.type = null;
            this.region = null;
        }
    }

    static <T extends AnyPersistent> void registerAllocation(T obj) {
        // trace(true, obj.addr(), "register allocation");
        // assert(!obj.getType().isValueBased());
        ((XRoot)(heap.getRoot())).registerObject(obj.addr());
    }

    static void deregisterAllocation(long addr) {
        // trace(true, addr, "deregister allocation");
        ((XRoot)(heap.getRoot())).deregisterObject(addr);
    }

    boolean isValueBased() {return type.isValueBased();}

    // only called by Root during bootstrap of Object directory PersistentHashMap
    @SuppressWarnings("unchecked")
    public static <T extends AnyPersistent> T fromPointer(ObjectPointer<T> p) {
        // trace(p.addr(), "creating object from pointer of type %s", p.type().getName());
        T obj = null;
        try {
            obj = (T)p.type().getReconstructor().newInstance(p);
        }
        catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e.getCause());
        }
        ObjectCache.uncommittedConstruction(obj);
        return obj;
    }

    static void free(long addr) {
        // trace(true, addr, "free called");
        //if (!Config.REMOVE_FROM_OBJECT_CACHE_ON_ENQUEUE) ObjectCache.remove(addr);
        ObjectCache.remove(addr);
        MemoryRegion reg = new UncheckedPersistentMemoryRegion(addr);
        Transaction.run(() -> {
            // trace(true, addr, "freeing object region %d ", reg.addr());
            heap.freeRegion(reg);
            if (heap instanceof XHeap && ((XHeap)heap).getDebugMode() == true) {
                ((XRoot)(heap.getRoot())).removeFromAllObjects(addr);
            }
            //CycleCollector.removeFromCandidates(addr);
        });
    }

    // TODO: this will go away
    @SuppressWarnings("unchecked")
    public ObjectPointer getPointer() {
        return new ObjectPointer(type, region);
    }

    ObjectType getType() {
        return type;
    }

    MemoryRegion region() {
        return region;
    }

    long addr() {return region.addr();}

    byte getRegionByte(long offset) {
        // trace(true, "AP.getRegionByte(%d)", offset);
        return region.getByte(offset);
    }

    short getRegionShort(long offset) {
        // trace(true, "AP.getRegionShort(%d)", offset);
        return region.getShort(offset);
    }

    int getRegionInt(long offset) {
        // trace(true, "AP.getRegionInt(%d)", offset);
        return region.getInt(offset);
    }

    long getRegionLong(long offset) {
        // trace(true, "AP.getRegionLong(%d)", offset);
        return region.getLong(offset);
    }

    abstract byte getByte(long offset);
    abstract short getShort(long offset);
    abstract int getInt(long offset);
    abstract long getLong(long offset);
    abstract<T extends AnyPersistent> T getObject(long offset, PersistentType type);
    abstract<T extends AnyPersistent> T getValueObject(long offset, PersistentType type);

    @SuppressWarnings("unchecked")
    protected <T extends AnyPersistent> T getObject(long offset) {
        return getObject(offset, Types.OBJECT);
    }

    void setByte(long offset, byte value) {
        if (!isValueBased()) {
            Transaction.run(() -> {
                region.putByte(offset, value);
            }, this);
        }
        else {
            Util.synchronizedBlock(this, () -> {
                region.putByte(offset, value);
            });
        }
    }

    void setShort(long offset, short value) {
        if (!isValueBased()) {
            Transaction.run(() -> {
                region.putShort(offset, value);
            }, this);
        }
        else {
            Util.synchronizedBlock(this, () -> {
                region.putShort(offset, value);
            });
        }
    }

    void setInt(long offset, int value) {
        if (!isValueBased()) {
            Transaction.run(() -> {
                region.putInt(offset, value);
            }, this);
        }
        else {
            Util.synchronizedBlock(this, () -> {
                region.putInt(offset, value);
            });
        }
    }

    void setLong(long offset, long value) {
        // trace(true, "AP.setLong(%d, %d)", offset, value);
        if (!isValueBased()) {
            Transaction.run(() -> {
                region.putLong(offset, value);
            }, this);
        }
        else {
            Util.synchronizedBlock(this, () -> {
                region.putLong(offset, value);
            });
        }
    }

    void setObject(long offset, AnyPersistent value) {
        Transaction.run(() -> {
            AnyPersistent old = ObjectCache.get(getLong(offset), true);
            //trace(true, "AP.setObject(%d, value = %d, old = %d)", offset, value == null ? -1 : value.addr(), old == null ? -1 : old.addr());
            Transaction.run(() -> {
                if (value != null) value.addReference();
                if (old != null) old.deleteReference(false);
                setLong(offset, value == null ? 0 : value.addr());
            }, value, old);
        }, this);
    }

    void setValueObject(long offset, AnyPersistent  value) {
        // trace(true, "AP.setValueObject(%d)", offset);
        if (value == null) return;  // should be exception?
        if (!isValueBased()) {
            Transaction.run(() -> {
                MemoryRegion dstRegion = region();
                MemoryRegion srcRegion = value.region();
                // trace(true, "AnyPersistent setValueObject src addr = %d, dst addr = %d, size = %d", srcRegion.addr(), dstRegion.addr() + offset, value.getType().getSize());
                Util.memCopy(value.getType(), type, srcRegion, 0, dstRegion, offset, value.getType().getSize());
            }, this);
        }
        else {
            Util.synchronizedBlock(this, () -> {
                MemoryRegion dstRegion = region();
                MemoryRegion srcRegion = value.region();
                // trace(true, "AnyPersistent setValueObject src addr = %d, dst addr = %d, size = %d", srcRegion.addr(), dstRegion.addr() + offset, value.getType().getSize());
                Util.memCopy(value.getType(), getType(), srcRegion, 0, dstRegion, offset, value.getType().getSize());
            });
        }
    }

    void setRegionByte(long offset, byte value) {
        region.putDurableByte(offset, value);
    }

    void setRegionShort(long offset, short value) {
        region.putDurableShort(offset, value);
    }

    void setRegionInt(long offset, int value) {
        region.putDurableInt(offset, value);
    }

    void setRegionLong(long offset, long value) {
        region.putDurableLong(offset, value);
    }

    void setRawByte(long offset, byte value) {
        region.putRawByte(offset, value);
    }

    void setRawShort(long offset, short value) {
        region.putRawShort(offset, value);
    }

    void setRawInt(long offset, int value) {
        region.putRawInt(offset, value);
    }

    void setRawLong(long offset, long value) {
        region.putRawLong(offset, value);
    }

    //TODO: identity beyond one JVM instance, should use == where comparison is same VM instance
    public final boolean is(AnyPersistent obj) {
        return addr() == obj.addr();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof AnyPersistent && ((AnyPersistent)obj).is(this);
    }

    @Override
    public int hashCode() {
        return (int)addr();
    }

    protected List<PersistentType> types() {
        return ((ObjectType<?>)type).getTypes();
    }

    protected int fieldCount() {
        return types().size();
    }

    protected long offset(int index) {
        return ((ObjectType<?>)type).getOffset(index);
    }

    @SuppressWarnings("unchecked")
    private void initializeField(long offset, PersistentType t) {
        // System.out.format("type = %s, initializeField(%d. %s)\n", type, offset, t);
        if (t == Types.BYTE) setByte(offset, (byte)0);
        else if (t == Types.SHORT) setShort(offset, (short)0);
        else if (t == Types.INT) setInt(offset, 0);
        else if (t == Types.LONG) {setLong(offset, 0L);}
        else if (t == Types.FLOAT) setInt(offset, Float.floatToIntBits(0f));
        else if (t == Types.DOUBLE) setLong(offset,  Double.doubleToLongBits(0d));
        else if (t == Types.CHAR) setInt(offset, 0);
        else if (t == Types.BOOLEAN) setByte(offset, (byte)0);
        else if (t instanceof ValueBasedObjectType) {
            // System.out.println("initalizing " + t);
            ValueBasedObjectType vt = (ValueBasedObjectType)t;
            List<PersistentType> ts = vt.getTypes();
            for (int i = 0; i < ts.size(); i++) initializeField(vt.getOffset(i), ts.get(i));
        }
        else if (t instanceof ObjectType) setObject(offset, null);
    }

    // we can turn this off after debug since only Field-based getters and setters are public
    // and those provide static type safety and internally assigned indexes
    protected int check(int index, PersistentType testType) {
        // boolean result = true;
        // if (index < 0 || index >= fieldCount()) throw new IndexOutOfBoundsException("No such field index: " + index);
        // PersistentType t = types().get(index);
        // if (t instanceof ObjectType && testType instanceof ObjectType) {
        //     ObjectType<?> fieldType = (ObjectType)t;
        //     ObjectType<?> test = (ObjectType) testType;
        //     if (!test.cls().isAssignableFrom(fieldType.cls())) result = false;
        //     else if (t != testType) result = false;
        //     if (!result) throw new RuntimeException("Type mismatch in " + getType().cls() + " at index " + index + ": expected " + testType + ", found " + types().get(index));
        // }
        return index;
    }

    private void initHeader(ClassInfo classInfo) {
        setRegionLong(Header.TYPE.getOffset(Header.CLASS_INFO), classInfo.getRegion().addr());
    }

    static String typeNameFromRegion(MemoryRegion region) {
        ClassInfo ci = ClassInfo.getClassInfo(region.addr());
        return ci.className();
    }

    synchronized int getRefCount() {
        return region().getInt(Header.TYPE.getOffset(Header.REF_COUNT));
    }

    void incRefCount() {
        Transaction.run(() -> {
            int oldCount = region.getInt(Header.TYPE.getOffset(Header.REF_COUNT));
            region.putInt(Header.TYPE.getOffset(Header.REF_COUNT), oldCount + 1);
            if (oldCount == 0) deregisterAllocation(region.addr());
             // trace(true, addr(), "incRefCount(), type = %s, old = %d, new = %d", type, oldCount, getRefCount());
        }, this);
    }

    int decRefCount() {
        return Transaction.run(() -> {
            int oldCount = region.getInt(Header.TYPE.getOffset(Header.REF_COUNT));
            int newCount = oldCount - 1;
            // trace(true, addr(), "decRefCount, type = %s, old = %d, new = %d", type, oldCount, newCount);
            if (newCount < 0) {
               // trace(true, addr(), "decRef below 0");
               new RuntimeException().printStackTrace(); System.exit(-1);
            }
            region.putInt(Header.TYPE.getOffset(Header.REF_COUNT), newCount);
            return newCount;
        }, this);
    }

    void flushRegion() {
        region.flush(0, type.getAllocationSize());
    }

    void flushHeader() {
        region.flush(0, Header.TYPE.getAllocationSize());
    }

    void addReference() {
        Transaction.run(() -> {
            incRefCount();
            //setColor(CycleCollector.BLACK);
        }, this);
    }
    //TODO: analyze serialization / isolation of this
    //synchronized void deleteReference(boolean live) {
/*    public void deleteReference(boolean live) {
        // assert(!type.isValueBased());
        // trace(true, addr(), "deleteReference(%s)", live);
        Deque<Long> addrsToDelete = new ArrayDeque<>();
        ObjectCache.Ref ref;
        long address = addr();
        // Transaction.run(() -> {
        int count = getRefCount();
        if (live) ObjectCache.remove(address);
        else count = count == 0 ? 0 : decRefCount();
        if (count == 0 && (ref = ObjectCache.getReference(address, true)).isForAdmin()) {
            addrsToDelete.push(addr());
            while (!addrsToDelete.isEmpty()) {
                long addrToDelete = addrsToDelete.pop();
                Iterator<Long> childAddresses = getChildAddressIterator(addrToDelete);
                ArrayList<ObjectCache.Ref<AnyPersistent>> children = new ArrayList<>();
                while (childAddresses.hasNext()) {
                    children.add(ObjectCache.getReference(childAddresses.next(), true));
                }
                for (ObjectCache.Ref<AnyPersistent> childRef : children) {
                    AnyPersistent child = childRef.get();
                    Transaction.run(() -> {
                        long childAddr = child.addr();
                        int crc = child.getRefCount() == 0 ? 0 : child.decRefCount();
                        if (crc == 0 && childRef.isForAdmin()) {
                            addrsToDelete.push(childAddr);
                        } else {
                            //CycleCollector.addCandidate(childAddr);
                            if (crc == 0) registerAllocation(child); 
                        }
                    }, child);
                    childRef.clear();
                }
                Transaction.removeFromReconstructions(addrToDelete);
                ref.clear();
                free(addrToDelete);
                deregisterAllocation(addrToDelete);
    
            }
        } else {
            if (count == 0) registerAllocation(this);
            //CycleCollector.addCandidate(addr());
        }
    }*/


    //TODO: analyze serialization / isolation of this
    //synchronized void deleteReference(boolean live) {
    public void deleteReference(boolean live) {
        // assert(!type.isValueBased());
        // trace(true, addr(), "deleteReference(%s)", live);
        long address = addr();
        // Transaction.run(() -> {
        Transaction.run(() -> {
            int count = getRefCount();
            boolean isCleanup = !live && count == 0;
            if (live) ObjectCache.remove(address);
            else count = count == 0 ? 0 : decRefCount();
            boolean reachable = (!live && !(ObjectCache.getReference(address, true)).isForAdmin());
            if (count == 0 && !reachable) {
                Deque<Long> addrsToDelete = new ArrayDeque<>();
                addrsToDelete.push(addr());
                while (!addrsToDelete.isEmpty()) {
                    long addrToDelete = addrsToDelete.pop();
                    Iterator<Long> childAddresses = getChildAddressIterator(addrToDelete);
                    ArrayList<ObjectCache.Ref<AnyPersistent>> children = new ArrayList<>();
                    while (childAddresses.hasNext()) {
                        children.add(ObjectCache.getReference(childAddresses.next(), true));
                    }
                    for (ObjectCache.Ref<AnyPersistent> childRef : children) {
                        AnyPersistent child = childRef.get();
                        Transaction.run(() -> {
                            long childAddr = child.addr();
                            int crc = child.decRefCount();
                            if (crc == 0 && childRef.isForAdmin()) {
                                addrsToDelete.push(childAddr);
                            } else {
                                //CycleCollector.addCandidate(childAddr);
                                if (crc == 0) registerAllocation(child); 
                            }
                        }, child);
                    }
                    Transaction.removeFromReconstructions(addrToDelete);
                    free(addrToDelete);
                    if (!isCleanup) deregisterAllocation(addrToDelete);
    
                }
            } else {
                if (count == 0) registerAllocation(this);
                //CycleCollector.addCandidate(addr());
            }
            Transaction.getTransaction().reconstructions.clear();
        });
    }

    //TODO: make non-public, currently called from XRoot
    public static void deleteResidualReferences(long address, int count) {
        // trace(true, address, "deleteResidualReferences: %d", count);
        AnyPersistent obj = ObjectCache.get(address, true);
         //assert(!obj.getType().isValueBased());
        obj.deleteReference(false);
    }

    static String classNameForRegion(MemoryRegion reg) {
        long classInfoAddr = reg.getLong(0);  // get class info address
        return ClassInfo.getClassInfo(classInfoAddr).className();
    }

    static Iterator<Long> getChildAddressIterator(long address) {
        // trace(true, address, "getChildAddressIterator");
        MemoryRegion reg = ObjectCache.get(address, true).region();
        String typeName = classNameForRegion(reg);
        ObjectType<?> type = Types.typeForName(typeName);
        ArrayList<Long> childAddresses = new ArrayList<>();
        if (type instanceof ArrayType) {
            ArrayType<?> arrType = (ArrayType)type;
            Class<?> arrClass = arrType.cls();
            if (arrClass == lib.util.persistent.PersistentValueArray.class ||
                arrClass == lib.util.persistent.PersistentValueArray.class) {
                return childAddresses.iterator();
            }
            PersistentType et = arrType.getElementType();
            if (et == Types.OBJECT || (et instanceof ObjectType && !((ObjectType)et).isValueBased())) {
                int length = reg.getInt(ArrayType.LENGTH_OFFSET);
                for (int i = 0; i < length; i++) {
                    long childAddr = reg.getLong(arrType.getElementOffset(i));
                    if (childAddr != 0) {
                        childAddresses.add(childAddr);
                    }
                }
            }
        }
        else if (type instanceof ObjectType) {
            if (!((ObjectType)type).isValueBased()) {
                for (int i = Header.TYPE.fieldCount(); i < type.fieldCount(); i++) {
                    List<PersistentType> types = type.getTypes();
                    PersistentType ctype = types.get(i);
                    if (ctype instanceof ObjectType && !((ObjectType)ctype).isValueBased() || types.get(i) == Types.OBJECT) {
                        long childAddr = reg.getLong(type.getOffset(i));
                        if (childAddr != 0) {
                            childAddresses.add(childAddr);
                        }
                    }
                }
            }
        }
        else {
            throw new RuntimeException("getChildAddressIterator: unexpected type");
        }
        return childAddresses.iterator();
    }

    // void setColor(byte color) {
    //     setColor(color, false);
    // }

    // void setColor(byte color, boolean cc) {
    //     if (CycleCollector.isProcessing() && !cc) {
    //         CycleCollector.stashObjColorChange(addr(), color);
    //     } else {
    //         region.putByte(Header.TYPE.getOffset(Header.REF_COLOR), color);
    //     }
    // }

    // byte getColor() {
    //     return region.getByte(Header.TYPE.getOffset(Header.REF_COLOR));
    // }

    boolean tryLock(Transaction transaction) {
        int max = transaction.timeout() + timeoutArray[timeoutCursor++ & TIMEOUT_MASK];
        boolean success = tryLock(max);
        if (success) {
            transaction.timeout(Config.MONITOR_ENTER_TIMEOUT);
        }
        else {
            transaction.timeout(Math.min((int)(transaction.timeout() * Config.MONITOR_ENTER_TIMEOUT_INCREASE_FACTOR), Config.MAX_MONITOR_ENTER_TIMEOUT));
        }
        return success;
    }

    void lock() {
        lock.lock();
    }

    boolean tryLock(long timeout) {
        boolean success = false;
        if (!Config.USE_BLOCKING_LOCKS_FOR_DEBUG) {
            try {
                success = lock.tryLock(timeout, TimeUnit.MILLISECONDS);
            }
            catch (InterruptedException ie) {throw new RuntimeException(ie.getMessage());}
        }
        else {
            lock();
            success = true;
        }
        if (Config.ENABLE_LOCK_STATS) if (success) Stats.current.locks.acquired++; else Stats.current.locks.timeouts++;
        return success;
    }

    void unlock() {
        lock.unlock();
    }

    // used internally only.
    static AnyPersistent asLock() {return new AsLock();}

    private static class AsLock extends AnyPersistent {
        public AsLock() {
            super((ObjectPointer<?>)null);
            if (Config.ENABLE_ALLOC_STATS) Stats.current.allocStats.update(AsLock.class.getName(), 0, 70, 1);  // uncomment for allocation stats
        }

        byte getByte(long offset) {throw new UnsupportedOperationException();}
        short getShort(long offset) {throw new UnsupportedOperationException();}
        int getInt(long offset) {throw new UnsupportedOperationException();}
        long getLong(long offset) {throw new UnsupportedOperationException();}
        <T extends AnyPersistent> T getObject(long offset, PersistentType type) {throw new UnsupportedOperationException();}
        <T extends AnyPersistent> T getValueObject(long offset, PersistentType type) {throw new UnsupportedOperationException();}
    }
}
