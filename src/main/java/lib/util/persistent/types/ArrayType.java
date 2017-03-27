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

package lib.util.persistent.types;

import lib.util.persistent.PersistentObject;
import java.util.ArrayList;

public class ArrayType<T extends PersistentObject> extends ObjectType<T> { 
    protected PersistentType elementType;
    protected long elementSize;
    public static final long LENGTH_OFFSET = ObjectType.FIELDS_OFFSET;
    public static final long ELEMENTS_OFFSET = LENGTH_OFFSET + Types.INT.getSize();

    public ArrayType(Class<T> cls, PersistentType elementType) {
        super(cls);
        this.elementType = elementType;
        this.elementSize = elementType.getSize();
    }

    public PersistentType getElementType() {
        return elementType;
    }

    public long getAllocationSize(int count) {
        return ELEMENTS_OFFSET + count * elementSize;
    }

    public long getElementOffset(int index) {
        return ELEMENTS_OFFSET + index * elementSize;
    }

    public long getElementOffset(int index, long size) {
         return ELEMENTS_OFFSET + index * size;
    }
       
}
