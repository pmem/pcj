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
import java.util.ArrayList;
import static lib.util.persistent.Trace.trace;

public class XTransaction implements Transaction {

    public static ThreadLocal<TransactionInfo> tlInfo = ThreadLocal.withInitial(() -> new TransactionInfo());

    static {
        System.loadLibrary("Persistent");
    }

    XTransaction() {
        TransactionInfo info = tlInfo.get();
        info.transaction = this;
        if (info.depth == 0) info.state = Transaction.State.None;
        info.depth++;
    }

    public static void addNewObject(PersistentObject obj) {
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

    public void addLockedObject(PersistentObject obj) {
        TransactionInfo info = tlInfo.get();
        info.locked.add(obj);
    }

    private void releaseLocks() {
        //trace("releaseLocks called, depth = %d, this = %s", depth.get(), this);
        TransactionInfo info = tlInfo.get();
        ArrayList<PersistentObject> toUnlock = info.locked;
        for (int i = toUnlock.size() - 1; i >= 0; i--) {
            PersistentObject obj = toUnlock.get(i);
            obj.monitorExit();
        }
        toUnlock.clear();
    }

    public Transaction start(boolean block, PersistentObject... toLock) {
        TransactionInfo info = tlInfo.get();
        //trace("start transaction, block = %s, depth = %d, this = %s", block, info.depth, this);
        ArrayList<PersistentObject> objs = new ArrayList<>();
        ArrayList<PersistentObject> lockedObjs = new ArrayList<>();
        for (PersistentObject obj : toLock) {
            if (obj != null) objs.add(obj);
        }
        boolean didLock = PersistentObject.monitorEnter(objs, lockedObjs, block);
        if (!didLock && !block) {
            //trace("failed to get transaction locks");
            // assert(lockedObjs.isEmpty());
            throw new TransactionRetryException("Failed to get transaction locks");
        }
        info.locked.addAll(lockedObjs);
        //trace(true, "in start, depth = %d, state = %s", info.depth, info.state);
        if (info.depth == 1 && info.state == Transaction.State.None) {
            info.state = Transaction.State.Active;
            nativeStartTransaction();
        }
        return this;
    }

    public void commit() {
        // trace("commit called, state = %s, depth = %d, this = %s", state.get(), depth.get(), this);
        TransactionInfo info = tlInfo.get();
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
            for (PersistentObject obj : info.constructions) {
                ObjectCache.committedConstruction(obj);
            }
            info.constructions.clear();
            releaseLocks();
        }
        info.depth--;
    }

    public void abort() {
        //trace("abort called, state = %s, depth = %d, this = %s", state.get(), depth.get(), this);
        TransactionInfo info = tlInfo.get();
        if (info.state != Transaction.State.Active) {
            info.state = Transaction.State.Aborted;
            releaseLocks();
            return;
        }
        if (info.depth == 1) {
            nativeAbortTransaction();
            //trace("nativeAbortTransaction called");
            info.constructions.clear();
            //trace("abort: constructions cleared");
            info.state = Transaction.State.Aborted;
            releaseLocks();
        }
    }

    public Transaction.State state() {
        return tlInfo.get().state;
    }

    private native void nativeStartTransaction();
    private native void nativeEndTransaction();
    private native void nativeAbortTransaction();
}
