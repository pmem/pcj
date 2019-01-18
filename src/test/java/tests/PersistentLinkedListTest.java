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

public class PersistentLinkedListTest {

    static boolean verbose = false;
    public static void main(String[] args) {
        PersistentMemoryProvider.getDefaultProvider().getHeap().open();
        verbose = true;
        run();
    }

    public static boolean run() {
        System.out.println("****************PersistentLinkedList Tests*************");
        return testAdd() && testGetAndSet() && testInsert() && testRemove() && testToString() && testPersistence();
    }

    private static String threadSafeId(String id) {
   	 return id + "_" + Thread.currentThread().getId();
    }
    
    @SuppressWarnings("unchecked")
    private static PersistentLinkedList<PersistentInteger> getList() {
   	  String id = threadSafeId("tests.persistent_linked_list");
   	  PersistentLinkedList<PersistentInteger> list = ObjectDirectory.get(id, PersistentLinkedList.class);
        if (list == null) {
            list = new PersistentLinkedList<>();
            ObjectDirectory.put(id, list);
        }
        return list;
    }

    private static boolean checkEquals(PersistentInteger pi, int i) {
        return (pi.intValue() == i);
    }

    public static boolean testAdd() {
        if (verbose) System.out.println("PersistentLinkedList: testing add()");
        PersistentLinkedList<PersistentInteger> list = getList();
        list.clear();
        assert(list.size() == 0);
        for(int i = 0; i < 5; i++) list.add(new PersistentInteger(i));
        assert(list.size() == 5);
        return true;
    }

    public static boolean testGetAndSet() {
        if (verbose) System.out.println("PersistentLinkedList: testing get() and set()");
        PersistentLinkedList<PersistentInteger> list = getList();
        list.clear();
        assert(list.size() == 0);
        for(int i = 0; i < 5; i++) list.add(new PersistentInteger(i));
        assert(list.size() == 5);

        for(int i = 0; i < 5; i++) assert(checkEquals(list.get(i), i));
        for(int i = 0; i < 5; i++) list.set(i, new PersistentInteger(4-i));
        for(int i = 0; i < 5; i++) assert(checkEquals(list.get(i), 4-i));
        return true;
    }

    public static boolean testInsert() {
        if (verbose) System.out.println("PersistentLinkedList: testing insert()");
        PersistentLinkedList<PersistentInteger> list = getList();
        list.clear();
        assert(list.size() == 0);
        for(int i = 0; i < 5; i++) list.add(new PersistentInteger(i));
        assert(list.size() == 5);

        list.insert(0, new PersistentInteger(-1));
        list.insert(3, new PersistentInteger(22));
        list.insert(list.size(), new PersistentInteger(9999));
        assert(list.size() == 8);
        assert(checkEquals(list.get(0), -1));
        assert(checkEquals(list.get(3), 22));
        assert(checkEquals(list.get(list.size()-1), 9999));
        return true;
    }

    public static boolean testRemove() {
        if (verbose) System.out.println("PersistentLinkedList: testing remove()");
        PersistentLinkedList<PersistentInteger> list = getList();
        list.clear();
        assert(list.size() == 0);
        for(int i = 0; i < 5; i++) list.add(new PersistentInteger(i));
        assert(list.size() == 5);

        list.insert(0, new PersistentInteger(-1));
        list.insert(3, new PersistentInteger(22));
        list.insert(list.size(), new PersistentInteger(9999));
        assert(list.size() == 8);
        list.remove(3);
        list.remove(0);
        list.remove(list.size()-1);
        assert(list.size() == 5);
        for(int i = 0; i < 5; i++) assert(checkEquals(list.get(i), i));
        return true;
    }

    public static boolean testToString() {
        if (verbose) System.out.println("PersistentLinkedList: testing toString()");
        PersistentLinkedList<PersistentInteger> list = getList();
        list.clear();
        assert(list.toString().equals("NULL"));
        for(int i = 0; i < 5; i++) list.add(new PersistentInteger(i));
        assert(list.toString().equals("0-->1-->2-->3-->4-->NULL"));
        list.clear();
        assert(list.toString().equals("NULL"));
        return true;
    }

    @SuppressWarnings("unchecked")
    public static synchronized boolean testPersistence() {
        if (verbose) System.out.println("PersistentLinkedList: testing persistence across vm runs");
        String id = "tests.linked_list_persistance";
        PersistentLinkedList<PersistentInteger> list = ObjectDirectory.get(id, PersistentLinkedList.class);
        if (list == null) {
            list = new PersistentLinkedList<>();
            ObjectDirectory.put(id, list);
            if (verbose) System.out.println("Saving to pmem");
            for(int i = 0; i < 5; i++) list.add(new PersistentInteger(i));
        }
        else if (verbose) System.out.println("Retrieving from pmem");
        assert(list.size() == 5 && list.toString().equals("0-->1-->2-->3-->4-->NULL"));
        return true;
    }
}
