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
import lib.util.persistent.types.FinalByteField;
import lib.util.persistent.types.FinalShortField;
import lib.util.persistent.types.FinalIntField;
import lib.util.persistent.types.FinalLongField;
import lib.util.persistent.types.FinalFloatField;
import lib.util.persistent.types.FinalDoubleField;
import lib.util.persistent.types.FinalCharField;
import lib.util.persistent.types.FinalBooleanField;
import lib.util.persistent.types.FinalObjectField;
import java.lang.reflect.Constructor;
import static lib.util.persistent.Trace.*;
import java.util.function.Consumer;

// TODO: refactor base classes to honor Liskov Substitution Principle
@SuppressWarnings("sunapi")
public class PersistentObject extends PersistentImmutableObject {
    public PersistentObject(ObjectType<? extends PersistentObject> type) {
        super(type);
    }

    @SuppressWarnings("unchecked")
    protected <T extends PersistentObject> PersistentObject(ObjectType<? extends PersistentObject> type, Consumer<T> initializer) {
        super(type, initializer);
    }

    <T extends AnyPersistent> PersistentObject(ObjectType<T> type, MemoryRegion region) {
        // trace(region.addr(), "creating object of type %s", type.getName());
        super(type, region);
    }

    public PersistentObject(ObjectPointer<? extends AnyPersistent> p) {
        super(p);
    }

    @Override
    protected byte getByte(long offset) {
        // return Util.synchronizedBlock(this, () -> pointer.region().getByte(offset));
        byte ans;
        TransactionInfo info = lib.xpersistent.XTransaction.tlInfo.get();
        boolean inTransaction = info.state == Transaction.State.Active;
        if (!inTransaction) {
            monitorEnter();
            ans = pointer.region().getByte(offset);
            monitorExit();
        }
        else {
            boolean success = monitorEnterTimeout();
            if (success) {
                info.transaction.addLockedObject(this);
                return pointer.region().getByte(offset);
            }
            else {
                if (inTransaction) throw new TransactionRetryException();
                else throw new RuntimeException("failed to acquire lock (timeout)");
            }
        }
        return ans;
    }

    @Override
    protected short getShort(long offset) {
        // return Util.synchronizedBlock(this, () -> pointer.region().getShort(offset));
        short ans;
        TransactionInfo info = lib.xpersistent.XTransaction.tlInfo.get();
        boolean inTransaction = info.state == Transaction.State.Active;
        if (!inTransaction) {
            monitorEnter();
            ans = pointer.region().getShort(offset);
            monitorExit();
        }
        else {
            boolean success = monitorEnterTimeout();
            if (success) {
                info.transaction.addLockedObject(this);
                ans = pointer.region().getShort(offset);
            }
            else {
                if (inTransaction) throw new TransactionRetryException();
                else throw new RuntimeException("failed to acquire lock (timeout)");
            }
        }
        return ans;
    }

    @Override
    protected int getInt(long offset) {
        // return Util.synchronizedBlock(this, () -> pointer.region().getInt(offset));
        int ans;
        TransactionInfo info = lib.xpersistent.XTransaction.tlInfo.get();
        boolean inTransaction = info.state == Transaction.State.Active;
        if (!inTransaction) {
            monitorEnter();
            ans = pointer.region().getInt(offset);
            monitorExit();
        }
        else {
            boolean success = monitorEnterTimeout();
            if (success) {
                info.transaction.addLockedObject(this);
                ans = pointer.region().getInt(offset);
            }
            else {
                if (inTransaction) throw new TransactionRetryException();
                else throw new RuntimeException("failed to acquire lock (timeout)");
            }
        }
        return ans;
    }

    @Override
    protected long getLong(long offset) {
        // trace(true, "PO getLong(%d)", offset);
        // return Util.synchronizedBlock(this, () -> pointer.region().getLong(offset));
        long ans;
        TransactionInfo info = lib.xpersistent.XTransaction.tlInfo.get();
        boolean inTransaction = info.state == Transaction.State.Active;
        if (!inTransaction) {
            monitorEnter();
            ans = pointer.region().getLong(offset);
            monitorExit();
        }
        else {
            boolean success = monitorEnterTimeout();
            if (success) {
                info.transaction.addLockedObject(this);
                ans = pointer.region().getLong(offset);
            }
            else {
                if (inTransaction) throw new TransactionRetryException();
                else throw new RuntimeException("failed to acquire lock (timeout)");
            }
        }
        return ans;
    }

