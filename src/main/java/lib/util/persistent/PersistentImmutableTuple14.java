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

public class PersistentImmutableTuple14<T1 extends AnyPersistent, T2 extends AnyPersistent, T3 extends AnyPersistent, T4 extends AnyPersistent, T5 extends AnyPersistent, T6 extends AnyPersistent, T7 extends AnyPersistent, T8 extends AnyPersistent, T9 extends AnyPersistent, T10 extends AnyPersistent, T11 extends AnyPersistent, T12 extends AnyPersistent, T13 extends AnyPersistent, T14 extends AnyPersistent> extends PersistentImmutableObject {
    private static final ObjectField<AnyPersistent> FIELD1 = new ObjectField<>();
    private static final ObjectField<AnyPersistent> FIELD2 = new ObjectField<>();
    private static final ObjectField<AnyPersistent> FIELD3 = new ObjectField<>();
    private static final ObjectField<AnyPersistent> FIELD4 = new ObjectField<>();
    private static final ObjectField<AnyPersistent> FIELD5 = new ObjectField<>();
    private static final ObjectField<AnyPersistent> FIELD6 = new ObjectField<>();
    private static final ObjectField<AnyPersistent> FIELD7 = new ObjectField<>();
    private static final ObjectField<AnyPersistent> FIELD8 = new ObjectField<>();
    private static final ObjectField<AnyPersistent> FIELD9 = new ObjectField<>();
    private static final ObjectField<AnyPersistent> FIELD10 = new ObjectField<>();
    private static final ObjectField<AnyPersistent> FIELD11 = new ObjectField<>();
    private static final ObjectField<AnyPersistent> FIELD12 = new ObjectField<>();
    private static final ObjectField<AnyPersistent> FIELD13 = new ObjectField<>();
    private static final ObjectField<AnyPersistent> FIELD14 = new ObjectField<>();
    public static final ObjectType<PersistentImmutableTuple14> TYPE = ObjectType.withFields(PersistentImmutableTuple14.class, FIELD1, FIELD2, FIELD3, FIELD4, FIELD5, FIELD6, FIELD7, FIELD8, FIELD9, FIELD10, FIELD11, FIELD12, FIELD13, FIELD14);

    public PersistentImmutableTuple14(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8, T9 t9, T10 t10, T11 t11, T12 t12, T13 t13, T14 t14) {
        this(TYPE, t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14);
    }

    protected PersistentImmutableTuple14(ObjectType<? extends PersistentImmutableTuple14> type, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8, T9 t9, T10 t10, T11 t11, T12 t12, T13 t13, T14 t14) {
        super(type, (PersistentImmutableTuple14 self) -> {
            Transaction.run(() -> {
                self.initObjectField(FIELD1, t1);
                self.initObjectField(FIELD2, t2);
                self.initObjectField(FIELD3, t3);
                self.initObjectField(FIELD4, t4);
                self.initObjectField(FIELD5, t5);
                self.initObjectField(FIELD6, t6);
                self.initObjectField(FIELD7, t7);
                self.initObjectField(FIELD8, t8);
                self.initObjectField(FIELD9, t9);
                self.initObjectField(FIELD10, t10);
                self.initObjectField(FIELD11, t11);
                self.initObjectField(FIELD12, t12);
                self.initObjectField(FIELD13, t13);
                self.initObjectField(FIELD14, t14); 
            });
        });
    }

    protected PersistentImmutableTuple14(ObjectPointer<? extends PersistentImmutableTuple14> p) {super(p);}

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

    @SuppressWarnings("unchecked")
        public T12 _12() {
            return (T12)getObjectField(FIELD12);
        }

    @SuppressWarnings("unchecked")
        public T13 _13() {
            return (T13)getObjectField(FIELD13);
        }

    @SuppressWarnings("unchecked")
        public T14 _14() {
            return (T14)getObjectField(FIELD14);
        }


    public String toString() {
        return "Tuple14(" + _1() + ", " + _2() + ", " + _3() + ", " + _4() + ", " + _5() + ", " + _6() + ", " + _7() + ", " + _8() + ", " + _9() + ", " + _10() + ", " + _11() + ", " + _12() + ", " + _13() + ", " + _14() + ")";
    }

    public int hashCode() {
        return _1().hashCode() + _2().hashCode() + _3().hashCode() + _4().hashCode() + _5().hashCode() + _6().hashCode() + _7().hashCode() + _8().hashCode() + _9().hashCode() + _10().hashCode() + _11().hashCode() + _12().hashCode() + _13().hashCode() + _14().hashCode();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof PersistentImmutableTuple14)) return false;

        PersistentImmutableTuple14 that = (PersistentImmutableTuple14)obj;
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
            !(this._11().equals(that._11())) ||
            !(this._12().equals(that._12())) ||
            !(this._13().equals(that._13())) ||
            !(this._14().equals(that._14())))
            return false;

        return true;
    } 
}