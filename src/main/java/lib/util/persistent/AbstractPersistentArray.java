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

abstract class AbstractPersistentArray extends AbstractPersistentImmutableArray {
    AbstractPersistentArray(ArrayType<? extends PersistentObject> type, int count) {
        super(type, count);
    }

    AbstractPersistentArray(ObjectPointer<? extends AbstractPersistentArray> p) {super(p);}

    @Override byte getByteElement(int index) {return getByte(elementOffset(check(index)));}
    @Override short getShortElement(int index) {return getShort(elementOffset(check(index)));}
    @Override int getIntElement(int index) {return getInt(elementOffset(check(index)));}
    @Override long getLongElement(int index) {return getLong(elementOffset(check(index)));}
    @Override float getFloatElement(int index) {return Float.intBitsToFloat(getInt(elementOffset(check(index))));}
    @Override double getDoubleElement(int index) {return Double.longBitsToDouble(getLong(elementOffset(check(index))));}
    @Override char getCharElement(int index) {return (char)getInt(elementOffset(check(index)));}
    @Override boolean getBooleanElement(int index) {return getByte(elementOffset(check(index))) == (byte)0 ? false : true;}
    @Override PersistentObject getObjectElement(int index) {return getObject(elementOffset(check(index)));}

    final void setByteElement(int index, byte value) {setByte(elementOffset(check(index)), value);}
    final void setShortElement(int index, short value) {setShort(elementOffset(check(index)), value);}
    final void setIntElement(int index, int value) {setInt(elementOffset(check(index)), value);}
    final void setLongElement(int index, long value) {setLong(elementOffset(check(index)), value);}
    final void setFloatElement(int index, float value) {setInt(elementOffset(check(index)), Float.floatToIntBits(value));}
    final void setDoubleElement(int index, double value) {setLong(elementOffset(check(index)), Double.doubleToLongBits(value));}
    final void setCharElement(int index, char value) {setInt(elementOffset(check(index)), (int)value);}
    final void setBooleanElement(int index, boolean value) {setByte(elementOffset(check(index)), value ? (byte)1 : (byte)0);}
    final void setObjectElement(int index, PersistentObject value) {setObject(elementOffset(check(index)), value);}
    final void setValueElement(int index, PersistentValue value) {super.setValueElement(index, value);}

    @Override public int length() {
        return getInt(ArrayType.LENGTH_OFFSET);
    }

    @Override protected long elementOffset(int index) {
        return ((ArrayType)getPointer().type()).getElementOffset(index);
    }
}




