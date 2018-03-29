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

package lib.util.persistent.types;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import lib.util.persistent.PersistentObject;
import lib.util.persistent.AnyPersistent;
import lib.util.persistent.ObjectDirectory;
import lib.util.persistent.PersistentString;
import lib.util.persistent.Header;
import java.lang.reflect.Field;
import static lib.util.persistent.Trace.*;

public class ValueBasedObjectType<T extends AnyPersistent> extends ObjectType<T> {
    private ValueType valueType;

    ValueBasedObjectType(Class<T> cls, ValueType valueType) {
        super(cls, valueType);
        this.valueType = valueType;
        // System.out.println("created VBOT for " + valueType + ", size = " + getSize() + ", allocSize = " + getAllocationSize());
    }

    public ValueType getValueType() {return valueType;}

    @Override public <U extends AnyPersistent> ObjectType<U> extendWith(Class<U> cls, PersistentType... ts) {
        throw new UnsupportedOperationException();
    }

    @Override public <U extends AnyPersistent> ObjectType<U> extendWith(Class<U> cls, PersistentField... fs) {
        throw new UnsupportedOperationException();
    }

    @Override public long getAllocationSize() {
        return valueType.getSize();
    }

    @Override public String getName() {
        return cls.getName();
    }

    @Override public long getSize() {
        return valueType.getSize();
    }

    @Override public List<PersistentType> getTypes() {
        return valueType.getTypes();
    }

    @Override public List<PersistentType> staticTypes() {
        throw new UnsupportedOperationException();
    }

    @Override public long getOffset(int index) {
        // trace("VBOT getOffset(%d) -> %d", index, valueType.getOffset(index));
        return valueType.getOffset(index);
    }

    @Override public int fieldCount() {
        return valueType.fieldCount();
    }

    @Override public int baseIndex() {
        throw new UnsupportedOperationException();
    }

    @Override public PersistentObject statics() {
        throw new UnsupportedOperationException();
    }

    @Override public String toString() {
        return "ValueBasedObjectType(" + getName() + ", valueType = " + valueType + ")";
    }
}

