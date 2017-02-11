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

import lib.persistent.Util;
import lib.persistent.Transaction;
import lib.persistent.TransactionError;
import lib.persistent.spi.PersistentMemoryProvider;

public class XTransaction implements Transaction {

    static {
        System.loadLibrary("Persistent");
        Util.openPool();
    }

    private static ThreadLocal<Transaction.State> state = null;
    private static ThreadLocal<Integer> depth = null;

    XTransaction() {
        if (state == null) {
            state = new ThreadLocal<>();
        }
        if (depth == null) {
            depth = new ThreadLocal<>();
            depth.set(0);
        }
        if (depth.get() == 0)
            state.set(Transaction.State.None);
        depth.set(depth.get() + 1);
    }

    public Transaction update(Transaction.Update update) {
        if (state.get() != Transaction.State.Active) throw new TransactionError("In update: transaction not active");
        update.run();
        // System.out.println("ran update " + update);
        return this;
    }

    public Transaction start() {
        if (depth.get() == 1 && state.get() == Transaction.State.None) {
            state.set(Transaction.State.Active);
            nativeStartTransaction();
        }
        return this;
    }

    public void commit() {
        if (depth.get() == 1) {
            if (state.get() == Transaction.State.None)
                throw new TransactionError("In commit: transaction not active");
            if (state.get() == Transaction.State.Aborted) {
                depth.set(depth.get() - 1);
                return;
            }
            nativeEndTransaction();
            state.set(Transaction.State.Committed);
        }
        depth.set(depth.get() - 1);
    }

    public void abort() {
        if (state.get() != Transaction.State.Active) throw new TransactionError("In abort: transaction not active");
        if (depth.get() == 1) {
            nativeAbortTransaction();
            state.set(Transaction.State.Aborted);
        }
    }

    public Transaction.State state() {
        return state.get();
    }

    private native void nativeStartTransaction();
    private native void nativeEndTransaction();
    private native void nativeAbortTransaction();
}
