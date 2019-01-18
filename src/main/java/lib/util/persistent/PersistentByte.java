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

public final class PersistentByte extends PersistentImmutableObject implements Comparable<PersistentByte> { 
    private static final ByteField BYTE = new ByteField();
    private static final ObjectType<PersistentByte> TYPE = ObjectType.withFields(PersistentByte.class, BYTE);

    public PersistentByte(byte x) {
        super(TYPE, (PersistentByte self) -> {
            self.initByteField(BYTE, x);
        });
    }

    private PersistentByte(ObjectPointer<PersistentByte> p) {
        super(p);
    }

    public byte byteValue() {
        return getByteField(BYTE);
    }

    @Override
    public String toString() {
        return Integer.toString((int)byteValue(), 10);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof PersistentByte) {
            return this.byteValue() == ((PersistentByte)o).byteValue();  
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (int) byteValue();
    }

    public int compareTo(PersistentByte o) {
        byte x = this.byteValue();
        byte y = o.byteValue();
        return (x - y);
    }
}