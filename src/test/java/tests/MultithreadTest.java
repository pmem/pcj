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
import lib.util.persistent.spi.PersistentMemoryProvider;

public class MultithreadTest {

    static final int NUM_THREADS = 10;
    static final int NUM_ITERATIONS = 500;
    static boolean verbose = false;

    public static void main(String[] args) {
        Trace.enable(false);
        Stats.enable(true);
        PersistentMemoryProvider.getDefaultProvider().getHeap().open();
        verbose = true;
        run();
        lib.util.persistent.Stats.printStats();
    }

    public static boolean run() {
        System.out.println("****************Multithread Tests**********************");
        return testBasic();
    }

    public static boolean testBasic() {
        if (verbose) System.out.println("****************Testing " + (NUM_THREADS * NUM_ITERATIONS) + " insertions***************");

        for (int k = 0; k < 1; k++) {
            Thread[] threads = new Thread[NUM_THREADS];
            PersistentSkipListMap<PersistentString, PersistentInteger> m1 = new PersistentSkipListMap<>();
            long start = System.nanoTime();
            Box<Integer> bi = new Box<>();
            for (int i = 0; i < threads.length; i++) {
                threads[i] = new Thread( ()->{
                    for (int j = 0; j < NUM_ITERATIONS; j++) {
                        bi.set(j);
                        Transaction.run(() -> {
                            PersistentString key = new PersistentString("hello" + bi.get());
                            m1.put(key, new PersistentInteger(bi.get()));
                        });
                    }
                });
                threads[i].start();
            }
            for (int i = 0; i < threads.length; i++) {
                try {
                    threads[i].join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    System.exit(-1);
                }
            }
            long end = System.nanoTime();
            //System.out.println("Total time " + (end - start));
            //System.out.println("size is " + m1.size());
            assert(m1.size() == NUM_ITERATIONS);
            m1.clear();
        }
        return true;
    }
}
