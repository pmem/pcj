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

import lib.xpersistent.XTransaction;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.Map;
import java.util.Comparator;
import lib.util.persistent.types.ObjectType;


/* Notes:
    -- to enable AllocationStats, find lines marked "uncomment for allocation stats" and comment back in
*/

public class Stats {
    static boolean enable = Config.ENABLE_STATS;
    public static Stats current;

    public ObjectCacheStats objectCache;
    public MemoryStats memory;
    public TransactionStats transactions;
    public LockStats locks;
    public AllocationStats allocStats;

    static {
        current = new Stats();
    }

    private Stats() {
        objectCache = new ObjectCacheStats();
        memory = new MemoryStats();
        transactions = new TransactionStats();
        locks = new LockStats();
        allocStats = new AllocationStats(AllocationStats.Key.NPV);
     }

    public static void enable(boolean e) {enable = e;}
    
    public static boolean enabled() {
        return enable;
    }

    public void clear() {
        objectCache.clear();
        memory.clear();
        transactions.clear();
        locks.clear();
        allocStats.clear();
    }

    public static Stats reset() {
        Stats ans = current;
        current = new Stats();
        current.objectCache.maxSize = ans.objectCache.maxSize;
        return ans;
    }

    public static class ObjectCacheStats {
        public long simpleHits;
        public long promotedHits;
        public long simpleMisses;
        public long referentMisses;
        public long maxSize;

        ObjectCacheStats() {
            clear();
        }

        public void clear() {
            simpleHits = -1;
            promotedHits = 0;
            simpleMisses = -1;
            referentMisses = 0;    
            maxSize = -1;
        }        
    }

    public static class MemoryStats {
        public long constructions;
        public long reconstructions;
        public long enqueued;
    
        MemoryStats() {
            clear();
        }

        public void clear() {
            constructions = -1;
            reconstructions = -1;
            enqueued = 0;        
        }
    }


    public static class TransactionStats {
        public long total;
        public long topLevel;
        public long maxDepth;
        public long totalRetries;
        public long runCalls;
        private long maxRetries;
        public long failures;

        TransactionStats() {
            clear();
        }

        public void clear() {
            total = -1;
            topLevel = -1;
            maxDepth = -1;
            runCalls = 0;
            totalRetries = 0;
            maxRetries = 0;
            failures = 0;            
        }

        public void updateMaxRetries(int retries) {
            if (retries > maxRetries) maxRetries = retries;
        }
    }

    public static class LockStats {
        public long acquired;
        public long timeouts;

        LockStats() {
            clear();
        }

        public void clear() {
            acquired = -1;
            timeouts = -1;
        }
    }

    public static class AllocationStats {
        private static final String AS_LOCK_NAME = "AnyPersistent.asLock()";
        public static final long WRAPPER_PER_INSTANCE = 80;
        private ConcurrentSkipListMap<Key, Record> data;

        private static class Key implements Comparable<Key> {
            private String className;
            private long pSize;
            private long vSize;

            public Key(String className, long pSize, long vSize) {
                this.className = className;
                this.pSize = pSize;
                this.vSize = vSize;
            }

            public int compareTo(Key that) {
                return NPV.compare(this, that);
            }

            public static Comparator<Key> NPV = (Key k1, Key k2) -> {
                int nCompare = k1.className.compareTo(k2.className);
                if (nCompare != 0) return nCompare;
                int vCompare = Long.compare(k1.vSize, k2.vSize);
                if (vCompare != 0) return vCompare;
                return Long.compare(k1.pSize, k2.pSize);
            };

            public static Comparator<Key> VPN = (Key k1, Key k2) -> {
                int vCompare = Long.compare(k1.vSize, k2.vSize);
                if (vCompare != 0) return vCompare;
                int pCompare = Long.compare(k1.pSize, k2.pSize);
                if (pCompare != 0) return pCompare;
                return k1.className.compareTo(k2.className);
            };
    

            public static Comparator<Key> PNV = (Key k1, Key k2) -> {
                int pCompare = Long.compare(k1.pSize, k2.pSize);
                if (pCompare != 0) return pCompare;
                int vCompare = Long.compare(k1.vSize, k2.vSize);
                if (vCompare != 0) return vCompare;
                return k1.className.compareTo(k2.className);
            };
        }

        private static class Record {
            private long instances;
        }

        public AllocationStats(Comparator<Key> comparator) {
            this.data = new ConcurrentSkipListMap<>(comparator);
        }

        public AllocationStats() {
            this(Key.NPV);
        }

        public void clear() {data.clear();}

        public void update(String className, long pSize, long vSize, long count) {
            Key key = new Key(className, pSize, vSize);
            Record current = data.get(key);
            if (current == null) {
                current = new Record();
                data.put(key, current);
            }
            current.instances += count;
        }

