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

public class ObjectDirectoryTest {

    static boolean verbose = false;
    static PersistentHashMap<PersistentInteger, PersistentString> hmap;
    static PersistentSkipListMap<PersistentInteger, PersistentString> smap;
    static PersistentLinkedQueue<PersistentInteger> lq;
    static PersistentArrayList<PersistentString> arrlist;
    static PersistentTuple2<PersistentInteger, PersistentString> tuple;

    static PersistentIntArray iarr;
    static PersistentDoubleArray darr;
    static PersistentFloatArray farr;
    static PersistentLongArray larr;
    static PersistentCharArray carr;
    static PersistentBooleanArray barr;
    static PersistentByteArray byarr;

    static PersistentInteger integer;
    static PersistentString string;
    static PersistentDouble Double;
    static PersistentFloat Float;
    static PersistentLong Long;
    static PersistentCharacter Char;
    static PersistentBoolean bool;
    static PersistentByte Byte;

    public static void main (String[] args) {
        PersistentMemoryProvider.getDefaultProvider().getHeap().open();
        verbose = true;
        run();
    }

    public static boolean run() {
        System.out.println("****************ObjectDirectory Tests******************");
        return testInsertion() &&
        testRetrieval() &&
        testRemoval();
    }

    @SuppressWarnings("unchecked")
    public static boolean testInsertion() {
        if (verbose) System.out.println("****************Testing insertion**********************");

        ObjectDirectory.put("a1", hmap = new PersistentHashMap<>());
        ObjectDirectory.put("a1", smap = new PersistentSkipListMap<>());
        ObjectDirectory.put("a1", lq = new PersistentLinkedQueue<>());
        ObjectDirectory.put("a1", arrlist = new PersistentArrayList<>());
        ObjectDirectory.put("a1", tuple = new PersistentTuple2<>());

        ObjectDirectory.put("a1", iarr = new PersistentIntArray(1));
        ObjectDirectory.put("a1", darr = new PersistentDoubleArray(1));
        ObjectDirectory.put("a1", farr = new PersistentFloatArray(1));
        ObjectDirectory.put("a1", larr = new PersistentLongArray(1));
        ObjectDirectory.put("a1", carr = new PersistentCharArray(1));
        ObjectDirectory.put("a1", barr = new PersistentBooleanArray(1));
        ObjectDirectory.put("a1", byarr = new PersistentByteArray(1));

        ObjectDirectory.put("a1", integer = new PersistentInteger(1));
        ObjectDirectory.put("a1", string = new PersistentString("world"));
        ObjectDirectory.put("a1", Double =  new PersistentDouble(4.0));
        ObjectDirectory.put("a1", Float = new PersistentFloat(4.4f));
        ObjectDirectory.put("a1", Long = new PersistentLong(34444L));
        ObjectDirectory.put("a1", Char = new PersistentCharacter('r'));
        ObjectDirectory.put("a1", bool = new PersistentBoolean(true));
        ObjectDirectory.put("a1", Byte = new PersistentByte((byte)34));

        return true;
    }

    @SuppressWarnings("unchecked")
    public static boolean testRetrieval() {
        if (verbose) System.out.println("****************Testing retrieval**********************");

        assert(hmap.is(ObjectDirectory.get("a1", PersistentHashMap.class)));
        assert(smap.is(ObjectDirectory.get("a1", PersistentSkipListMap.class)));
        assert(lq.is(ObjectDirectory.get("a1", PersistentLinkedQueue.class)));
        assert(arrlist.is(ObjectDirectory.get("a1", PersistentArrayList.class)));
        assert(tuple.is(ObjectDirectory.get("a1", PersistentTuple2.class)));

        assert(iarr.is(ObjectDirectory.get("a1", PersistentIntArray.class)));
        assert(darr.is(ObjectDirectory.get("a1", PersistentDoubleArray.class)));
        assert(farr.is(ObjectDirectory.get("a1", PersistentFloatArray.class)));
        assert(larr.is(ObjectDirectory.get("a1", PersistentLongArray.class)));
        assert(carr.is(ObjectDirectory.get("a1", PersistentCharArray.class)));
        assert(barr.is(ObjectDirectory.get("a1", PersistentBooleanArray.class)));
        assert(byarr.is(ObjectDirectory.get("a1", PersistentByteArray.class)));

        assert(integer.is(ObjectDirectory.get("a1", PersistentInteger.class)));
        assert(string.is(ObjectDirectory.get("a1", PersistentString.class)));
        assert(Double.is(ObjectDirectory.get("a1", PersistentDouble.class)));
        assert(Float.is(ObjectDirectory.get("a1", PersistentFloat.class)));
        assert(Long.is(ObjectDirectory.get("a1", PersistentLong.class)));
        assert(Char.is(ObjectDirectory.get("a1", PersistentCharacter.class)));
        assert(bool.is(ObjectDirectory.get("a1", PersistentBoolean.class)));
        assert(Byte.is(ObjectDirectory.get("a1", PersistentByte.class)));


        return true;
    }

