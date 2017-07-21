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

import lib.util.persistent.types.ByteField;
import lib.util.persistent.types.ShortField;
import lib.util.persistent.types.IntField;
import lib.util.persistent.types.LongField;
import lib.util.persistent.types.FloatField;
import lib.util.persistent.types.DoubleField;
import lib.util.persistent.types.CharField;
import lib.util.persistent.types.BooleanField;
import lib.util.persistent.types.ValueField;
import lib.util.persistent.types.ValueType;
import lib.util.persistent.spi.PersistentMemoryProvider;

// TODO: factor common accessor code out of PersistentValue and PersistentObject
public abstract class PersistentValue implements Persistent<PersistentValue> {
    // FIXME: Compatible only with the default provider
    private static final PersistentHeap heap = PersistentMemoryProvider.getDefaultProvider().getHeap();

    private final ValuePointer<? extends PersistentValue> pointer;

    protected <T extends PersistentValue> PersistentValue(ValueType type, Class<T> cls) {
        this.pointer = new ValuePointer<T>(type, heap.allocateRegion(type.getSize()), cls);
    }

    protected PersistentValue(ValuePointer<? extends PersistentValue> pointer) {
        this.pointer = pointer;
    }

    public byte getByte(long offset) {return pointer.region().getByte(offset);}
    public short getShort(long offset) {return pointer.region().getShort(offset);}
    public int getInt(long offset) {return pointer.region().getInt(offset);}
    public long getLong(long offset) {return pointer.region().getLong(offset);}
    
    public void setByte(long offset, byte value) {pointer.region().putByte(offset, value);}
    public void setShort(long offset, short value) {pointer.region().putShort(offset, value);}
    public void setInt(long offset, int value) {pointer.region().putInt(offset, value);}
    public void setLong(long offset, long value) {pointer.region().putLong(offset, value);}

    public byte getByteField(ByteField f) {return getByte(offset(check(f.getIndex())));}
    public short getShortField(ShortField f) {return getShort(offset(check(f.getIndex())));}
    public int getIntField(IntField f) {return getInt(offset(check(f.getIndex())));}
    public long getLongField(LongField f) {return getLong(offset(check(f.getIndex())));}
    public float getFloatField(FloatField f) {return Float.intBitsToFloat(getInt(offset(check(f.getIndex()))));}
    public double getDoubleField(DoubleField f) {return Double.longBitsToDouble(getLong(offset(check(f.getIndex()))));}
    public char getCharField(CharField f) {return (char)getInt(offset(check(f.getIndex())));}
    public boolean getBooleanField(BooleanField f) {return getByte(offset(check(f.getIndex()))) == 0 ? false : true;}

    @SuppressWarnings("unchecked")
    public <T extends PersistentValue> T getValueField(ValueField<T> f) {  
        MemoryRegion srcRegion = getPointer().region();
        MemoryRegion dstRegion = heap.allocateRegion(f.getType().getSize());
        // System.out.println(String.format("getValueField @ index %d, src addr = %d, dst addr = %d, size = %d", f.getIndex(), srcRegion.addr(), dstRegion.addr(), f.getType().getSize()));
        synchronized(srcRegion) {
            heap.memcpy(srcRegion, offset(f.getIndex()), dstRegion, 0, f.getType().getSize());
        }
        return (T)new ValuePointer((ValueType)f.getType(), dstRegion, f.cls()).deref();
    }

    public void setByteField(ByteField f, byte value) {setByte(offset(check(f.getIndex())), value);}
    public void setShortField(ShortField f, short value) {setShort(offset(check(f.getIndex())), value);}
    public void setIntField(IntField f, int value) {setInt(offset(check(f.getIndex())), value);}
    public void setLongField(LongField f, long value) {setLong(offset(check(f.getIndex())), value);}
    public void setFloatField(FloatField f, float value) {setInt(offset(check(f.getIndex())), Float.floatToIntBits(value));}
    public void setDoubleField(DoubleField f, double value) {setLong(offset(check(f.getIndex())), Double.doubleToLongBits(value));}
    public void setCharField(CharField f, char value) {setInt(offset(check(f.getIndex())), (int)value);}
    public void setBooleanField(BooleanField f, boolean value) {setByte(offset(check(f.getIndex())), value ? (byte)1 : (byte)0);}

    public <T extends PersistentValue> void setValueField(ValueField<T> f, T value) {
        MemoryRegion dstRegion = getPointer().region();
        long dstOffset = offset(f.getIndex());
        MemoryRegion srcRegion = value.getPointer().region();
        // System.out.println(String.format("setValueField @index %d, src addr = %d, dst addr = %d, size = %d", f.getIndex(), srcRegion.addr(), dstRegion.addr() + dstOffset, f.getType().getSize()));
        synchronized(dstRegion) {
            synchronized(srcRegion) {
                heap.memcpy(srcRegion, 0, dstRegion, dstOffset, f.getType().getSize());
            }
        }
    }

    public ValuePointer<? extends PersistentValue> getPointer() {
        return pointer;
    }

    private long offset(int index) {
        return (getPointer().type()).getOffset(index);
    }

    private int check(int index) {
        if (index < 0 || index >= getPointer().type().getTypes().size()) throw new IndexOutOfBoundsException("No such field index: " + index);
        return index;
    }

}