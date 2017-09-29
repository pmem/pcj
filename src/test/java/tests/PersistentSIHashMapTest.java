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
import lib.util.persistent.types.*;
import lib.util.persistent.spi.PersistentMemoryProvider;

import java.util.*;
import java.lang.*;
import java.util.concurrent.*;

public class PersistentSIHashMapTest {
    static boolean verbose = false;

    public static void main(String[] args) {
        PersistentMemoryProvider.getDefaultProvider().getHeap().open();
        verbose = true;
        run();
    }

    public static boolean run() {
        System.out.println("****************PersistentSIHashMap Tests**************");
        return testInsertion() &&
               testRemoval() &&
               testIteration() &&
               testClear() &&
               testMultithread() &&
               testReplaceAll();
    }

    static String safeThreadID(String id) {
        return id + "_" + Thread.currentThread().getId();
    }

    static PersistentSIHashMap<PersistentInteger, PersistentString> getMap() {
        String id = safeThreadID("tests.persistent_sihashmap");
        @SuppressWarnings("unchecked")
            PersistentSIHashMap<PersistentInteger, PersistentString> map = ObjectDirectory.get(id, PersistentSIHashMap.class);
        if (map == null) {
            map = new PersistentSIHashMap<>();
            ObjectDirectory.put(id, map);
        }
        return map;
    }

    static boolean testInsertion() {
        if (verbose) System.out.println("****************Testing insertion**********************");
        PersistentSIHashMap<PersistentInteger, PersistentString> map = getMap();
        map.clear();
        for (int i = 0; i < 1000; i++) {
            map.put(new PersistentInteger(i), new PersistentString("test_" + i));
        }
        assert(map.size() == 1000);
        for (int i = 0; i < 1000; i++) {
            assert(map.get(new PersistentInteger(i)).equals(new PersistentString("test_" + i)));
        }
        return true;
    }

    static boolean testRemoval() {
        if (verbose) System.out.println("****************Testing removal************************");
        PersistentSIHashMap<PersistentInteger, PersistentString> map = getMap();
        for (int i = 0; i < 1000; i++) {
            map.remove(new PersistentInteger(i));
            assert(map.size() == 999 - i);
            assert(map.get(new PersistentInteger(i)) == null);
        }
        assert(map.isEmpty());
        return true;
    }

    static boolean testIteration() {
        if (verbose) System.out.println("****************Testing iteration**********************");
        PersistentSIHashMap<PersistentInteger, PersistentString> map = getMap();

        assert(map != null);
        assert(map.size() == 0);

        for (int i = 0; i < 1000; i++) {
            map.put(new PersistentInteger(i), new PersistentString("test_" + i));
        }

        Set<Map.Entry<PersistentInteger, PersistentString>> es = map.entrySet();
        assert(es.size() == 1000);

        for (Map.Entry<PersistentInteger, PersistentString> e : es) {
            assert(map.get(e.getKey()).equals(e.getValue()));
            assert(map.containsKey(e.getKey()));
            assert(map.containsValue(e.getValue()));
        }

        return true;
    }

    static boolean testClear() {
        if (verbose) System.out.println("****************Testing clear**************************");
        final PersistentSIHashMap<PersistentInteger, PersistentString> map = getMap();

        assert(map.size() == 1000);
        map.clear();
        assert(map.size() == 0);
        return true;
    }

    static boolean testMultithread() {
        if (verbose) System.out.println("****************Testing multithread********************");
        final PersistentSIHashMap<PersistentInteger, PersistentString> map = getMap();

        final int iterations = 1000;
        Thread[] threads = new Thread[5];
        for (int i = 0; i < threads.length; i++) {
            final int ii = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < iterations; j++) {
                    final int jj = j;
                    Transaction.run(() -> {
                        map.put(new PersistentInteger(jj), new PersistentString("test_" + jj));
                    });
                }
            });
            threads[i].start();
        }
        try {
            for (int i = 0; i < threads.length; i++)
                threads[i].join();
        } catch (Exception e) { e.printStackTrace(); }
        assert(map.size() == iterations);

        for (int i = 0; i < iterations; i++) {
            assert(map.get(new PersistentInteger(i)).equals(new PersistentString("test_" + i)));
        }

        final int removeInterval = iterations / threads.length;
        for (int i = 0; i < threads.length; i++) {
            final int ii = i;
            threads[i] = new Thread(() -> {
                for (int j = ii * removeInterval; j < (ii + 1) * removeInterval; j++) {
                    map.remove(new PersistentInteger(j));
                }
            });
            threads[i].start();
        }
        try {
            for (int i = 0; i < threads.length; i++)
                threads[i].join();
        } catch (Exception e) { e.printStackTrace(); }

        assert(map.size() == 0);
        for (int i = 0; i < iterations; i++) {
            assert(map.containsKey(new PersistentInteger(i)) == false);
            assert(map.containsValue(new PersistentString("test_" + i)) == false);
            assert(map.get(new PersistentInteger(i)) == null);
        }
        return true;
    }

    static boolean testReplaceAll() {
        if (verbose) System.out.println("****************Testing replaceAll*********************");
        PersistentSIHashMap<PersistentInteger, PersistentString> map = getMap();

        map.clear();
        for (int i = 0; i < 10; i++) {
            map.put(new PersistentInteger(i), new PersistentString("test_" + i));
        }

        map.replaceAll((PersistentInteger i, PersistentString s) -> {
            return new PersistentString(s.toString() + i.toString());
        });

        for (int i = 0; i < 10; i++) {
            assert(map.get(new PersistentInteger(i)).equals(new PersistentString("test_" + i + i)));
        }
        return true;
    }

}