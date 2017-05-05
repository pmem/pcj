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

public class PersistentSkipListMapTest {

static boolean verbose = false;

    public static void main (String[] args) {
        PersistentMemoryProvider.getDefaultProvider().getHeap().open();
        verbose = true;
        run();
    }

    public static boolean run() {
        System.out.println("****************PersistentSkipListMap Tests************");
        return testInsertion() &&
        testRemoval() &&
        testIteration() &&
        testSubMap() &&
        testPutAll() &&
        testSubmapOthers();
    }

    private static String threadSafeId(String id) {
 	  	 return id + "_" + Thread.currentThread().getId();
 	}

 	@SuppressWarnings("unchecked")
 	private static PersistentSkipListMap<PersistentInteger, PersistentString> getSkipListMap() {
 		String id = threadSafeId("tests.persistent_skiplist_map");
 		PersistentSkipListMap<PersistentInteger, PersistentString> map = ObjectDirectory.get(id,PersistentSkipListMap.class);
 		if(map == null) {
 			map = new PersistentSkipListMap<>();
 			ObjectDirectory.put(id, map);
 		}
 		return map;
 	}

    @SuppressWarnings("unchecked")
    public static boolean testInsertion() {
        if (verbose) System.out.println("****************Testing insertion**********************");

        PersistentSkipListMap<PersistentInteger, PersistentString> map = getSkipListMap();
        map.clear();
        assert(map.size() == 0);

        PersistentInteger key = new PersistentInteger(1);
        PersistentString val = new PersistentString("world");

        assert(map.get(key) == null);

        PersistentString out = map.put(key, val);
        assert(out == null);
        assert(map.get(key) != null);
        assert(map.get(key).toString().equals("world"));
        assert(map.size() == 1);

        //PersistentInteger key2 =  new PersistentInteger(4);
        PersistentString val2 = new PersistentString("javad");
        out = map.put(key, val2);
        assert(out.toString().equals("world"));

        //System.out.println("size is " +map.size());
        //map.clear();
        return true;
    }

    @SuppressWarnings("unchecked")
    public static boolean testRemoval() {
        if (verbose) System.out.println("****************Testing removal************************");

        PersistentSkipListMap<PersistentInteger, PersistentString> map = getSkipListMap();
        assert(map.size() == 1);
        map.clear();

        PersistentInteger key = new PersistentInteger(1);
        PersistentString val = new PersistentString("world");

        map.put(key, val);
        assert(map.size() == 1);

        PersistentInteger key2 = new PersistentInteger(1);
        PersistentString out = map.remove(key2);
        assert(out.toString().equals("world"));
        assert(map.size() == 0);
        assert(map.isEmpty() == true);

        map.put(key2, val);
        assert(map.get(key).toString().equals("world"));
        assert(map.size() == 1);

        out = map.remove(key);
        assert(out.toString().equals("world"));
        assert(map.size() == 0);
        assert(map.isEmpty() == true);

        return true;
    }

