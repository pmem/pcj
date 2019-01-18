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
import lib.util.persistent.front.PersistentClass;

@PersistentClass
public final class Long256 extends PersistentObject {
    public static final LongField X0 = new LongField();
    public static final LongField X1 = new LongField();
    public static final LongField X2 = new LongField();
    public static final LongField X3 = new LongField();
    public static final ObjectType<Long256> TYPE = ObjectType.withValueFields(Long256.class, X0, X1, X2, X3);

    public Long256(long x0, long x1, long x2, long x3) {
        super(TYPE);
        setX0(x0);
        setX1(x1);
        setX2(x2);
        setX3(x3);
    }

    private Long256(ObjectPointer<Long256> p) {
        super(p);
    }

    public long getX0() {return getLongField(X0);}
    public long getX1() {return getLongField(X1);}
    public long getX2() {return getLongField(X2);}
    public long getX3() {return getLongField(X3);}

    public void setX0(long x0) {setLongField(X0, x0);}
    public void setX1(long x1) {setLongField(X1, x1);}
    public void setX2(long x2) {setLongField(X2, x2);}
    public void setX3(long x3) {setLongField(X3, x3);}

    public String toString() {
        return "Long256(" + getX0() + ", " + getX1() + ", " + getX2() + ", " + getX3() + ")";
    } 
}