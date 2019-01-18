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
import lib.util.persistent.types.ArrayType;
import lib.util.persistent.types.ReferenceArrayType;
import lib.util.persistent.types.ObjectType;

public class PersistentImmutableValueArray<T extends AnyPersistent> extends PersistentImmutableArray<T> {
    private static ArrayType<PersistentImmutableValueArray> TYPE = new ReferenceArrayType<>(PersistentImmutableValueArray.class, Types.GENERIC_REFERENCE);

    @SafeVarargs
    @SuppressWarnings("unchecked")
    public PersistentImmutableValueArray(Class<T> elementClass, T... ts) {
        super(new ReferenceArrayType<PersistentImmutableValueArray>(PersistentImmutableValueArray.class, Types.typeForClass(elementClass)), ts);
        TYPE = (ArrayType<PersistentImmutableValueArray>)this.getType();
    }

    protected PersistentImmutableValueArray(ObjectPointer<? extends PersistentImmutableValueArray<T>> pointer) {
        super(pointer);
    }

    @Override
    void setObjectElement(int index, AnyPersistent value) {
        // System.out.format("setObjectElement(%d, %s), offset = %d\n", index, value, elementOffset(index));
        setValueObject(elementOffset(checkIndex(index)), value);
    }

    @Override
    AnyPersistent getObjectElement(int index) {
        // System.out.format("getObjectElement(%d), offset = %d\n", index, elementOffset(index));
        return getValueObject(elementOffset(checkIndex(index)), getElementType());
    }

}
