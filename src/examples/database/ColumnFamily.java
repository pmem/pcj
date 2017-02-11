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

package examples.database;

import lib.persistent.*;

class ColumnFamily extends PersistentTuple3<PersistentTreeMap<Key, Value>, PersistentLong, PersistentArray<PersistentString>> {

    public ColumnFamily(String[] colNames) {
        Transaction.run(() -> {
            _1(new PersistentTreeMap<Key, Value>());
            _2(new PersistentLong(colNames.length));
            PersistentArray<PersistentString> pColNames = new PersistentArray<>(colNames.length);
            for (int i = 0; i < colNames.length; i++) {
                pColNames.set(i, new PersistentString(colNames[i]));
            }
            _3(pColNames);
        });
    }

    public PersistentTreeMap<Key, Value> cf() {
        return _1();
    }

    public PersistentLong colCount() {
        return _2();
    }

    public PersistentArray<PersistentString> colNames() {
        return _3();
    }

    public void put(Key k, Value v) {
        _1().put(k, v);
    }

    public Value get(Key k) {
        return _1().get(k);
    }

    public Value remove(Key k) {
        return _1().remove(k);
    }
}
