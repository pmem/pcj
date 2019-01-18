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

public class IndirectValueObjectType<T extends AnyPersistent> extends DirectValueObjectType<T> {
    public static long FIELDS_OFFSET = Types.POINTER.size(); // room for ClassInfo pointer;
    public static long CLASS_INFO_OFFSET = 0;
    
    public static <T extends AnyPersistent> IndirectValueObjectType<T> withFields(Class<T> cls, ValueBasedField... fs) {
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
        long size = FIELDS_OFFSET;
        if (fieldTypes.size() > 0) {
            offsets[0] = size;
            size += ((PersistentField)fs[0]).getType().size();
            for (int i = 1; i < fs.length; i++) {
                offsets[i] = size;
                size += ((PersistentField)fs[i]).getType().size();
            }
        }
        IndirectValueObjectType<T> ans = new IndirectValueObjectType<>(cls, fieldTypes, offsets, size);
        return ans;
    }

    protected IndirectValueObjectType(Class<T> cls, List<PersistentType> fieldTypes, long[] offsets, long size) {
        super(cls, ObjectType.Kind.IndirectValue, fieldTypes, offsets, size);
        // System.out.println("IVOT for class " + cls + ", size = " + size);
    }

    @Override 
    public boolean equals(Object obj) {
        return obj instanceof IndirectValueObjectType && ((IndirectValueObjectType)obj).fieldTypes().equals(fieldTypes());
    }

    @Override 
    public String toString() {
        StringBuilder buff = new StringBuilder();
        buff.append("IndirectValueObjectType(");
        for (PersistentType t : fieldTypes()) {
            buff.append(t).append(" ");
        }
        buff.append(")");
        return buff.toString();
    }
}
