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
import lib.util.persistent.types.ValueArrayType;
import lib.util.persistent.types.ArrayType;
import lib.util.persistent.types.ValueType;
import java.lang.reflect.Array;

public final class PersistentValueArray<T extends PersistentValue> extends AbstractPersistentArray {
    public static final ArrayType<PersistentValueArray> TYPE = new ArrayType<>(PersistentValueArray.class, Types.VALUE);

    public PersistentValueArray(Class<T> boxingClass, int size) {
      super(new ValueArrayType<PersistentValueArray>(PersistentValueArray.class, boxingClass), size);
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    public PersistentValueArray(T... ts) {
        this((Class<T>)ts[0].getClass(), ts.length);
        for (int i = 0; i < ts.length; i++) setValueElement(i, ts[i]);
    }

    public PersistentValueArray(ObjectPointer<PersistentArray> pointer) {
        super(pointer);
    }

    @SuppressWarnings("unchecked")
    public T get(int index, Class<T> boxingClass) {
        return (T)getValueElement(index, boxingClass);
    }

    public void set(int index, T value) {
        setValueElement(index, value);
    }

    @SuppressWarnings("unchecked")
    public T[] toArray(T[] a, Class<T> boxingClass) {
        T[] ans = (T[])new PersistentValue[length()];
        int len = length();
        for (int i = 0; i < len; i++) ans[i] = get(i, boxingClass);
        return ans;
    }
}
