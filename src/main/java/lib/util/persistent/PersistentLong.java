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

public final class PersistentLong extends PersistentImmutableObject implements Comparable<PersistentLong> { 
    private static final LongField LONG = new LongField();
    private static final ObjectType<PersistentLong> TYPE = ObjectType.withFields(PersistentLong.class, LONG);

    public PersistentLong(long x) {
        super(TYPE, (PersistentLong self) -> {
            self.initLongField(LONG, x);
        });
    }

    private PersistentLong(ObjectPointer<PersistentLong> p) {
        super(p);
    }

    public long longValue() {
        return getLongField(LONG);
    }

    @Override
    public String toString() {
        return Long.toString(longValue());}

    @Override
    public boolean equals(Object o) {
        if (o instanceof PersistentLong) {
            return this.longValue() == ((PersistentLong)o).longValue();  
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (int)(longValue() ^ (longValue() >>> 32));
    }

    public int compareTo(PersistentLong o) {
        long x = this.longValue();
        long y = o.longValue();
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }
}