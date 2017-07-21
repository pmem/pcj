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

import lib.util.persistent.types.ObjectType;
import lib.util.persistent.types.ObjectField;
import lib.util.persistent.front.PersistentClass;

@PersistentClass
public class PersistentImmutableTuple11<T1 extends PersistentObject, T2 extends PersistentObject, T3 extends PersistentObject, T4 extends PersistentObject, T5 extends PersistentObject, T6 extends PersistentObject, T7 extends PersistentObject, T8 extends PersistentObject, T9 extends PersistentObject, T10 extends PersistentObject, T11 extends PersistentObject> extends PersistentObject {
    private static final long serialVersionUID = 1L;
    private static final ObjectField<PersistentObject> FIELD1 = new ObjectField<>();
    private static final ObjectField<PersistentObject> FIELD2 = new ObjectField<>();
    private static final ObjectField<PersistentObject> FIELD3 = new ObjectField<>();
    private static final ObjectField<PersistentObject> FIELD4 = new ObjectField<>();
    private static final ObjectField<PersistentObject> FIELD5 = new ObjectField<>();
    private static final ObjectField<PersistentObject> FIELD6 = new ObjectField<>();
    private static final ObjectField<PersistentObject> FIELD7 = new ObjectField<>();
    private static final ObjectField<PersistentObject> FIELD8 = new ObjectField<>();
    private static final ObjectField<PersistentObject> FIELD9 = new ObjectField<>();
    private static final ObjectField<PersistentObject> FIELD10 = new ObjectField<>();
    private static final ObjectField<PersistentObject> FIELD11 = new ObjectField<>();
    public static final ObjectType<PersistentImmutableTuple11> TYPE = ObjectType.fromFields(PersistentImmutableTuple11.class, FIELD1, FIELD2, FIELD3, FIELD4, FIELD5, FIELD6, FIELD7, FIELD8, FIELD9, FIELD10, FIELD11);

    public PersistentImmutableTuple11(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8, T9 t9, T10 t10, T11 t11) {
        this(TYPE, t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11);
    }

    protected PersistentImmutableTuple11(ObjectType<? extends PersistentImmutableTuple11> type, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8, T9 t9, T10 t10, T11 t11) {
        super(type);
        Transaction.run(() -> {
            _1(t1);
            _2(t2);
            _3(t3);
            _4(t4);
            _5(t5);
            _6(t6);
            _7(t7);
            _8(t8);
            _9(t9);
            _10(t10);
            _11(t11); 
        });
    }

    protected PersistentImmutableTuple11(ObjectPointer<? extends PersistentImmutableTuple11> p) {super(p);}

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

    @SuppressWarnings("unchecked")
        public T5 _5() {
            return (T5)getObjectField(FIELD5);
        }

    @SuppressWarnings("unchecked")
        public T6 _6() {
            return (T6)getObjectField(FIELD6);
        }

    @SuppressWarnings("unchecked")
        public T7 _7() {
            return (T7)getObjectField(FIELD7);
        }

    @SuppressWarnings("unchecked")
        public T8 _8() {
            return (T8)getObjectField(FIELD8);
        }

    @SuppressWarnings("unchecked")
        public T9 _9() {
            return (T9)getObjectField(FIELD9);
        }

    @SuppressWarnings("unchecked")
        public T10 _10() {
            return (T10)getObjectField(FIELD10);
        }

    @SuppressWarnings("unchecked")
        public T11 _11() {
            return (T11)getObjectField(FIELD11);
        }

    private void _1(T1 t1) {
            setObjectField(FIELD1, t1);
        }

    private void _2(T2 t2) {
            setObjectField(FIELD2, t2);
        }

    private void _3(T3 t3) {
            setObjectField(FIELD3, t3);
        }

    private void _4(T4 t4) {
            setObjectField(FIELD4, t4);
        }

    private void _5(T5 t5) {
            setObjectField(FIELD5, t5);
        }

    private void _6(T6 t6) {
            setObjectField(FIELD6, t6);
        }

    private void _7(T7 t7) {
            setObjectField(FIELD7, t7);
        }

    private void _8(T8 t8) {
            setObjectField(FIELD8, t8);
        }

    private void _9(T9 t9) {
            setObjectField(FIELD9, t9);
        }

    private void _10(T10 t10) {
            setObjectField(FIELD10, t10);
        }

    private void _11(T11 t11) {
            setObjectField(FIELD11, t11);
        }

    public String toString() {
        return "Tuple11(" + _1() + ", " + _2() + ", " + _3() + ", " + _4() + ", " + _5() + ", " + _6() + ", " + _7() + ", " + _8() + ", " + _9() + ", " + _10() + ", " + _11() + ")";
    }

    public int hashCode() {
        return _1().hashCode() + _2().hashCode() + _3().hashCode() + _4().hashCode() + _5().hashCode() + _6().hashCode() + _7().hashCode() + _8().hashCode() + _9().hashCode() + _10().hashCode() + _11().hashCode();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof PersistentImmutableTuple11)) return false;

        PersistentImmutableTuple11 that = (PersistentImmutableTuple11)obj;
        if (!(this._1().equals(that._1())) ||
            !(this._2().equals(that._2())) ||
            !(this._3().equals(that._3())) ||
            !(this._4().equals(that._4())) ||
            !(this._5().equals(that._5())) ||
            !(this._6().equals(that._6())) ||
            !(this._7().equals(that._7())) ||
            !(this._8().equals(that._8())) ||
            !(this._9().equals(that._9())) ||
            !(this._10().equals(that._10())) ||
            !(this._11().equals(that._11())))
            return false;

        return true;
    } 
}