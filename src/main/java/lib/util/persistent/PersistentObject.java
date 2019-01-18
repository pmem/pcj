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
import java.lang.reflect.Constructor;
import static lib.util.persistent.Trace.*;
import java.util.function.Consumer;

@SuppressWarnings("sunapi")
public class PersistentObject extends AbstractPersistentObject {
    private static final String LOCK_FAIL_MESSAGE = "failed to acquire lock (timeout) in getObject";

    protected PersistentObject(ObjectType<? extends PersistentObject> type) {
        super(type);
    }

    @SuppressWarnings("unchecked")
    protected <T extends PersistentObject> PersistentObject(ObjectType<? extends PersistentObject> type, Consumer<T> initializer) {
        super(type, initializer);
    }

    <T extends AnyPersistent> PersistentObject(ObjectType<T> type, MemoryRegion region) {
        super(type, region);
        // trace(region.addr(), "created object of type %s", type.name());
    }

    protected PersistentObject(ObjectPointer<? extends AnyPersistent> p) {
        super(p);
    }

    @Override
    byte getByte(long offset) {
        // return Util.synchronizedBlock(this, () -> region().getByte(offset));
        byte ans;
        Transaction transaction = Transaction.getTransaction();
        boolean inTransaction = transaction != null && transaction.isActive();
        if (!inTransaction) {
            lock();
            try {ans = getRegionByte(offset);}
            finally {unlock();}
        }
        else {
            boolean success = tryLock(transaction);
            if (success) {
                transaction.addLockedObject(this);
                return region().getByte(offset);
            }
            else {
                if (inTransaction) throw new TransactionRetryException();
                else throw new RuntimeException("failed to acquire lock (timeout)");
            }
        }
        return ans;
    }

    @Override
    short getShort(long offset) {
        // return Util.synchronizedBlock(this, () -> region().getShort(offset));
        short ans;
        Transaction transaction = Transaction.getTransaction();
        boolean inTransaction = transaction != null && transaction.isActive();
        if (!inTransaction) {
            lock();
            try {ans = getRegionShort(offset);}
            finally {unlock();}
        }
        else {
            boolean success = tryLock(transaction);
            if (success) {
                transaction.addLockedObject(this);
                ans = region().getShort(offset);
            }
            else {
                if (inTransaction) throw new TransactionRetryException();
                else throw new RuntimeException("failed to acquire lock (timeout)");
            }
        }
        return ans;
    }

    @Override
    int getInt(long offset) {
        // return Util.synchronizedBlock(this, () -> region().getInt(offset));
        int ans;
        Transaction transaction = Transaction.getTransaction();
        boolean inTransaction = transaction != null && transaction.isActive();
        if (!inTransaction) {
            lock();
            try {ans = getRegionInt(offset);}
            finally {unlock();}
        }
        else {
            boolean success = tryLock(transaction);
            if (success) {
                transaction.addLockedObject(this);
                ans = region().getInt(offset);
            }
            else {
                if (inTransaction) throw new TransactionRetryException();
                else throw new RuntimeException("failed to acquire lock (timeout)");
            }
        }
        return ans;
    }

    @Override
    long getLong(long offset) {
        // trace(true, "PO.getLong(%d)", offset);
        // return Util.synchronizedBlock(this, () -> region().getLong(offset));
        long ans;
        Transaction transaction = Transaction.getTransaction();
        boolean inTransaction = transaction != null && transaction.isActive();
        if (!inTransaction) {
            lock();
            try {ans = getRegionLong(offset);}
            finally {unlock();}
        }
        else if (tryLock(transaction)) {
            transaction.addLockedObject(this);
            ans = getRegionLong(offset);
        }
        else {
            throw new TransactionRetryException();
        }
        return ans;
    }

    @Override
    @SuppressWarnings("unchecked")
    <T extends AnyPersistent> T getObject(long offset, PersistentType type) {
        // trace(true, "PO.getObject(%d, %s)", offset, type);
        T ans = null;
        Transaction transaction = Transaction.getTransaction();
        boolean inTransaction = transaction != null && transaction.isActive();
        if (!inTransaction) {
            lock();
            try {
                // TODO: ObjectCache.get() can acquire locks, need to analyze for deadlock 
                long objAddr = region.getLong(offset);
                if (objAddr != 0) ans = (T)ObjectCache.get(objAddr);
            }
            finally {unlock();}
        }
        else {
            if (tryLock(transaction)) {
                transaction.addLockedObject(this);
                long objAddr = region.getLong(offset);
                if (objAddr != 0) ans = (T)ObjectCache.get(objAddr);
            }
            else throw new TransactionRetryException(LOCK_FAIL_MESSAGE);
        }
        return ans;
    }

