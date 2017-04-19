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
import java.util.concurrent.locks.ReentrantLock;
import java.lang.reflect.Constructor;

import lib.xpersistent.XHeap;
import lib.xpersistent.XRoot;

public class PersistentObject implements Persistent<PersistentObject> {
    private static final ThreadLocal<Boolean> weakConstruction = ThreadLocal.withInitial(() -> false);
    static final PersistentHeap heap = PersistentMemoryProvider.getDefaultProvider().getHeap();

    private final ObjectPointer<? extends PersistentObject> pointer;
    public final ReentrantLock lock = new ReentrantLock();

    public PersistentObject(ObjectType<? extends PersistentObject> type) {
        this(type, PersistentMemoryProvider.getDefaultProvider().getHeap().allocateRegion(type.getAllocationSize()));
    }

    <T extends PersistentObject> PersistentObject(ObjectType<T> type, MemoryRegion region) {
        // System.out.println("Creating object of type " + type.getName() + " at address " + region.addr());
        this.pointer = new ObjectPointer<T>(type, region);
        List<PersistentType> ts = type.getTypes();
        for (int i = 0; i < ts.size(); i++) initializeField(i, ts.get(i));
        Transaction.run(() -> {
            setTypeName(type.getName());
            setVersion(99);
            initForGC();
        });
        ObjectCache.addReference(this);
    }

    protected PersistentObject(ObjectPointer<? extends PersistentObject> p) {
        this.pointer = p;
        if (weakConstruction.get()) weakConstruction.set(false);
        else initForGC();
    }

    void initForGC() {
        Transaction.run(() -> {
            incRefCount();
            ObjectDirectory.registerObject(this);
            if (heap instanceof XHeap && ((XHeap)heap).getDebugMode() == true) {
                ((XRoot)(heap.getRoot())).addToAllObjects(getPointer().region().addr());
            }
        });
    }

    // only called by Root during bootstrap of Object directory PersistentHashMap
    @SuppressWarnings("unchecked")
    public static <T extends PersistentObject> T weakFromPointer(ObjectPointer<T> p) {
        weakConstruction.set(true);
        try {
            Class<T> cls = p.type().cls();
            Constructor ctor = cls.getDeclaredConstructor(ObjectPointer.class);
            ctor.setAccessible(true);
            T obj = (T)ctor.newInstance(p);
            return obj;
        }
        catch (Exception e) {e.printStackTrace();}
        weakConstruction.set(false);
        return null;
    }

    public static PersistentObject getObjectAtAddress(long addr) {
        return getObjectAtAddress(addr, ObjectCache.STRONG);
    }

    @SuppressWarnings("unchecked")
    static PersistentObject getObjectAtAddress(long addr, boolean strength) {
        assert(addr != 0);
        PersistentObject obj = ObjectCache.getReference(addr, strength);
        return obj;
    }

    static synchronized int decRefCountAtAddressBy(long addr, int n) {
        // System.out.format("decRefCountAtAddressBy(%d, %d)\n", addr, n);
        PersistentObject obj = getObjectAtAddress(addr, ObjectCache.WEAK);
        return (obj == null) ? 0 : obj.decRefCountBy(n);
    }
    // end special path functions

    @SuppressWarnings("unchecked")
    void delete() {
        long addr = pointer.region().addr();
        // System.out.format("delete called on object (%s) at address %d\n", getPointer().type().cls(), addr);
        List<PersistentType> ts = types();
        List<PersistentObject> children = new java.util.ArrayList<>();
        for (int i = 0; i < ts.size(); i++) {
            if ((ts.get(i) instanceof ObjectType || ts.get(i) == Types.OBJECT) && getLong(getPointer().type().getOffset(i)) != 0) {
                long childAddress = getLong(getPointer().type().getOffset(i));
                PersistentObject obj = getObjectAtAddress(childAddress, ObjectCache.WEAK);
                children.add(obj);
                obj.lock.lock();
            }
        }
        Transaction.run(() -> {
            for (PersistentObject obj : children) {
                obj.decRefCount();
            }
            refColor(CycleCollector.BLACK);
            //if (!CycleCollector.isCandidate(addr)) {
                free();
            //}
        });
        for (PersistentObject obj : children) {
            obj.lock.unlock();
        }
    }

