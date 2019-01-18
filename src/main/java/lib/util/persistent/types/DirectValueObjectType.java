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

import java.util.List;
import java.util.ArrayList;
import lib.util.persistent.AnyPersistent;

public class DirectValueObjectType<T extends AnyPersistent> extends ObjectType<T> {
    private long allocationSize;

    public static <T extends AnyPersistent> DirectValueObjectType<T> withFields(Class<T> cls, ValueBasedField... fs) {
        int fieldCount = fs.length;
        List<PersistentType> fieldTypes = new ArrayList<>();
        int index = 0;
        for (ValueBasedField vf : fs) {
            PersistentField f = (PersistentField)vf;
            f.setIndex(index);
            index++;
            fieldTypes.add(f.getType());
            // insertType(f.getType(), fieldTypes);
        }
        long[] offsets = new long[fieldCount];
        long size = 0;
        if (fieldTypes.size() > 0) {
            offsets[0] = size;
            size += ((PersistentField)fs[0]).getType().size();
            for (int i = 1; i < fs.length; i++) {
                offsets[i] = size;
                size += ((PersistentField)fs[i]).getType().size();
            }
        }
        DirectValueObjectType<T> ans = new DirectValueObjectType<>(cls, ObjectType.Kind.DirectValue, fieldTypes, offsets, size);
        return ans;
    }

    public int fieldCount() {return offsets.length;}

    protected DirectValueObjectType(Class<T> cls, List<PersistentType> fieldTypes, long[] offsets, long size) {
        this(cls, ObjectType.Kind.DirectValue, fieldTypes, offsets, size);
    }

    protected DirectValueObjectType(Class<T> cls, ObjectType.Kind kind, List<PersistentType> fieldTypes, long[] offsets, long size) {
        super(cls, kind);
        this.fieldTypes = fieldTypes;
        this.offsets = offsets;
        this.allocationSize = size;
    }
    
    @Override public long size() {return allocationSize();}

    @Override public long allocationSize() {return allocationSize;}

    @Override 
    public boolean equals(Object obj) {
        return obj instanceof DirectValueObjectType && ((DirectValueObjectType)obj).fieldTypes().equals(fieldTypes());
    }

    @Override 
    public String toString() {
        StringBuilder buff = new StringBuilder();
        buff.append("DirectValueObjectType(");
        for (PersistentType t : fieldTypes()) {
            buff.append(t).append("");
        }
        buff.append(")");
        return buff.toString();
    }
}
