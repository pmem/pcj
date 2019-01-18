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

import lib.util.persistent.types.Types;
import lib.util.persistent.types.ArrayType;
import lib.util.persistent.types.ReferenceArrayType;

public final class PersistentImmutableLongArray extends AbstractPersistentImmutableArray {
    private static final ArrayType<PersistentImmutableLongArray> TYPE = new ReferenceArrayType<>(PersistentImmutableLongArray.class, Types.LONG);

    public PersistentImmutableLongArray(long[] array) {
        super(TYPE, array.length, array);
    }

    private PersistentImmutableLongArray(ObjectPointer<PersistentImmutableLongArray> pointer) {
        super(pointer);
    }

    public long get(int index) {
        return getLongElement(index);
    }

    public long[] toArray() {
        long[] ans = new long[length()];
        int len = length();
        for (int i = 0; i < len; i++) ans[i] = getLongElement(i);
        return ans;
    }
}