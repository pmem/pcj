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

public final class PersistentDoubleArray extends AbstractPersistentMutableArray {
    private static final ArrayType<PersistentDoubleArray> TYPE = new ReferenceArrayType<>(PersistentDoubleArray.class, Types.DOUBLE);

    public PersistentDoubleArray(int size) {
        super(TYPE, size);
    }

    public PersistentDoubleArray(double[] array) {
        this(array.length);
        for (int i = 0; i < array.length; i++) setDoubleElement(i, array[i]);
    }

    private PersistentDoubleArray(ObjectPointer<PersistentDoubleArray> pointer) {
        super(pointer);
    }

    public double get(int index) {
        return getDoubleElement(index);
    }

    public void set(int index, double value) {
        setDoubleElement(index, value);
    }

    public synchronized double[] toArray() {
        double[] ans = new double[length()];
        int len = length();
        for (int i = 0; i < len; i++) ans[i] = getDoubleElement(i);
        return ans;
    }
}