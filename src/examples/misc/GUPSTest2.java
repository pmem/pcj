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

package examples.misc;

import java.util.*;
import lib.util.persistent.*;
import lib.util.persistent.types.*;
import lib.util.persistent.spi.PersistentMemoryProvider;

public class GUPSTest2 {
    static PersistentSkipListMap<PersistentInteger, PersistentInteger> map;

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: examples.misc.GUPSTest2 START_THREADS END_THREADS TOTAL_TRANSACTIONS [WRITE_FRACTION]");
            System.exit(1);
        }
        int START_THREADS = Math.max(Integer.parseInt(args[0]), 1);
        int END_THREADS = Math.max(Integer.parseInt(args[1]), 1);
        int NUM_TRANSACTIONS = Integer.parseInt(args[2]);
        float WRITE_FRACTION = args.length > 3 ? Float.parseFloat(args[3]) : 0.5f;
        System.out.println("WRITE_FRACTION = " + WRITE_FRACTION);

        PersistentMemoryProvider.getDefaultProvider().getHeap().open();
        if ((map = ObjectDirectory.get("m1", PersistentSkipListMap.class)) == null) {
            System.out.println("New test map created.");
            map = new PersistentSkipListMap<PersistentInteger, PersistentInteger>();
            ObjectDirectory.put("m1", map);
        }

        int deltaT = START_THREADS < END_THREADS ? 1 : -1;
        int nt = START_THREADS;
        int nWrites = (int)((float)NUM_TRANSACTIONS * WRITE_FRACTION);
        int nReads = NUM_TRANSACTIONS - nWrites;
        System.out.format("nWrites = %d, nReads = %d\n", nWrites, nReads);
        System.out.format("threads   time (sec.)   rate (trans./sec.)\n");
        System.out.format("=======   ===========   ==================\n");
        while (nt != END_THREADS + deltaT) {
            int tWrites = nWrites / nt;
            int tReads = nReads / nt;
            Thread[] threads = new Thread[nt];
            long start = System.nanoTime();
            int ni = NUM_TRANSACTIONS / nt;
            for (int j = 0; j < threads.length; j++) {
                threads[j] = new Thread(()->{
                    int kint = (int)Thread.currentThread().getId() * NUM_TRANSACTIONS;
                    for (int i = 0; i < tWrites; i++) {
                        int nextInt = kint + i;
                        PersistentInteger key = new PersistentInteger(nextInt);
                        PersistentInteger val = new PersistentInteger(123);
                        Object old = map.put(key, val);
                        assert(old == null);
                    }
                    int kmax = (int)(tReads / tWrites);
                    for (int k = 0; k < kmax; k++) {
                        for (int i = 0; i < tWrites; i++) {
                            int nextInt = kint + i;
                            PersistentInteger key = new PersistentInteger(nextInt);
                            Object obj = map.get(key);
                        }
                    }
                });
                threads[j].start();
            }
            for (int j = 0; j < threads.length; j++) {
                try {
                    threads[j].join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    System.exit(-1);
                }
            }
            long end = System.nanoTime();
            float dur = (float)(end - start) / 1e9f ;
            System.out.format("%7d%14.2f%,21.1f\n", nt, dur, (float)NUM_TRANSACTIONS / (float)dur);
            map.clear();
            nt += deltaT;
        }
    }
}

