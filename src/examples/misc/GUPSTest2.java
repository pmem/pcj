package examples.misc;

import java.util.*;
import lib.util.persistent.*;
import lib.util.persistent.types.*;
import lib.util.persistent.spi.PersistentMemoryProvider;

public class GUPSTest2 {

    static PersistentSkipListMap map;

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: examples.misc.GUPSTest2 START_THREADS END_THREADS TOTAL_TRANSACTIONS [WRITE_FRACTION]");
            System.exit(1);
        }
        int START_THREADS = Math.max(Integer.parseInt(args[0]), 1);
        int END_THREADS = Integer.parseInt(args[1]);
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
        while (nt != END_THREADS + deltaT) {
            // for (int nt = MIN_THREADS; nt <= MAX_THREADS; nt += deltaT) {         
            Thread[] threads = new Thread[nt];
            long start = System.nanoTime();
            int ni = NUM_TRANSACTIONS / nt;
            for (int j = 0; j < threads.length; j++) {
                threads[j] = new Thread( ()->{
                    int kint = (int)Thread.currentThread().getId() * NUM_TRANSACTIONS;
                    int wcount = (int)(ni * WRITE_FRACTION);
                    for (int i = 0; i < wcount; i++) {
                        int nextInt = kint + i;
                        PersistentInteger key = new PersistentInteger(nextInt);
                        PersistentInteger val = new PersistentInteger(1);
                        Object old = map.put(key, val);
                        assert(old == null);
                    }
                    System.gc();
                    int rcount = ni - wcount;
                    int kmax = (int)(rcount / wcount);
                    for (int k = 0; k < kmax; k++) {
                        for (int i = 0; i < wcount; i++) {
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
            System.out.format("threads = %3d: \t%8.1f transactions/sec.\n", nt, (float)NUM_TRANSACTIONS / (float)dur);
            map.clear();
            nt += deltaT;
        }
    }
}

