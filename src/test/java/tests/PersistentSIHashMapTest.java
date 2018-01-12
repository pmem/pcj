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
    static TreeSet<PersistentUUID> set = new TreeSet<PersistentUUID>();

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
               testReplaceAll() &&
               testVolatileGet();
    }

    static String safeThreadID(String id) {
        return id + "_" + Thread.currentThread().getId();
    }

    static PersistentSIHashMap<PersistentUUID, PersistentString> getMap() {
        String id = safeThreadID("tests.persistent_sihashmap");
        @SuppressWarnings("unchecked")
            PersistentSIHashMap<PersistentUUID, PersistentString> map = ObjectDirectory.get(id, PersistentSIHashMap.class);
        if (map == null) {
            map = new PersistentSIHashMap<>();
            ObjectDirectory.put(id, map);
        }
        return map;
    }

    static boolean testInsertion() {
        if (verbose) System.out.println("****************Testing insertion**********************");
        PersistentSIHashMap<PersistentUUID, PersistentString> map = getMap();
        map.clear();
        for (int i = 0; i < 1000; i++) {
            PersistentUUID uuid = PersistentUUID.randomUUID();
            set.add(uuid);
        }
        assert(set.size() == 1000);

        int i=0;
        for (PersistentUUID uuid : set) {
            map.put(uuid, new PersistentString("test_" + i++));
        }
        assert(map.size() == 1000);

        i=0;
        for (PersistentUUID uuid : set) {
            assert(map.get(uuid).equals(new PersistentString("test_" + i++)));
        }
        return true;
    }

    static boolean testRemoval() {
        if (verbose) System.out.println("****************Testing removal************************");
        PersistentSIHashMap<PersistentUUID, PersistentString> map = getMap();
        int i=0;
        for (PersistentUUID uuid : set) {
            map.remove(uuid);
            assert(map.size() == 999 - i++);
            assert(map.get(uuid) == null);
        }
        assert(map.isEmpty());
        return true;
    }

    static boolean testIteration() {
        if (verbose) System.out.println("****************Testing iteration**********************");
        PersistentSIHashMap<PersistentUUID, PersistentString> map = getMap();

        assert(map != null);
        assert(map.size() == 0);

        for (int i = 0; i < 1000; i++) {
            map.put(PersistentUUID.randomUUID(), new PersistentString("test_" + i));
        }

        Set<Map.Entry<PersistentUUID, PersistentString>> es = map.entrySet();
        assert(es.size() == 1000);

        for (Map.Entry<PersistentUUID, PersistentString> e : es) {
            assert(map.get(e.getKey()).equals(e.getValue()));
            assert(map.containsKey(e.getKey()));
            assert(map.containsValue(e.getValue()));
        }

        return true;
    }

    static boolean testClear() {
        if (verbose) System.out.println("****************Testing clear**************************");
        final PersistentSIHashMap<PersistentUUID, PersistentString> map = getMap();

        assert(map.size() == 1000);
        map.clear();
        assert(map.size() == 0);
        return true;
    }

    static boolean testMultithread() {
        if (verbose) System.out.println("****************Testing multithread********************");
        final PersistentSIHashMap<PersistentUUID, PersistentString> map = getMap();

        final int iterations = 1000;
        
        Thread[] threads = new Thread[5];
        for (int i = 0; i < threads.length; i++) {
            final int ii = i;
            threads[i] = new Thread(() -> {
                int j=0;
                for (PersistentUUID uuid : set) {
                    final int jj = j++;
                    Transaction.run(() -> {
                        map.put(uuid, new PersistentString("test_" + jj));
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

        int i=0;
        for (PersistentUUID uuid : set) {
            assert(map.get(uuid).equals(new PersistentString("test_" + i++)));
        }

        final int removeInterval = iterations / threads.length;
        PersistentUUID[] arr = set.toArray(new PersistentUUID[0]);
        for (i = 0; i < threads.length; i++) {
            final int ii = i;
            threads[i] = new Thread(() -> {
                for (int j = ii * removeInterval; j < (ii + 1) * removeInterval; j++) {
                    map.remove(arr[j]);
                }
            });
            threads[i].start();
        }
        try {
            for (i = 0; i < threads.length; i++)
                threads[i].join();
        } catch (Exception e) { e.printStackTrace(); }

        assert(map.size() == 0);
        i = 0;
        for (PersistentUUID uuid : set) {
            assert(map.containsKey(uuid) == false);
            assert(map.containsValue(new PersistentString("test_" + i)) == false);
            assert(map.get(uuid) == null);
        }
        return true;
    }

    static boolean testReplaceAll() {
        if (verbose) System.out.println("****************Testing replaceAll*********************");
        PersistentSIHashMap<PersistentUUID, PersistentString> map = getMap();

        map.clear();
        int i=0;
        for (PersistentUUID uuid : set) {
            map.put(uuid, new PersistentString("test_" + i++));
            if (i >= 10) break;
        }

        map.replaceAll((PersistentUUID u, PersistentString s) -> {
            return new PersistentString(s.toString() + u.toString());
        });

        i=0;
        for (PersistentUUID uuid : set) {
            assert(map.get(uuid).equals(new PersistentString("test_" + i++ + uuid.toString())));
            if (i >= 10) break;
        }
        return true;
    }

    static boolean testVolatileGet() {
        if (verbose) System.out.println("****************Testing volatileGet********************");
        PersistentSIHashMap<PersistentUUID, PersistentString> map = getMap();

        map.clear();
        int i=0;
        for (PersistentUUID uuid : set) {
            map.put(uuid , new PersistentString("test_" + i++));
            if (i >= 10) break;
        }

        i=0;
        for (PersistentUUID uuid : set) {
            assert(map.get(UUID.fromString(uuid.toString()), PersistentUUID.class).equals(new PersistentString("test_" + i++)));
            if (i >= 10) break;
        }

        return true;
    }

}