    //TODO: optimize 4 parts: acquire lock, allocate volatile, copy P->V, construct box
    @Override
    @SuppressWarnings("unchecked") 
    <T extends AnyPersistent> T getValueObject(long offset, PersistentType pt) {
        // trace(true, "PO.getValueObject(%d, %s)", offset, pt);
        ObjectType hostType = getType();
        ObjectType.Kind hostKind = hostType.kind();
        ObjectType fieldType = (ObjectType)pt;
        ObjectType.Kind fieldKind = fieldType.kind();
        MemoryRegion srcRegion;
        long srcRegionSize;
        MemoryRegion dstRegion;
        T obj = null;
        switch (fieldKind) {
            case IndirectValue :
                switch (hostKind) {
                    case Reference : 
                        srcRegion = new lib.xpersistent.UncheckedPersistentMemoryRegion(region().getLong(offset));
                        srcRegionSize = fieldType.allocationSize(); //srcRegion.getInt(0);
                        dstRegion = new VolatileMemoryRegion(srcRegionSize);
                        Util.memCopy(hostType, fieldType, srcRegion, offset, dstRegion, 0L, srcRegionSize);
                        obj = AnyPersistent.reconstruct(new ObjectPointer<T>(fieldType, dstRegion));
                        break;
                    case DirectValue : 
                        throw new RuntimeException("NYI");
                    case IndirectValue :
                        throw new RuntimeException("NYI");
                    default : throw new RuntimeException("Unsupported Kind: " + hostKind);
                }
                break;
            case DirectValue : 
                switch (hostKind) {
                    case IndirectValue :
                        long size = fieldType.allocationSize();
                        srcRegion = region();
                        dstRegion = new VolatileMemoryRegion(size);
                        Util.memCopy(hostType, fieldType, srcRegion, offset, dstRegion, 0, size); 
                        obj = AnyPersistent.reconstruct(new ObjectPointer<T>(fieldType, dstRegion));
                        break;
                    case DirectValue :
                    case Reference :
                        srcRegion = region();
                        dstRegion = new VolatileMemoryRegion(fieldType.size());
                        Util.memCopy(hostType, fieldType, srcRegion, offset, dstRegion, 0L, fieldType.size());
                        obj = AnyPersistent.reconstruct(new ObjectPointer<T>(fieldType, dstRegion));
                        break;
                    default : throw new RuntimeException("Unsupported Kind: " + hostKind);
                }
                break;
            default : throw new RuntimeException("Unsupported Kind: " + fieldKind);
        }
        if (obj != null) obj.onGet();                
        return obj;
    }

    public void setByteField(ByteField f, byte value) {setByte(offset(f.getIndex()), value);}
    public void setShortField(ShortField f, short value) {setShort(offset(f.getIndex()), value);}
    public void setIntField(IntField f, int value) {setInt(offset(f.getIndex()), value);}
    public void setLongField(LongField f, long value) {setLong(offset(f.getIndex()), value);}
    public void setFloatField(FloatField f, float value) {setInt(offset(f.getIndex()), Float.floatToIntBits(value));}
    public void setDoubleField(DoubleField f, double value) {setLong(offset(f.getIndex()), Double.doubleToLongBits(value));}
    public void setCharField(CharField f, char value) {setInt(offset(f.getIndex()), (int)value);}
    public void setBooleanField(BooleanField f, boolean value) {setByte(offset(f.getIndex()), value ? (byte)1 : (byte)0);}
    
    public <T extends AnyPersistent> void setObjectField(ObjectField<T> f, T value) {
        setObject(offset(f.getIndex()), value);
    }
    
    public <T extends AnyPersistent> void setObjectField(ValueField<T> f, T value) {
        // trace(true, "PO.setLongField(%s) : VF", f); 
        if (f.getType().size() != value.getType().size()) throw new IllegalArgumentException("value types do not match");
        setValueObject(offset(f.getIndex()), value);
    }

    @SuppressWarnings("unchecked")
    public <T extends AnyPersistent> void setObjectField(GenericField<? extends AnyPersistent> f, T value) {
        if (value == null) return;
        ObjectType<T> type = value.getType();
        switch (type.kind()) {
            case Reference :
                setObject(offset(f.getIndex()), value);
                break;
            case IndirectValue :
                setValueObject(offset(f.getIndex()), value);
                break;
            default : 
                throw new RuntimeException("setObjectField incompatible type : " + type);
        }
    }
}
