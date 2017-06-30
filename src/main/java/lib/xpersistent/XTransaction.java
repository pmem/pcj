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

    private static ThreadLocal<Transaction.State> state = new ThreadLocal<>();
    public static ThreadLocal<Integer> depth = new ThreadLocal<>();
    private static ThreadLocal<ArrayList<PersistentObject>> locked = new ThreadLocal<>();
    private static ThreadLocal<ArrayList<PersistentObject>> constructions = new ThreadLocal<>();

    static {
        System.loadLibrary("Persistent");
    }

    XTransaction() {
        if (state.get() == null) {
            state.set(Transaction.State.None);
        }
        if (depth.get() == null) {
            depth.set(0);
        }
        if (depth.get() == 0) {
            state.set(Transaction.State.None);
        }
        if (locked.get() == null) {
            locked.set(new ArrayList<PersistentObject>());
        }
        if (constructions.get() == null) {
            constructions.set(new ArrayList<PersistentObject>());
        }
        depth.set(depth.get() + 1);
    }

    public static void addNewObject(PersistentObject obj) {
        // trace(obj.getPointer().addr(), "addNewObject called");
        constructions.get().add(obj);
    }

    public Transaction update(Transaction.Update update) {
        if (state.get() != Transaction.State.Active) {
            throw new TransactionError("In update: transaction not active");
        }
        update.run();
        return this;
    }

    private void releaseLocks() {
        // trace("releaseLocks called, depth = %d, this = %s", depth.get(), this);
        ArrayList<PersistentObject> toUnlock = locked.get();
        for (int i = toUnlock.size() - 1; i >= 0; i--) {
            PersistentObject obj = toUnlock.get(i);
            obj.monitorExit();
        }
        locked.get().clear();
    }

    public Transaction start(PersistentObject... toLock) {
        // trace("start transaction, depth = %d, this = %s", depth.get(), this);
        ArrayList<PersistentObject> objs = new ArrayList<>();
        ArrayList<PersistentObject> lockedObjs = new ArrayList<>();
        for (PersistentObject obj : toLock) {
            if (obj != null) objs.add(obj); 
        }
        boolean didLock = PersistentObject.monitorEnter(objs, lockedObjs);
        if (!didLock) {
            // trace("failed to get transaction locks");
            // assert(lockedObjs.isEmpty());
            throw new TransactionRetryException("failed to get transaction locks");
        }
        locked.get().addAll(lockedObjs);
        if (depth.get() == 1 && state.get() == Transaction.State.None) {
            state.set(Transaction.State.Active);
            nativeStartTransaction();
        }
        return this;
    }

    public void commit() {
        // trace("commit called, state = %s, depth = %d, this = %s", state.get(), depth.get(), this);
        if (depth.get() == 1) {
            if (state.get() == Transaction.State.None) {
                return;
            }
            if (state.get() == Transaction.State.Aborted) {
                depth.set(depth.get() - 1);
                return;
            }
            nativeEndTransaction();
            state.set(Transaction.State.Committed);
            for (PersistentObject obj : constructions.get()) {
                ObjectCache.committedConstruction(obj);
            }
            constructions.get().clear();
            releaseLocks();
        }
        depth.set(depth.get() - 1);
    }

    public void abort() {
        // trace("abort called, state = %s, depth = %d, this = %s", state.get(), depth.get(), this);
        if (state.get() != Transaction.State.Active) {
            state.set(Transaction.State.Aborted);
            releaseLocks();
            return;
        }
        if (depth.get() == 1) {
            // remove locked objects from ObjectCache
            ArrayList<PersistentObject> toUnlock = locked.get();
            for (int i = toUnlock.size() - 1; i >= 0; i--) {
                ObjectCache.remove(toUnlock.get(i).getPointer().addr());
            }
            nativeAbortTransaction();
            // trace("nativeAbortTransaction called");
            constructions.get().clear();
            // trace("abort: constructions cleared");
            state.set(Transaction.State.Aborted);
            releaseLocks();
        }
    }

    public Transaction.State state() {
        return state.get();
    }

    private native void nativeStartTransaction();
    private native void nativeEndTransaction();
    private native void nativeAbortTransaction();
}
