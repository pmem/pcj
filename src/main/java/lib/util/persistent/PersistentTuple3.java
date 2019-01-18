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
import lib.util.persistent.types.ObjectType;
import lib.util.persistent.types.ObjectField;

public class PersistentTuple3<T1 extends AnyPersistent, T2 extends AnyPersistent, T3 extends AnyPersistent> extends PersistentObject {
    private static final ObjectField<AnyPersistent> FIELD1 = new ObjectField<>();
    private static final ObjectField<AnyPersistent> FIELD2 = new ObjectField<>();
    private static final ObjectField<AnyPersistent> FIELD3 = new ObjectField<>(); 
    public static final ObjectType<PersistentTuple3> TYPE = ObjectType.withFields(PersistentTuple3.class, FIELD1, FIELD2, FIELD3);

    public PersistentTuple3() {
        super(TYPE);
    }

    public PersistentTuple3(T1 t1, T2 t2, T3 t3) {
        this(TYPE, t1, t2, t3);
    }

    protected PersistentTuple3(ObjectType<? extends PersistentTuple3> type, T1 t1, T2 t2, T3 t3) {
        super(type);
        Transaction.run(() -> {
            _1(t1);
            _2(t2);
            _3(t3); 
        });
    }

    protected PersistentTuple3(ObjectPointer<? extends PersistentTuple3> p) {super(p);}

    @SuppressWarnings("unchecked")
        public T1 _1() {
            return (T1)getObjectField(FIELD1);
        }

    @SuppressWarnings("unchecked")
        public T2 _2() {
            return (T2)getObjectField(FIELD2);
        }

    @SuppressWarnings("unchecked")
        public T3 _3() {
            return (T3)getObjectField(FIELD3);
        }

    public void _1(T1 t1) {
            setObjectField(FIELD1, t1);
        }

    public void _2(T2 t2) {
            setObjectField(FIELD2, t2);
        }

    public void _3(T3 t3) {
            setObjectField(FIELD3, t3);
        }

    public String toString() {
        return "Tuple3(" + _1() + ", " + _2() + ", " + _3() + ")";
    }

    public synchronized int hashCode() {
        return _1().hashCode() + _2().hashCode() + _3().hashCode();
    }

    public synchronized boolean equals(Object obj) {
        if (!(obj instanceof PersistentTuple3)) return false;

        PersistentTuple3 that = (PersistentTuple3)obj;
        if (!(this._1().equals(that._1())) ||
            !(this._2().equals(that._2())) ||
            !(this._3().equals(that._3())))
            return false;

        return true;
    } 
}