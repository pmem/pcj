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

public class PersistentTuple4<T1 extends AnyPersistent, T2 extends AnyPersistent, T3 extends AnyPersistent, T4 extends AnyPersistent> extends PersistentObject {
    private static final ObjectField<AnyPersistent> FIELD1 = new ObjectField<>();
    private static final ObjectField<AnyPersistent> FIELD2 = new ObjectField<>();
    private static final ObjectField<AnyPersistent> FIELD3 = new ObjectField<>();
    private static final ObjectField<AnyPersistent> FIELD4 = new ObjectField<>(); 
    public static final ObjectType<PersistentTuple4> TYPE = ObjectType.withFields(PersistentTuple4.class, FIELD1, FIELD2, FIELD3, FIELD4);

    public PersistentTuple4() {
        super(TYPE);
    }

    public PersistentTuple4(T1 t1, T2 t2, T3 t3, T4 t4) {
        this(TYPE, t1, t2, t3, t4);
    }

    protected PersistentTuple4(ObjectType<? extends PersistentTuple4> type, T1 t1, T2 t2, T3 t3, T4 t4) {
        super(type);
        Transaction.run(() -> {
            _1(t1);
            _2(t2);
            _3(t3);
            _4(t4); 
        });
    }

    protected PersistentTuple4(ObjectPointer<? extends PersistentTuple4> p) {super(p);}

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

    @SuppressWarnings("unchecked")
        public T4 _4() {
            return (T4)getObjectField(FIELD4);
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

    public void _4(T4 t4) {
            setObjectField(FIELD4, t4);
        }

    public String toString() {
        return "Tuple4(" + _1() + ", " + _2() + ", " + _3() + ", " + _4() + ")";
    }

    public synchronized int hashCode() {
        return _1().hashCode() + _2().hashCode() + _3().hashCode() + _4().hashCode();
    }

    public synchronized boolean equals(Object obj) {
        if (!(obj instanceof PersistentTuple4)) return false;

        PersistentTuple4 that = (PersistentTuple4)obj;
        if (!(this._1().equals(that._1())) ||
            !(this._2().equals(that._2())) ||
            !(this._3().equals(that._3())) ||
            !(this._4().equals(that._4())))
            return false;

        return true;
    } 
}