    @SuppressWarnings("unchecked")
    public static boolean testIteration() {
        if (verbose) System.out.println("****************Testing iteration**********************");

        PersistentSkipListMap<PersistentInteger, PersistentString> map = getSkipListMap();
        //map.clear();
        assert(map.size() == 0);

        TreeMap<PersistentInteger, PersistentString> tmap = new TreeMap<PersistentInteger, PersistentString>();

        PersistentInteger key1 = new PersistentInteger(1);
        PersistentString val1 = new PersistentString("world");

        PersistentInteger key2 = new PersistentInteger(2);
        PersistentString val2 = new PersistentString("java");

        PersistentInteger key3 = new PersistentInteger(4);
        PersistentString val3 = new PersistentString("bye");

        PersistentInteger key4 = new PersistentInteger(3);
        PersistentString val4 = new PersistentString("world");

        map.put(key1, val1);
        map.put(key2, val2);
        map.put(key3, val3);
        map.put(key4, val4);

        assert(map.size() == 4);

        tmap.put(key1, val1);
        tmap.put(key2, val2);
        tmap.put(key3, val3);
        tmap.put(key4, val4);

        Set<Map.Entry<PersistentInteger, PersistentString>> s1 = map.entrySet();
        Set<Map.Entry<PersistentInteger, PersistentString>> s2 = tmap.entrySet();
        assert(s1.size() == s2.size());
        assert(s1.size() == 4);

        Iterator<Map.Entry<PersistentInteger, PersistentString>> sit1 = s1.iterator();
        Iterator<Map.Entry<PersistentInteger, PersistentString>> sit2 = s2.iterator();

        while (sit1.hasNext() || sit2.hasNext()) {
            Map.Entry<PersistentInteger, PersistentString> e1 = sit1.next();
            Map.Entry<PersistentInteger, PersistentString> e2 = sit2.next();
            assert(e1.getKey().equals(e2.getKey()));
            assert(e1.getValue().equals(e2.getValue()));
        }

        Set<PersistentInteger> ks1 = map.keySet();
        Set<PersistentInteger> ks2 = tmap.keySet();
        assert(ks1.size() == ks2.size());
        assert(ks1.size() == 4);

        Iterator<PersistentInteger> ksit1 = ks1.iterator();
        Iterator<PersistentInteger> ksit2 = ks2.iterator();
        while ((ksit1.hasNext() == true) || (ksit2.hasNext() ==  true)) {
            PersistentInteger a = ksit1.next();
            PersistentInteger b = ksit2.next();
            assert(a.equals(b));
        }

        Collection<PersistentString> vc1 = map.values();
        Collection<PersistentString> vc2 = tmap.values();
        assert(vc1.size() == vc2.size());
        assert(vc1.size() == 4);

        Iterator<PersistentString> vit1 = vc1.iterator();
        Iterator<PersistentString> vit2 = vc2.iterator();

        while (vit1.hasNext() || vit2.hasNext()) {
            PersistentString a = vit1.next();
            PersistentString b = vit2.next();
            assert(a.equals(b));
        }

        PersistentInteger key5 = new PersistentInteger(4);
        PersistentString val5 = new PersistentString("bye");

        assert(map.containsKey(key5) == true);
        assert(map.containsValue(val5) == true);

        PersistentInteger key6 = new PersistentInteger(6);
        PersistentString val6 = new PersistentString("weird");

        assert(map.containsKey(key6) == false);
        assert(map.containsValue(val6) == false);

        assert(map.firstKey().equals(tmap.firstKey()));
        assert(map.get(map.firstKey()).equals(tmap.get(tmap.firstKey())));

        assert(map.lastKey().equals(tmap.lastKey()));
        assert(map.get(map.lastKey()).equals(tmap.get(tmap.lastKey())));

        map.put(key6, val6);
        tmap.put(key6, val6);
        assert(map.size() == 5);
        assert(s1.size() == s2.size());
        assert(s1.size() == 5);

        sit1 = s1.iterator();
        sit2 = s2.iterator();

        while (sit1.hasNext() || sit2.hasNext()) {
            Map.Entry<PersistentInteger, PersistentString> e1 = sit1.next();
            Map.Entry<PersistentInteger, PersistentString> e2 = sit2.next();
            assert(e1.getKey().equals(e2.getKey()));
            assert(e1.getValue().equals(e2.getValue()));
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    public static boolean testSubMap() {
        if (verbose) System.out.println("****************Testing submap*************************");
        PersistentSkipListMap<PersistentInteger, PersistentString> map = getSkipListMap();
        TreeMap<PersistentInteger, PersistentString> staging = new TreeMap<PersistentInteger, PersistentString>();

        assert(map.size() == 5);
        map.clear();

        PersistentInteger keyA = new PersistentInteger((int)'a');
        PersistentInteger keyE = new PersistentInteger((int)'e');
        PersistentInteger keyI = new PersistentInteger((int)'i');
        PersistentInteger keyO = new PersistentInteger((int)'o');
        PersistentInteger keyU = new PersistentInteger((int)'u');

        PersistentString val = new PersistentString("val");

        staging.put(keyA, val);
        staging.put(keyE, val);
        staging.put(keyI, val);
        staging.put(keyO, val);
        staging.put(keyU, val);

        map.putAll(staging);

        if (verbose) System.out.println("****************Testing headmap************************");
        headMapTests(map, val);

        map.clear();
        map.putAll(staging);

        if (verbose) System.out.println("****************Testing tailmap************************");
        tailMapTests(map, val);

        map.clear();
        map.putAll(staging);

        if (verbose) System.out.println("****************Testing submap*************************");
        subMapTests(map, val);

        map.clear();
        return true;
    }

    /*
     * All submap tests break down into four big cases, based on where a provided key can fall (example of headmap)
     * - Before beginning of map (smaller than all existing keys)    - case 1
     * - Between two existing keys on map                            - case 2
     * - Exactly equal to one of the existing keys on map            - case 3
     * - After end of map (larger than all existing keys)            - case 4
     * Similar cases for tailmap (larger than all existing keys for case 1, etc.) and submap.
     */

    @SuppressWarnings("unchecked")
    private static boolean headMapTests(PersistentSkipListMap map, PersistentString val) {

        PersistentInteger headKey = new PersistentInteger((int)'Z');
        SortedMap<PersistentInteger, PersistentString> hm = map.headMap(headKey);   // Z < a, case 1
        assert(hm.size() == 0);


        PersistentInteger key1 = new PersistentInteger((int)'U');
        map.put(key1, val);
        assert(hm.size() == 1);
        assert(hm.firstKey().intValue()==((int)'U'));

        headKey = new PersistentInteger((int)'f');
        hm = map.headMap(headKey);   // e < f < i, case 2
        assert(hm.size() == 3);  // E, a, e

        headKey = new PersistentInteger((int)'o');
        hm = map.headMap(headKey);   // case 3
        assert(hm.size() == 4);  // E, a, e, i

        headKey = new PersistentInteger((int)'w');
        hm = map.headMap(headKey);   // w > u, case 4
        assert(hm.size() == 6);  // E, a, e, i, o, u

        PersistentInteger key2 = new PersistentInteger((int)'v');
        hm.put(key2, val);
        assert(hm.size() == 7);
        assert(map.size() == 7);

        PersistentInteger key3 = new PersistentInteger((int)'z');
        map.put(key3, val);
        assert(hm.size() == 7);
        assert(map.size() == 8);

        PersistentInteger key4 = new PersistentInteger((int)'w');
        map.put(key4, val);
        assert(hm.size() == 7);
        assert(map.size() == 9);

        boolean caught = false;
        try {
            hm.put(key3, val);
        } catch (IllegalArgumentException e) {
            caught = true;
        }
        assert(caught);

        headKey = new PersistentInteger((int)'O');
        hm = map.headMap(headKey);

        PersistentInteger tailKey = new PersistentInteger((int)'E');
        SortedMap<PersistentInteger, PersistentString> htm = hm.tailMap(tailKey);
        assert(htm.size() == 0);

        PersistentInteger key5 = new PersistentInteger((int)'L');
        map.put(key5, val);
        assert(htm.size() == 1);
        assert(hm.size() == 1);
        assert(map.size() == 10);

        PersistentInteger startKey = new PersistentInteger((int)'F');
        PersistentInteger endKey = new PersistentInteger((int)'L');
        SortedMap<PersistentInteger, PersistentString> htsm = htm.subMap(startKey, endKey);
        assert(htsm.size() == 0);

        PersistentInteger key6 = new PersistentInteger((int)'G');
        hm.put(key6, val);
        assert(htsm.size() == 1);
        assert(hm.size() == 2);
        assert(htm.size() == 2);
        assert(map.size() == 11);

        PersistentInteger headKey2 = new PersistentInteger((int)'P');
        caught = false;
        try {
            SortedMap<PersistentInteger, PersistentString> hhm = hm.headMap(headKey2);
        } catch (IllegalArgumentException e) {
            caught = true;
        }
        assert(caught);
        return true;
    }

    @SuppressWarnings("unchecked")
    private static boolean tailMapTests(PersistentSkipListMap map, PersistentString val) {

        PersistentInteger tailKey = new PersistentInteger((int)'w');
        SortedMap<PersistentInteger, PersistentString> tm = map.tailMap(tailKey);  // w > u, case 1
        assert(tm.size() == 0);

        PersistentInteger key1 = new PersistentInteger((int)'z');
        map.put(key1, val);
        assert(tm.size() == 1);

        tailKey = new PersistentInteger((int)'p');
        tm = map.tailMap(tailKey);    // o < p < u, case 2
        assert(tm.size() == 2);  // u, z

        tailKey = new PersistentInteger((int)'i');
        tm = map.tailMap(tailKey);    // case 3
        assert(tm.size() == 4); // i, o, u, z

        tailKey = new PersistentInteger((int)'W');
        tm = map.tailMap(tailKey);    // W > a, case 4
        assert(tm.size() == 6);

        PersistentInteger key2 = new PersistentInteger((int)'Z');
        tm.put(key2, val);
        assert(tm.size() == 7);
        assert(map.size() == 7);

        PersistentInteger key3 = new PersistentInteger((int)'L');
        map.put(key3, val);
        assert(tm.size() == 7);
        assert(map.size() == 8);

        PersistentInteger key4 = new PersistentInteger((int)'W');
        map.put(key4, val);
        assert(tm.size() == 8);
        assert(map.size() == 9);

        PersistentInteger key5 = new PersistentInteger((int)'0');
        boolean caught = false;
        try {
            tm.put(key5, val);
        } catch (IllegalArgumentException e) {
            caught = true;
        }
        assert(caught);

        tailKey = new PersistentInteger((int)'u');
        tm = map.tailMap(tailKey);
        assert(tm.size() == 2);

        PersistentInteger headKey = new PersistentInteger((int)'z');
        SortedMap<PersistentInteger, PersistentString> thm = tm.headMap(headKey);
        assert(thm.size() == 1);

        PersistentInteger key6 = new PersistentInteger((int)'w');
        map.put(key6, val);
        assert(thm.size() == 2);
        assert(tm.size() == 3);
        assert(map.size() == 10);

        PersistentInteger startKey = new PersistentInteger((int)'u');
        PersistentInteger endKey = new PersistentInteger((int)'w');
        SortedMap<PersistentInteger, PersistentString> thsm = thm.subMap(startKey, endKey);
        assert(thsm.size() == 1);

        assert(thsm.remove(endKey) == null);
        assert(map.get(endKey) != null);

        PersistentInteger tailKey2 = new PersistentInteger((int)'d');
        caught = false;
        try {
            SortedMap<PersistentInteger, PersistentString> ttm = tm.tailMap(tailKey2);
        } catch (IllegalArgumentException e) {
            caught = true;
        }
        assert(caught);

        PersistentInteger tailKey3 = new PersistentInteger((int)'u');
        PersistentInteger tailKey4 = new PersistentInteger((int)'w');
        SortedMap<PersistentInteger, PersistentString> ttm = tm.tailMap(tailKey3);
        SortedMap<PersistentInteger, PersistentString> tttm = ttm.tailMap(tailKey4);

        assert(ttm.size() == 3);
        assert(tttm.size() == 2);
        return true;
    }

    @SuppressWarnings("unchecked")
    private static boolean subMapTests(PersistentSkipListMap map, PersistentString val) {

        PersistentInteger startKey1 = new PersistentInteger((int)'f');
        PersistentInteger endKey1 = new PersistentInteger((int)'z');
        SortedMap<PersistentInteger, PersistentString> sm = map.subMap(startKey1, endKey1);
        assert(sm.size() == 3);

        startKey1 = new PersistentInteger((int)'Z');
        endKey1 = new PersistentInteger((int)'z');
        sm = map.subMap(startKey1, endKey1);
        assert(sm.size() == 5);

        startKey1 = new PersistentInteger((int)'a');
        endKey1 = new PersistentInteger((int)'u');
        sm = map.subMap(startKey1, endKey1);
        assert(sm.size() == 4);

        startKey1 = new PersistentInteger((int)'G');
        endKey1 = new PersistentInteger((int)'a');
        sm = map.subMap(startKey1, endKey1);
        assert(sm.size() == 0);

        startKey1 = new PersistentInteger((int)'u');
        endKey1 = new PersistentInteger((int)'z');
        sm = map.subMap(startKey1, endKey1);
        assert(sm.size() == 1);

        startKey1 = new PersistentInteger((int)'f');
        endKey1 = new PersistentInteger((int)'h');
        sm = map.subMap(startKey1, endKey1);
        assert(sm.size() == 0);

        PersistentInteger key1 = new PersistentInteger((int)'f');
        map.put(key1, val);
        assert(sm.size() == 1);
        assert(map.size() == 6);

        PersistentInteger key2 = new PersistentInteger((int)'h');
        boolean caught = false;
        try {
            sm.put(key2, val);
        } catch (IllegalArgumentException e) {
            caught = true;
        }
        assert(caught);

        startKey1 = new PersistentInteger((int)'U');
        endKey1 = new PersistentInteger((int)'d');
        sm = map.subMap(startKey1, endKey1);
        assert(sm.size() == 1);

        PersistentInteger key3 = new PersistentInteger((int)'Z');
        sm.put(key3, val);
        assert(sm.size() == 2);
        assert(map.size() == 7);

        PersistentInteger headKey = new PersistentInteger((int)'b');
        SortedMap<PersistentInteger, PersistentString> shm = sm.headMap(headKey);
        assert(shm.size() == 2);

        PersistentInteger key4 = new PersistentInteger((int)'c');
        sm.put(key4, val);
        assert(sm.size() == 3);
        assert(shm.size() == 2);
        assert(map.size() == 8);

        PersistentInteger tailKey = new PersistentInteger((int)'a');
        SortedMap<PersistentInteger, PersistentString> shtm = shm.tailMap(tailKey);
        assert(shtm.size() == 1);

        PersistentInteger key5 = new PersistentInteger((int)'W');
        shm.put(key5, val);
        assert(shm.size() == 3);
        assert(sm.size() == 4);
        assert(shtm.size() == 1);
        assert(map.size() == 9);
        return true;
    }

    @SuppressWarnings("unchecked")
    public static boolean testPutAll() {
        if (verbose) System.out.println("****************Testing putAll*************************");
        PersistentSkipListMap<PersistentInteger, PersistentString> map = getSkipListMap();
        assert(map.size() == 0);

        ConcurrentSkipListMap<PersistentInteger, PersistentString> staging = new ConcurrentSkipListMap<PersistentInteger, PersistentString>();

        PersistentInteger key1 = new PersistentInteger(1);
        PersistentString val1 = new PersistentString("cups");

        PersistentInteger key2 = new PersistentInteger(2);
        PersistentString val2 = new PersistentString("java");

        PersistentInteger key3 = new PersistentInteger(3);
        PersistentString val3 = new PersistentString("bye");

        PersistentInteger key4 = new PersistentInteger(4);
        PersistentString val4 = new PersistentString("world");

        staging.put(key1, val1);
        staging.put(key2, val2);
        staging.put(key3, val3);
        staging.put(key4, val4);

        map.putAll(staging);
//        assert(map.equals(staging));
//        assert(staging.equals(map));
        Set<Map.Entry<PersistentInteger, PersistentString>> s1 = map.entrySet();
        Set<Map.Entry<PersistentInteger, PersistentString>> s2 = staging.entrySet();
        assert(s1.size() == s2.size());
        assert(s1.size() == 4);

        Iterator<Map.Entry<PersistentInteger, PersistentString>> sit1 = s1.iterator();
        Iterator<Map.Entry<PersistentInteger, PersistentString>> sit2 = s2.iterator();

        while (sit1.hasNext() || sit2.hasNext()) {
            Map.Entry<PersistentInteger, PersistentString> e1 = sit1.next();
            Map.Entry<PersistentInteger, PersistentString> e2 = sit2.next();
            assert(e1.getKey().equals(e2.getKey()));
            assert(e1.getValue().equals(e2.getValue()));
        }


        map.clear();
        return true;
    }

    @SuppressWarnings("unchecked")
    public static boolean testSubmapOthers() {
        if (verbose) System.out.println("****************Testing Submap Other Functions*********");
        String id = "tests.persistent_skiplist_map2_" + Thread.currentThread().getId();
        PersistentSkipListMap<PersistentString, PersistentString> map = ObjectDirectory.get(id,PersistentSkipListMap.class);
        if (map == null) ObjectDirectory.put(id, map = new PersistentSkipListMap<>());
        //map.clear();
        assert(map.size() == 0);

        PersistentString key1 = new PersistentString("reese");
        PersistentString val1 = new PersistentString("cups");

        PersistentString key2 = new PersistentString("hi");
        PersistentString val2 = new PersistentString("java");

        PersistentString key3 = new PersistentString("goody");
        PersistentString val3 = new PersistentString("byeby");

        PersistentString key4 = new PersistentString("hillo");
        PersistentString val4 = new PersistentString("world");

        map.put(key1, val1);
        map.put(key2, val2);
        map.put(key3, val3);
        map.put(key4, val4);

        PersistentString startKey = new PersistentString("hi");
        PersistentString endKey = new PersistentString("world");
        SortedMap<PersistentString, PersistentString> sm = map.subMap(startKey, endKey);

        TreeMap<PersistentString, PersistentString> staging = new TreeMap<PersistentString, PersistentString>();

        PersistentString key5 = new PersistentString("go");
        PersistentString val5 = new PersistentString("weird");

        PersistentString key6 = new PersistentString("word");
        PersistentString val6 = new PersistentString("excel");

        staging.put(key5, val5);
        staging.put(key6, val6);

        boolean caught = false;
        try {
            sm.putAll(staging);
        } catch (IllegalArgumentException e) {
            caught = true;
        }
        assert(caught);

        staging.clear();

        sm.clear();
        assert(map.size() == 1);
        assert(map.firstKey().toString().equals("goody"));
        assert(map.get(map.firstKey()).toString().equals("byeby"));

        PersistentString hihi = new PersistentString("hihi");
        caught = false;
        try {
            sm.compute(key3, (k, v) -> (v == null ? hihi : (v = new PersistentString("java"))));
        } catch (IllegalArgumentException e) {
            caught = true;
        }
        assert(caught);
        assert(map.get(key3).toString().equals("byeby"));

        caught = false;
        try {
            sm.compute(key3, (k, v) -> (v = new PersistentString("java")));
        } catch (IllegalArgumentException e) {
            caught = true;
        }
        assert(caught);
        map.clear();
        return true;
    }
}
