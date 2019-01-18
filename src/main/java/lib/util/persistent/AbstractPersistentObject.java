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
import lib.util.persistent.types.GenericField;
import lib.util.persistent.types.FinalByteField;
import lib.util.persistent.types.FinalShortField;
import lib.util.persistent.types.FinalIntField;
import lib.util.persistent.types.FinalLongField;
import lib.util.persistent.types.FinalFloatField;
import lib.util.persistent.types.FinalDoubleField;
import lib.util.persistent.types.FinalCharField;
import lib.util.persistent.types.FinalBooleanField;
import lib.util.persistent.types.FinalObjectField;
import lib.util.persistent.types.FinalValueField;
import lib.util.persistent.types.PersistentField;
import lib.xpersistent.UncheckedPersistentMemoryRegion;
import java.lang.reflect.Constructor;
import static lib.util.persistent.Trace.*;
import java.util.function.Consumer;
import java.util.Arrays;

@SuppressWarnings("sunapi")
abstract class AbstractPersistentObject extends AnyPersistent {
    private boolean uninitializedFieldState; 

    public AbstractPersistentObject(ObjectType<? extends AbstractPersistentObject> type) {
        super(type);
        if (Config.ENABLE_ALLOC_STATS) Stats.current.allocStats.update(type.cls().getName(), type.allocationSize(), Stats.AllocationStats.WRAPPER_PER_INSTANCE + 8, 1);  // uncomment for allocation stats
    }

    @SuppressWarnings("unchecked")
    protected <T extends AnyPersistent> AbstractPersistentObject(ObjectType<? extends AbstractPersistentObject> type, Consumer<T> initializer) {
        this(type);
        uninitializedFieldState = true;
        initializer.accept((T)this);
        uninitializedFieldState = false;//null;        
    }

    <T extends AnyPersistent> AbstractPersistentObject(ObjectType<T> type, MemoryRegion region) {
        super(type, region);
    }

    public AbstractPersistentObject(ObjectPointer<? extends AnyPersistent> p) {
        super(p);
        if (Config.ENABLE_ALLOC_STATS) Stats.current.allocStats.update(p.type().cls().getName() + "<rctor>", 0, Stats.AllocationStats.WRAPPER_PER_INSTANCE + 8, 1);   // uncomment for allocation stats
    }

    public byte getByteField(ByteField f) {return getByte(offset(f.getIndex()));}
    public short getShortField(ShortField f) {return getShort(offset(f.getIndex()));}
    public int getIntField(IntField f) {return getInt(offset(f.getIndex()));}
    public long getLongField(LongField f) {/*trace(true, "APO.getLongField(%s)", f); */return getLong(offset(f.getIndex()));}
    public float getFloatField(FloatField f) {return Float.intBitsToFloat(getInt(offset(f.getIndex())));}
    public double getDoubleField(DoubleField f) {return Double.longBitsToDouble(getLong(offset(f.getIndex())));}
    public char getCharField(CharField f) {return (char)getInt(offset(f.getIndex()));}
    public boolean getBooleanField(BooleanField f) {return getByte(offset(f.getIndex())) == 0 ? false : true;}

    @SuppressWarnings("unchecked") public <T extends AnyPersistent> T getObjectField(ObjectField<T> f) {
        // trace(true, "APO.getObjectField(%s) : OF", f);        
        return (T)getObject(offset(f.getIndex()), f.getType());
    }

    @SuppressWarnings("unchecked") public <T extends AnyPersistent> T getObjectField(ValueField<T> f) {
        // trace(true, "APO.getObjectField(%s) : VF", f);        
        return getValueObject(offset(f.getIndex()), f.getType());
    }

