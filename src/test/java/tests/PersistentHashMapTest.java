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
import java.util.concurrent.*;

public class PersistentHashMapTest {

    static boolean verbose = false;

    public static void main (String[] args) {
        PersistentMemoryProvider.getDefaultProvider().getHeap().open();
        verbose = true;
        run();
    }

    public static boolean run() {
        System.out.println("****************PersistentHashMap Tests****************");
        return testInsertion() &&
        testRemoval() &&
        testIteration();
    }

 	private static String threadSafeId(String id) {
 	  	 return id + "_" + Thread.currentThread().getId();
 	}

 	@SuppressWarnings("unchecked")
 	private static PersistentHashMap<PersistentInteger, PersistentString> getHashMap() {
 		String id = threadSafeId("tests.persistent_hashmap");
 		PersistentHashMap<PersistentInteger, PersistentString> map = ObjectDirectory.get(id,PersistentHashMap.class);
 		if(map == null) {
 			map = new PersistentHashMap<>();
 			ObjectDirectory.put(id, map);
 		}
 		return map;
 	}

    @SuppressWarnings("unchecked")
    public static boolean testInsertion() {
        if (verbose) System.out.println("****************Testing insertion**********************");

        PersistentHashMap<PersistentInteger, PersistentString> map = getHashMap();
        assert(map.size() == 0);

        PersistentInteger key = new PersistentInteger(1);
        PersistentString val = new PersistentString("world");

        assert(map.get(key) == null);

        PersistentString out = map.put(key, val);
        assert(out == null);
        assert(map.get(key) != null);
        assert(map.get(key).toString().equals("world"));
        assert(map.size() == 1);

        PersistentInteger key2 =  new PersistentInteger(4);
        PersistentString val2 = new PersistentString("javad");
        out = map.put(key2, val2);
        assert(out == null);

        assert(map.size() == 2);
        return true;
    }

    @SuppressWarnings("unchecked")
    public static boolean testRemoval() {
        if (verbose) System.out.println("****************Testing removal************************");

        PersistentHashMap<PersistentInteger, PersistentString> map = getHashMap();
        assert(map != null);
        assert(map.size() == 2);

        PersistentInteger key = new PersistentInteger(1);
        PersistentString val = new PersistentString("world");

        PersistentInteger key2 = new PersistentInteger(1);
        PersistentString out = map.remove(key2);
        assert(out.toString().equals("world"));
        PersistentInteger key3 = new PersistentInteger(4);
        out = map.remove(key3);
        assert(out.toString().equals("javad"));

        assert(map.size() == 0);

        return true;
    }

    @SuppressWarnings("unchecked")
    public static boolean testIteration() {
        if (verbose) System.out.println("****************Testing iteration**********************");

        PersistentHashMap<PersistentInteger, PersistentString> map = getHashMap();
        assert(map != null);
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

        map.remove(key1);
        map.remove(key2);
        map.remove(key3);
        map.remove(key4);
        map.remove(key5);
        map.remove(key6);

        return true;
    }
 }
