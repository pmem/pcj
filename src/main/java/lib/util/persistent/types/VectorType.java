/* Copyright (C) 2017 Intel Corporation
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

import lib.util.persistent.AnyPersistent;
import java.util.ArrayList;

public class VectorType<T extends AnyPersistent> extends IndirectValueArrayType<T> { 
    public static final long LENGTH_OFFSET = 0;
    public static final long ELEMENTS_OFFSET = LENGTH_OFFSET + Types.INT.size();

    public VectorType(Class<T> cls, PersistentType elementType) {
        super(cls, elementType);
    }

    @Override
    public long allocationSize(int count) {
        return ELEMENTS_OFFSET + count * elementSize();
    }

    @Override
    public long elementOffset(int index) {
        System.out.format("VectorType.getElementOffset(%d), returning %d\n", index, ELEMENTS_OFFSET + index * elementSize());
        return ELEMENTS_OFFSET + index * elementSize();
    }

    @Override
    public long elementOffset(int index, long size) {
        return ELEMENTS_OFFSET + index * size;
    }

    @Override
    public String toString() {
        return "VectorType(" + name() + ", " + elementType() + ")";
    }

       
}
