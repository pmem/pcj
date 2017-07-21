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

import lib.util.persistent.types.*;
import lib.util.persistent.types.ObjectType;
import lib.util.persistent.front.PersistentClass;

@PersistentClass
public final class PersistentDouble extends PersistentObject
  implements Comparable<PersistentDouble> {
    private static final DoubleField DOUBLE = new DoubleField();
    private static final ObjectType<PersistentDouble> TYPE = ObjectType.fromFields(PersistentDouble.class, DOUBLE);
    private static final long serialVersionUID = 1L;

    public PersistentDouble(double x) {
        super(TYPE);
        setDoubleField(DOUBLE, x);
    }

    private PersistentDouble(ObjectPointer<PersistentDouble> p) {
        super(p);
    }

    public double doubleValue() {
        return getDoubleField(DOUBLE);
    }

    @Override
    public String toString() {
        return Double.toString(doubleValue());}

    @Override
    public boolean equals(Object o) {
        if (o instanceof PersistentDouble) {
            return ((Double)this.doubleValue()).equals((Double)(((PersistentDouble)o).doubleValue()));
  
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(doubleValue());}

    public int compareTo(PersistentDouble o) {
        double x = this.doubleValue();
        double y = o.doubleValue();
        return Double.compare(x, y);
    }
}