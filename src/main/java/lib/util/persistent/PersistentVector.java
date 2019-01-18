/* Copyright (C) 2018 Intel Corporation
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
import lib.util.persistent.types.VectorType;
import lib.util.persistent.types.ObjectType;
import lib.util.persistent.types.ArrayType;
import java.lang.reflect.Array;
import java.util.Arrays;
import lib.xpersistent.*;
import lib.util.persistent.spi.PersistentMemoryProvider;

public class PersistentVector<T extends AnyPersistent> extends PersistentObject {
    private ArrayType<PersistentVector> TYPE = new VectorType<PersistentVector>(PersistentVector.class, Types.VALUE);
    private MemoryRegion dataRegion;

    @SuppressWarnings("unchecked")
    public PersistentVector(Class<T> elementClass, int size) {
        super(new VectorType<PersistentVector>(PersistentVector.class, Types.typeForClass(elementClass)), new VolatileMemoryRegion(size * ((ObjectType)Types.typeForClass(elementClass)).allocationSize()));
        // TYPE = new VectorType<PersistentVector>(PersistentVector.class, Types.typeForClass(elementClass));
        // MemoryRegion boxRegion = new VolatileMemoryRegion(Types.POINTER.size());
        TYPE = (ArrayType<PersistentVector>)getType();
        this.dataRegion = new VolatileMemoryRegion(TYPE.allocationSize(size));
    }

    @SafeVarargs
    public PersistentVector(Class<T> elementClass, T... ts) {
        this(elementClass, ts.length);
        for (int i = 0; i < ts.length; i++) set(i, ts[i]);
    }

    protected PersistentVector(ObjectPointer<? extends PersistentVector<T>> pointer) {
        super(pointer);
    }

    public void set(int index, T value) {
        setValueObject(TYPE.elementOffset(index), value);
    }

    public T get(int index) {
        return getValueObject(TYPE.elementOffset(index), TYPE.elementType());
    }

    // @Override
    // void setObjectElement(int index, AnyPersistent value) {
    //     // System.out.format("setObjectElement(%d, %s), offset = %d\n", index, value, elementOffset(index));
    //     setValueObject(elementOffset(check(index)), value);
    // }

    // @Override
    // AnyPersistent getObjectElement(int index) {
    //     return getValueObject(elementOffset(check(index)), getElementType());
    // }

}
