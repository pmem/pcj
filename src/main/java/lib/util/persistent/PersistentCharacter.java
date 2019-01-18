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

public final class PersistentCharacter extends PersistentImmutableObject implements Comparable<PersistentCharacter> { 
    private static final CharField CHAR = new CharField();
    private static final ObjectType<PersistentCharacter> TYPE = ObjectType.withFields(PersistentCharacter.class, CHAR);

    public PersistentCharacter(char x) {
        super(TYPE, (PersistentCharacter self) -> {
            self.initCharField(CHAR, x);
        });
    }

    private PersistentCharacter(ObjectPointer<PersistentCharacter> p) {
        super(p);
    }

    public char charValue() {
        return getCharField(CHAR);
    }

    @Override
    public String toString() {
        return String.valueOf(charValue());
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof PersistentCharacter) {
            return this.charValue() == ((PersistentCharacter)o).charValue();  
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (int) charValue();
    }

    public int compareTo(PersistentCharacter o) {
        char x = this.charValue();
        char y = o.charValue();
        return (x - y);
    }
}