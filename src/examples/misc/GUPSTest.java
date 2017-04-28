package examples.misc;

import java.util.*;
import lib.util.persistent.*;
import lib.util.persistent.types.*;
import lib.util.persistent.spi.PersistentMemoryProvider;

public class GUPSTest {

    static PersistentSkipListMap map;

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: examples.misc.GUPSTest NUM_THREADS NUM_ITERATIONS");
            System.exit(1);
        }
        int NUM_THREADS = Integer.parseInt(args[0]);
        int NUM_ITERATIONS = Integer.parseInt(args[1]);

        PersistentMemoryProvider.getDefaultProvider().getHeap().open();
        if ((map = ObjectDirectory.get("m1", PersistentSkipListMap.class)) == null) {
            System.out.println("not in the object directory so recreating");
            map = new PersistentSkipListMap<PersistentInteger, PersistentInteger>();
            ObjectDirectory.put("m1", map);
        }

        System.out.println("Size of map is " + map.size() + ".");
        Thread[] threads = new Thread[NUM_THREADS];
        Random rnd = new Random();
        long start = System.nanoTime();
        for (int j = 0; j < threads.length; j++) {
            threads[j] = new Thread( ()->{
                for (int i = 0; i < NUM_ITERATIONS; i++) {
                    int nextInt = rnd.nextInt();
                    PersistentInteger key = new PersistentInteger(nextInt);
                    PersistentInteger newVal = new PersistentInteger(1);
                    PersistentInteger oldVal;
        //            System.out.println(i + "...putting key: " + key + " value: " + newVal);
                    oldVal = (PersistentInteger)map.putIfAbsent(key, newVal);
                    if (oldVal != null) {  // there is an old value
                        int existing = oldVal.intValue();
                        newVal = new PersistentInteger(existing+1);
                        System.out.println("    Collision! Instead putting key: " + key + " value: " + newVal);
                        map.put(key, newVal);
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
        System.out.println("Total time " + (float)(end - start) / 1e9f + " seconds.");
        System.out.println("Size of map is " + map.size() + ".");
//        System.out.println(map);
        /*for (Map.Entry<Persistentfer, PersistentByteBuffer> e : map.entrySet()) {
            PersistentByteBuffer.deallocate(e.getKey());
            PersistentByteBuffer.deallocate(e.getValue());
        }*/
        map.clear();
        //Util.debugPool();
    }
}

