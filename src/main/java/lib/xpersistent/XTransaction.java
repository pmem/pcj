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

import lib.util.persistent.*;
import lib.util.persistent.spi.PersistentMemoryProvider;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import static lib.util.persistent.Trace.trace;

public class XTransaction implements Transaction {

    public static ThreadLocal<TransactionInfo> tlInfo = ThreadLocal.withInitial(() -> new TransactionInfo());

    static {
        System.loadLibrary("Persistent");
        lib.util.persistent.spi.PersistentMemoryProvider.getDefaultProvider().getHeap().open();
    }

    XTransaction() {
        TransactionInfo info = tlInfo.get();
        info.transaction = this;
        if (info.depth == 0) info.state = Transaction.State.None;
        info.depth++;
    }

    public static void addNewObject(AnyPersistent obj) {
        // trace(obj.getPointer().addr(), "addNewObject called");
        tlInfo.get().constructions.add(obj);
    }

    public Transaction update(Transaction.Update update) {
        if (tlInfo.get().state != Transaction.State.Active) {
            throw new TransactionError("In update: transaction not active");
        }
        update.run();
        return this;
    }

    public void addLockedObject(AnyPersistent obj) {
        TransactionInfo info = tlInfo.get();
        info.locked.add(obj);
    }

    private void releaseLocks() {
        // trace("releaseLocks called, depth = %d, this = %s", depth.get(), this);
        TransactionInfo info = tlInfo.get();
        ArrayList<AnyPersistent> toUnlock = info.locked;
        for (int i = toUnlock.size() - 1; i >= 0; i--) {
            AnyPersistent obj = toUnlock.get(i);
            obj.monitorExit();
        }
        toUnlock.clear();
    }

    public Transaction start(boolean block, AnyPersistent... toLock) {
        TransactionInfo info = tlInfo.get();
        for (AnyPersistent obj : toLock) {
            if (obj == null) continue;
            if (!block) {
                if (!obj.monitorEnterTimeout()) throw new TransactionRetryException("failed to get transaction locks");
            }
            else obj.monitorEnter();
            info.locked.add(obj);
        }
        if (info.depth == 1 && info.state == Transaction.State.None) {
            info.state = Transaction.State.Active;
            nativeStartTransaction();
        }
        return this;
    }

    public void commit() {
        TransactionInfo info = tlInfo.get();
        // trace("commit called, state = %s, depth = %d, this = %s", info.state, info.depth, this);
        if (info.depth == 1) {
            if (info.state == Transaction.State.None) {
                return;
            }
            if (info.state == Transaction.State.Aborted) {
                info.depth--;
                return;
            }
            nativeEndTransaction();
            info.state = Transaction.State.Committed;
            for (AnyPersistent obj : info.constructions) {
                ObjectCache.committedConstruction(obj);
            }
            info.constructions.clear();
            releaseLocks();
        }
        info.depth--;
        if (info.depth == 0) {
            List<Runnable> commitHandlers = info.commitHandlers();
            if (commitHandlers != null) {
                Collections.reverse(commitHandlers);
                Runnable[] handlers = commitHandlers.toArray(new Runnable[0]);
                commitHandlers.clear();
                for (Runnable r : handlers) r.run();
            }
        }
    }

    public void abort() {
        // trace("abort called, state = %s, depth = %d, this = %s", state.get(), depth.get(), this);
        TransactionInfo info = tlInfo.get();
        if (info.state != Transaction.State.Active) {
            info.state = Transaction.State.Aborted;
            releaseLocks();
            return;
        }
        if (info.depth == 1) {
            nativeAbortTransaction();
            // trace("nativeAbortTransaction called");
            info.constructions.clear();
            // trace("abort: constructions cleared");
            info.state = Transaction.State.Aborted;
            releaseLocks();
            List<Runnable> abortHandlers = info.abortHandlers();
            if (abortHandlers != null) {
                Collections.reverse(abortHandlers);
                Runnable[] handlers = abortHandlers.toArray(new Runnable[0]);
                abortHandlers.clear();
                for (Runnable r : handlers) r.run();
            }
        }
    }

    public Transaction.State state() {
        return tlInfo.get().state;
    }

    private native void nativeStartTransaction();
    private native void nativeEndTransaction();
    private native void nativeAbortTransaction();
}
