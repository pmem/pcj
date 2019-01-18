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

public final class PersistentShort extends PersistentImmutableObject implements Comparable<PersistentShort> { 
    private static final ShortField SHORT = new ShortField();
    private static final ObjectType<PersistentShort> TYPE = ObjectType.withFields(PersistentShort.class, SHORT);

    public PersistentShort(short x) {
        super(TYPE, (PersistentShort self) -> {
            self.initShortField(SHORT, x);
        });
    }

    private PersistentShort(ObjectPointer<PersistentShort> p) {
        super(p);
    }

    public short shortValue() {
        return getShortField(SHORT);
    }

    @Override
    public String toString() {
        return Integer.toString((int)shortValue(), 10);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof PersistentShort) {
            return this.shortValue() == ((PersistentShort)o).shortValue();  
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (int) shortValue();
    }

    public int compareTo(PersistentShort o) {
        short x = this.shortValue();
        short y = o.shortValue();
        return (x - y);
    }
}