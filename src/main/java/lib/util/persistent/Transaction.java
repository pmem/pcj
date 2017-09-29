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
import static lib.util.persistent.Trace.*;

// temp 
import lib.xpersistent.XTransaction;

public interface Transaction extends AutoCloseable {
	public interface Update {
		public void run();
	}
    ExecutorService outerThreadPool = Executors.newCachedThreadPool((Runnable r) -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

	public enum State {None, Initializing, Active, Committed, Aborted}

    public static void run(PersistentMemoryProvider provider, Update update, AnyPersistent... toLock) {
        boolean success = false;
        TransactionInfo info = XTransaction.tlInfo.get();
        while (!success && info.attempts <= Config.MAX_TRANSACTION_ATTEMPTS) {
            Transaction t = provider.newTransaction();
            // for stats
            Stats.current.transactions.total++;
            int currentDepth = info.depth;
            if (currentDepth == 1) Stats.current.transactions.topLevel++;
            Stats.current.transactions.maxDepth = Math.max(Stats.current.transactions.maxDepth, currentDepth);
            // end for stats
            try {
                boolean block = Config.BLOCK_ON_MAX_TRANSACTION_ATTEMPTS && info.attempts == Config.MAX_TRANSACTION_ATTEMPTS;
                // trace(true, "%s about to call start, attempts = %d, depth = %d, block = %s", t, info.attempts, info.depth, block);
                t.start(block, toLock);
                t.update(update);
                success = true;
            }
            catch (Throwable e) {
                success = false;
                // trace(true, "%s Transaction.run() caught %s, depth = %d", t,  e, info.depth);
                t.abort();
                // trace(true, "called abort on %s", t);
                if (info.depth > 1) throw e; // unwind stack
                else if (!(e instanceof TransactionRetryException)) throw e; // not a retry-able exception
                // retry
            } 
            finally {
                t.commit();
            }
            if (!success) {
                info.attempts++;     
                Stats.current.transactions.totalRetries++;
                Stats.current.transactions.updateMaxRetries(info.attempts - 1);
                int sleepTime = info.retryDelay + Util.randomInt(info.retryDelay);
                // trace(true, "old retryDelay = %d, new retryDelay = %d", info.retryDelay, Math.min((int)(info.retryDelay * Config.TRANSACTION_RETRY_DELAY_INCREASE_FACTOR), Config.MAX_TRANSACTION_RETRY_DELAY)); 
                info.retryDelay = Math.min((int)(info.retryDelay * Config.TRANSACTION_RETRY_DELAY_INCREASE_FACTOR), Config.MAX_TRANSACTION_RETRY_DELAY);
                // trace("retry #%d, sleepTime = %d", info.attempts - 1, sleepTime);
                try {Thread.sleep(sleepTime);} catch(InterruptedException ie) {ie.printStackTrace();}
            }
        }
        // trace(true, "after while, depth = %d, success = %s, attempts = %d",  info.depth, success,  info.attempts);
        if (!success) {
            Stats.current.transactions.failures++;
            trace(true, "failed transaction");
            RuntimeException e = new TransactionException(String.format("failed to execute transaction after %d attempts", info.attempts));            
            if (Config.EXIT_ON_TRANSACTION_FAILURE) {
                e.printStackTrace();
                Stats.printStats();
                System.exit(-1);
            }
            throw e;
        }
        else {
            // trace(true, "transaction success, attempts = %d, depth = %d",info.attempts, info.depth);
            if (info.depth == 1) {
                info.attempts = 1;
                info.retryDelay = Config.BASE_TRANSACTION_RETRY_DELAY;
            }
        }
    }

    public static void run(Update update, AnyPersistent... toLock) {
        run(PersistentMemoryProvider.getDefaultProvider(), update, toLock);
    }

    public static void runOuter(Update update, PersistentObject... toLock) {
        Box<Throwable> errorBox = new Box<>();

        Future<?> outer = outerThreadPool.submit(() -> {
            XTransaction.tlInfo.get().init();
            try {Transaction.run(update, toLock);}
            catch (Throwable error) {errorBox.set(error);}
        });

        try { outer.get();} 
        catch (InterruptedException ie) {throw new RuntimeException(ie);}
        catch (ExecutionException ee) {throw new RuntimeException(ee);}
        Throwable error = errorBox.get();
        if (error != null) throw new RuntimeException(error);
    }

	public static Transaction newInstance() {
		return PersistentMemoryProvider.getDefaultProvider().newTransaction();
	}

	public Transaction update(Update update);
    public Transaction start(boolean block, AnyPersistent... toLock);
	public void commit();
	public void abort();
	public State state();

	default void close() {commit();}
}
