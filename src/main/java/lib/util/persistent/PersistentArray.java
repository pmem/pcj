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
import java.lang.reflect.Array;
import java.util.Arrays;
import lib.xpersistent.*;
import static lib.util.persistent.Trace.*;

public class PersistentArray<T extends AnyPersistent> extends AbstractPersistentMutableArray {
    private static final ArrayType<PersistentArray> TYPE = new ReferenceArrayType<>(PersistentArray.class, Types.GENERIC_REFERENCE);

    public static <A extends PersistentArray, T extends AnyPersistent> ArrayType<A> typeForClasses(Class<A> arrayClass, Class<T> elementClass) {
        return new ReferenceArrayType<>(arrayClass, Types.typeForClass(elementClass));
    }

    @SuppressWarnings("unchecked")
    public static <T extends AnyPersistent> PersistentArray<T> forElementClass(Class<T> elementClass, int size) {
        // System.out.format("PersistentArray forElementClass(%s, %d)\n", elementClass, size);
        PersistentArray<T> ans = null;
        ObjectType ot = Types.objectTypeForClass(elementClass);
        if (ot.valueBased()) {
            ans = new PersistentValueArray<T>(elementClass, size);
        }
        else {
            ans = new PersistentArray<T>(size);
        }
        return ans;
    }

    public <T extends AnyPersistent> PersistentArray<T> make(int size) {
        return Transaction.run(() -> {
            return new PersistentArray<>(size);
        });
    }

    public PersistentArray(int size) {
        super(TYPE, size);
    }


    public PersistentArray(Class<? extends AnyPersistent> elementClass, int size) {
        super(new ReferenceArrayType<PersistentArray>(PersistentArray.class, Types.typeForClass(elementClass)), size);
    }

    protected PersistentArray(ArrayType<? extends PersistentArray> type, int size) {
        super(type, size);
    }

    @SafeVarargs
    public PersistentArray(T... ts) {
        this(ts.length);
        for (int i = 0; i < ts.length; i++) setObjectElement(i, ts[i]);
    }

    protected PersistentArray(ObjectPointer<? extends PersistentArray<T>> pointer) {
        super(pointer);
    }

    @SuppressWarnings("unchecked")
    public T get(int index) {
        // trace(true, "PA.get(%d)", index);
        return (T)getObjectElement(index);
    }

    public void set(int index, T value) {
        // trace(true, "PA.set(%d)", index);
        setObjectElement(index, value);
    }

    @SafeVarargs
    public final void insert(int index, T... ts) {
        Transaction.run(() -> {
            for (int i = index; i < index + ts.length; i++) setObjectElement(i, ts[i-index]);
        }, this);
    }

    public AnyPersistent[] toArray() {
        return super.toObjectArray();
    }

    public T[] toArray(T[] a) {
        return toObjectArray(a);
    }

    public String toString() {
        return Util.synchronizedBlock(this, () -> {
            int iMax = this.length() - 1;
            if (iMax == -1) return "[]";

            StringBuilder b = new StringBuilder();
            b.append('[');
            for (int i = 0; ; i++) {
                if (this.get(i) != null)
                    b.append(this.get(i).toString());
                else
                    b.append("NULL");
                if (i == iMax)
                    return b.append(']').toString();
                b.append(", ");
            }
        });
    }
}
