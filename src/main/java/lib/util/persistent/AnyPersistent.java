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
import lib.util.persistent.types.IndirectValueObjectType;
import lib.util.persistent.types.ValueType;
import lib.util.persistent.types.ArrayType;
import lib.util.persistent.types.ReferenceArrayType;
import lib.util.persistent.types.ReferenceArrayType;
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
            this(type, type.kind() == ObjectType.Kind.DirectValue ? new VolatileMemoryRegion(type.allocationSize())
                                           : type.kind() == ObjectType.Kind.IndirectValue ? new VolatileMemoryRegion(type.allocationSize())
                                           : heap.allocateObjectRegion(type.allocationSize()));
    }

    @SuppressWarnings("unchecked")
    <T extends AnyPersistent> AnyPersistent(ObjectType<T> type, MemoryRegion region) {
        // trace(true, region.addr(), "creating object of type %s, region = %s", type.name(), region);
        if (Config.ENABLE_MEMORY_STATS) Stats.current.memory.constructions++;
        this.lock = new ReentrantLock(true);
        this.type = type;
        this.region = region;
        if (!type.valueBased()) {
            initHeader(ClassInfo.getClassInfo(type.name()));
            Ref<T> ref = new Ref<>((T)this);
            if(!Transaction.addNewObject(this, ref)) ObjectCache.add(region.addr(), ref);
        }
    }

    protected AnyPersistent(ObjectPointer<? extends AnyPersistent> p) {
        // trace(true, p.region().addr(), "recreating object of type %s", p.type().name());
        lock = new ReentrantLock(true);
        if (Config.ENABLE_MEMORY_STATS) Stats.current.memory.reconstructions++;
        if (p != null) {
            this.type = p.type();
            this.region = p.region();
        }
        else {
            this.type = null;
            this.region = null;
        }
    }

    @SuppressWarnings("unchecked")
    static <T extends AnyPersistent> T reconstruct(ObjectPointer<T> p) {
        // special case for reconstructing an indirect value
        T obj = null;
        try {
            Constructor ctor = ((ObjectType)p.type()).getReconstructor();
            obj = (T)ctor.newInstance(p);
            obj.onReconstruction();
        }
        catch (Exception e) {
            Throwable cause = e.getCause();
            if (e instanceof InvocationTargetException && cause instanceof TransactionRetryException) {
                throw (TransactionRetryException)e;
            }
            else throw new RuntimeException(cause);
        }
        return obj;
    }

    boolean isValueBased() {return type.valueBased();}

    // only called by Root during bootstrap of Object directory PersistentHashMap
    @SuppressWarnings("unchecked")
    public static <T extends AnyPersistent> T fromPointer(ObjectPointer<T> p) {
        // trace(p.addr(), "creating object from pointer of type %s", p.type().name());
        T obj = reconstruct(p);
        ObjectCache.uncommittedConstruction(obj);
        return obj;
    }

    static void free(long addr) {
        // trace(true, addr, "free called");
        ObjectCache.remove(addr);
        MemoryRegion reg = new UncheckedPersistentMemoryRegion(addr);
        Transaction.run(() -> {
            // trace(true, addr, "freeing object region %d ", reg.addr());
            heap.freeRegion(reg);
            //CycleCollector.removeFromCandidates(addr);
        });
    }

    // TODO: this will go away
    @SuppressWarnings("unchecked")
    public ObjectPointer getPointer() {
        return new ObjectPointer(type, region);
    }

    public ObjectType getType() {
        return type;
    }

    MemoryRegion region() {
        return region;
    }

    long addr() {return region.addr();}

    void onReconstruction() {}
    void onSet() {}
    void onGet() {}
    void onFree(long offset) {}

    byte getRegionByte(long offset) {
        return region.getByte(offset);
    }

    short getRegionShort(long offset) {
        return region.getShort(offset);
    }

    int getRegionInt(long offset) {
        return region.getInt(offset);
    }

    long getRegionLong(long offset) {
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
        return getObject(offset, Types.GENERIC_REFERENCE);
    }

    void setByte(long offset, byte value) {
        Transaction tx = Transaction.getActiveTransaction();
        if (tx != null) {
            Transaction.run(tx, () -> {
                region.putByte(offset, value);
            }, this);
        }
        else {
            lock();
            try {
                if (isValueBased()) region.putByte(offset, value);
                else region.putDurableByte(offset, value);
            }
            finally {unlock();}
        }
    }

    void setShort(long offset, short value) {
        Transaction tx = Transaction.getActiveTransaction();
        if (tx != null) {
            Transaction.run(tx, () -> {
                region.putShort(offset, value);
            }, this);
        }
        else {
            lock();
            try {
                if (isValueBased()) region.putShort(offset, value);
                else region.putDurableShort(offset, value);
            }
            finally {unlock();}
        }
    }

    void setInt(long offset, int value) {
        Transaction tx = Transaction.getActiveTransaction();
        if (tx != null) {
            Transaction.run(tx, () -> {
                region.putInt(offset, value);
            }, this);
        }
        else {
            lock();
            try {
                if (isValueBased()) region.putInt(offset, value);
                else region.putDurableInt(offset, value);
            }
            finally {unlock();}
        }
    }

    void setLong(long offset, long value) {
        // trace(true, "AP.setLong(%d, %d)", offset, value);
        Transaction tx = Transaction.getActiveTransaction();
        if (tx != null) {
            Transaction.run(tx, () -> {
                region.putLong(offset, value);
            }, this);
        }
        else {
            lock();
            try {
                if (isValueBased()) region.putLong(offset, value);
                else region.putDurableLong(offset, value);
            }
            finally {unlock();}
        }
    }

    void setObject(long offset, AnyPersistent value) {
        Transaction.run(() -> {
            AnyPersistent old = ObjectCache.get(getLong(offset), true);
            // trace(true, "AP.setObject(%d, value = %d, old = %d)", offset, value == null ? -1 : value.addr(), old == null ? -1 : old.addr());
            Transaction.run(() -> {
                if (value != null) value.addReference();
                if (old != null) old.deleteReference(false);
                region.putLong(offset, value == null ? 0 : value.addr());
            }, value, old);
        }, this);
    }

    @SuppressWarnings("unchecked")
    void setValueObject(long offset, AnyPersistent value) {
        if (value == null) return;  // TODO: should be exception
        // trace(true, "AP.setValueObject(%d), hostType = %s, value's type = %s", offset, getType().cls(), value.getType().cls());
        value.onSet();
        ObjectType hostType = getType();
        ObjectType valueType = value.getType();
        ObjectType.Kind hostKind = hostType.kind();
        ObjectType.Kind valueKind = valueType.kind();
        MemoryRegion srcRegion = value.region(); 
        long srcSize = valueType.size(); 
        MemoryRegion dstRegion = null;
        switch (hostKind) {
            case Reference: dstRegion = valueKind == ObjectType.Kind.IndirectValue ? heap.allocateRegion(srcSize) : region(); break;
            case DirectValue: dstRegion = valueKind == ObjectType.Kind.IndirectValue ? new VolatileMemoryRegion(srcSize) : region(); break;
            case IndirectValue: dstRegion = valueKind == ObjectType.Kind.IndirectValue ? new VolatileMemoryRegion(srcSize) : region(); break;
        }
        long dstOffset = offset; 
        Util.memCopy(valueType, hostType, srcRegion, 0, dstRegion, dstOffset, srcSize);
        
        // after-copy work
        if (valueKind == ObjectType.Kind.IndirectValue) {
            switch (hostKind) {
                case Reference : 
                    region().putRawLong(offset, dstRegion.addr());
                    long classInfoAddress = ClassInfo.getClassInfo(valueType.name()).getRegion().addr();
                    dstRegion.putRawLong(IndirectValueObjectType.CLASS_INFO_OFFSET, classInfoAddress);
                    break;
                case DirectValue : /* don't know how to store IV in DV yet*/
                    throw new RuntimeException("NYI");
                case IndirectValue :  /*don't know how to nest IVs yet*/
                    throw new RuntimeException("NYI");
                default : throw new RuntimeException("Unsupported Kind: " + hostKind);
            }
        }
        // TODO:
        // write nested indirect values
        // List<PersistentType> valueFieldTypes = valueType.fieldTypes();
        // for (PersistentType t : valueFieldTypes) {
        //     if (t instanceof ObjectType && ((ObjectType)t).kind() == ObjectType.Kind.IndirectValue) {
        //         // System.out.println("read field of type " + t + " and write here");
        //         // read indirect field and write to this object
        //     }
        // } 
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
        return ((ObjectType<?>)type).fieldTypes();
    }

    protected int fieldCount() {
        return types().size();
    }

    protected long offset(int index) {
        return ((ObjectType<?>)type).offset(index);
    }

    private void initHeader(ClassInfo classInfo) {
        setRegionLong(Header.TYPE.offset(Header.CLASS_INFO), classInfo.getRegion().addr());
    }

    static String typeNameFromRegion(MemoryRegion region) {
        ClassInfo ci = ClassInfo.getClassInfo(region.addr());
        return ci.className();
    }

    synchronized int getRefCount() {
        return region().getInt(Header.TYPE.offset(Header.REF_COUNT));
    }

    void incRefCount() {
        Transaction.run(() -> {
            int oldCount = region.getInt(Header.TYPE.offset(Header.REF_COUNT));
            region.putInt(Header.TYPE.offset(Header.REF_COUNT), oldCount + 1);
            //  trace(true, addr(), "incRefCount(), type = %s, old = %d, new = %d", type, oldCount, getRefCount());
        }, this);
    }

    int decRefCount() {
        return Transaction.run(() -> {
            int oldCount = region.getInt(Header.TYPE.offset(Header.REF_COUNT));
            int newCount = oldCount - 1;
            // trace(true, addr(), "decRefCount, type = %s, old = %d, new = %d", type, oldCount, newCount);
            if (newCount < 0) {
               new RuntimeException().printStackTrace(); System.exit(-1);
            }
            region.putInt(Header.TYPE.offset(Header.REF_COUNT), newCount);
            return newCount;
        }, this);
    }

    void flushRegion() {
        region.flush(0, type.allocationSize());
    }

    void flushHeader() {
        region.flush(0, Header.TYPE.allocationSize());
    }

    void addReference() {
        // Transaction.run(() -> {
            incRefCount();
            //setColor(CycleCollector.BLACK);
        // }, this);
    }

    //TODO: analyze serialization / isolation of this
    public void deleteReference(boolean live) {
        // trace(true, addr(), "deleteReference(%s), type = %s ", live, getType());
        long address = addr();
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
                        long childAddress = childAddresses.next();
                        children.add(ObjectCache.getReference(childAddress, true));
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
                            }
                        }, child);
                    }
                    Transaction.removeFromReconstructions(addrToDelete);
                    free(addrToDelete);

                }
            } else {
                //CycleCollector.addCandidate(addr());
            }
            if(live) Transaction.getTransaction().reconstructions.clear();
        });
    }

    //TODO: make non-public, currently called from XRoot
    public static void deleteResidualReferences(long address) {
        // trace(true, address, "deleteResidualReferences: %d", count);
        AnyPersistent obj = ObjectCache.get(address, true);
        obj.deleteReference(false);
    }

    static String classNameForRegion(MemoryRegion reg) {
        long classInfoAddr = reg.getLong(0);  // get class info address
        return ClassInfo.getClassInfo(classInfoAddr).className();
    }

    @SuppressWarnings("unchecked")
    static Iterator<Long> getChildAddressIterator(long address) {
        // trace(true, address, "getChildAddressIterator");
        MemoryRegion parentRegion = ObjectCache.get(address, true).region();
        String typeName = classNameForRegion(parentRegion);
        ObjectType<?> parentType = Types.typeForName(typeName);
        ArrayList<Long> childAddresses = new ArrayList<>();
        if (parentType instanceof ArrayType && parentType.kind() == ObjectType.Kind.Reference) {
            ArrayType<?> arrType = (ArrayType)parentType;
            PersistentType et = arrType.elementType();
            if (et instanceof ObjectType) {
                ObjectType<AnyPersistent> eot = (ObjectType)et;
                int length = parentRegion.getInt(ReferenceArrayType.LENGTH_OFFSET);
                if (eot.valueBased()) {
                    if (PersistentByteVector.class.isAssignableFrom(eot.cls())) {
                        for (int i = 0; i < length; i++) {
                            long childAddr = parentRegion.getLong(arrType.elementOffset(i));
                            long childOffset = parentRegion.addr() + parentType.offset(i);
                            AnyPersistent obj = reconstruct(new ObjectPointer<AnyPersistent>(eot, new UncheckedPersistentMemoryRegion(childAddr)));
                            obj.onFree(childOffset);
                        }
                    }
                }
                else {
                    for (int i = 0; i < length; i++) {
                        long childAddr = parentRegion.getLong(arrType.elementOffset(i));
                        if (childAddr != 0) childAddresses.add(childAddr);
                    }
                }
            }
        }
        else if (parentType instanceof ObjectType) {
            for (int i = Header.TYPE.fieldCount(); i < parentType.fieldCount(); i++) {
                List<PersistentType> types = parentType.fieldTypes();
                PersistentType ctype = types.get(i);
                if (ctype instanceof ObjectType) {
                    long childAddr = parentRegion.getLong(parentType.offset(i));
                    ObjectType ot = (ObjectType)ctype;
                    if (ot.valueBased()) {
                        if (PersistentByteVector.class.isAssignableFrom(ot.cls())) {
                            AnyPersistent obj = reconstruct(new ObjectPointer<AnyPersistent>(ot, new UncheckedPersistentMemoryRegion(childAddr)));
                            long childOffset = parentRegion.addr() + parentType.offset(i);
                            obj.onFree(childOffset);
                        }
                        else if (ot.kind() == ObjectType.Kind.IndirectValue) {
                            // System.out.println("free child indirect value");
                        }
                    }
                    else if (childAddr != 0) {
                        childAddresses.add(childAddr);
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
    //         region.putByte(Header.TYPE.offset(Header.REF_COLOR), color);
    //     }
    // }

    // byte getColor() {
    //     return region.getByte(Header.TYPE.offset(Header.REF_COLOR));
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
