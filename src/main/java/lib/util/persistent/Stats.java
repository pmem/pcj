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
    public static ObjectCacheStats objectCache = new ObjectCacheStats();
    public static MemoryStats memory = new MemoryStats();
    public static TransactionStats transactions = new TransactionStats();
    public static LockStats locks = new LockStats();

    public static void enable(boolean e) {enable = e;}
    public static void disableOverride(boolean e) {disableOverride = e;}

    public static boolean enabled() {
        return (enable && !disableOverride);
    }

    public static class ObjectCacheStats {
        public static long simpleHits = -1;
        public static long promotedHits = -1;
        public static long simpleMisses = -1;
        public static long referentMisses = -1;
    }

    public static class MemoryStats {
        public static long constructions = 0;
        public static long reconstructions = 0;
        public static long enqueued = 0;
    }

    public static class TransactionStats {
        public static long total = -1;
        public static long topLevel = -1;
        public static long maxDepth = -1;
        public static long totalRetries = 0;
        private static long maxRetries = 0;
        public static long failures = 0;

        public static void updateMaxRetries(int retries) {
            if (retries > maxRetries) maxRetries = retries;
        }
    }

    public static class LockStats {
        public static long acquired = -1;
        public static long timeouts = -1;
        public static long spinIterations = -1;
    }

    private static String format1 = "%,15d"; 

    private static String format(long value) {
        return String.format(value == -1 ? "%15s" : "%,15d", value == -1 ? "N/A" : value);
    }

    public static void printObjectCacheStats() {
        if (!enabled()) return;
        System.out.println("\n       ObjectCache Stats");
        System.out.println(  "-------------------------------"); 
        System.out.println("simpleHits     :" + format(objectCache.simpleHits)); 
        System.out.println("promotedHits   :" + format(objectCache.promotedHits)); 
        System.out.println("simpleMisses   :" + format(objectCache.simpleMisses)); 
        System.out.println("referentMisses :" + format(objectCache.referentMisses)); 
    }

    public static void printMemoryStats() {
        if (!enabled()) return;
        System.out.println("\n         Memory Stats");
        System.out.println(  "-------------------------------"); 
        System.out.println("constructions  :" + format(memory.constructions)); 
        System.out.println("reconstructions:" + format(memory.reconstructions)); 
        System.out.println("enqueued       :" + format(memory.enqueued)); 
    }

    public static void printTransactionStats() {
        if (!enabled()) return;
        System.out.println("\n       Transaction Stats");
        System.out.println(  "-------------------------------");         
        System.out.println("total          :" + format(transactions.total));
        System.out.println("topLevel       :" + format(transactions.topLevel));
        System.out.println("maxDepth       :" + format(transactions.maxDepth));
        System.out.println("totalRetries   :" + format(transactions.totalRetries));
        System.out.println("maxRetries     :" + format(transactions.maxRetries));
        System.out.println("failures       :" + format(transactions.failures));
    }

    public static void printLockStats() {
        if (!enabled()) return;
        System.out.println("\n          Lock Stats");
        System.out.println(  "-------------------------------");         
        System.out.println("aquired        :" + format(locks.acquired));
        System.out.println("timeouts       :" + format(locks.timeouts));
        System.out.println("spinIterations :" + format(locks.spinIterations));
    }

    public static void printStats() {
        if (!enabled()) return;
        printObjectCacheStats();
        printMemoryStats();
        printTransactionStats();
        printLockStats();
        System.out.println();
    }        
}