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
        int attempts = 1;
        int MAX_ATTEMPTS = 25;

        while (!success && attempts <= MAX_ATTEMPTS) {
            Transaction t = provider.newTransaction();
            // for stats
            // Stats.transactions.total++;
            // int currentDepth = XTransaction.depth.get();
            // if (currentDepth == 1) Stats.transactions.topLevel++;
            // Stats.transactions.maxDepth = Math.max(Stats.transactions.maxDepth, currentDepth);
            // end for stats
            try {
                t.start(toLock);
                t.update(update);
                success = true;
            }
            catch (Throwable e) {
                trace("Transaction.run() caught %s, depth = %d",  e, XTransaction.depth.get());
                t.abort();
                trace("called abort on %s", t);
                if (XTransaction.depth.get() != 1) throw e; // unwind stack
                else if (!(e instanceof TransactionRetryException)) throw e; // not a retry-able exception
                // retry
            } 
            finally {
                t.commit();
            }
            attempts++;
            if (!success) {
                // Stats.transactions.retries++;
                trace(true, "retry #%d", attempts - 1);
                try {Thread.sleep(attempts);} catch(InterruptedException ie) {ie.printStackTrace();}
            }
        }
        if (!success) {
            throw new TransactionException(String.format("failed to execute transaction after %d attempts", attempts));
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
    Transaction start(PersistentObject... toLock);
	void commit();
	void abort();
	State state();

	default void close() {commit();}
}
