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
import static lib.util.persistent.Trace.*;

public final class Transaction {
    private static ThreadLocal<Transaction> threadsTransaction;
    private static ExecutorService outerThreadPool; 
    private TransactionCore core;
    private ArrayList<AnyPersistent> locked;
    private ArrayList<AnyPersistent> constructions;
    private static int attempts;
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
        this.locked = new ArrayList<>();
        this.constructions = new ArrayList<>();
        this.timeout = Config.MONITOR_ENTER_TIMEOUT;
        this.depth = 0;
        this.state = Transaction.State.None;    
    }

    static Transaction getTransaction() {
        return threadsTransaction.get();
    }

    private static void setTransaction(Transaction transaction) {
        threadsTransaction.set(transaction);
    }

    static void run(PersistentMemoryProvider provider, Runnable update, Runnable onCommit, Runnable onAbort, AnyPersistent toLock1, AnyPersistent toLock2) {
        Stats.current.transactions.runCalls++;
        boolean success = false;
        Transaction transaction = getTransaction();
        if (transaction != null && transaction.state == Transaction.State.Active) {
            if (toLock1 != null) transaction.aquireLock(false, toLock1);
            if (toLock2 != null) transaction.aquireLock(false, toLock2);
            if (onCommit != null) transaction.addCommitHandler(onCommit);
            if (onAbort != null) transaction.addAbortHandler(onAbort);    
            update.run();
        }
        else {
            int attempts = 1;
            int sleepTime = Config.MONITOR_ENTER_TIMEOUT;
            int retryDelay = Config.BASE_TRANSACTION_RETRY_DELAY; 
            while (!success && attempts <= Config.MAX_TRANSACTION_ATTEMPTS) {
                transaction = new Transaction(provider.newTransaction());
                setTransaction(transaction);
                if (transaction.depth == 0) transaction.state = Transaction.State.None;
                transaction.depth++;
                // for stats
                int currentDepth = transaction.depth;
                Stats.current.transactions.total++;
                Stats.current.transactions.topLevel++;
                Stats.current.transactions.maxDepth = Math.max(Stats.current.transactions.maxDepth, currentDepth);
                // end for stats
                try {
                    boolean block = Config.BLOCK_ON_MAX_TRANSACTION_ATTEMPTS && attempts == Config.MAX_TRANSACTION_ATTEMPTS;
                    // trace(true, "%s about to call start, attempts = %d, depth = %d, block = %s", t, attempts, transaction.depth, block);
                    if (onCommit != null) transaction.addCommitHandler(onCommit);
                    if (onAbort != null) transaction.addAbortHandler(onAbort);    
                    transaction.start(block, toLock1, toLock2);
                    transaction.update(update);
                    success = true;
                }
                catch (Throwable e) {
                    success = false;
                    // trace(true, "%s Transaction.run() caught %s, depth = %d", t,  e, transaction.depth);
                    if (e instanceof PersistenceException) {
                        e.printStackTrace();
                        System.out.println("A fatal error has occurred, unable to continue, exiting: " + e);
                        System.exit(-1);
                    }
                    transaction.abort();
                    if (transaction.depth > 1 || !(e instanceof TransactionRetryException)) throw e; // unwind stack or not a retry-able exception
                    // retry
                }
                finally {
                    transaction.commit();
                }
                if (!success) {
                    attempts++;
                    Stats.current.transactions.totalRetries++;
                    Stats.current.transactions.updateMaxRetries(attempts - 1);
                    sleepTime = retryDelay + Util.randomInt(retryDelay);
                    retryDelay = Math.min((int)(retryDelay * Config.TRANSACTION_RETRY_DELAY_INCREASE_FACTOR), Config.MAX_TRANSACTION_RETRY_DELAY);
                    // trace(true, "retry #%d, sleepTime = %d", attempts - 1, sleepTime);
                    try {Thread.sleep(sleepTime);} catch(InterruptedException ie) {ie.printStackTrace();}
                }
            }
            if (!success) {
                Stats.current.transactions.failures++;
                trace(true, "failed transaction");
                RuntimeException e = new TransactionException(String.format("failed to execute transaction after %d attempts", attempts));
                if (Config.EXIT_ON_TRANSACTION_FAILURE) {
                    e.printStackTrace();
                    Stats.printStats();
                    System.exit(-1);
                }
                throw e;
            }
            else {
                if (transaction.depth == 1) {
                    attempts = 1;
                }
            }
        }
    }

    public static void run(PersistentMemoryProvider provider, Runnable update) {
        run(provider, update, null, null, null, null);
    }

    public static void run(Runnable update) {
        run(PersistentMemoryProvider.getDefaultProvider(), update, null, null, null, null);
    }

    public static void run(Runnable update, AnyPersistent toLock) {
        run(PersistentMemoryProvider.getDefaultProvider(), update, null, null, toLock, null);
    }

    public static void run(Runnable update, AnyPersistent toLock1, AnyPersistent toLock2) {
        run(PersistentMemoryProvider.getDefaultProvider(), update, null, null, toLock1, toLock2);
    }

    // public static void run(Runnable update, AnyPersistent... toLock) {
    //     System.out.println("DING DING");
    //     run(PersistentMemoryProvider.getDefaultProvider(), update, null, null, toLock[0], toLock[1]);
    // }

    public static void run(Runnable update, Runnable onCommit) {
        run(PersistentMemoryProvider.getDefaultProvider(), update, onCommit, null, null, null);
    }

    public static void run(Runnable update, Runnable onCommit, Runnable onAbort) {
        run(PersistentMemoryProvider.getDefaultProvider(), update, onCommit, onAbort, null, null);
    }

    public static void runOuter(Runnable update) {
        Box<Throwable> errorBox = new Box<>();
        Future<?> outer = outerThreadPool.submit(() -> {
            try {Transaction.run(update);}
            catch (Throwable error) { errorBox.set(error);}
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

    public boolean isActive() {
        return state == State.Active;
    }

    static void addNewObject(AnyPersistent obj) {
        getTransaction().constructions.add(obj);
    }

    void addLockedObject(AnyPersistent obj) {
        locked.add(obj);
    }

    private void aquireLock(boolean block, AnyPersistent obj) {
        if (!block) {
            if (!obj.monitorEnterTimeout()) throw new TransactionRetryException("failed to get transaction locks");
        }
        else obj.monitorEnter();
        locked.add(obj);
    }        

    private void releaseLocks() {
        for (int i = locked.size() - 1; i >= 0; i--) {
            AnyPersistent obj = locked.get(i);
            obj.monitorExit();
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

    private void update(Runnable update) {
        update.run();
    }

    private void start(boolean block, AnyPersistent toLock1, AnyPersistent toLock2) {
        if (toLock1 != null) aquireLock(block, toLock1);
        if (toLock2 != null) aquireLock(block, toLock2);
        if (depth == 1 && state == Transaction.State.None) {
            state = Transaction.State.Active;
            core.start();
        }
    }

    private void commit() {
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
            for (AnyPersistent obj : constructions) {
                ObjectCache.committedConstruction(obj);
            }
            constructions.clear();
            releaseLocks();
        }
        depth--;
        if (depth == 0) {
            clearAbortHandlers();
            if (commitHandlers != null) {
                Collections.reverse(commitHandlers);
                Runnable[] handlers = commitHandlers.toArray(new Runnable[0]);
                commitHandlers.clear();
                for (Runnable r : handlers) r.run();
            }
        }
    }

    private void abort() {
        if (state != Transaction.State.Active) {
            state = Transaction.State.Aborted;
            releaseLocks();
            return;
        }
        if (depth == 1) {
            core.abort();
            constructions.clear();
            state = Transaction.State.Aborted;
            releaseLocks();
            if (abortHandlers != null) {
                Collections.reverse(abortHandlers);
                Runnable[] handlers = abortHandlers.toArray(new Runnable[0]);
                abortHandlers.clear();
                for (Runnable r : handlers) r.run();
            }
        }
    }
}
