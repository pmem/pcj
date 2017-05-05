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
import java.util.ArrayList;
import java.util.Vector;
import java.util.LinkedList;

public class PersistentArrayListTest {

	static boolean verbose = false;
	public static void main(String[] args) {
		PersistentMemoryProvider.getDefaultProvider().getHeap().open();
		verbose = true;
		run();
	}

	public static boolean run() {
		System.out.println("****************PersistentArrayList Tests**************");
		return testAdd() && testGetAndSet() && testRemove() && testAddAll() && testEnsureCapacity() && testIndexOf() && testToArray() && testPersistence();
	}

	private static String threadSafeId(String id) {
  	 return id + "_" + Thread.currentThread().getId();
   }

	@SuppressWarnings("unchecked")
	private static PersistentArrayList<PersistentInteger> getList() {
		String id = threadSafeId("tests.persistent_array_list");
		PersistentArrayList<PersistentInteger> list = ObjectDirectory.get(id, PersistentArrayList.class);
		if (list == null) {
			list = new PersistentArrayList<PersistentInteger>();
			ObjectDirectory.put(id, list);
		}
		return list;
	}

	public static boolean testAdd() {
		if (verbose) System.out.println("PersistentArrayList: testing add()");
		PersistentArrayList<PersistentInteger> list = getList();
		list.clear();
		assert(list.size() == 0 && list.isEmpty());
		for(int i = 0; i < 5; i++) list.add(new PersistentInteger(i));
		assert(list.size() == 5);

		for(int i = 5; i < 10; i++) list.add(i, new PersistentInteger(i));
		assert(list.size() == 10);
		return true;
	}

	public static boolean testAddAll() {
		if (verbose) System.out.println("PersistentArrayList: testing addAll()");
		PersistentArrayList<PersistentInteger> list = getList();
		list.clear();
		assert(list.size() == 0 && list.isEmpty());
		ArrayList<PersistentInteger> tmpList1 = new ArrayList<>();
		for(int i = 0; i < 5; i++) tmpList1.add(new PersistentInteger(i));
		list.addAll(tmpList1);
		assert(list.size() == 5);

		Vector<PersistentInteger> tmpList2 = new Vector<>(5);
		for(int i = 0; i < 5; i++) tmpList2.add(new PersistentInteger(i+5));
		list.addAll(0, tmpList2);
		assert(list.size() == 10);

		LinkedList<PersistentInteger> tmpList3 = new LinkedList<PersistentInteger>();
		for(int i = 1; i <= 5; i++) tmpList3.add(new PersistentInteger(-i));
		list.addAll(3, tmpList3);
		assert(list.size() == 15);
		if(verbose) {
			for(PersistentInteger e: list) System.out.print(e.intValue() + " ");
			System.out.println();
		}
		return true;
	}

	private static boolean checkEquals(PersistentInteger pi, int i) {
		return (pi.intValue() == i);
	}

	public static boolean testGetAndSet() {
		if (verbose) System.out.println("PersistentArrayList: testing get() and set()");
		PersistentArrayList<PersistentInteger> list = getList();
		list.clear();
		assert(list.size() == 0);
		for(int i = 0; i < 5; i++) list.add(new PersistentInteger(i));
		assert(list.size() == 5);

		for(int i = 0; i < 5; i++) assert(checkEquals(list.get(i), i));
		for(int i = 0; i < 5; i++) list.set(i, new PersistentInteger(4-i));
		for(int i = 0; i < 5; i++) assert(checkEquals(list.get(i), 4-i));
		return true;
	}

	public static boolean testRemove() {
		if (verbose) System.out.println("PersistentArrayList: testing remove()");
		PersistentArrayList<PersistentInteger> list = getList();
		list.clear();
		assert(list.size() == 0);
		for(int i = 0; i < 10; i++) list.add(new PersistentInteger(i));
		assert(list.size() == 10);

		list.remove(0);
		list.remove(5);
		list.remove(list.size()-1);
		list.remove(new PersistentInteger(0));
		list.remove(new PersistentInteger(4));
		assert(list.size() == 6);
		return true;
	}

	public static boolean testEnsureCapacity() {
		if (verbose) System.out.println("PersistentArrayList: testing ensureCapacity()");
		PersistentArrayList<PersistentInteger> list = getList();
		list.clear();
		assert(list.size() == 0);

		list.ensureCapacity(15);
		for(int i = 0; i < 25; i++) list.add(new PersistentInteger(i));
		assert(list.size() == 25);
		return true;
	}

	public static boolean testIndexOf() {
		if (verbose) System.out.println("PersistentArrayList: testing indexOf()");
		PersistentArrayList<PersistentInteger> list = getList();
		list.clear();
		assert(list.size() == 0);

		for(int i = 0; i < 5; i++) list.add(new PersistentInteger(i));
		for(int i = 0; i < 5; i++) list.add(new PersistentInteger(i));
		assert(list.size() == 10);
		assert(list.indexOf(new PersistentInteger(0)) == 0);
		assert(list.lastIndexOf(new PersistentInteger(0)) == 5);
		assert(list.indexOf(new PersistentInteger(4)) == 4);
		assert(list.lastIndexOf(new PersistentInteger(4)) == 9);
		return true;
	}

   public static boolean testToArray() {
       if (verbose) System.out.println("PersistentArrayList: testing toArray()");
       PersistentArrayList<PersistentInteger> list = getList();
       list.clear();
       assert(list.size() == 0);
       for(int i = 0; i < 5; i++) list.add(new PersistentInteger(i));
       PersistentInteger[] arr1 = new PersistentInteger[5];
       arr1 = list.toArray(arr1);
       int num = 0;
       for(PersistentInteger e : arr1) assert(e.intValue() == num++);

       PersistentInteger[] arr2 = new PersistentInteger[3];
       arr2 = list.toArray(arr2);
       num = 0;
       for(PersistentInteger e : arr2) assert(e.intValue() == num++);
       return true;
   }

   @SuppressWarnings("unchecked")
   public static synchronized boolean testPersistence() {
       if (verbose) System.out.println("PersistentArrayList: testing persistence across vm runs...");
       String id = "tests.array_list_persistance";
       PersistentArrayList<PersistentInteger> list = ObjectDirectory.get(id, PersistentArrayList.class);
       if (list == null) {
           list = new PersistentArrayList<PersistentInteger>();
           ObjectDirectory.put(id, list);
           if (verbose) System.out.println("saving to pmem");
           for(int i = 0; i < 5; i++) list.add(new PersistentInteger(i));
       }
       else if (verbose) System.out.println("retrieving from pmem");
       assert(list.size() == 5 && list.toString().equals("[0, 1, 2, 3, 4]"));
       return true;
   }
}














