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
import static lib.util.persistent.Util.*;

public class PersistentTupleTest {

    static boolean verbose = false;
    public static void main(String[] args) {
        verbose = true;
        run();
    }

    public static boolean run() {
        System.out.println("****************PersistentTuple Tests******************");
        return tuple3SetTest() &&
               tuple5EqualityTest() &&
               tuple4HashTest();
    }

    public static boolean tuple3SetTest() {
        if (verbose) System.out.println("****************Testing put/get************************");
        PersistentTuple3<PersistentString, PersistentInteger, PersistentSkipListMap> t3 = new PersistentTuple3<>();
        PersistentString ps = persistent("hello");
        PersistentInteger pi = persistent(5);
        PersistentSkipListMap<PersistentDouble, PersistentFloat> pslm = new PersistentSkipListMap<>();
        t3._1(ps);
        t3._2(pi);
        t3._3(pslm);
        assert(t3._1().equals(new PersistentString("hello")));
        assert(t3._2().equals(new PersistentInteger(5)));
        assert(t3._3().equals(new PersistentSkipListMap<PersistentDouble, PersistentFloat>()));
        assert(t3._1() == ps);
        assert(t3._2() == pi);
        assert(t3._3() == pslm);
        assert(t3._1().equals(ps));
        assert(t3._2().equals(pi));
        assert(t3._3().equals(pslm));
        return true;
    }

    public static boolean tuple5EqualityTest() {
        if (verbose) System.out.println("****************Testing equality***********************");
        PersistentTuple5<PersistentString, PersistentLong, PersistentShort, PersistentHashMap, PersistentBoolean> t5 = new PersistentTuple5<>();
        PersistentTuple5<PersistentString, PersistentLong, PersistentShort, PersistentHashMap, PersistentBoolean> t5_same = new PersistentTuple5<>();
        PersistentString ps = persistent("world");
        PersistentLong pl = persistent(10L);
        PersistentShort psh = persistent((short)120);
        PersistentHashMap<PersistentTuple2, PersistentAtomicReference> phm = new PersistentHashMap<>();
        PersistentBoolean pb = persistent(true);
        t5._1(ps);
        t5._2(pl);
        t5._3(psh);
        t5._4(phm);
        t5._5(pb);
        t5_same._1(ps);
        t5_same._2(pl);
        t5_same._3(psh);
        t5_same._4(phm);
        t5_same._5(pb);
        assert(t5.equals(t5_same));
        t5_same._3(persistent((short)120));
        assert(t5.equals(t5_same));
        t5_same._5(persistent(false));
        assert(!(t5.equals(t5_same)));
        PersistentTuple5<PersistentString, PersistentShort, PersistentLong, PersistentHashMap, PersistentBoolean> t5_reordered = new PersistentTuple5<>();
        t5_reordered._1(ps);
        t5_reordered._2(psh);
        t5_reordered._3(pl);
        t5_reordered._4(phm);
        t5_reordered._5(pb);
        assert(!(t5.equals(t5_reordered)));
        return true;
    }

    public static boolean tuple4HashTest() {
        if (verbose) System.out.println("****************Testing hashing************************");
        PersistentArray<PersistentInteger> pa = new PersistentArray<>(5);
        PersistentLinkedList<PersistentLong> pll = new PersistentLinkedList<>();
        PersistentTuple1<PersistentFloat> pt1 = new PersistentTuple1<>();
        PersistentArrayList<PersistentTuple2> pal = new PersistentArrayList<>();
        PersistentTuple4<PersistentArray, PersistentLinkedList, PersistentTuple1, PersistentArrayList> t4 = new PersistentTuple4<>(pa, pll, pt1, pal);
        PersistentTuple4<PersistentArray, PersistentLinkedList, PersistentTuple1, PersistentArrayList> t4_same = new PersistentTuple4<>(pa, pll, pt1, pal);
        for (int i = 0; i < 5; i++) {
            pa.set(i, persistent(i));
        }
        for (int i = 0; i < 10; i++) {
            pll.add(persistent((long)i));
        }
        pt1._1(persistent(0.5f));
        for (int i = 0; i < 10; i++) {
            pal.add(new PersistentTuple2<PersistentInteger, PersistentInteger>(persistent(i), persistent(i)));
        }
        PersistentHashMap<PersistentTuple4, PersistentTuple4> phm = new PersistentHashMap<>();
        assert(phm.size() == 0);
        phm.put(t4, t4_same);
        assert(phm.size() == 1);
        phm.put(t4, t4);
        assert(phm.size() == 1);
        assert(phm.get(t4) == t4);
        assert(phm.get(t4).equals(t4));
        phm.put(t4_same, t4);
        assert(phm.size() == 1);
        assert(phm.get(t4_same) == t4);
        assert(phm.get(t4_same).equals(t4));
        assert(phm.get(t4_same).equals(t4_same));
        return true;
    }
}