    synchronized void free() {
        ObjectCache.removeReference(pointer.region().addr());
        PersistentHeap heap = PersistentMemoryProvider.getDefaultProvider().getHeap();
        MemoryRegion nameRegion = heap.regionFromAddress(getLongField(Header.TYPE_NAME));
        synchronized(nameRegion) {
            Transaction.run(() -> {
                heap.freeRegion(nameRegion);
                heap.freeRegion(pointer.region());
                if (heap instanceof XHeap && ((XHeap)heap).getDebugMode() == true) {
                    ((XRoot)(heap.getRoot())).removeFromAllObjects(pointer.region().addr());
                }
            });
        }
    }

    public ObjectPointer<? extends PersistentObject> getPointer() {return pointer;}

    ObjectType getType() {return pointer.type();}

    byte getByte(long offset) {return pointer.region().getByte(offset);}
    short getShort(long offset) {return pointer.region().getShort(offset);}
    int getInt(long offset) {return pointer.region().getInt(offset);}
    long getLong(long offset) {return pointer.region().getLong(offset);}

    // assume MemoryRegion will be in transactional mode
    void setByte(long offset, byte value) {pointer.region().putByte(offset, value);}
    void setShort(long offset, short value) {pointer.region().putShort(offset, value);}
    void setInt(long offset, int value) {pointer.region().putInt(offset, value);}
    void setLong(long offset, long value) {pointer.region().putLong(offset, value);}

    @SuppressWarnings("unchecked")
    synchronized <T extends PersistentObject> T getObject(long offset) {
        long valueAddr = getLong(offset);
        if (valueAddr == 0) return null;
        return (T)ObjectCache.getReference(valueAddr);
    }

    synchronized void setObject(long offset, PersistentObject value) {
        PersistentObject old = getObject(offset);
        Transaction.run(() -> {
            if (value != null) value.incRefCount();
            if (old != null) old.decRefCount();
            setLong(offset, value == null ? 0 : value.getPointer().region().addr());
        }, value, old);
    }

    byte getByteField(int index) {return getByte(offset(check(index, Types.BYTE)));}
    short getShortField(int index) {return getShort(offset(check(index, Types.SHORT)));}
    int getIntField(int index) {return getInt(offset(check(index, Types.INT)));}
    long getLongField(int index) {return getLong(offset(check(index, Types.LONG)));}
    float getFloatField(int index) {return Float.intBitsToFloat(getInt(offset(check(index, Types.FLOAT))));}
    double getDoubleField(int index) {return Double.longBitsToDouble(getLong(offset(check(index, Types.DOUBLE))));}
    char getCharField(int index) {return (char)getInt(offset(check(index, Types.CHAR)));}
    boolean getBooleanField(int index) {return getByte(offset(check(index, Types.BOOLEAN))) == 0 ? false : true;}
    PersistentObject getObjectField(int index) {return getObject(offset(check(index, Types.OBJECT)));}

    void setByteField(int index, byte value) {setByte(offset(check(index, Types.BYTE)), value);}
    void setShortField(int index, short value) {setShort(offset(check(index, Types.SHORT)), value);}
    void setIntField(int index, int value) {setInt(offset(check(index, Types.INT)), value);}
    void setLongField(int index, long value) {setLong(offset(check(index, Types.LONG)), value);}
    void setFloatField(int index, float value) {setInt(offset(check(index, Types.FLOAT)), Float.floatToIntBits(value));}
    void setDoubleField(int index, double value) {setLong(offset(check(index, Types.DOUBLE)), Double.doubleToLongBits(value));}
    void setCharField(int index, char value) {setInt(offset(check(index, Types.CHAR)), (int)value);}
    void setBooleanField(int index, boolean value) {setByte(offset(check(index, Types.BOOLEAN)), value ? (byte)1 : (byte)0);}
    void setObjectField(int index, PersistentObject value) {setObject(offset(check(index, Types.OBJECT)), value);}

