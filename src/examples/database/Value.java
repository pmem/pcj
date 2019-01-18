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
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class Value extends PersistentObject {
    private static final ObjectField<PersistentArray> CELLS = new ObjectField<>(PersistentArray.class);
    public static final ObjectType<Value> TYPE = ObjectType.withFields(Value.class, CELLS);

    public Value(int size) {
        super(TYPE);
        Transaction.run(() -> {
            setObjectField(CELLS, new PersistentArray<Cell>(size));
        });
    }

    public Value(ObjectPointer<? extends Value> p) { super(p); }

    public Value(ObjectType<? extends Value> type) { super(type); }

    @SuppressWarnings("unchecked")
    private PersistentArray<Cell> cells() {
        return getObjectField(CELLS);
    }

    public void set(int index, Cell c) {
        cells().set(index, c);
    }

    public Cell get(int index) {
        return (Cell)(cells().get(index));
    }

    @Override
    public String toString() {
        int iMax = cells().length() - 1;
        if (iMax == -1) return "[]";

        StringBuilder b = new StringBuilder();
//        b.append('[');
        for (int i = 0; ; i++) {
            if (cells().get(i) != null)
                b.append(((Cell)(cells().get(i))).val().toString());
            else
                b.append("NULL");
            if (i == iMax)
                return b.toString();
 //               return b.append(']').toString();
            b.append(", ");
        }
    }

}
