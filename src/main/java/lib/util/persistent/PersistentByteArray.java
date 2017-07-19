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
import lib.util.persistent.front.PersistentClass;

@PersistentClass
public final class PersistentByteArray
  extends AbstractPersistentArray {
    private static final long serialVersionUID = 1L;
    private static final ArrayType<PersistentByteArray> TYPE = new ArrayType<>(PersistentByteArray.class, Types.BYTE);

    public PersistentByteArray(int size) {
        super(TYPE, size);
    }

    public PersistentByteArray(byte[] array) {
        this(array.length);
        for (int i = 0; i < array.length; i++) setByteElement(i, array[i]);
    }

    private PersistentByteArray(ObjectPointer<PersistentByteArray> pointer) {
        super(pointer);
    }

    public synchronized  byte get(int index) {
        return getByteElement(index);
    }

    public synchronized void set(int index, byte value) {
        setByteElement(index, value);
    }

    public synchronized byte[] toArray() {
        byte[] ans = new byte[length()];
        int len = length();
        for (int i = 0; i < len; i++) ans[i] = getByteElement(i);
        return ans;
    }
}