    @Override
    @SuppressWarnings("unchecked")
    <T extends AnyPersistent> T getObject(long offset, PersistentType type) {
        // trace(true, "PO getObject(%d, %s)", offset, type);
        T ans = null;
        TransactionInfo info = lib.xpersistent.XTransaction.tlInfo.get();
        boolean inTransaction = info.state == Transaction.State.Active;
        boolean success = (inTransaction ? monitorEnterTimeout() : monitorEnterTimeout(5000));
        if (success) {
            try {
                if (inTransaction) info.transaction.addLockedObject(this);
                long valueAddr = getLong(offset);
                if (valueAddr != 0) ans = (T)ObjectCache.get(valueAddr);
            }
            finally {
                if (!inTransaction) monitorExit();
            }
        }
        else {
            String message = "failed to acquire lock (timeout) in getObject";
            // trace(true, getPointer().addr(), message + ", inTransaction = %s", inTransaction);
            if (inTransaction) throw new TransactionRetryException(message);
            else throw new RuntimeException(message);
        }
        return ans;
    }

    @SuppressWarnings("unchecked") <T extends AnyPersistent> T getValueObject(long offset, PersistentType type) {
        MemoryRegion srcRegion = getPointer().region();
        MemoryRegion dstRegion = new VolatileMemoryRegion(type.getSize());
        // trace(true, "getObject (valueBased) src addr = %d, srcOffset = %d, dst  = %s, size = %d", srcRegion.addr(), offset, dstRegion, type.getSize());
        Util.memCopy(getPointer().type(), (ObjectType)type, srcRegion, offset, dstRegion, 0L, type.getSize());
        T obj = null;
        try {
            Constructor ctor = ((ObjectType)type).cls().getDeclaredConstructor(ObjectPointer.class);
            ctor.setAccessible(true);
            ObjectPointer p = new ObjectPointer<T>((ObjectType)type, dstRegion);
            obj = (T)ctor.newInstance(p);
        }
        catch (Exception e) {e.printStackTrace();}
        return obj;
    }

    public byte getByteField(ByteField f) {return getByte(offset(check(f.getIndex(), Types.BYTE)));}
    public short getShortField(ShortField f) {return getShort(offset(check(f.getIndex(), Types.SHORT)));}
    public int getIntField(IntField f) {return getInt(offset(check(f.getIndex(), Types.INT)));}
    public long getLongField(LongField f) {/*System.out.println("PO getLongField LF"); */return getLong(offset(check(f.getIndex(), Types.LONG)));}
    public float getFloatField(FloatField f) {return Float.intBitsToFloat(getInt(offset(check(f.getIndex(), Types.FLOAT))));}
    public double getDoubleField(DoubleField f) {return Double.longBitsToDouble(getLong(offset(check(f.getIndex(), Types.DOUBLE))));}
    public char getCharField(CharField f) {return (char)getInt(offset(check(f.getIndex(), Types.CHAR)));}
    public boolean getBooleanField(BooleanField f) {return getByte(offset(check(f.getIndex(), Types.BOOLEAN))) == 0 ? false : true;}

    @SuppressWarnings("unchecked") public <T extends AnyPersistent> T getObjectField(ObjectField<T> f) {
        return (T)getObject(offset(f.getIndex()), f.getType());
    }

    @SuppressWarnings("unchecked") public <T extends AnyPersistent> T getObjectField(ValueField<T> f) {
        return getValueObject(offset(f.getIndex()), f.getType());
    }

    @SuppressWarnings("unchecked") public <T extends AnyPersistent> T getObjectField(FinalObjectField<T> f) {
        return (T)super.getObject(offset(f.getIndex()), f.getType());
    }

    public void setByteField(ByteField f, byte value) {setByte(offset(check(f.getIndex(), Types.BYTE)), value);}
    public void setShortField(ShortField f, short value) {setShort(offset(check(f.getIndex(), Types.SHORT)), value);}
    public void setIntField(IntField f, int value) {setInt(offset(check(f.getIndex(), Types.INT)), value);}
    public void setLongField(LongField f, long value) {/*System.out.println("PMO setLongField"); */setLong(offset(check(f.getIndex(), Types.LONG)), value);}
    public void setFloatField(FloatField f, float value) {setInt(offset(check(f.getIndex(), Types.FLOAT)), Float.floatToIntBits(value));}
    public void setDoubleField(DoubleField f, double value) {setLong(offset(check(f.getIndex(), Types.DOUBLE)), Double.doubleToLongBits(value));}
    public void setCharField(CharField f, char value) {setInt(offset(check(f.getIndex(), Types.CHAR)), (int)value);}
    public void setBooleanField(BooleanField f, boolean value) {setByte(offset(check(f.getIndex(), Types.BOOLEAN)), value ? (byte)1 : (byte)0);}
    public <T extends AnyPersistent> void setObjectField(ObjectField<T> f, T value) {setObjectField(f.getIndex(), value);}
    public <T extends AnyPersistent> void setObjectField(ValueField<T> f, T value) {setValueObject(offset(f.getIndex()), value);}
}
