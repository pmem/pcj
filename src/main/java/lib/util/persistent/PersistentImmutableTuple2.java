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

public class PersistentImmutableTuple2<T1 extends AnyPersistent, T2 extends AnyPersistent> extends PersistentImmutableObject {
    private static final ObjectField<AnyPersistent> FIELD1 = new ObjectField<>();
    private static final ObjectField<AnyPersistent> FIELD2 = new ObjectField<>();
    public static final ObjectType<PersistentImmutableTuple2> TYPE = ObjectType.withFields(PersistentImmutableTuple2.class, FIELD1, FIELD2);

    public PersistentImmutableTuple2(T1 t1, T2 t2) {
        this(TYPE, t1, t2);
    }

    protected PersistentImmutableTuple2(ObjectType<? extends PersistentImmutableTuple2> type, T1 t1, T2 t2) {
        super(type, (PersistentImmutableTuple2 self) -> {
            Transaction.run(() -> {
                self.initObjectField(FIELD1, t1);
                self.initObjectField(FIELD2, t2); 
            });
        });
    }

    protected PersistentImmutableTuple2(ObjectPointer<? extends PersistentImmutableTuple2> p) {super(p);}

    @SuppressWarnings("unchecked")
        public T1 _1() {
            return (T1)getObjectField(FIELD1);
        }

    @SuppressWarnings("unchecked")
        public T2 _2() {
            return (T2)getObjectField(FIELD2);
        }


    public String toString() {
        return "Tuple2(" + _1() + ", " + _2() + ")";
    }

    public int hashCode() {
        return _1().hashCode() + _2().hashCode();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof PersistentImmutableTuple2)) return false;

        PersistentImmutableTuple2 that = (PersistentImmutableTuple2)obj;
        if (!(this._1().equals(that._1())) ||
            !(this._2().equals(that._2())))
            return false;

        return true;
    } 
}