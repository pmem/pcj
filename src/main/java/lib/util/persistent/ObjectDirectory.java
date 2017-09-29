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

import lib.util.persistent.spi.PersistentMemoryProvider;

public class ObjectDirectory {
    static PersistentHashMap<PersistentString, AnyPersistent> map;

    static {
        lib.util.persistent.spi.PersistentMemoryProvider.getDefaultProvider().getHeap().open();
    }


    @SuppressWarnings("unchecked")
    public static <T extends AnyPersistent> T get(String s, Class<T> cls) {
        return (T)map.get(new PersistentString(s + cls.getName()));
    }

    public static <T extends AnyPersistent> void put(String s, T obj) {
        map.put(new PersistentString(s + obj.getClass().getName()), obj);
    }

    @SuppressWarnings("unchecked")
    public static <T extends AnyPersistent> T remove(String s, Class<T> cls) {
        return (T)map.remove(new PersistentString(s + cls.getName()));
    }

    public static void initialize() {
        map = PersistentMemoryProvider.getDefaultProvider().getHeap().getRoot().getObjectDirectory();
    }
}
