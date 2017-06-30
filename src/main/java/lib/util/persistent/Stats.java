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
    public static TransactionStats transactions = new TransactionStats();
    public static LockStats locks = new LockStats();

    public static void enable(boolean e) {enable = e;}
    public static void disableOverride(boolean e) {disableOverride = e;}

    public static boolean isEnabled() {
        return (enable && !disableOverride);
    }

    public static class ObjectCacheStats {
        public static long simpleHits = 0;
        public static long promotedHits = 0;
        public static long simpleMisses = 0;
        public static long referentMisses = 0;
    }

    public static class TransactionStats {
        public static long total = 0;
        public static long topLevel = 0;
        public static long maxDepth = 0;
        public static long retries = 0;
    }

    public static class LockStats {
        public static long acquired = 0;
        public static long timeouts = 0;
        public static long spinIterations = 0;
    }

    public static void printObjectCacheStats() {
        if (!isEnabled()) return;
        System.out.format("\n       ObjectCache Stats\n");
        System.out.format(  "-------------------------------\n"); 
        System.out.format("simpleHits     :%,15d\n", objectCache.simpleHits); 
        System.out.format("promotedHits   :%,15d\n", objectCache.promotedHits); 
        System.out.format("simpleMisses   :%,15d\n", objectCache.simpleMisses); 
        System.out.format("referentMisses :%,15d\n", objectCache.referentMisses); 
    }

    public static void printTransactionStats() {
        if (!isEnabled()) return;
        System.out.format("\n       Transaction Stats\n");
        System.out.format(  "-------------------------------\n");         
        System.out.format("total          :%,15d\n", transactions.total);
        System.out.format("topLevel       :%,15d\n", transactions.topLevel);
        System.out.format("maxDepth       :%,15d\n", transactions.maxDepth);
        System.out.format("retries        :%,15d\n", transactions.retries);
    }

    public static void printLockStats() {
        if (!isEnabled()) return;
        System.out.format("\n          Lock Stats\n");
        System.out.format(  "-------------------------------\n");         
        System.out.format("aquired        :%,15d\n", locks.acquired);
        System.out.format("timeouts       :%,15d\n", locks.timeouts);
        System.out.format("spinIterations :%,15d\n", locks.spinIterations);
    }

    public static void printStats() {
        if (!isEnabled()) return;
        printObjectCacheStats();
        printTransactionStats();
        printLockStats();
        System.out.println();
    }        
}