    @SuppressWarnings("unchecked") public <T extends AnyPersistent> T getObjectField(GenericField<? extends AnyPersistent> f) {
        // trace(true, "APO.getObjectField(%s) : GF, index = %d, offset for index = %d", f, f.getIndex(), offset(f.getIndex())); 
        long fieldAddress = getRegionLong(offset(f.getIndex()));
        long classInfoAddress = new UncheckedPersistentMemoryRegion(fieldAddress).getLong(0);
        ClassInfo classInfo = ClassInfo.getClassInfo(classInfoAddress);
        ObjectType objectType = (ObjectType)classInfo.getType();
        if (objectType.kind() == ObjectType.Kind.Reference) return (T)getObject(offset(f.getIndex()), objectType);
        else if (objectType.kind() == ObjectType.Kind.IndirectValue) return (T)getValueObject(offset(f.getIndex()), objectType);
        else throw new RuntimeException("GenericField with incompatible type: " + objectType); 
    }

    public byte getByteField(FinalByteField f) {return getRegionByte(offset(f.getIndex()));}
    public short getShortField(FinalShortField f) {return getRegionShort(offset(f.getIndex()));}
    public int getIntField(FinalIntField f) {return getRegionInt(offset(f.getIndex()));}
    public long getLongField(FinalLongField f) {/*trace(true, "APO.getLongField(%s) : FLF", f); */return getRegionLong(offset(f.getIndex()));}
    public float getFloatField(FinalFloatField f) {return Float.intBitsToFloat(getRegionInt(offset(f.getIndex())));}
    public double getDoubleField(FinalDoubleField f) {return Double.longBitsToDouble(getRegionLong(offset(f.getIndex())));}
    public char getCharField(FinalCharField f) {return (char)getRegionInt(offset(f.getIndex()));}
    public boolean getBooleanField(FinalBooleanField f) {return getRegionByte(offset(f.getIndex())) == 0 ? false : true;}

    @SuppressWarnings("unchecked") public <T extends AnyPersistent> T getObjectField(FinalObjectField<T> f) {
        // trace(true, "APO.getObjectField(%s) : FOF", f);
        T ans = null;
        long addr = getRegionLong(offset(f.getIndex()));
        if (addr != 0) ans = (T)ObjectCache.get(addr);
        return ans;
    }


    public void initByteField(FinalByteField f, byte value) {checkUninitializedField(f); setRegionByte(offset(f.getIndex()), value);}
    public void initShortField(FinalShortField f, short value) {checkUninitializedField(f); setRegionShort(offset(f.getIndex()), value);}
    public void initIntField(FinalIntField f, int value) {checkUninitializedField(f); setRegionInt(offset(f.getIndex()), value);}
    public void initLongField(FinalLongField f, long value) {/*trace(true, "APO.initLongField(%s) : FLF", f);*/checkUninitializedField(f); setRegionLong(offset(f.getIndex()), value);}
    public void initFloatField(FinalFloatField f, float value) {checkUninitializedField(f); setRegionInt(offset(f.getIndex()), Float.floatToIntBits(value));}
    public void initDoubleField(FinalDoubleField f, double value) {checkUninitializedField(f); setRegionLong(offset(f.getIndex()), Double.doubleToLongBits(value));}
    public void initCharField(FinalCharField f, char value) {checkUninitializedField(f); setRegionInt(offset(f.getIndex()), (int)value);}
    public void initBooleanField(FinalBooleanField f, boolean value) {checkUninitializedField(f); setRegionByte(offset(f.getIndex()), value ? (byte)1 : (byte)0);}
    
    public <T extends AnyPersistent> void initObjectField(FinalObjectField<T> f, T value) {
        /*trace(true, "APO.initObjectField(%s) : FOF", f); */
        if (value != null) {
            checkUninitializedField(f);
            Transaction.run(() -> {
                setLong(offset(f.getIndex()), value.addr());
                value.addReference();
            }, this);
        }
    }

    public <T extends AnyPersistent> void initObjectField(FinalValueField<T> f, T value) {
        // trace(true, "APO.initObjectField(%s) : FVF", f); 
        if (value != null) {
            checkUninitializedField(f);
            value.onSet();
            MemoryRegion region = region();
            long offset = offset(f.getIndex());
            byte[] bytes = ((VolatileMemoryRegion)value.region()).getBytes();
            region.putRawBytes(offset, bytes);
            region.flush(offset, bytes.length);
        }
    }

    private void checkUninitializedField(PersistentField f) {
        if (!uninitializedFieldState) throw new RuntimeException("field already initialized");
    }
}

