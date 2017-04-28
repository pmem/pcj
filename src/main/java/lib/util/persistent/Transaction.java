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
import java.util.concurrent.locks.ReentrantLock;

public interface Transaction extends AutoCloseable {
	public interface Update {
		public void run();
	}

	public enum State {None, Active, Committed, Aborted}

    /*public static void run(PersistentMemoryProvider provider, Update update, PersistentObject... toLock) {
        ReentrantLock[] locks = new ReentrantLock[toLock.length];
        for (int i = 0; i < toLock.length; i++) {
            if (toLock[i] != null) {
                locks[i] = toLock[i].getPointer().region().getLock();
            }
        }
        Transaction t = provider.newTransaction();
        try {
            t.start(locks);
            t.update(update);
        } catch (Throwable e) {
            //e.printStackTrace();
            t.abort();
            throw e;
        } finally {
            t.commit();
        }
    }*/

    public static void run(PersistentMemoryProvider provider, Update update) {
        run(provider, update, new ReentrantLock[0]);
    }

    public static void run(PersistentMemoryProvider provider, Update update, PersistentObject toLock1) {
        Object dummy = new Object();
        synchronized(toLock1 == null ? dummy : toLock1) {
            ReentrantLock[] locks = new ReentrantLock[1];
            locks[0] = toLock1 == null ? null : toLock1.getPointer().region().getLock();
            run(provider, update, locks);
        }
    }

    public static void run(PersistentMemoryProvider provider, Update update, PersistentObject toLock1, PersistentObject toLock2) {
        Object dummy = new Object();
        synchronized(toLock1 == null ? dummy : toLock1) {
            synchronized(toLock2 == null ? dummy : toLock2) {
                ReentrantLock[] locks = new ReentrantLock[2];
                locks[0] = toLock1 == null ? null : toLock1.getPointer().region().getLock();
                locks[1] = toLock2 == null ? null : toLock2.getPointer().region().getLock();
                run(provider, update, locks);
            }
        }
    }

    public static void run(PersistentMemoryProvider provider, Update update, PersistentObject toLock1, PersistentObject toLock2, PersistentObject toLock3) {
        Object dummy = new Object();
        synchronized(toLock1 == null ? dummy : toLock1) {
            synchronized(toLock2 == null ? dummy : toLock2) {
                synchronized(toLock3 == null ? dummy : toLock3) {
                    ReentrantLock[] locks = new ReentrantLock[3];
                    locks[0] = toLock1 == null ? null : toLock1.getPointer().region().getLock();
                    locks[1] = toLock2 == null ? null : toLock2.getPointer().region().getLock();
                    locks[2] = toLock3 == null ? null : toLock3.getPointer().region().getLock();
                    run(provider, update, locks);
                }
            }
        }
    }

    public static void run(PersistentMemoryProvider provider, Update update, PersistentObject toLock1, PersistentObject toLock2, PersistentObject toLock3, PersistentObject toLock4) {
        Object dummy = new Object();
        synchronized(toLock1 == null ? dummy : toLock1) {
            synchronized(toLock2 == null ? dummy : toLock2) {
                synchronized(toLock3 == null ? dummy : toLock3) {
                    synchronized(toLock4 == null ? dummy : toLock4) {
                        ReentrantLock[] locks = new ReentrantLock[4];
                        locks[0] = toLock1 == null ? null : toLock1.getPointer().region().getLock();
                        locks[1] = toLock2 == null ? null : toLock2.getPointer().region().getLock();
                        locks[2] = toLock3 == null ? null : toLock3.getPointer().region().getLock();
                        locks[3] = toLock4 == null ? null : toLock4.getPointer().region().getLock();
                        run(provider, update, locks);
                    }
                }
            }
        }
    }

    public static void run(PersistentMemoryProvider provider, Update update, PersistentObject toLock1, PersistentObject toLock2, PersistentObject toLock3, PersistentObject toLock4, PersistentObject toLock5) {
        Object dummy = new Object();
        synchronized(toLock1 == null ? dummy : toLock1) {
            synchronized(toLock2 == null ? dummy : toLock2) {
                synchronized(toLock3 == null ? dummy : toLock3) {
                    synchronized(toLock4 == null ? dummy : toLock4) {
                        synchronized(toLock5 == null ? dummy : toLock5) {
                            ReentrantLock[] locks = new ReentrantLock[5];
                            locks[0] = toLock1 == null ? null : toLock1.getPointer().region().getLock();
                            locks[1] = toLock2 == null ? null : toLock2.getPointer().region().getLock();
                            locks[2] = toLock3 == null ? null : toLock3.getPointer().region().getLock();
                            locks[3] = toLock4 == null ? null : toLock4.getPointer().region().getLock();
                            locks[4] = toLock5 == null ? null : toLock5.getPointer().region().getLock();
                            run(provider, update, locks);
                        }
                    }
                }
            }
        }
    }

    static void run(PersistentMemoryProvider provider, Update update, MemoryRegion... toLock) {
        ReentrantLock[] locks = new ReentrantLock[toLock.length];
        for (int i = 0; i < toLock.length; i++) {
            if (toLock[i] != null) {
                locks[i] = toLock[i].getLock();
            }
        }
        run(provider, update, locks);
    }

    static void run(PersistentMemoryProvider provider, Update update, ReentrantLock[] locks) {
        Transaction t = provider.newTransaction();
        try {
            t.start(locks);
            t.update(update);
        } catch (Throwable e) {
            // e.printStackTrace();
            t.abort();
            throw e;
        } finally {
            t.commit();
        }
    }

    public static void run(Update update) {
        run(PersistentMemoryProvider.getDefaultProvider(), update, new MemoryRegion[0]);
    }

    /*public static void run(Update update, PersistentObject... toLock) {
        run(PersistentMemoryProvider.getDefaultProvider(), update, toLock);
    }*/

    public static void run(Update update, PersistentObject toLock1) {
        run(PersistentMemoryProvider.getDefaultProvider(), update, toLock1);
    }

    public static void run(Update update, PersistentObject toLock1, PersistentObject toLock2) {
        run(PersistentMemoryProvider.getDefaultProvider(), update, toLock1, toLock2);
    }

    public static void run(Update update, PersistentObject toLock1, PersistentObject toLock2, PersistentObject toLock3) {
        run(PersistentMemoryProvider.getDefaultProvider(), update, toLock1, toLock2, toLock3);
    }

    public static void run(Update update, PersistentObject toLock1, PersistentObject toLock2, PersistentObject toLock3, PersistentObject toLock4) {
        run(PersistentMemoryProvider.getDefaultProvider(), update, toLock1, toLock2, toLock3, toLock4);
    }

    public static void run(Update update, PersistentObject toLock1, PersistentObject toLock2, PersistentObject toLock3, PersistentObject toLock4, PersistentObject toLock5) {
        run(PersistentMemoryProvider.getDefaultProvider(), update, toLock1, toLock2, toLock3, toLock4, toLock5);
    }

    public static void run(Update update, MemoryRegion... toLock) {
        run(PersistentMemoryProvider.getDefaultProvider(), update, toLock);
    }

	public static Transaction newInstance() {
		return PersistentMemoryProvider.getDefaultProvider().newTransaction();
	}

	public Transaction update(Update update);
    public Transaction start(ReentrantLock... locks);
	public void commit();
	public void abort();
	public State state();

	default void close() {commit();}
}
