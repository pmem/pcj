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
public final class PersistentFloat extends PersistentObject
  implements Comparable<PersistentFloat> {
    private static final FloatField FLOAT = new FloatField();
    private static final ObjectType<PersistentFloat> TYPE = ObjectType.fromFields(PersistentFloat.class, FLOAT);
    private static final long serialVersionUID = 1L;

    public PersistentFloat(float x) {
        super(TYPE);
        setFloatField(FLOAT, x);
    }

    private PersistentFloat(ObjectPointer<PersistentFloat> p) {
        super(p);
    }

    public float floatValue() {
        return getFloatField(FLOAT);
    }

    @Override
    public String toString() {
        return Float.toString(floatValue());}

    @Override
    public boolean equals(Object o) {
        if (o instanceof PersistentFloat) {
            return ((Float)this.floatValue()).equals((Float)(((PersistentFloat)o).floatValue()));
  
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Float.hashCode(floatValue());}

    public int compareTo(PersistentFloat o) {
        float x = this.floatValue();
        float y = o.floatValue();
        return Float.compare(x, y);
    }
}