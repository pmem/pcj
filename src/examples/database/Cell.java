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

public class Cell extends PersistentTuple3<PersistentString, PersistentByteBuffer, PersistentLong> {
    public Cell(PersistentString colName, String val, long ts) {
        Transaction.run(() -> {
            _1(colName);
            _2(PersistentByteBuffer.allocate(val.length()).put(val.getBytes()).rewind());
            _3(new PersistentLong(ts));
        });
    }

    public PersistentString colName() {
        return _1();
    }

    public PersistentByteBuffer val() {
        return _2();
    }

    public PersistentLong ts() {
        return _3();
    }

    public void colName(PersistentString colName) {
        _1(colName);
    }

    public void val(PersistentByteBuffer val) {
        _2(val);
    }

    public void ts(PersistentLong ts) {
        _3(ts);
    }
}