    public byte getByteField(ByteField f) {return getByte(offset(check(f.getIndex(), Types.BYTE)));}
    public short getShortField(ShortField f) {return getShort(offset(check(f.getIndex(), Types.SHORT)));}
    public int getIntField(IntField f) {return getInt(offset(check(f.getIndex(), Types.INT)));}
    public long getLongField(LongField f) {return getLong(offset(check(f.getIndex(), Types.LONG)));}
    public float getFloatField(FloatField f) {return Float.intBitsToFloat(getInt(offset(check(f.getIndex(), Types.FLOAT))));}
    public double getDoubleField(DoubleField f) {return Double.longBitsToDouble(getLong(offset(check(f.getIndex(), Types.DOUBLE))));}
    public char getCharField(CharField f) {return (char)getInt(offset(check(f.getIndex(), Types.CHAR)));}
    public boolean getBooleanField(BooleanField f) {return getByte(offset(check(f.getIndex(), Types.BOOLEAN))) == 0 ? false : true;}
    @SuppressWarnings("unchecked") public <T extends PersistentObject> T getObjectField(ObjectField<T> f) {return (T)getObjectField(f.getIndex());}

    @SuppressWarnings("unchecked")
    public synchronized <T extends PersistentValue> T getValueField(ValueField<T> f) {
        MemoryRegion srcRegion = heap.regionFromAddress(getPointer().region().addr());
        MemoryRegion dstRegion = heap.allocateRegion(f.getType().getSize());
        // System.out.println(String.format("getValueField src addr = %d, dst addr = %d, size = %d", srcRegion.addr(), dstRegion.addr(), f.getType().getSize()));
        synchronized(srcRegion) {
            synchronized(dstRegion) {
                ((lib.xpersistent.XHeap)heap).memcpy(srcRegion, offset(f.getIndex()), dstRegion, 0, f.getType().getSize());
            }
        }
        return (T)new ValuePointer((ValueType)f.getType(), dstRegion, f.cls()).deref();
    }

    public synchronized <T extends PersistentValue> void setValueField(ValueField<T> f, T value) {
        MemoryRegion dstRegion = getPointer().region();
        long dstOffset = offset(f.getIndex());
        MemoryRegion srcRegion = value.getPointer().region();
        // System.out.println(String.format("setValueField src addr = %d, dst addr = %d, size = %d", srcRegion.addr(), dstRegion.addr() + dstOffset, f.getType().getSize()));
        synchronized(srcRegion) {
            ((lib.xpersistent.XHeap)heap).memcpy(srcRegion, 0, dstRegion, dstOffset, f.getType().getSize());
        }
    }

    public void setByteField(ByteField f, byte value) {setByte(offset(check(f.getIndex(), Types.BYTE)), value);}
    public void setShortField(ShortField f, short value) {setShort(offset(check(f.getIndex(), Types.SHORT)), value);}
    public void setIntField(IntField f, int value) {setInt(offset(check(f.getIndex(), Types.INT)), value);}
    public void setLongField(LongField f, long value) {setLong(offset(check(f.getIndex(), Types.LONG)), value);}
    public void setFloatField(FloatField f, float value) {setInt(offset(check(f.getIndex(), Types.FLOAT)), Float.floatToIntBits(value));}
    public void setDoubleField(DoubleField f, double value) {setLong(offset(check(f.getIndex(), Types.DOUBLE)), Double.doubleToLongBits(value));}
    public void setCharField(CharField f, char value) {setInt(offset(check(f.getIndex(), Types.CHAR)), (int)value);}
    public void setBooleanField(BooleanField f, boolean value) {setByte(offset(check(f.getIndex(), Types.BOOLEAN)), value ? (byte)1 : (byte)0);}
    public <T extends PersistentObject> void setObjectField(ObjectField<T> f, T value) {setObjectField(f.getIndex(), value);}

