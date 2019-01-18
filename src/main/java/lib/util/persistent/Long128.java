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

import lib.util.persistent.types.LongField;
import lib.util.persistent.types.ValueType;
import lib.util.persistent.types.ObjectType;
import lib.util.persistent.types.ObjectField;
import lib.util.persistent.types.ArrayType;
import lib.util.persistent.types.ReferenceArrayType;
import lib.util.persistent.front.PersistentClass;
import lib.util.persistent.PersistentArray;
import static lib.util.persistent.Trace.*;

public final class Long128 extends PersistentObject {
    private static final LongField X0 = new LongField();
    private static final LongField X1 = new LongField();
    public static final ObjectType<Long128> TYPE = ObjectType.withValueFields(Long128.class, X0, X1);

    public Long128(long x0, long x1) {
        super(TYPE);
        setX0(x0);
        setX1(x1);
    }

    private Long128(ObjectPointer<Long128> p) {
        super(p);
    }

    public long getX0() {return getLongField(X0);}
    public long getX1() {return getLongField(X1);}

    public void setX0(long x0) {setLongField(X0, x0);}
    public void setX1(long x1) {setLongField(X1, x1);}

    public String toString() {
        return "Long128(" + getX0() + ", " + getX1() + ")";
    }

    public static class Foo extends PersistentObject {
        public static final ObjectField<Long128> L = new ObjectField<>(Long128.class);
        public static final ObjectType<Foo> TYPE = ObjectType.withFields(Foo.class, L);

        public Foo(Long128 x) {
            super(TYPE);
            set(x);
        } 

        public Foo(ObjectPointer<Foo> p) {super(p);}
        public void set(Long128 x) {setObjectField(L, x);}
        public Long128 get() {return getObjectField(L);}
    }

    public static class Long128Array extends AbstractPersistentMutableArray {
        public static final ArrayType<Long128Array> TYPE = new ReferenceArrayType<>(Long128Array.class, Long128.TYPE);

        public Long128Array(int size) {
          super(TYPE, size);
        }

        public Long128Array(ObjectPointer<Long128Array> pointer) {
            super(pointer);
        }

        @SuppressWarnings("unchecked")
        public Long128 get(int index) {
            return (Long128)getObjectElement(index);
        }

        public void set(int index, Long128 value) {
            setObjectElement(index, value);
        }

        @SuppressWarnings("unchecked")
        public Long128[] toArray(Long128[] a) {
            Long128[] ans = new Long128[length()];
            int len = length();
            for (int i = 0; i < len; i++) ans[i] = get(i);
            return ans;
        }
    }

    public static void main(String[] args) {
        Trace.enable(true);
        trace(true, "new Long128");
        Long128 x = new Long128(123, 234);
        System.out.println("x = " + x);
        trace(true, "new Foo(x)");
        Foo foo = new Foo(x);
        trace(true, "foo.get()");
        Long128 y = foo.get();
        System.out.println("y = " + y);
        Long128 z = new Long128(y.getX0(), y.getX1());
        System.out.println("z = " + z);
        Foo foo1 = new Foo(z);
        System.out.println("- - - - - - - - - - - - - - - - - -");
        Long128Array a = new Long128Array(10);
        a.set(5, x);
        a.set(7, y);
        System.out.println("a(5) = " + a.get(5));
        PersistentArray<Long128> a1 = new PersistentArray<>(Long128.class, 10);
        a1.set(5, x);
        System.out.println("a1(5) = " + a1.get(5));
        System.out.println("-----------------------------------");
    }

 
}