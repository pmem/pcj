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

public class ValueType implements Container {
    private final List<PersistentType> types;
    private final long[] offsets;
    private final long size;

    public static ValueType fromFields(PersistentField... fs) {
        int fieldCount = fs.length;
        List<PersistentType> types = new ArrayList<>();
        int index = 0;
        for (PersistentField f : fs) {
            f.setIndex(index);
            index++;
            insertType(f.getType(), types);
        }
        long[] offsets = new long[fieldCount];
        long size = 0;
        if (types.size() > 0) {
            offsets[0] = size;
            size += fs[0].getType().getSize();
            for (int i = 1; i < fs.length; i++) {
                offsets[i] = size;
                size += fs[i].getType().getSize();
            }
        }
        return new ValueType(types, offsets, size);
    }

    private static void insertType(PersistentType t, List<PersistentType> types) {
        if (t instanceof Container) {
            for (PersistentType child : ((Container)t).getTypes()) insertType(child, types);
        }
        else types.add(t);
    }

    private ValueType(List<PersistentType> types, long[] offsets, long size) {
        this.types = types;
        this.offsets = offsets;
        this.size = size;
        // for (int i = 0; i < offsets.length; i++) {
        //     System.out.println(String.format("offset @ index %d =  %d", i, offsets[i]));
        // }
    }

    @Override
    public List<PersistentType> getTypes() {
        return types;
    }

    @Override 
    public long getOffset(int index) {
        return offsets[index];
    }

    @Override 
    public long getSize() {
        return size;
    }

    @Override 
    public boolean equals(Object obj) {
        return obj instanceof ValueType && ((ValueType)obj).getTypes().equals(getTypes());
    }

    @Override
    public int hashCode() {
        return getTypes().hashCode();
    }

    @Override 
    public String toString() {
        StringBuilder buff = new StringBuilder();
        buff.append("ValueType(");
        for (PersistentType t : getTypes()) {
            buff.append(t).append(" ");
        }
        buff.append(")");
        return buff.toString();
    }
}