    // identity
    public final boolean is(PersistentObject obj) {
        return getPointer().region().addr() == obj.getPointer().region().addr();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PersistentObject && ((PersistentObject)obj).is(this);
    }

    @Override
    public int hashCode() {
        return (int)getPointer().region().addr();
    }

    private List<PersistentType> types() {
        return ((ObjectType<?>)pointer.type()).getTypes();
    }

    private int fieldCount() {
        return types().size();
    }

    private long offset(int index) {
        return ((ObjectType<?>)getPointer().type()).getOffset(index);
    }

    private void initializeField(int index, PersistentType t)
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

    // we can turn this off after debug since only Field-based getters and setters are public
    // and those provide static type safety and internally assigned indexes
    private int check(int index, PersistentType testType) {
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

    private int getVersion() {
        return getIntField(Header.VERSION);
    }

    private void setVersion(int version) {
        setIntField(Header.VERSION, version);
    }

    private void setTypeName(String name) {
        Transaction.run(() -> {
            RawString rs = new RawString(name);
            setLongField(Header.TYPE_NAME, rs.getRegion().addr());
        });
    }

    private String typeName() {
        long addr = getLongField(Header.TYPE_NAME);
        PersistentHeap heap = PersistentMemoryProvider.getDefaultProvider().getHeap();
        MemoryRegion region = heap.regionFromAddress(addr);
        return new RawString(region).toString();
    }

    static String typeNameFromRegion(MemoryRegion region) {
        return new RawString(region).toString();
    }

    int getRefCount() {
        return getIntField(Header.REF_COUNT);
    }

    synchronized void incRefCountBy(int n) {
        // System.out.println(String.format("incRefCountBy %s @ %d by %d (%d -> %d)", getPointer().type().cls(), getPointer().addr(), n, getRefCount(), getRefCount() + n));
        Transaction.run(() -> {
            setIntField(Header.REF_COUNT, getRefCount() + n);
            CycleCollector.markBlack(this);
        });
    }

    void incRefCount() {
        incRefCountBy(1);
    }

    void speculativeIncRefCountBy(int n) {
        // System.out.println(String.format("speculativeIncRefCountBy %s @ %d by %d (%d -> %d)", getPointer().type().cls(), getPointer().addr(), n, getRefCount(), getRefCount() + n));
        setIntField(Header.REF_COUNT, getRefCount() + n);
    }

    private synchronized int decRefCountBy(int n) {
        // System.out.format("decRefCountBy %s @ %d by %d (%d -> %d)\n", getPointer().type().cls(), getPointer().addr(), n, getRefCount(), getRefCount() - n);
        if (getRefCount() <= 0) {
            System.out.println(String.format("decRefCountBy %s @ %d by %d (%d -> %d)", getPointer().type().cls(), getPointer().addr(), n, getRefCount(), getRefCount() - n));
            assert(false);
        }
        int newCount = getRefCount() - n;
        if (newCount < 0) {
            System.out.println("DEBUG: decRef by " + n + ", newCount = " + newCount + ", obj = " + this);
            new Exception().printStackTrace();
            System.exit(-1);
        }
        if (newCount == 0) {
            Transaction.run(() -> {
                setIntField(Header.REF_COUNT, newCount);
                delete();
            });
        }
        else {
            Transaction.run(() -> {
                setIntField(Header.REF_COUNT, newCount);
                // System.out.println(String.format("else clause, set REF_COUNT of %d to %d", getPointer().addr(), newCount));
                CycleCollector.candidate(this);
            });
        }
        return newCount;
    }

    int decRefCount() {
        return decRefCountBy(1);
    }

    void speculativeDecRefCountBy(int n) {
        // System.out.println(String.format("speculativeDecRefCountBy %s @ %d by %d (%d -> %d)", getPointer().type().cls(), getPointer().addr(), n, getRefCount(), getRefCount() - n));
        setIntField(Header.REF_COUNT, getRefCount() - n);
    }

    byte refColor() {
        return getByteField(Header.REF_COLOR);
    }

    void refColor(byte refColor) {
        setByteField(Header.REF_COLOR, refColor);
    }
}
