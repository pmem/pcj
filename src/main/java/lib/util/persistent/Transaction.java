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

public interface Transaction extends AutoCloseable {
	public interface Update {
		public void run();
	}

	public enum State {None, Active, Committed, Aborted}

    public static void run(PersistentMemoryProvider provider, Update update) {
        Transaction t = provider.newTransaction();
        try {
            t.start();
            t.update(update);
        } catch (Throwable e) {
            //e.printStackTrace();
            t.abort();
            throw e;
        } finally {
            t.commit();
        }
    }

	public static void run(Update update) {
		run(PersistentMemoryProvider.getDefaultProvider(), update);
	}

	public static Transaction newInstance() {
		return PersistentMemoryProvider.getDefaultProvider().newTransaction();
	}

	public Transaction update(Update update);
    public Transaction start();
	public void commit();
	public void abort();
	public State state();

	default void close() {commit();}
}


