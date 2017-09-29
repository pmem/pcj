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

public class Stats {
    static boolean enable = false;
    static boolean disableOverride = false;
    public static Stats current;

    public ObjectCacheStats objectCache;
    public MemoryStats memory;
    public TransactionStats transactions;
    public LockStats locks;

    static {
        current = new Stats();
    }

    private Stats() {
        objectCache = new ObjectCacheStats();
        memory = new MemoryStats();
        transactions = new TransactionStats();
        locks = new LockStats();
     }

    public static void enable(boolean e) {enable = e;}
    public static void disableOverride(boolean e) {disableOverride = e;}

    public static boolean enabled() {
        return (enable && !disableOverride);
    }

    public void clear() {
        objectCache.clear();
        memory.clear();
        transactions.clear();
        locks.clear();
    }

    public static Stats reset() {
        Stats ans = current;
        current = new Stats();
        return ans;
    }

    public static class ObjectCacheStats {
        public long simpleHits;
        public long promotedHits;
        public long simpleMisses;
        public long referentMisses;

        ObjectCacheStats() {
            clear();
        }

        public void clear() {
            simpleHits = -1;
            promotedHits = -1;
            simpleMisses = -1;
            referentMisses = -1;    
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
            constructions = 0;
            reconstructions = 0;
            enqueued = 0;        
        }
    }


    public static class TransactionStats {
        public long total = -1;
        public long topLevel = -1;
        public long maxDepth = -1;
        public long totalRetries = 0;
        private long maxRetries = 0;
        public long failures = 0;

        TransactionStats() {
            clear();
        }

        public void clear() {
            total = -1;
            topLevel = -1;
            maxDepth = -1;
            totalRetries = 0;
            maxRetries = 0;
            failures = 0;            
        }

        public void updateMaxRetries(int retries) {
            if (retries > maxRetries) maxRetries = retries;
        }
    }

    public static class LockStats {
        public long acquired = -1;
        public long timeouts = -1;
        public long spinIterations = -1;

        LockStats() {
            clear();
        }

        public void clear() {
            acquired = -1;
            timeouts = - 1;
            spinIterations = -1;
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
        System.out.println("aquired        :" + format(stats.locks.acquired));
        System.out.println("timeouts       :" + format(stats.locks.timeouts));
        System.out.println("spinIterations :" + format(stats.locks.spinIterations));
        System.out.println();
    }

    public static void printStats() {
        printStats(null, current);
    }

    public static void printStats(String header, Stats stats) {
        if (!enabled()) return;
        if (header != null) {
            System.out.format("\n======= %s ===========\n\n", header); 
        }
        // printObjectCacheStats(stats);
        printMemoryStats(stats);
        printTransactionStats(stats);
        // printLockStats(stats);
        System.out.println();
    }        
}