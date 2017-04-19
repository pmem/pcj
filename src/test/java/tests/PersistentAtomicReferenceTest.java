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

package tests;

import lib.util.persistent.*;

public class PersistentAtomicReferenceTest {
    static boolean verbose = false;
    public static void main(String[] arg) {
        verbose = true;
        run();
    }

    public static boolean run() {
        System.out.println("****************PersistentAtomicReference Tests********");
        return casTest();
    }

    public static boolean casTest() {
        if (verbose) System.out.println("****************Testing CAS****************************");
        int nThreads = 100;
        Thread[] ts = new Thread[nThreads];
        final PersistentAtomicReference<PersistentInteger> par = new PersistentAtomicReference<>(new PersistentInteger(0));
        for (int i = 0; i < ts.length; i++) {
            ts[i] = new Thread(() -> {
                for (;;) {
                    if (par.compareAndSet(par.get(), new PersistentInteger(par.get().intValue() + 1)))
                        break;
                }
            });
            ts[i].start();
        }
        try {
            for (int i = 0; i < ts.length; i++) {
                ts[i].join();
            }
        } catch (InterruptedException e) { e.printStackTrace(); }
        assert(par.get().equals(new PersistentInteger(ts.length)));
        return true;
    }
}
