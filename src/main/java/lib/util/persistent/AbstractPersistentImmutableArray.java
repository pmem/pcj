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
import lib.util.persistent.types.ValueType;
import lib.util.persistent.types.Types;
import lib.util.persistent.types.PersistentType;
import lib.util.persistent.types.ObjectType;
import lib.util.persistent.spi.PersistentMemoryProvider;

abstract class AbstractPersistentImmutableArray extends PersistentObject {

    protected AbstractPersistentImmutableArray(ArrayType<? extends PersistentObject> type, int count, Object data) {
        super(type, PersistentMemoryProvider.getDefaultProvider().getHeap().allocateRegion(type.getAllocationSize(count)));
        setInt(ArrayType.LENGTH_OFFSET, count);
        initializeElements(data, type.getElementType());
    }

    protected AbstractPersistentImmutableArray(ArrayType<? extends PersistentObject> type, int count) {
        super(type, PersistentMemoryProvider.getDefaultProvider().getHeap().allocateRegion(type.getAllocationSize(count)));
        setInt(ArrayType.LENGTH_OFFSET, count);
        for (int i = 0; i < count; i++) {initializeElement(i, type.getElementType());}
    }

    protected AbstractPersistentImmutableArray(ObjectPointer<? extends AbstractPersistentImmutableArray>  p) {
        super(p);
    }

    byte getByteElement(int index) {return getByte(elementOffset(check(index)));}
    short getShortElement(int index) {return getShort(elementOffset(check(index)));}
    int getIntElement(int index) {return getInt(elementOffset(check(index)));}
    long getLongElement(int index) {return getLong(elementOffset(check(index)));}
    float getFloatElement(int index) {return Float.intBitsToFloat(getInt(elementOffset(check(index))));}
    double getDoubleElement(int index) {return Double.longBitsToDouble(getLong(elementOffset(check(index))));}
    char getCharElement(int index) {return (char)getInt(elementOffset(check(index)));}
    boolean getBooleanElement(int index) {return getByte(elementOffset(check(index))) == (byte)0 ? false : true;}
    PersistentObject getObjectElement(int index) {return getObject(elementOffset(check(index)));}

    @SuppressWarnings("unchecked")
    protected PersistentValue getValueElement(int index, Class<? extends PersistentValue> cls) {
        try {
            ValueType type = Types.valueTypeForClass(cls);
            long size = type.getSize();
            MemoryRegion srcRegion = getPointer().region();
            long offset = elementOffset(index, type.getSize());
            MemoryRegion dstRegion = heap.allocateRegion(size);
            // System.out.println(String.format("getValueElement src addr = %d, dst addr = %d, size = %d", srcRegion.addr() + offset, dstRegion.addr(), size));
            synchronized(srcRegion) {
                ((lib.xpersistent.XHeap)heap).memcpy(srcRegion, offset, dstRegion, 0, size);
            }
            return cls.cast(new ValuePointer(type, dstRegion, cls).deref());
        } catch (Exception e) {throw new RuntimeException("unable to extract type from class " + cls);}
    }

    private void setByteElement(int index, byte value) {setByte(elementOffset(check(index)), value);}
    private void setShortElement(int index, short value) {setShort(elementOffset(check(index)), value);}
    private void setIntElement(int index, int value) {setInt(elementOffset(check(index)), value);}
    private void setLongElement(int index, long value) {setLong(elementOffset(check(index)), value);}
    private void setFloatElement(int index, float value) {setInt(elementOffset(check(index)), Float.floatToIntBits(value));}
    private void setDoubleElement(int index, double value) {setLong(elementOffset(check(index)), Double.doubleToLongBits(value));}
    private void setCharElement(int index, char value) {setInt(elementOffset(check(index)), (int)value);}
    private void setBooleanElement(int index, boolean value) {setByte(elementOffset(check(index)), value ? (byte)1 : (byte)0);}

    void setObjectElement(int index, PersistentObject value) {
        setObject(elementOffset(check(index)), value);
    }

