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
public final class PersistentCharArray
  extends AbstractPersistentArray {
    private static final long serialVersionUID = 1L;
    private static final ArrayType<PersistentCharArray> TYPE = new ArrayType<>(PersistentCharArray.class, Types.CHAR);

    public PersistentCharArray(int size) {
        super(TYPE, size);
    }

    public PersistentCharArray(char[] array) {
        this(array.length);
        for (int i = 0; i < array.length; i++) setCharElement(i, array[i]);
    }

    private PersistentCharArray(ObjectPointer<PersistentCharArray> pointer) {
        super(pointer);
    }

    public synchronized  char get(int index) {
        return getCharElement(index);
    }

    public synchronized void set(int index, char value) {
        setCharElement(index, value);
    }

    public synchronized char[] toArray() {
        char[] ans = new char[length()];
        int len = length();
        for (int i = 0; i < len; i++) ans[i] = getCharElement(i);
        return ans;
    }
}