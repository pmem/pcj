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
import java.util.function.Supplier;
import java.util.function.Consumer;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.TimeUnit;

import sun.misc.Unsafe;

@SuppressWarnings("sunapi")
public abstract class AnyPersistent {
    protected static final PersistentHeap heap = PersistentMemoryProvider.getDefaultProvider().getHeap();
    protected static Random random = new Random(System.nanoTime());
    protected static Unsafe UNSAFE;

    protected final ObjectPointer<? extends AnyPersistent> pointer;
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
    }

    public AnyPersistent(ObjectType<? extends AnyPersistent> type) {
        // would like to group the allocation transaction and initialization transaction
        // can't just put one here because this constructor call must be first line
        // Transaction.run(() -> {
            this(type, type.isValueBased() ? new VolatileMemoryRegion(type.getAllocationSize()) : heap.allocateRegion(type.getAllocationSize()));
            // trace(true, "AnyPersistent ctor from %s, allocationSize = %d", type, type.getAllocationSize());
        // });
    }

    <T extends AnyPersistent> AnyPersistent(ObjectType<T> type, MemoryRegion region) {
        // trace(true, region.addr(), "creating object of type %s", type.getName());
        lock = new ReentrantLock(true);
        Stats.current.memory.constructions++;
        this.pointer = new ObjectPointer<T>(type, region);
        List<PersistentType> ts = type.getTypes();
        Transaction.run(() -> {
            for (int i = 0; i < ts.size(); i++) initializeField(offset(i), ts.get(i));
            if (!(type instanceof ValueBasedObjectType)) {
                setTypeName(type.getName());
                setVersion(99);
                initForGC();
                if (heap instanceof XHeap && ((XHeap)heap).getDebugMode() == true) {
                    ((XRoot)(heap.getRoot())).addToAllObjects(getPointer().region().addr());
                }
                ObjectCache.add(this);
            }
        }, this);
        // ObjectCache.add(this);
    }

    protected AnyPersistent(ObjectPointer<? extends AnyPersistent> p) {
        // trace(true, p.region().addr(), "recreating object of type %s", p.type().getName());
        lock = new ReentrantLock(true);
        Stats.current.memory.reconstructions++;
        this.pointer = p;
    }

    boolean isValueBased() {return getPointer().type().isValueBased();}

    void initForGC() {
        Transaction.run(() -> {
            incRefCount();
            ObjectCache.registerObject(this);
        }, this);
    }

    // only called by Root during bootstrap of Object directory PersistentHashMap
    @SuppressWarnings("unchecked")
    public static <T extends AnyPersistent> T fromPointer(ObjectPointer<T> p) {
        // trace(p.addr(), "creating object from pointer of type %s", p.type().getName());
        try {
            Class<T> cls = p.type().cls();
            Constructor ctor = cls.getDeclaredConstructor(ObjectPointer.class);
            ctor.setAccessible(true);
            T obj = (T)ctor.newInstance(p);
            return obj;
        }
        catch (Exception e) {e.printStackTrace();}
        return null;
    }

    static void free(long addr) {
        // trace(addr, "free called");
        ObjectCache.remove(addr);
        MemoryRegion reg = new UncheckedPersistentMemoryRegion(addr);
        MemoryRegion nameRegion = new UncheckedPersistentMemoryRegion(reg.getLong(Header.TYPE.getOffset(Header.TYPE_NAME)));
        Transaction.run(() -> {
            // trace(addr, "freeing object region %d and name region %d", reg.addr(), nameRegion.addr());
            heap.freeRegion(nameRegion);
            heap.freeRegion(reg);
            if (heap instanceof XHeap && ((XHeap)heap).getDebugMode() == true) {
                ((XRoot)(heap.getRoot())).removeFromAllObjects(addr);
            }
            CycleCollector.removeFromCandidates(addr);
        });
    }

    public ObjectPointer<? extends AnyPersistent> getPointer() {
        return pointer;
    }

    ObjectType getType() {
        return pointer.type();
    }

    protected byte getRegionByte(long offset) {
        // trace(true, "AP.getRegionByte(%d)", offset);
        return pointer.region().getByte(offset);
    }

    protected short getRegionShort(long offset) {
        // trace(true, "AP.getRegionShort(%d)", offset);
        return pointer.region().getShort(offset);
    }

    protected int getRegionInt(long offset) {
        // trace(true, "AP.getRegionInt(%d)", offset);
        return pointer.region().getInt(offset);
    }

    protected long getRegionLong(long offset) {
        // trace(true, "AP.getRegionLong(%d)", offset);
        return pointer.region().getLong(offset);
    }

    abstract byte getByte(long offset);
    abstract short getShort(long offset);
    abstract int getInt(long offset);
    abstract long getLong(long offset);
    abstract<T extends AnyPersistent> T getObject(long offset);

    void setByte(long offset, byte value) {
        Transaction.run(() -> {
            pointer.region().putByte(offset, value);
        }, this);
    }

    void setShort(long offset, short value) {
        Transaction.run(() -> {
            pointer.region().putShort(offset, value);
        }, this);
    }

    void setInt(long offset, int value) {
        Transaction.run(() -> {
            pointer.region().putInt(offset, value);
        }, this);
    }

    void setLong(long offset, long value) {
        Transaction.run(() -> {
            pointer.region().putLong(offset, value);
        }, this);
    }

    void setObject(long offset, AnyPersistent value) {
        if (value != null && value.getPointer().type().isValueBased()) {
            MemoryRegion dstRegion = getPointer().region();
            MemoryRegion srcRegion = value.getPointer().region();
            // trace(true, "setObject (valueBased) src addr = %d, dst addr = %d, size = %d", srcRegion.addr(), dstRegion.addr() + offset, value.getType().getSize());
            Util.memCopy(value.getPointer().type(), getPointer().type(), srcRegion, 0, dstRegion, offset, value.getType().getSize());
        }
        else {
            Transaction.run(() -> {
                AnyPersistent old = ObjectCache.get(getLong(offset), true);
                Transaction.run(() -> {
                    if (value != null) value.addReference();
                    if (old != null) old.deleteReference();
                    setLong(offset, value == null ? 0 : value.getPointer().addr());
                }, value, old);
            }, this);
        }
    }

    // TODO: only used interally, may go away
    void setByteField(int index, byte value) {setByte(offset(check(index, Types.BYTE)), value);}
    void setShortField(int index, short value) {setShort(offset(check(index, Types.SHORT)), value);}
    void setIntField(int index, int value) {setInt(offset(check(index, Types.INT)), value);}
    void setLongField(int index, long value) {setLong(offset(check(index, Types.LONG)), value);}
    void setFloatField(int index, float value) {setInt(offset(check(index, Types.FLOAT)), Float.floatToIntBits(value));}
    void setDoubleField(int index, double value) {setLong(offset(check(index, Types.DOUBLE)), Double.doubleToLongBits(value));}
    void setCharField(int index, char value) {setInt(offset(check(index, Types.CHAR)), (int)value);}
    void setBooleanField(int index, boolean value) {setByte(offset(check(index, Types.BOOLEAN)), value ? (byte)1 : (byte)0);}
    void setObjectField(int index, AnyPersistent value) {setObject(offset(check(index, Types.OBJECT)), value);}

    // TODO: identity beyond one JVM instance, this is no longer needed because of ObjectCache
    public final boolean is(AnyPersistent obj) {
        return getPointer().region().addr() == obj.getPointer().region().addr();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof AnyPersistent && ((AnyPersistent)obj).is(this);
    }

    @Override
    public int hashCode() {
        return (int)getPointer().region().addr();
    }

    protected List<PersistentType> types() {
        return ((ObjectType<?>)pointer.type()).getTypes();
    }

    protected int fieldCount() {
        return types().size();
    }

    protected long offset(int index) {
        return ((ObjectType<?>)getPointer().type()).getOffset(index);
    }

    protected void initializeField(int index, PersistentType t)
    {
        if (t == Types.BYTE) setByteField(index, (byte)0);
        else if (t == Types.SHORT) setShortField(index, (short)0);
        else if (t == Types.INT) setIntField(index, 0);
        else if (t == Types.LONG) setLongField(index, 0L);
        else if (t == Types.FLOAT) setFloatField(index, 0f);
        else if (t == Types.DOUBLE) setDoubleField(index, 0d);
        else if (t == Types.CHAR) setCharField(index, (char)0);
        else if (t == Types.BOOLEAN) setBooleanField(index, false);
        else if (t instanceof ObjectType) setObjectField(index, null);
    }

    @SuppressWarnings("unchecked")
    private void initializeField(long offset, PersistentType t) {
        // System.out.format("type = %s, initializeField(%d. %s)\n", getPointer().type(), offset, t);
        if (t == Types.BYTE) setByte(offset, (byte)0);
        else if (t == Types.SHORT) setShort(offset, (short)0);
        else if (t == Types.INT) setInt(offset, 0);
        else if (t == Types.LONG) {/*System.out.println("mem region " + getPointer().addr() + ": init offset " + offset + " to 0L");*/ setLong(offset, 0L);}
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
        boolean result = true;
        if (index < 0 || index >= fieldCount()) throw new IndexOutOfBoundsException("No such field index: " + index);
        PersistentType t = types().get(index);
        if (t instanceof ObjectType && testType instanceof ObjectType) {
            ObjectType<?> fieldType = (ObjectType)t;
            ObjectType<?> test = (ObjectType) testType;
            if (!test.cls().isAssignableFrom(fieldType.cls())) result = false;
            else if (t != testType) result = false;
            if (!result) throw new RuntimeException("Type mismatch in " + getType().cls() + " at index " + index + ": expected " + testType + ", found " + types().get(index));
        }
        return index;
    }

    protected int getVersion() {
        return getInt(offset(Header.VERSION));
    }

    protected void setVersion(int version) {
        setIntField(Header.VERSION, version);
    }

    protected void setTypeName(String name) {
        Transaction.run(() -> {
            RawString rs = new RawString(name);
            setLongField(Header.TYPE_NAME, rs.getRegion().addr());
        }, this);
    }

    static String typeNameFromRegion(MemoryRegion region) {
        return new RawString(region).toString();
    }

     synchronized int getRefCount() {
        MemoryRegion reg = getPointer().region();
        return reg.getInt(Header.TYPE.getOffset(Header.REF_COUNT));
    }

    void incRefCount() {
        MemoryRegion reg = getPointer().region();
        Transaction.run(() -> {
            int oldCount = reg.getInt(Header.TYPE.getOffset(Header.REF_COUNT));
            reg.putInt(Header.TYPE.getOffset(Header.REF_COUNT), oldCount + 1);
            // trace(getPointer().addr(), "incRefCount(), type = %s, old = %d, new = %d",getPointer().type(), oldCount, getRefCount());
        }, this);
    }

    int decRefCount() {
        MemoryRegion reg = getPointer().region();
        Box<Integer> newCount = new Box<>();
        Transaction.run(() -> {
            int oldCount = reg.getInt(Header.TYPE.getOffset(Header.REF_COUNT));
            newCount.set(oldCount - 1);
            // trace(getPointer().addr(), "decRefCount, type = %s, old = %d, new = %d", getPointer().type(), oldCount, newCount.get());
            if (newCount.get() < 0) {
               trace(true, reg.addr(), "decRef below 0");
               new RuntimeException().printStackTrace(); System.exit(-1);}
            reg.putInt(Header.TYPE.getOffset(Header.REF_COUNT), newCount.get());
        }, this);
        return newCount.get();
    }

    void addReference() {
        Transaction.run(() -> {
            incRefCount();
            setColor(CycleCollector.BLACK);
        }, this);
    }

    synchronized void deleteReference() {
        assert(!getPointer().type().isValueBased());
        Deque<Long> addrsToDelete = new ArrayDeque<>();
        MemoryRegion reg = getPointer().region();
        Transaction.run(() -> {
            int count = 0;
            int newCount = decRefCount();
            if (newCount == 0) {
                // trace(getPointer().addr(), "deleteReference, newCount == 0");
                addrsToDelete.push(getPointer().addr());
                while (!addrsToDelete.isEmpty()) {
                    long addrToDelete = addrsToDelete.pop();
                    Iterator<Long> childAddresses = getChildAddressIterator(addrToDelete);
                    ArrayList<AnyPersistent> children = new ArrayList<>();
                    while (childAddresses.hasNext()) {
                        children.add(ObjectCache.get(childAddresses.next(), true));
                    }
                    // Transaction.run(() -> {
                    // }, children.toArray(new AnyPersistent[0]));
                    for (AnyPersistent child : children) {
                        assert(!child.getPointer().type().isValueBased());
                        Transaction.run(() -> {
                            long childAddr = child.getPointer().addr();
                            int crc = child.decRefCount();
                            if (crc == 0) {
                                addrsToDelete.push(childAddr);
                            } else {
                                CycleCollector.addCandidate(childAddr);
                            }
                        }, child);
                    }
                    free(addrToDelete);
                }
            } else {
                CycleCollector.addCandidate(getPointer().addr());
            }
        }, this);
    }

    public static void deleteResidualReferences(long address, int count) {
        AnyPersistent obj = ObjectCache.get(address, true);
        assert(!obj.getPointer().type().isValueBased());
        Transaction.run(() -> {
            int rc = obj.getRefCount();
            trace(address, "deleteResidualReferences %d, refCount = %d", count, obj.getRefCount());
            if (obj.getRefCount() < count) {
                trace(true, address, "refCount %d < count %d", obj.getRefCount(), count);
                System.exit(-1);
            }
            for (int i = 0; i < count - 1; i++) obj.decRefCount();
            obj.deleteReference();
        }, obj);
    }

    static String classNameForRegion(MemoryRegion reg) {
        long typeNameAddr = reg.getLong(0);
        MemoryRegion typeNameRegion = new UncheckedPersistentMemoryRegion(typeNameAddr);
        return AnyPersistent.typeNameFromRegion(typeNameRegion);
    }

    static Iterator<Long> getChildAddressIterator(long address) {
        trace(address, "getChildAddressIterator");
        MemoryRegion reg = ObjectCache.get(address, true).getPointer().region();
        String typeName = classNameForRegion(reg);
        ObjectType<?> type = Types.typeForName(typeName);

        ArrayList<Long> childAddresses = new ArrayList<>();
        if (type instanceof ArrayType) {
            ArrayType<?> arrType = (ArrayType)type;
            if (arrType.getElementType() == Types.OBJECT) {
                trace(address, "elementType == Types.OBJECT");
                int length = reg.getInt(ArrayType.LENGTH_OFFSET);
                for (int i = 0; i < length; i++) {
                    long childAddr = reg.getLong(arrType.getElementOffset(i));
                    if (childAddr != 0) {
                        childAddresses.add(childAddr);
                    }
                }
            }
        } else if (type instanceof ObjectType) {
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
        } else {
            throw new RuntimeException("getChildAddressIterator: unexpected type");
        }
        return childAddresses.iterator();
    }

    void setColor(byte color) {
        setColor(color, false);
    }

    void setColor(byte color, boolean cc) {
        if (CycleCollector.isProcessing() && !cc) {
            CycleCollector.stashObjColorChange(getPointer().addr(), color);
        } else {
            getPointer().region().putByte(Header.TYPE.getOffset(Header.REF_COLOR), color);
        }
    }

    byte getColor() {
        return getPointer().region().getByte(Header.TYPE.getOffset(Header.REF_COLOR));
    }

    public static boolean monitorEnter(List<AnyPersistent> toLock, List<AnyPersistent> locked, boolean block) {
        trace("monitorEnter (lists), starting toLock = %d, locked = %d, block = %s", toLock.size(), locked.size(), block);
        // toLock.sort((x, y) -> Long.compare(x.getPointer().addr(), y.getPointer().addr()));
        boolean success = true;
        for (AnyPersistent obj : toLock) {
            if (!block) {
                if (!obj.monitorEnterTimeout()) {
                    success = false;
                    // trace("TIMEOUT exceeded");
                    for(AnyPersistent lockedObj : locked) {
                        lockedObj.monitorExit();
                        // trace("removed locked obj %d", obj.getPointer().addr());
                    }
                    locked.clear();
                    break;
                }
                else {
                    locked.add(obj);
                    // trace("added locked obj %d", obj.getPointer().addr());
                }
            }
            else obj.monitorEnter();
        }
        // trace("monitorEnter (lists), exiting toLock = %d, locked = %d", toLock.size(), locked.size());
        return success;
    }

    public boolean monitorEnterTimeout() {
        TransactionInfo info = lib.xpersistent.XTransaction.tlInfo.get();
        int max = info.timeout + random.nextInt(info.timeout);
        boolean success = monitorEnterTimeout(max);
        if (success) {
            info.timeout = Config.MONITOR_ENTER_TIMEOUT;
        }
        else {
            info.timeout = Math.min((int)(info.timeout * Config.MONITOR_ENTER_TIMEOUT_INCREASE_FACTOR), Config.MAX_MONITOR_ENTER_TIMEOUT);
        }
        return success;
    }

    public void monitorEnter() {
        // trace(true, getPointer().addr(), "blocking monitorEnter for %s, attempt = %d", this.getPointer().addr(), lib.xpersistent.XTransaction.tlInfo.get().attempts);
        if (Config.USE_SEPARATE_TRANSACTION_LOCKS) lock.lock();
        else UNSAFE.monitorEnter(this);
        // trace(true, getPointer().addr(), "blocking monitorEnter for %s exit", this.getPointer().addr());
    }

    public boolean monitorEnterTimeout(long timeout) {
        if (Config.USE_BLOCKING_LOCKS_FOR_DEBUG) {
            monitorEnter();
            return true;
        }
        if (Config.USE_SEPARATE_TRANSACTION_LOCKS) {
            try {
                return lock.tryLock(timeout, TimeUnit.MILLISECONDS);
            }
            catch (InterruptedException ie) {throw new RuntimeException(ie.getMessage());}
        }
        // fallthrough
        boolean success = false;
        long start = System.currentTimeMillis();
        int count = 0;
        do {
            success = UNSAFE.tryMonitorEnter(this);
            if (success) break;
            count++;
            // Stats.current.locks.spinIterations++;
            // if (count > 2000) try {count = 0; Thread.sleep(1);} catch (InterruptedException ie) {ie.printStackTrace();}
        } while (System.currentTimeMillis() - start < timeout);
        // if (success) Stats.current.locks.acquired++;
        // else Stats.current.locks.timeouts++;
        return success;
    }

    public void monitorExit() {
        if (Config.USE_SEPARATE_TRANSACTION_LOCKS) lock.unlock();
        else UNSAFE.monitorExit(this);
    }
}
