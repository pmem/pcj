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

public class PersistentTuple1<T1 extends AnyPersistent> extends PersistentObject {
    private static final ObjectField<AnyPersistent> FIELD1 = new ObjectField<>(); 
    public static final ObjectType<PersistentTuple1> TYPE = ObjectType.withFields(PersistentTuple1.class, FIELD1);

    public PersistentTuple1() {
        super(TYPE);
    }

    public PersistentTuple1(T1 t1) {
        this(TYPE, t1);
    }

    protected PersistentTuple1(ObjectType<? extends PersistentTuple1> type, T1 t1) {
        super(type);
        Transaction.run(() -> {
            _1(t1); 
        });
    }

    protected PersistentTuple1(ObjectPointer<? extends PersistentTuple1> p) {super(p);}

    @SuppressWarnings("unchecked")
        public T1 _1() {
            return (T1)getObjectField(FIELD1);
        }

    public void _1(T1 t1) {
            setObjectField(FIELD1, t1);
        }

    public String toString() {
        return "Tuple1(" + _1() + ")";
    }

    public synchronized int hashCode() {
        return _1().hashCode();
    }

    public synchronized boolean equals(Object obj) {
        if (!(obj instanceof PersistentTuple1)) return false;

        PersistentTuple1 that = (PersistentTuple1)obj;
        if (!(this._1().equals(that._1())))
            return false;

        return true;
    } 
}