    @SuppressWarnings("unchecked")
    public static boolean testRemoval() {
        if (verbose) System.out.println("****************Testing removal************************");

        ObjectDirectory.remove("a1", PersistentHashMap.class);
        ObjectDirectory.remove("a1", PersistentSkipListMap.class);
        ObjectDirectory.remove("a1", PersistentLinkedQueue.class);
        ObjectDirectory.remove("a1", PersistentArrayList.class);
        ObjectDirectory.remove("a1", PersistentTuple2.class);
        ObjectDirectory.remove("a1", PersistentIntArray.class);
        ObjectDirectory.remove("a1", PersistentDoubleArray.class);
        ObjectDirectory.remove("a1", PersistentFloatArray.class);
        ObjectDirectory.remove("a1", PersistentLongArray.class);
        ObjectDirectory.remove("a1", PersistentCharArray.class);
        ObjectDirectory.remove("a1", PersistentBooleanArray.class);
        ObjectDirectory.remove("a1", PersistentByteArray.class);
        ObjectDirectory.remove("a1", PersistentInteger.class);
        ObjectDirectory.remove("a1", PersistentString.class);
        ObjectDirectory.remove("a1", PersistentDouble.class);
        ObjectDirectory.remove("a1", PersistentFloat.class);
        ObjectDirectory.remove("a1", PersistentLong.class);
        ObjectDirectory.remove("a1", PersistentCharacter.class);
        ObjectDirectory.remove("a1", PersistentBoolean.class);
        ObjectDirectory.remove("a1", PersistentByte.class);

        assert(ObjectDirectory.get("a1", PersistentHashMap.class) == null);
        assert(ObjectDirectory.get("a1", PersistentSkipListMap.class) == null);
        assert(ObjectDirectory.get("a1", PersistentLinkedQueue.class) == null);
        assert(ObjectDirectory.get("a1", PersistentArrayList.class) == null);
        assert(ObjectDirectory.get("a1", PersistentTuple2.class) == null);

        assert(ObjectDirectory.get("a1", PersistentIntArray.class) == null);
        assert(ObjectDirectory.get("a1", PersistentDoubleArray.class) == null);
        assert(ObjectDirectory.get("a1", PersistentFloatArray.class) == null);
        assert(ObjectDirectory.get("a1", PersistentLongArray.class) == null);
        assert(ObjectDirectory.get("a1", PersistentCharArray.class) == null);
        assert(ObjectDirectory.get("a1", PersistentBooleanArray.class) == null);
        assert(ObjectDirectory.get("a1", PersistentByteArray.class) == null);

        assert(ObjectDirectory.get("a1", PersistentInteger.class) == null);
        assert(ObjectDirectory.get("a1", PersistentString.class) == null);
        assert(ObjectDirectory.get("a1", PersistentDouble.class) == null);
        assert(ObjectDirectory.get("a1", PersistentFloat.class) == null);
        assert(ObjectDirectory.get("a1", PersistentLong.class) == null);
        assert(ObjectDirectory.get("a1", PersistentCharacter.class) == null);
        assert(ObjectDirectory.get("a1", PersistentBoolean.class) == null);
        assert(ObjectDirectory.get("a1", PersistentByte.class) == null);


        return true;
    }

}
