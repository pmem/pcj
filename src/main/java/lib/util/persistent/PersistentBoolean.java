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
import lib.util.persistent.types.Types;

public final class PersistentBoolean extends PersistentImmutableObject implements Comparable<PersistentBoolean> { 
    private static final BooleanField BOOLEAN = new BooleanField();
    private static final ObjectType<PersistentBoolean> TYPE = ObjectType.withFields(PersistentBoolean.class, BOOLEAN);

    public PersistentBoolean(boolean x) {
        super(TYPE, (PersistentBoolean self) -> {
            self.initBooleanField(BOOLEAN, x);
        });
    }

    private PersistentBoolean(ObjectPointer<PersistentBoolean> p) {
        super(p);
    }

    public boolean booleanValue() {
        return getBooleanField(BOOLEAN);
    }

    @Override
    public String toString() {
        return booleanValue() ? "true" : "false";
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof PersistentBoolean) {
            return this.booleanValue() == ((PersistentBoolean)o).booleanValue();  
        }
        return false;
    }

    @Override
    public int hashCode() {
        return booleanValue() ? 1231 : 1237;
    }

    public int compareTo(PersistentBoolean o) {
        boolean x = this.booleanValue();
        boolean y = o.booleanValue();
        return (x == y) ? 0 : (x ? 1 : -1);
    }
}