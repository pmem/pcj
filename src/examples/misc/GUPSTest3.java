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

import lib.xpersistent.XTransaction;

public class GUPSTest3 {
    static PersistentSkipListMap<PersistentInteger, Value> map;
    private static final int WRITE = 0;
    private static final int READ = 1;


    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: examples.misc.GUPSTest3 START_THREADS END_THREADS NUM_WRITES [READ_MULITPLIER] [SLEEP_MS]");
            System.exit(1);
        }
        int START_THREADS = Math.max(Integer.parseInt(args[0]), 1);
        int END_THREADS = Math.max(Integer.parseInt(args[1]), 1);
        int NUM_WRITES = Integer.parseInt(args[2]);
        int READ_MULTIPLIER = args.length > 3 ? Integer.parseInt(args[3]) : 1;
        int SLEEP_MS = args.length > 4 ? Integer.parseInt(args[4]) : 0;
        System.out.format("READ_MULTIPLIER = %d\n", READ_MULTIPLIER);

        Stats.enable(true);
        PersistentMemoryProvider.getDefaultProvider().getHeap().open();
        map = new PersistentSkipListMap<PersistentInteger, Value>();           
        int deltaT = START_THREADS < END_THREADS ? 1 : -1;
        int nt = START_THREADS;
        int nWrites = NUM_WRITES;
        int nReads = NUM_WRITES * READ_MULTIPLIER;
        int nTransactions = nWrites + nReads;     
        System.out.format("nWrites = %,d, nReads = %,d\n", nWrites, nReads);
        System.out.format("threads   time (sec.)   trans./sec.   reads/sec.   writes/sec.\n");
        System.out.format("=======   ===========   ===========   ==========   ===========\n");
        while (nt != END_THREADS + deltaT) {
            int tWrites = nWrites / nt;
            int tReads = nReads / nt;
            Thread[] threads = new Thread[nt];
            RunTimer timer = new RunTimer(2);
            //writes
            for (int j = 0; j < threads.length; j++) {
                threads[j] = new Thread(()->{
                    int kint = (int)Thread.currentThread().getId() * nTransactions;
                    for (int i = 0; i < tWrites; i++) {
                        int nextInt = kint + i;
                        PersistentInteger key = new PersistentInteger(nextInt);
                        Value val = new Value();
                        Object old = map.put(key, val);
                        assert(old == null);
                        sleep(SLEEP_MS);
                    }
                });
            }
            timer.begin(WRITE);
            run(threads);
            timer.end(WRITE);
            // reads
            for (int j = 0; j < threads.length; j++) {
                threads[j] = new Thread(()->{
                    int kint = (int)Thread.currentThread().getId() * nTransactions;
                    for (int k = 0; k < READ_MULTIPLIER; k++) {
                        for (int i = 0; i < tWrites; i++) {
                            int nextInt = kint + i;
                            PersistentInteger key = new PersistentInteger(nextInt);
                            Value obj = map.get(key);
                            sleep(SLEEP_MS);
                        }
                    }
                });
            }
            timer.begin(READ);
            run(threads);
            timer.end(READ);
            float totalDuration = timer.getTotalDuration();
            System.out.format("%7d%14.2f%,14.0f%,13.0f%,14.0f\n", nt, totalDuration, (float)nTransactions / totalDuration, nReads / timer.getDuration(READ), nWrites / timer.getDuration(WRITE));
            map.clear();
            nt += deltaT;
        }
        // lib.util.persistent.Stats.printStats();
    }

    private static void run(Thread[] ts) {
        for (Thread t : ts) t.start();
        for (int j = 0; j < ts.length; j++) {
            try {
                ts[j].join();
            } 
            catch (InterruptedException e) {
                throw new RuntimeException(e.getMessage());
            }
        }
    }

    private static void sleep(int ms) {
        if (ms <= 0) return;
        try{
            Thread.sleep(ms);
        } 
        catch(Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }


    private static class RunTimer {
        private long[] starts;
        private long[] durations;
        private int size;

        public RunTimer(int size) {
            this.size = size;
            reset();
        }

        public void begin(int i) {
            starts[i] = System.nanoTime();
        }

        public void end(int i) {
            durations[i] = System.nanoTime() - starts[i];
        }

        public float getDuration(int i) {
            return (float)durations[i] / 1e9f;
        }
        
        public float getTotalDuration() {
            long sum = 0;
            for (long x : durations) sum += x;
            return (float)sum / 1e9f;
        }

        public void reset() {
            this.starts = new long[size];
            this.durations = new long[size];
        }
    }

    public final static class Value extends PersistentObject {
        private static final LongField ID = new LongField();
        private static final StringField NAME = new StringField();
        private static final ObjectType<Value> TYPE = ObjectType.withFields(Value.class, ID, NAME);

        public Value() {
            super(TYPE);
            setLongField(ID, 12345);
            setObjectField(NAME, new PersistentString("Mars"));
        }

        public Value(ObjectPointer<Value> p) {super(p);}
    }
}

