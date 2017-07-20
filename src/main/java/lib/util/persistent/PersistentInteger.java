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
public final class PersistentInteger extends PersistentObject
  implements Comparable<PersistentInteger> {
    private static final IntField INT = new IntField();
    public static final ObjectType<PersistentInteger> TYPE = ObjectType.fromFields(PersistentInteger.class, INT);
    private static final long serialVersionUID = 1L;

    public PersistentInteger(int x) {
        super(TYPE);
        setIntField(INT, x);
    }

    private PersistentInteger(ObjectPointer<PersistentInteger> p) {
        super(p);
    }

    public int intValue() {
        return getIntField(INT);
    }

    @Override
    public String toString() {
        return Integer.toString(intValue());}

    @Override
    public boolean equals(Object o) {
        if (o instanceof PersistentInteger) {
            return this.intValue() == ((PersistentInteger)o).intValue();  
        }
        return false;
    }

    @Override
    public int hashCode() {
        return intValue();
    }

    public int compareTo(PersistentInteger o) {
        int x = this.intValue();
        int y = o.intValue();
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }
}