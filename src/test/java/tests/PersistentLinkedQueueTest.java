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

public class PersistentLinkedQueueTest {

    static boolean verbose = false;
    public static void main(String[] args) {
        PersistentMemoryProvider.getDefaultProvider().getHeap().open();
        verbose = true;
        run();
    }

    public static boolean run() {
        System.out.println("****************PersistentLinkedQueue Tests************");
        return testAdd() && testAddAll() && testPeekAndPoll()
                && testRemove() && testIterator()
                && testToArray() && testToString() && testPersistence();
    }

    private static String threadSafeId(String id) {
   	 return id + "_" + Thread.currentThread().getId();
    }

    @SuppressWarnings("unchecked")
    private static PersistentLinkedQueue<PersistentInteger> getLinkedQueue() {
   	  String id = threadSafeId("tests.persistent_linked_queue");
        PersistentLinkedQueue<PersistentInteger> queue = ObjectDirectory.get(id, PersistentLinkedQueue.class);
        if (queue == null) {
            queue = new PersistentLinkedQueue<PersistentInteger>();
            ObjectDirectory.put(id, queue);
        }
        return queue;
    }

    public static boolean testAdd() {
        if (verbose) System.out.println("PersistentLinkedQueue: testing add()");
        PersistentLinkedQueue<PersistentInteger> q = getLinkedQueue();
        q.clear();
        assert(q.size() == 0);
        for(int i = 0; i < 5; i++) q.add(new PersistentInteger(i));
        assert(q.size() == 5);
        return true;
    }

    public static boolean testAddAll() {
        if (verbose) System.out.println("PersistentLinkedQueue: testing addAll()");
        PersistentLinkedQueue<PersistentInteger> q = getLinkedQueue();
        ArrayList<PersistentInteger> al = new ArrayList<>();
        q.clear();
        assert(q.size() == 0);
        for(int i = 0; i < 5; i++) al.add(new PersistentInteger(i));
        q.addAll(al);
        assert(q.size() == 5);
        return true;
    }

    public static boolean testPeekAndPoll() {
        if (verbose) System.out.println("PersistentLinkedQueue: testing poll() & peek()");
        PersistentLinkedQueue<PersistentInteger> q = getLinkedQueue();
        q.clear();
        assert(q.size() == 0);
        for(int i = 0; i < 5; i++) q.add(new PersistentInteger(i));
        assert(q.size() == 5);
        for(int i = 0; i < 5; i++) {
            assert(q.peek().intValue() == i);
            assert(q.poll().intValue() == i);
        }
        assert(q.size() == 0);
        return true;
    }

    public static boolean testRemove() {
        if (verbose) System.out.println("PersistentLinkedQueue: testing remove()");
        PersistentLinkedQueue<PersistentInteger> q = getLinkedQueue();
        ArrayList<PersistentInteger> al = new ArrayList<>();
        q.clear();
        assert(q.size() == 0);
        for(int i = 0; i < 5; i++) al.add(new PersistentInteger(i));
        q.addAll(al);
        assert(q.size() == 5);
        checkRemove(q, 0);
        checkRemove(q, 4);
        checkRemove(q, 2);
        assert(!q.remove(new PersistentInteger(7)));
        assert(!q.isEmpty());
        return true;
    }

    private static void checkRemove(PersistentLinkedQueue<PersistentInteger> q, int i) {
        int sz = q.size();
        PersistentInteger z = new PersistentInteger(i);
        assert(q.contains(z));
        q.remove(z);
        assert(!q.contains(z));
        assert(q.size() == sz-1);

    }

    public static boolean testToArray() {
        if (verbose) System.out.println("PersistentLinkedQueue: testing toArray()");
        PersistentLinkedQueue<PersistentInteger> q = getLinkedQueue();
        q.clear();
        assert(q.size() == 0);
        for(int i = 0; i < 5; i++) q.add(new PersistentInteger(i));
        PersistentInteger[] arr1 = new PersistentInteger[5];
        arr1 = q.toArray(arr1);
        int num = 0;
        for(PersistentInteger e : arr1) assert(e.intValue() == num++);

        PersistentInteger[] arr2 = new PersistentInteger[3];
        arr2 = q.toArray(arr2);
        num = 0;
        for(PersistentInteger e : arr2) assert(e.intValue() == num++);
        num = 0;
        for(Object e : q.toArray()) {
            PersistentInteger i = (PersistentInteger) e;
            assert(i.intValue() == num++);
        }
        return true;
    }

    public static boolean testIterator() {
        if (verbose) System.out.println("PersistentLinkedQueue: testing iterator");
        PersistentLinkedQueue<PersistentInteger> q = getLinkedQueue();
        q.clear();
        assert(q.size() == 0);
        for(int i = 0; i < 5; i++) q.add(new PersistentInteger(i));
        int num = 0;
        for(PersistentInteger e : q) assert(e.intValue() == num++);
        return true;
    }

    public static boolean testToString() {
        if (verbose) System.out.println("PersistentLinkedQueue: testing toString()");
        PersistentLinkedQueue<PersistentInteger> q = getLinkedQueue();
        q.clear();
        assert(q.toString().equals("Empty Queue"));
        for(int i = 0; i < 5; i++) q.add(new PersistentInteger(i));
        assert(q.toString().equals("0-->1-->2-->3-->4-->NULL"));
        q.clear();
        assert(q.toString().equals("Empty Queue"));
        return true;
    }

    @SuppressWarnings("unchecked")
    public static synchronized boolean testPersistence() {
        if (verbose) System.out.println("PersistentLinkedQueue: testing persistence across vm runs...");
        String id = "tests.linked_queue_persistence";
        PersistentLinkedQueue<PersistentInteger> q = ObjectDirectory.get(id, PersistentLinkedQueue.class);
        if (q == null) {
            q = new PersistentLinkedQueue<PersistentInteger>();
            ObjectDirectory.put(id, q);
            if (verbose) System.out.println("saving to pmem");
            for(int i = 0; i < 5; i++) q.add(new PersistentInteger(i));
        }
        else if (verbose) System.out.println("retrieving from pmem");
        assert(q.size() == 5 && q.toString().equals("0-->1-->2-->3-->4-->NULL"));
        return true;
    }
}
