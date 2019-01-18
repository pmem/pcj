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

package lib.xpersistent;

import lib.util.persistent.TransactionCore;
import lib.util.persistent.spi.PersistentMemoryProvider;
import lib.util.persistent.PersistentHeap;

public class XPersistentMemory extends PersistentMemoryProvider {
    private PersistentHeap heap;

    public XPersistentMemory() {}

    @Override
    public String getName() {
        return "XPersistentMemory";
    }

    @Override
    public TransactionCore newTransaction() {
        return new XTransaction();
    }

    @Override
    public synchronized PersistentHeap getHeap() {
        if (this.heap == null) {
            this.heap = new XHeap();
        }
        return heap;
    }
}
