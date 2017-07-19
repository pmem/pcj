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
import static lib.util.persistent.Trace.*;

// temp 
import lib.xpersistent.XTransaction;

public interface Transaction extends AutoCloseable {
	interface Update {
		public void run();
	}

	enum State {None, Initializing, Active, Committed, Aborted}

    static void run(PersistentMemoryProvider provider, Update update, PersistentObject... toLock) {
        boolean success = false;
        TransactionInfo info = XTransaction.tlInfo.get();
        while (!success && info.attempts <= Config.MAX_TRANSACTION_RETRIES) {
            Transaction t = provider.newTransaction();
            // for stats
            Stats.transactions.total++;
            int currentDepth = info.depth;
            if (currentDepth == 1) Stats.transactions.topLevel++;
            Stats.transactions.maxDepth = Math.max(Stats.transactions.maxDepth, currentDepth);
            // end for stats
            try {
                boolean block = Config.BLOCK_ON_EXCEED_MAX_TRANSACTION_RETRIES && info.attempts == Config.MAX_TRANSACTION_RETRIES;
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
                Stats.transactions.totalRetries++;
                Stats.transactions.updateMaxRetries(info.attempts - 1);
                int sleepTime = info.retryDelay + Util.randomInt(info.retryDelay);
                // trace(true, "old retryDelay = %d, new retryDelay = %d", info.retryDelay, Math.min((int)(info.retryDelay * Config.TRANSACTION_RETRY_DELAY_INCREASE_FACTOR), Config.MAX_TRANSACTION_RETRY_DELAY)); 
                info.retryDelay = Math.min((int)(info.retryDelay * Config.TRANSACTION_RETRY_DELAY_INCREASE_FACTOR), Config.MAX_TRANSACTION_RETRY_DELAY);
                // trace("retry #%d, sleepTime = %d", info.attempts - 1, sleepTime);
                try {Thread.sleep(sleepTime);} catch(InterruptedException ie) {ie.printStackTrace();}
            }
        }
        // trace(true, "after while, depth = %d, success = %s, attempts = %d",  info.depth, success,  info.attempts);
        if (!success) {
            Stats.transactions.failures++;
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

    static void run(Update update, PersistentObject... toLock) {
        // FIXME: Compatible only with the default provider
        run(PersistentMemoryProvider.getDefaultProvider(), update, toLock);
    }

	static Transaction newInstance() {
        // FIXME: Compatible only with the default provider
		return PersistentMemoryProvider.getDefaultProvider().newTransaction();
	}
    static Transaction newInstance(PersistentMemoryProvider provider) {
        return provider.newTransaction();
    }

	Transaction update(Update update);
    Transaction start(boolean block, PersistentObject... toLock);
	void commit();
	void abort();
	State state();

	default void close() {commit();}
}