    synchronized <T extends PersistentValue> void setValueElement(int index, T value) {
        long size = value.getPointer().type().getSize();
        MemoryRegion dstRegion = getPointer().region();
        long dstOffset = elementOffset(index);
        MemoryRegion srcRegion = value.getPointer().region();
        // System.out.println(String.format("setValueElement src addr = %d, dst addr = %d, size = %d", srcRegion.addr(), dstRegion.addr() + dstOffset, size));
        synchronized(dstRegion) {
            synchronized(srcRegion) {
                ((lib.xpersistent.XHeap)heap).memcpy(srcRegion, 0, dstRegion, dstOffset, size);
            }
        }
    }

    public int length() {
        return getInt(ArrayType.LENGTH_OFFSET);
    }

    long elementOffset(int index) {
        return ((ArrayType)getPointer().type()).getElementOffset(index);
    }

    long elementOffset(int index, long size) {
        return ((ArrayType)getPointer().type()).getElementOffset(index, size);
    }

    int check(int index) {
        if (index < 0 || index >= length()) throw new IndexOutOfBoundsException("index " + index + " out of bounds");
        return index;
    }

   // only called during construction; thread-safe
    void initializeElement(int index, PersistentType t)
    {
        if (t == Types.BYTE) setByteElement(index, (byte)0);
        else if (t == Types.SHORT) setShortElement(index, (short)0);
        else if (t == Types.INT) setIntElement(index, 0);
        else if (t == Types.LONG) setLongElement(index, 0L);
        else if (t == Types.FLOAT) setFloatElement(index, 0f);
        else if (t == Types.DOUBLE) setDoubleElement(index, 0d);
        else if (t == Types.CHAR) setCharElement(index, (char)0);
        else if (t == Types.BOOLEAN) setBooleanElement(index, false);
        else if (t instanceof ObjectType) setObjectElement(index, null);
        else if (t instanceof ArrayType) setObjectElement(index, null);
    }

    @Override synchronized void delete() {
        // System.out.println(String.format("delete called on array (%s) at address %d", getPointer().type().cls(), getPointer().addr()));
        if (((ArrayType)getPointer().type()).getElementType() == Types.OBJECT) {
            Transaction.run(() -> {
                for (int i = 0; i < length(); i++) {
                    long target = getLong(elementOffset(i));
                    if (target != 0) {
                        PersistentObject obj = PersistentObject.weakGetObjectAtAddress(target);
                        obj.decRefCount();
                    }
                }
                super.delete();
            });
        }
    }

    // only called during construction; thread-safe
    private void initializeElements(Object data, PersistentType t)
    {
        if (t == Types.BYTE) {
            byte[] array = (byte[])data;
            for (int i = 0; i < array.length; i++) setByteElement(i, array[i]);
        }
        else if (t == Types.SHORT) {
            short[] array = (short[])data;
            for (int i = 0; i < array.length; i++) setShortElement(i, array[i]);
        }
        else if (t == Types.INT) {
            int[] array = (int[])data;
            for (int i = 0; i < array.length; i++) setIntElement(i, array[i]);
        }
        else if (t == Types.LONG) {
            long[] array = (long[])data;
            for (int i = 0; i < array.length; i++) setLongElement(i, array[i]);
        }
        else if (t == Types.FLOAT) {
            float[] array = (float[])data;
            for (int i = 0; i < array.length; i++) setFloatElement(i, array[i]);
        }
        else if (t == Types.DOUBLE) {
            double[] array = (double[])data;
            for (int i = 0; i < array.length; i++) setDoubleElement(i, array[i]);
        }
        else if (t == Types.CHAR) {
            char[] array = (char[])data;
            for (int i = 0; i < array.length; i++) setCharElement(i, array[i]);
        }
        else if (t == Types.BOOLEAN) {
            boolean[] array = (boolean[])data;
            for (int i = 0; i < array.length; i++) setBooleanElement(i, array[i]);
        }
        else if (t instanceof ObjectType) {
            PersistentObject[] array = (PersistentObject[])data;
            for (int i = 0; i < array.length; i++) setObjectElement(i, array[i]);
        }
        else if (t instanceof ArrayType) {
            PersistentObject[] array = (PersistentObject[])data;
            for (int i = 0; i < array.length; i++) setObjectElement(i, array[i]);
        }
    }
}