        public String toString() {
            StringBuilder buff = new StringBuilder();
            long totalInstances = 0;
            long totalPersistentBytes = 0;
            long totalVolatileBytes = 0;
            buff.append("                                                         Allocation Stats\n");
            buff.append("------------------------------------------------------------------------------------------------------------------------------------------------\n");         
            buff.append("                                                                                           Persistent    Persistent    Volatile      Volatile   \n");
            buff.append("Class Name                                                                     Instances   bytes each    bytes total   bytes each    bytes total\n");
            buff.append("------------------------------------------------------------------------------------------------------------------------------------------------\n");
            for (Map.Entry<Key, Record> entry : data.entrySet()) {
                Key key = entry.getKey();
                String cls = key.className;
                long persistentUnitSize = key.pSize;
                long volatileUnitSize = key.vSize;
                Record rec = entry.getValue();
                long clsPersistentTotal = rec.instances * persistentUnitSize;
                long clsVolatileTotal =  rec.instances * volatileUnitSize;
                totalInstances += rec.instances;
                totalPersistentBytes += clsPersistentTotal;
                totalVolatileBytes += clsVolatileTotal;
                buff.append(String.format("%-73s%,15d%,13d%,15d%,13d%,15d\n", cls, rec.instances, persistentUnitSize, clsPersistentTotal, volatileUnitSize, clsVolatileTotal));
            }
            buff.append("                                                                           -------------                 -----------               -------------\n");
            buff.append(String.format("                                                                         %,15d               %,13d             %,15d\n", totalInstances, totalPersistentBytes, totalVolatileBytes)); 
            return buff.toString();
        }
    }   

    private static String format1 = "%,15d"; 

    private static String format(long value) {
        return String.format(value == -1 ? "%15s" : "%,15d", value == -1 ? "N/A" : value);
    }

    public static void printObjectCacheStats() {printObjectCacheStats(current);}

    public static void printObjectCacheStats(Stats stats) {
        if (!enabled()) return;
        System.out.println("       ObjectCache Stats");
        System.out.println(  "-------------------------------"); 
        System.out.println("simpleHits     :" + format(stats.objectCache.simpleHits)); 
        System.out.println("promotedHits   :" + format(stats.objectCache.promotedHits)); 
        System.out.println("simpleMisses   :" + format(stats.objectCache.simpleMisses)); 
        System.out.println("referentMisses :" + format(stats.objectCache.referentMisses)); 
        System.out.println("maxSize        :" + format(stats.objectCache.maxSize)); 
        System.out.println();
    }

    public static void printMemoryStats() {printMemoryStats(current);}

    public static void printMemoryStats(Stats stats) {
        if (!enabled()) return;
        System.out.println("         Memory Stats");
        System.out.println(  "-------------------------------"); 
        System.out.println("constructions  :" + format(stats.memory.constructions)); 
        System.out.println("reconstructions:" + format(stats.memory.reconstructions)); 
        System.out.println("enqueued       :" + format(stats.memory.enqueued)); 
        System.out.println();
    }

    public static void printTransactionStats() {printTransactionStats(current);}

    public static void printTransactionStats(Stats stats) {
        if (!enabled()) return;
        System.out.println("       Transaction Stats");
        System.out.println(  "-------------------------------");         
        System.out.println("run calls      :" + format(stats.transactions.runCalls));
        System.out.println("total          :" + format(stats.transactions.total));
        System.out.println("topLevel       :" + format(stats.transactions.topLevel));
        System.out.println("maxDepth       :" + format(stats.transactions.maxDepth));
        System.out.println("totalRetries   :" + format(stats.transactions.totalRetries));
        System.out.println("maxRetries     :" + format(stats.transactions.maxRetries));
        System.out.println("failures       :" + format(stats.transactions.failures));
        System.out.println();
    }

    public static void printLockStats() {printLockStats(current);}

    public static void printLockStats(Stats stats) {
        if (!enabled()) return;
        System.out.println("          Lock Stats");
        System.out.println(  "-------------------------------");         
        System.out.println("acquired        :" + format(stats.locks.acquired));
        System.out.println("timeouts       :" + format(stats.locks.timeouts));
        System.out.println();
    }

    public static void printAllocationStats() {printLockStats(current);}

    public static void printAllocationStats(Stats stats) {
        if (!enabled()) return;
        System.out.println(stats.allocStats.toString());
    }

    public static void printStats() {
        printStats(null, current);
    }

    public static void printStats(String header, Stats stats) {
        if (!enabled()) return;
        if (header != null) {
            System.out.format("\n======= %s ===========\n\n", header); 
        }
        if (Config.ENABLE_OBJECT_CACHE_STATS) printObjectCacheStats(stats);
        if (Config.ENABLE_MEMORY_STATS) printMemoryStats(stats);
        if (Config.ENABLE_TRANSACTION_STATS) printTransactionStats(stats);
        if (Config.ENABLE_LOCK_STATS) printLockStats(stats);
        if (Config.ENABLE_ALLOC_STATS) printAllocationStats(stats);  // uncomment for allocation stats
        System.out.println();
    }  

}