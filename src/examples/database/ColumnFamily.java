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

import lib.util.persistent.*;
import lib.util.persistent.types.*;

public final class ColumnFamily extends PersistentObject {
    private static final ObjectField<PersistentSkipListMap> TABLE = new ObjectField<>(PersistentSkipListMap.class);
    private static final IntField COLCOUNT = new IntField();
    private static final ObjectField<PersistentArray> COLNAMES = new ObjectField<>(PersistentArray.class);
    public static final ObjectType<ColumnFamily> TYPE = ObjectType.withFields(ColumnFamily.class, TABLE, COLCOUNT, COLNAMES);

    public ColumnFamily(String[] colNames) {
        super(TYPE);
        Transaction.run(() -> {
            setObjectField(TABLE, new PersistentSkipListMap<Key, Value>());
            setIntField(COLCOUNT, colNames.length);
            PersistentArray<PersistentString> pColNames = new PersistentArray<>(colNames.length);
            for (int i = 0; i < colNames.length; i++) {
                pColNames.set(i, new PersistentString(colNames[i]));
            }
            setObjectField(COLNAMES, pColNames);
        });
    }

    public ColumnFamily(ObjectPointer<ColumnFamily> p) { super(p); }

    @SuppressWarnings("unchecked")
    public PersistentSkipListMap<Key, Value> table() {
        return (PersistentSkipListMap<Key, Value>)getObjectField(TABLE);
    }

    public int colCount() {
        return getIntField(COLCOUNT);
    }

    @SuppressWarnings("unchecked")
    public PersistentArray<PersistentString> colNames() {
        return (PersistentArray<PersistentString>)getObjectField(COLNAMES);
    }

    public void put(Key k, Value v) {
        table().put(k, v);
    }

    public Value get(Key k) {
        return table().get(k);
    }

    public boolean containsKey(Key k) {
        return table().containsKey(k);
    }

    public Value remove(Key k) {
        return table().remove(k);
    }

    public Value putIfAbsent(Key k, Value v) {
        return table().putIfAbsent(k, v);
    }
}
