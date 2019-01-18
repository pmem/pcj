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

import lib.util.persistent.AnyPersistent;
import lib.util.persistent.Bytes;
import java.util.ArrayList;
import java.util.HashMap;

// public class BytesType extends ValueType { 
public class BytesType extends DirectValueObjectType<Bytes> { 
    private static final HashMap<Long, BytesType> instances = new HashMap<>();
    private long size;

    public static BytesType forSize(long size) {
        BytesType ans = instances.get(size);
        if (ans == null) {
            ans = new BytesType(size);
            instances.put(size, ans);
            System.out.println("added " + ans);
        }
        return ans;
    }

    public BytesType(long size) {
        // super(new ArrayList<>(), new long[0], size);
        super(Bytes.class, new ArrayList<>(), new long[0], size);
        this.size = size;
    }

    public long size() {return allocationSize();}

    public long allocationSize() {
        return size;
    }

    public long getByteCount() {
        return size;
    }

    public long offset(long index) {
        return index;
    }

    @Override
    public String toString() {
        return "BytesType(" + size() + ")";
    }
}
