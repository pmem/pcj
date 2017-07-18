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
import lib.util.persistent.front.PersistentClass;

@PersistentClass
public class PersistentTuple5<T1 extends PersistentObject, T2 extends PersistentObject, T3 extends PersistentObject, T4 extends PersistentObject, T5 extends PersistentObject>
  extends PersistentObject {
    private static final long serialVersionUID = 1L;
    private static final ObjectField<PersistentObject> FIELD1 = new ObjectField<>();
    private static final ObjectField<PersistentObject> FIELD2 = new ObjectField<>();
    private static final ObjectField<PersistentObject> FIELD3 = new ObjectField<>();
    private static final ObjectField<PersistentObject> FIELD4 = new ObjectField<>();
    private static final ObjectField<PersistentObject> FIELD5 = new ObjectField<>();
    public static final ObjectType<PersistentTuple5> TYPE = ObjectType.fromFields(PersistentTuple5.class, FIELD1, FIELD2, FIELD3, FIELD4, FIELD5);

    public PersistentTuple5() {
        super(TYPE);
    }

    public PersistentTuple5(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) {
        this(TYPE, t1, t2, t3, t4, t5);
    }

    protected PersistentTuple5(ObjectType<? extends PersistentTuple5> type, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) {
        super(type);
        Transaction.run(() -> {
            _1(t1);
            _2(t2);
            _3(t3);
            _4(t4);
            _5(t5); 
        });
    }

    protected PersistentTuple5(ObjectPointer<? extends PersistentTuple5> p) {super(p);}

    @SuppressWarnings("unchecked")
        public synchronized T1 _1() {
            return (T1)getObjectField(FIELD1);
        }

    @SuppressWarnings("unchecked")
        public synchronized T2 _2() {
            return (T2)getObjectField(FIELD2);
        }

    @SuppressWarnings("unchecked")
        public synchronized T3 _3() {
            return (T3)getObjectField(FIELD3);
        }

    @SuppressWarnings("unchecked")
        public synchronized T4 _4() {
            return (T4)getObjectField(FIELD4);
        }

    @SuppressWarnings("unchecked")
        public synchronized T5 _5() {
            return (T5)getObjectField(FIELD5);
        }

    public synchronized void _1(T1 t1) {
            setObjectField(FIELD1, t1);
        }

    public synchronized void _2(T2 t2) {
            setObjectField(FIELD2, t2);
        }

    public synchronized void _3(T3 t3) {
            setObjectField(FIELD3, t3);
        }

    public synchronized void _4(T4 t4) {
            setObjectField(FIELD4, t4);
        }

    public synchronized void _5(T5 t5) {
            setObjectField(FIELD5, t5);
        }

    public String toString() {
        return "Tuple5(" + _1() + ", " + _2() + ", " + _3() + ", " + _4() + ", " + _5() + ")";
    }

    public synchronized int hashCode() {
        return _1().hashCode() + _2().hashCode() + _3().hashCode() + _4().hashCode() + _5().hashCode();
    }

    public synchronized boolean equals(Object obj) {
        if (!(obj instanceof PersistentTuple5)) return false;

        PersistentTuple5 that = (PersistentTuple5)obj;
        if (!(this._1().equals(that._1())) ||
            !(this._2().equals(that._2())) ||
            !(this._3().equals(that._3())) ||
            !(this._4().equals(that._4())) ||
            !(this._5().equals(that._5())))
            return false;

        return true;
    } 
}