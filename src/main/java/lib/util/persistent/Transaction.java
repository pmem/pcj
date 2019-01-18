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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Supplier;
import static lib.util.persistent.ObjectCache.Ref;
import static lib.util.persistent.Trace.*;

public final class Transaction {
    private static ThreadLocal<Transaction> threadsTransaction;
    private static ExecutorService outerThreadPool;
    private TransactionCore core;
    private ArrayList<AnyPersistent> locked;
    protected ArrayList<AnyPersistent> constructions;
    protected HashMap<Long, Ref> reconstructions;
    private int timeout;
    private int depth;
    private State state;
    private ArrayList<Runnable> commitHandlers;
    private ArrayList<Runnable> abortHandlers;

    static {
        threadsTransaction = new ThreadLocal<>();
        outerThreadPool = Executors.newCachedThreadPool((Runnable r) -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
    }

	public enum State {None, Initializing, Active, Committed, Aborted}

    private Transaction(TransactionCore core) {
        this.core = core;
        this.locked = new ArrayList<>(20);
        this.constructions = new ArrayList<>(20);
        this.reconstructions = new HashMap<>(20);
        this.timeout = Config.MONITOR_ENTER_TIMEOUT;
        this.depth = 0;
        this.state = Transaction.State.None;
    }

    private void reset() {
        locked.clear();
        constructions.clear();
        timeout = Config.MONITOR_ENTER_TIMEOUT;
        depth = 0;
        state = Transaction.State.None;
    }        

    static Transaction getTransaction() {
        return threadsTransaction.get();
    }

    private static void setTransaction(Transaction transaction) {
        threadsTransaction.set(transaction);
    }

    static <T> T run(PersistentMemoryProvider provider, Supplier<T> body, Runnable onCommit, Runnable onAbort, AnyPersistent toLock1, AnyPersistent toLock2) {
        return run(null, provider, body, onCommit, onAbort, toLock1, toLock2);
    }
        
    static <T> T run(Transaction tx, PersistentMemoryProvider provider, Supplier<T> body, Runnable onCommit, Runnable onAbort, AnyPersistent toLock1, AnyPersistent toLock2) {
        if (Config.ENABLE_TRANSACTION_STATS) Stats.current.transactions.runCalls++;
        T ans = null;
        boolean success = false;
        Transaction transaction = tx != null ? tx : getTransaction();
        if (transaction == null) {
                transaction = new Transaction(provider.newTransaction());
                setTransaction(transaction);
        }            
        int attempts = 1;
        int sleepTime = Config.MONITOR_ENTER_TIMEOUT;
        int retryDelay = Config.BASE_TRANSACTION_RETRY_DELAY;
        while (!success && attempts <= Config.MAX_TRANSACTION_ATTEMPTS) {
            if (transaction.state != Transaction.State.Active) {
                transaction.reset();
                if (Config.ENABLE_TRANSACTION_STATS) Stats.current.transactions.topLevel++;
            }
            transaction.depth++;
            if (Config.ENABLE_TRANSACTION_STATS) {
                int currentDepth = transaction.depth;
                Stats.current.transactions.total++;
                Stats.current.transactions.maxDepth = Math.max(Stats.current.transactions.maxDepth, currentDepth);
            }
            try {
                boolean block = Config.BLOCK_ON_MAX_TRANSACTION_ATTEMPTS && attempts == Config.MAX_TRANSACTION_ATTEMPTS;
                // trace(true, "%s about to call start, attempts = %d, depth = %d, block = %s", t, attempts, transaction.depth, block);
                if (onCommit != null) transaction.addCommitHandler(onCommit);
                if (onAbort != null) transaction.addAbortHandler(onAbort);
                transaction.start(block, toLock1, toLock2);
                ans = body.get();
                success = true;
            }
            catch (Throwable e) {
                transaction.abort(new TransactionException(e));
                success = false;
                // trace(true, "%s Transaction.run() caught %s, depth = %d", t,  e, transaction.depth);
                if (e instanceof PersistenceException) {
                    e.printStackTrace();
                    System.out.println("A fatal error has occurred, unable to continue, exiting: " + e);
                    // System.exit(-1);
                }
                if (transaction.depth > 1 || !(e instanceof TransactionRetryException)) throw e; // unwind stack or not a retry-able exception
                // retry
            }
            finally {
                transaction.commit();
            }
            if (!success) {
                attempts++;
                if (Config.ENABLE_TRANSACTION_STATS) {
                    Stats.current.transactions.totalRetries++;
                    Stats.current.transactions.updateMaxRetries(attempts - 1);
                }
                sleepTime = retryDelay + Util.randomInt(retryDelay);
                retryDelay = Math.min((int)(retryDelay * Config.TRANSACTION_RETRY_DELAY_INCREASE_FACTOR), Config.MAX_TRANSACTION_RETRY_DELAY);
                // trace(true, "retry #%d, sleepTime = %d", attempts - 1, sleepTime);
                try {Thread.sleep(sleepTime);} catch(InterruptedException ie) {ie.printStackTrace();}
            }
        }
        if (!success) {
            if (Config.ENABLE_TRANSACTION_STATS) Stats.current.transactions.failures++;
            trace(true, "failed transaction");
            RuntimeException e = new TransactionException(String.format("failed to execute transaction after %d attempts", attempts));
            if (Config.EXIT_ON_TRANSACTION_FAILURE) {
                e.printStackTrace();
                Stats.printStats();
                System.exit(-1);
            }
            throw e;
        }
        return ans;
    }

    public static void run(PersistentMemoryProvider provider, Runnable body) {
        run(provider, () -> {body.run(); return (Void)null;}, null, null, null, null);
    }

    public static void run(Runnable body) {
        run(PersistentMemoryProvider.getDefaultProvider(), () -> {body.run(); return (Void)null;}, null, null, null, null);
    }

    public static void run(Runnable body, AnyPersistent toLock) {
        run(PersistentMemoryProvider.getDefaultProvider(), () -> {body.run(); return (Void)null;}, null, null, toLock, null);
    }

    static void run(Transaction transaction, Runnable body, AnyPersistent toLock) {
        run(transaction, PersistentMemoryProvider.getDefaultProvider(), () -> {body.run(); return (Void)null;}, null, null, toLock, null);
    }

    public static void run(Runnable body, AnyPersistent toLock1, AnyPersistent toLock2) {
        run(PersistentMemoryProvider.getDefaultProvider(), () -> {body.run(); return (Void)null;}, null, null, toLock1, toLock2);
    }

    public static void run(Runnable body, Runnable onCommit) {
        run(PersistentMemoryProvider.getDefaultProvider(), () -> {body.run(); return (Void)null;}, onCommit, null, null, null);
    }

    public static void run(Runnable body, Runnable onCommit, Runnable onAbort) {
        run(PersistentMemoryProvider.getDefaultProvider(), () -> {body.run(); return (Void)null;}, onCommit, onAbort, null, null);
    }

    public static void run(Runnable body, Runnable onCommit, Runnable onAbort, AnyPersistent toLock1) {
        run(PersistentMemoryProvider.getDefaultProvider(), () -> {body.run(); return (Void)null;}, onCommit, onAbort, toLock1, null);
    }

    public static <T> T run(Supplier<T> body) {
        return run(PersistentMemoryProvider.getDefaultProvider(), body, null, null, null, null);
    }

    public static <T> T run(Supplier<T> body, AnyPersistent toLock) {
        return run(PersistentMemoryProvider.getDefaultProvider(), body, null, null, toLock, null);
    }

    public static <T> T run(Supplier<T> body, Runnable onCommit, Runnable onAbort, AnyPersistent toLock) {
        return run(PersistentMemoryProvider.getDefaultProvider(), body, onCommit, onAbort, toLock, null);
    }

    public static void runOuter(Runnable body) {
        Box<Throwable> errorBox = new Box<>();
        Future<?> outer = outerThreadPool.submit(() -> {
            try {Transaction.run(body);}
            catch (Throwable error) {errorBox.set(error);}
        });

        try {outer.get(); }
        catch (InterruptedException ie) {throw new RuntimeException(ie);}
        catch (ExecutionException ee) {throw new RuntimeException(ee);}
        Throwable error = errorBox.get();
        if (error != null) throw new RuntimeException(error);
    }

    public Transaction.State getState() {
        return state;
    }

    public static Transaction getActiveTransaction() {
        Transaction tx = getTransaction();
        return tx != null && tx.isActive() ? tx : null;
    }

    public boolean isActive() {
        return state == State.Active;
    }

    public static boolean isTransactionActive() {
        return getActiveTransaction() != null;
    }

/*    static <U extends AnyPersistent> Ref<U> addReconstructedObject(U obj) {
        ObjectCache.uncommittedConstruction(obj);
        Ref<U> ref = new Ref<>(obj);
        getTransaction().reconstructions.put(obj.getPointer().addr(), ref);
        return ref;
    }*/

    static <U extends AnyPersistent> boolean addReconstructedObject(Long address, Ref<U> ref) {
        boolean ret = false;
        Transaction tx = getTransaction();
        if (tx != null && tx.isActive()) {
            tx.reconstructions.put(address, ref);
            ObjectCache.uncommittedConstruction(ref.get());
            ret = true;
        }
        return ret;
    }

    static <U extends AnyPersistent> boolean addNewObject(AnyPersistent obj, Ref<U> ref) {
        boolean ret = false;
        Transaction tx = getTransaction();
        if (tx != null && tx.isActive()) {
            tx.constructions.add(obj);
            tx.reconstructions.put(obj.addr(), ref);
            ObjectCache.uncommittedConstruction(obj);
            ret = true;
        }
        return ret;
    }

    static Ref getReconstructedObject(Long address) {
        Transaction tx = getTransaction();
        return tx == null ? null : tx.reconstructions.get(address);
    }

    static void removeFromReconstructions(Long address) {
        Transaction tx = getTransaction();
        if(tx !=null) tx.reconstructions.remove(address);
    }

    void addLockedObject(AnyPersistent obj) {
        locked.add(obj);
    }

    private void acquireLock(boolean block, AnyPersistent obj) {
        if (!block) {
            if (!obj.tryLock(this)) throw new TransactionRetryException("failed to get transaction locks");
        }
        else obj.lock();
        locked.add(obj);
    }

    private void releaseLocks() {
        for (int i = locked.size() - 1; i >= 0; i--) {
            AnyPersistent obj = locked.get(i);
            obj.unlock();
        }
        locked.clear();
    }

    int timeout() {return timeout;}

    void timeout(int timeout) {this.timeout = timeout;}

    private List<Runnable> commitHandlers() {return commitHandlers;}

    private void addCommitHandler(Runnable r) {
        if (commitHandlers == null) commitHandlers = new ArrayList<>();
        commitHandlers.add(r);
    }

    private void clearCommitHandlers() {if (commitHandlers != null) commitHandlers.clear();}

    private List<Runnable> abortHandlers() {return abortHandlers;}

    private void addAbortHandler(Runnable r) {
        if (abortHandlers == null) abortHandlers = new ArrayList<>();
        abortHandlers.add(r);
    }

    private void clearAbortHandlers() {if (abortHandlers != null) abortHandlers.clear();}

    private void start(boolean block, AnyPersistent toLock1, AnyPersistent toLock2) {
        if (toLock1 != null) acquireLock(block, toLock1);
        if (toLock2 != null) acquireLock(block, toLock2);
        if (depth == 1 && state == Transaction.State.None) {
            state = Transaction.State.Active;
            core.start();
        }
    }

    @SuppressWarnings("unchecked")
    private <U extends AnyPersistent> void commit() {
        if (depth == 1) {
            if (state == Transaction.State.None) {
                return;
            }
            if (state == Transaction.State.Aborted) {
                depth--;
                return;
            }
            core.commit();
            state = Transaction.State.Committed;
            /*for (AnyPersistent obj : constructions) {
                ObjectCache.committedConstruction(obj);
                ObjectCache.add(obj);
            }*/
            for (Ref<U> ref : reconstructions.values()) {
                ObjectCache.committedConstruction(ref.get());
                ObjectCache.add(ref.getAddress(), ref);
            }
            constructions.clear();
            reconstructions.clear();
            releaseLocks();
        }
        depth--;
        if (depth == 0) {
            clearAbortHandlers();
            if (commitHandlers != null) {
                if (commitHandlers.size() > 1) Collections.reverse(commitHandlers);
                Runnable[] handlers = commitHandlers.toArray(new Runnable[0]);
                commitHandlers.clear();
                for (Runnable r : handlers) r.run();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <U extends AnyPersistent> void abort(TransactionException e) {
        if (state != Transaction.State.Active) {
            state = Transaction.State.Aborted;
            releaseLocks();
            return;
        }
        if (depth == 1) {
            core.abort(e);
            for (AnyPersistent obj : constructions) {
                // TODO: remove dependency on xpersistent package
                ((lib.xpersistent.UncheckedPersistentMemoryRegion)obj.region()).clear();
            }
            for (Ref<U> ref : reconstructions.values()) {
                ref.clear();
            }
            constructions.clear();
            reconstructions.clear();
            state = Transaction.State.Aborted;
            releaseLocks();
            clearCommitHandlers();
            if (abortHandlers != null) {
                if (abortHandlers.size() > 1) Collections.reverse(abortHandlers);
                Runnable[] handlers = abortHandlers.toArray(new Runnable[0]);
                abortHandlers.clear();
                for (Runnable r : handlers) r.run();
            }
        }
    }
}
