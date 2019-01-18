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

import java.util.List;
import java.util.ArrayList;
import lib.util.persistent.types.ArrayType;
import lib.util.persistent.types.ReferenceArrayType;
import lib.util.persistent.types.Types;
import lib.util.persistent.types.PersistentType;
import lib.util.persistent.types.ObjectType;
import lib.xpersistent.*;
import lib.util.persistent.spi.PersistentMemoryProvider;
import lib.util.persistent.PersistentHeap;
import static lib.util.persistent.Trace.*;

abstract class AbstractPersistentArray extends AnyPersistent {
    private int length = -1; // cache immutable length
    private static PersistentHeap heap = PersistentMemoryProvider.getDefaultProvider().getHeap();

    protected AbstractPersistentArray(ArrayType<? extends AnyPersistent> type, int count, Object data) {
        super(type, heap.allocateObjectRegion(type.allocationSize(count)));
        setRawInt(ReferenceArrayType.LENGTH_OFFSET, count);
        length = count;
        if (data != null) initializeElements(data, type.elementType());
        flushRegion();
        if (Config.ENABLE_ALLOC_STATS) Stats.current.allocStats.update(type.cls().getName(), type.allocationSize(count), Stats.AllocationStats.WRAPPER_PER_INSTANCE + 4, 1); // uncomment for allocation stats
    }

    protected AbstractPersistentArray(ArrayType<? extends AnyPersistent> type, int count) {
        this(type, count, null);
    }

    protected AbstractPersistentArray(ObjectPointer<? extends AbstractPersistentArray>  p) {
        super(p);
        length = getRegionInt(ReferenceArrayType.LENGTH_OFFSET);
        if (Config.ENABLE_ALLOC_STATS) Stats.current.allocStats.update(p.type().cls().getName() + "<rctor>", 0, Stats.AllocationStats.WRAPPER_PER_INSTANCE + 4, 1);  // uncomment for allocation stats
    }

    public byte getByteElement(int index) {return getByte(elementOffset(checkIndex(index)));}
    public short getShortElement(int index) {return getShort(elementOffset(checkIndex(index)));}
    public int getIntElement(int index) {return getInt(elementOffset(checkIndex(index)));}
    public long getLongElement(int index) {/*trace(true, "APA.getLongElement(%d)", index); */return getLong(elementOffset(checkIndex(index)));}
    public float getFloatElement(int index) {return Float.intBitsToFloat(getInt(elementOffset(checkIndex(index))));}
    public double getDoubleElement(int index) {return Double.longBitsToDouble(getLong(elementOffset(checkIndex(index))));}
    public char getCharElement(int index) {return (char)getInt(elementOffset(checkIndex(index)));}
    public boolean getBooleanElement(int index) {return getByte(elementOffset(checkIndex(index))) == (byte)0 ? false : true;}

    AnyPersistent getObjectElement(int index) {
        // trace(true, "APA.getObjectElement(%d)", index);
        return getObject(elementOffset(checkIndex(index)));
    }

    void flushRegion() {
        region.flush(0, ((ArrayType<?>)type).allocationSize(length));
    }

    private void setByteElement(int index, byte value) {setByte(elementOffset(checkIndex(index)), value);}
    private void setShortElement(int index, short value) {setShort(elementOffset(checkIndex(index)), value);}
    private void setIntElement(int index, int value) {setInt(elementOffset(checkIndex(index)), value);}
    private void setLongElement(int index, long value) {/*trace(true, "APA.setLongElement(%d)", index); */setLong(elementOffset(checkIndex(index)), value);}
    private void setFloatElement(int index, float value) {setInt(elementOffset(checkIndex(index)), Float.floatToIntBits(value));}
    private void setDoubleElement(int index, double value) {setLong(elementOffset(checkIndex(index)), Double.doubleToLongBits(value));}
    private void setCharElement(int index, char value) {setInt(elementOffset(checkIndex(index)), (int)value);}
    private void setBooleanElement(int index, boolean value) {setByte(elementOffset(checkIndex(index)), value ? (byte)1 : (byte)0);}

    void setObjectElement(int index, AnyPersistent value) {
        // trace(true, "APA.setObjectElement(%d)", index);
        setObject(elementOffset(checkIndex(index)), value);
    }

    public PersistentType getElementType() {
        return ((ArrayType)getType()).elementType();
    }

    public int length() {
        return length;
    }

    long elementOffset(int index) {
        return ((ArrayType)getType()).elementOffset(index);
    }

    long elementOffset(int index, long size) {
        return ((ArrayType)getType()).elementOffset(index, size);
    }

    int checkIndex(int index) {
        if (index < 0 || index >= length()) throw new IndexOutOfBoundsException("index " + index + " out of bounds");
        return index;
    }

    //only called during construction; thread-safe
    private void initializeElements(Object data, PersistentType t)
    {
        if (t == Types.BYTE) {
            byte[] array = (byte[])data;
            region().putRawBytes(ReferenceArrayType.ELEMENTS_OFFSET, array);
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
            AnyPersistent[] array = (AnyPersistent[])data;
            for (int i = 0; i < array.length; i++) setObjectElement(i, array[i]);
        }
        else if (t instanceof ArrayType) {
            AnyPersistent[] array = (AnyPersistent[])data;
            for (int i = 0; i < array.length; i++) setObjectElement(i, array[i]);
        }
    }

    protected <T extends AnyPersistent> T[] toObjectArray(T[] a) {
        T[] ans = a.length < length() ? java.util.Arrays.copyOf(a, length()): a;
        for (int i = 0; i < ans.length; i++) {
            long addr = getRegionLong(elementOffset(i));
            if (addr != 0) ans[i] = ObjectCache.get(addr);
        }
        if (ans.length > length()) ans[length()] = null;
        return ans;
    }

    protected AnyPersistent[] toObjectArray() {
        return toObjectArray(new AnyPersistent[length()]);
    }
}
