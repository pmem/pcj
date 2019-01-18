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

import lib.util.persistent.types.ArrayType;
import lib.util.persistent.types.ReferenceArrayType;
import lib.util.persistent.types.Types;
import lib.util.persistent.types.PersistentType;
import lib.util.persistent.types.ObjectType;
import java.lang.reflect.Constructor;
import static lib.util.persistent.Trace.*;

abstract class AbstractPersistentMutableArray extends AbstractPersistentArray {
    AbstractPersistentMutableArray(ArrayType<? extends AnyPersistent> type, int count) {
        super(type, count);
    }

    AbstractPersistentMutableArray(ObjectPointer<? extends AbstractPersistentMutableArray> p) {super(p);}

    void setByteElement(int index, byte value) {setByte(elementOffset(checkIndex(index)), value);}
    void setShortElement(int index, short value) {setShort(elementOffset(checkIndex(index)), value);}
    void setIntElement(int index, int value) {setInt(elementOffset(checkIndex(index)), value);}
    void setLongElement(int index, long value) {/*trace(true, "APMA.setLongElement(%d)", index); */setLong(elementOffset(checkIndex(index)), value);}
    void setFloatElement(int index, float value) {setInt(elementOffset(checkIndex(index)), Float.floatToIntBits(value));}
    void setDoubleElement(int index, double value) {setLong(elementOffset(checkIndex(index)), Double.doubleToLongBits(value));}
    void setCharElement(int index, char value) {setInt(elementOffset(checkIndex(index)), (int)value);}
    void setBooleanElement(int index, boolean value) {setByte(elementOffset(checkIndex(index)), value ? (byte)1 : (byte)0);}

    void setObjectElement(int index, AnyPersistent value) {
        // trace(true, "APMA.setObjectElement(%d)", index);
        setObject(elementOffset(checkIndex(index)), value);
    }

    @Override
    protected byte getByte(long offset) {
        return Util.synchronizedBlock(this, () -> region().getByte(offset));
    }

    @Override
    protected short getShort(long offset) {
        return Util.synchronizedBlock(this, () -> region().getShort(offset));
    }

    @Override
    protected int getInt(long offset) {
        return Util.synchronizedBlock(this, () -> region().getInt(offset));
    }

    @Override
    protected long getLong(long offset) {
        return Util.synchronizedBlock(this, () -> region().getLong(offset));
    }

    @Override
    @SuppressWarnings("unchecked")
    <T extends AnyPersistent> T getObject(long offset, PersistentType type) {
        // trace(true, "APMO.getObject(%d, %s)", offset, type);
        T ans = null;
        Transaction transaction = Transaction.getTransaction();
        boolean inTransaction = transaction != null && transaction.isActive();
        boolean success = (inTransaction ? tryLock(transaction) : tryLock(5000));
        if (success) {
            try {
                if (inTransaction) transaction.addLockedObject(this);
                long valueAddr = getLong(offset);
                if (valueAddr != 0) ans = (T)ObjectCache.get(valueAddr);
            }
            finally {
                if (!inTransaction) unlock();
            }
        }
        else {
            String message = "failed to acquire lock (timeout) in getObject";
            // trace(true, addr(), message + ", inTransaction = %s", inTransaction);
            if (inTransaction) throw new TransactionRetryException(message);
            else throw new RuntimeException(message);
        }
        return ans;
    }

    @SuppressWarnings("unchecked") <T extends AnyPersistent> T getValueObject(long offset, PersistentType type) {
        // trace(true, "APMO.getValueObject(%d, %s)", offset, type);
        MemoryRegion region = Util.synchronizedBlock(this, () -> {
            MemoryRegion srcRegion = region();
            MemoryRegion dstRegion = new VolatileMemoryRegion(type.size());
            // trace(true, "getObject (valueBased) type = %s, src addr = %d, srcOffset = %d, dst  = %s, size = %d", type, srcRegion.addr(), offset, dstRegion, type.size());
            Util.memCopy(getType(), (ObjectType)type, srcRegion, offset, dstRegion, 0L, type.size());
            return dstRegion;
        });
        return AnyPersistent.reconstruct(new ObjectPointer<T>((ObjectType)type, region));
    }

    @Override
    public <T extends AnyPersistent> T[] toObjectArray(T[] a) {
        return Util.synchronizedBlock(this, () -> {
            return super.toObjectArray(a);
        });
    }

    @Override
    public AnyPersistent[] toObjectArray() {
        return Util.synchronizedBlock(this, () -> {
            return super.toObjectArray();
        });
    }
}





