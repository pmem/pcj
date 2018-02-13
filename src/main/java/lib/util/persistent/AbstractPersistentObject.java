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
import lib.util.persistent.types.FinalByteField;
import lib.util.persistent.types.FinalShortField;
import lib.util.persistent.types.FinalIntField;
import lib.util.persistent.types.FinalLongField;
import lib.util.persistent.types.FinalFloatField;
import lib.util.persistent.types.FinalDoubleField;
import lib.util.persistent.types.FinalCharField;
import lib.util.persistent.types.FinalBooleanField;
import lib.util.persistent.types.FinalObjectField;
import lib.util.persistent.types.PersistentField;
import java.lang.reflect.Constructor;
import static lib.util.persistent.Trace.*;
import java.util.function.Consumer;
import java.util.Arrays;

@SuppressWarnings("sunapi")
abstract class AbstractPersistentObject extends AnyPersistent {
    protected boolean[] uninitializedFieldState; 

    public AbstractPersistentObject(ObjectType<? extends AbstractPersistentObject> type) {
        super(type);
        if (Config.ENABLE_ALLOC_STATS) Stats.current.allocStats.update(type.cls().getName(), type.getAllocationSize(), Stats.AllocationStats.WRAPPER_PER_INSTANCE + 8, 1);  // uncomment for allocation stats
    }

    @SuppressWarnings("unchecked")
    protected <T extends AnyPersistent> AbstractPersistentObject(ObjectType<? extends AbstractPersistentObject> type, Consumer<T> initializer) {
        this(type);
        uninitializedFieldState = new boolean[type.fieldCount()];
        Arrays.fill(uninitializedFieldState, true);
        Transaction.run(() -> {
            initializer.accept((T)this);
        });
        uninitializedFieldState = null;        
    }

    <T extends AnyPersistent> AbstractPersistentObject(ObjectType<T> type, MemoryRegion region) {
        super(type, region);
    }

    public AbstractPersistentObject(ObjectPointer<? extends AnyPersistent> p) {
        super(p);
        if (Config.ENABLE_ALLOC_STATS) Stats.current.allocStats.update(p.type().cls().getName() + "<rctor>", 0, Stats.AllocationStats.WRAPPER_PER_INSTANCE + 8, 1);   // uncomment for allocation stats
    }

    public byte getByteField(ByteField f) {return getByte(offset(check(f.getIndex(), Types.BYTE)));}
    public short getShortField(ShortField f) {return getShort(offset(check(f.getIndex(), Types.SHORT)));}
    public int getIntField(IntField f) {return getInt(offset(check(f.getIndex(), Types.INT)));}
    public long getLongField(LongField f) {/*trace(true, "APO.getLongField(%s)", f); */return getLong(offset(check(f.getIndex(), Types.LONG)));}
    public float getFloatField(FloatField f) {return Float.intBitsToFloat(getInt(offset(check(f.getIndex(), Types.FLOAT))));}
    public double getDoubleField(DoubleField f) {return Double.longBitsToDouble(getLong(offset(check(f.getIndex(), Types.DOUBLE))));}
    public char getCharField(CharField f) {return (char)getInt(offset(check(f.getIndex(), Types.CHAR)));}
    public boolean getBooleanField(BooleanField f) {return getByte(offset(check(f.getIndex(), Types.BOOLEAN))) == 0 ? false : true;}

    @SuppressWarnings("unchecked") public <T extends AnyPersistent> T getObjectField(ObjectField<T> f) {
        // trace(true, "APO.getObjectField(%s) : OF", f);        
        return (T)getObject(offset(f.getIndex()), f.getType());
    }

    @SuppressWarnings("unchecked") public <T extends AnyPersistent> T getObjectField(ValueField<T> f) {
        // trace(true, "APO.getObjectField(%s) : VF", f);        
        return getValueObject(offset(f.getIndex()), f.getType());
    }

    public byte getByteField(FinalByteField f) {return getRegionByte(offset(check(f.getIndex(), Types.BYTE)));}
    public short getShortField(FinalShortField f) {return getRegionShort(offset(check(f.getIndex(), Types.SHORT)));}
    public int getIntField(FinalIntField f) {return getRegionInt(offset(check(f.getIndex(), Types.INT)));}
    public long getLongField(FinalLongField f) {/*trace(true, "APO.getLongField(%s) : FLF", f); */return getRegionLong(offset(check(f.getIndex(), Types.LONG)));}
    public float getFloatField(FinalFloatField f) {return Float.intBitsToFloat(getRegionInt(offset(check(f.getIndex(), Types.FLOAT))));}
    public double getDoubleField(FinalDoubleField f) {return Double.longBitsToDouble(getRegionLong(offset(check(f.getIndex(), Types.DOUBLE))));}
    public char getCharField(FinalCharField f) {return (char)getRegionInt(offset(check(f.getIndex(), Types.CHAR)));}
    public boolean getBooleanField(FinalBooleanField f) {return getRegionByte(offset(check(f.getIndex(), Types.BOOLEAN))) == 0 ? false : true;}

    @SuppressWarnings("unchecked") public <T extends AnyPersistent> T getObjectField(FinalObjectField<T> f) {
        // trace(true, "APO.getObjectField(%s) : FOF", f);
        T ans = null;
        long addr = getRegionLong(offset(f.getIndex()));
        if (addr != 0) ans = (T)ObjectCache.get(addr);
        return ans;
    }

    public void initByteField(FinalByteField f, byte value) {checkUninitializedField(f); setByte(offset(check(f.getIndex(), Types.BYTE)), value);}
    public void initShortField(FinalShortField f, short value) {checkUninitializedField(f); setShort(offset(check(f.getIndex(), Types.SHORT)), value);}
    public void initIntField(FinalIntField f, int value) {checkUninitializedField(f); setInt(offset(check(f.getIndex(), Types.INT)), value);}
    public void initLongField(FinalLongField f, long value) {/*trace(true, "APO.initLongField(%s) : FLF", f);*/checkUninitializedField(f); setLong(offset(check(f.getIndex(), Types.LONG)), value);}
    public void initFloatField(FinalFloatField f, float value) {checkUninitializedField(f); setInt(offset(check(f.getIndex(), Types.FLOAT)), Float.floatToIntBits(value));}
    public void initDoubleField(FinalDoubleField f, double value) {checkUninitializedField(f); setLong(offset(check(f.getIndex(), Types.DOUBLE)), Double.doubleToLongBits(value));}
    public void initCharField(FinalCharField f, char value) {checkUninitializedField(f); setInt(offset(check(f.getIndex(), Types.CHAR)), (int)value);}
    public void initBooleanField(FinalBooleanField f, boolean value) {checkUninitializedField(f); setByte(offset(check(f.getIndex(), Types.BOOLEAN)), value ? (byte)1 : (byte)0);}
    public <T extends AnyPersistent> void initObjectField(FinalObjectField<T> f, T value) {/*trace(true, "APO.initObjectField(%s) : FOF", f);*/checkUninitializedField(f); setObjectField(f.getIndex(), value);}

    public <T extends AnyPersistent> void initObjectField(ObjectField<T> f, T value) {/*trace(true, "APO.initObjectField(%s) : OF", f); */checkUninitializedField(f); setObjectField(f.getIndex(), value);}

    private void checkUninitializedField(PersistentField f) {
        if (uninitializedFieldState == null || !uninitializedFieldState[f.getIndex()]) throw new RuntimeException("field already initialized");
    }
}

