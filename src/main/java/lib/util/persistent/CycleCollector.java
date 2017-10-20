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

import lib.util.persistent.spi.PersistentMemoryProvider;
import lib.xpersistent.XRoot;
import lib.xpersistent.XHeap;
import lib.xpersistent.PersistentConcurrentHashMapInternal;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Deque;
import java.util.ArrayDeque;

public class CycleCollector {

    public static final byte BLACK = 0;
    public static final byte PURPLE = 1;
    public static final byte GREY = 2;
    public static final byte WHITE = 3;

    // static HashSet<Long> candidatesSet = new HashSet<>();
    static PersistentConcurrentHashMapInternal candidatesSet;
    static HashSet<Long> freedCandidates = new HashSet<>();
    static XHeap heap;
    static XRoot root;
    static AtomicBoolean processing = new AtomicBoolean();
    static HashMap<Long, Byte> colorChangesWhileCollecting = new HashMap<>();

    // public static synchronized HashSet<Long> getCandidates() { return candidatesSet; }

    public static synchronized void collect() {
        heap = ((XHeap)(PersistentMemoryProvider.getDefaultProvider().getHeap()));
        root = ((XRoot)(heap.getRoot()));
        processing.set(true);
        candidatesSet = root.getCandidates();
        markCandidates();
        PersistentConcurrentHashMapInternal.EntryIterator iter = candidatesSet.iter();
        while (iter.hasNext()) {
            PersistentConcurrentHashMapInternal.NodeLL node = iter.next();
            Deque<Long> stack = new ArrayDeque<>();
            stack.push(node.getKey());
            scan(stack);
        }
        collectCandidates();
        // root.clearCandidates();
        candidatesSet.delete();
        processing.set(false);
        synchronized(colorChangesWhileCollecting) {
            for (Map.Entry<Long, Byte> e : colorChangesWhileCollecting.entrySet()) {
                PersistentObject obj = ObjectCache.get(e.getKey(), true);
                obj.setColor(e.getValue());
            }
            colorChangesWhileCollecting.clear();
        }
    }

    private static void markCandidates() {
        PersistentConcurrentHashMapInternal.EntryIterator iter = candidatesSet.iter();
        while (iter.hasNext()) {
            PersistentConcurrentHashMapInternal.NodeLL node = iter.next();
            Long l = node.getKey();
            AnyPersistent obj = ObjectCache.get(l, true);
            if (obj.getColor() == PURPLE) {
                Deque<Long> stack = new ArrayDeque<>();
                stack.push(l);
                markGrey(stack);
            } else {
                iter.remove();
                if (obj.getColor() == BLACK && obj.getRefCount() == 0) {
                    AnyPersistent.free(l);
                    root.removeFromAllObjects(l);
                }
            }
        }
    }

    private static void markGrey(Deque<Long> stack) {
        while (!stack.isEmpty()) {
            Long l = stack.pop();
            AnyPersistent obj = ObjectCache.get(l, true);
            if (obj.getColor() != GREY) {
                Iterator<Long> childAddresses = AnyPersistent.getChildAddressIterator(l);
                while (childAddresses.hasNext()) {
                    long childAddr = childAddresses.next();
                    // System.out.println("processing child addr " + childAddr);
                    ObjectCache.get(childAddr, true).decRefCount();
                    stack.push(childAddr);
                }
                obj.setColor(GREY, true);
            }
        }
    }

    private static void scan(Deque<Long> stack) {
        while (!stack.isEmpty()) {
            Long l = stack.pop();
            AnyPersistent obj = ObjectCache.get(l, true);
            if (obj.getColor() == GREY) {
                if (obj.getRefCount() > 0) {
                    Deque<Long> scanBlackStack = new ArrayDeque<>();
                    scanBlackStack.push(l);
                    scanBlack(scanBlackStack);
                } else {
                    Iterator<Long> childAddresses = AnyPersistent.getChildAddressIterator(l);
                    while (childAddresses.hasNext()) {
                        long childAddr = childAddresses.next();
                        stack.push(childAddr);
                    }
                    obj.setColor(WHITE, true);
                }
            }
        }
    }

    private static void scanBlack(Deque<Long> stack) {
        while (!stack.isEmpty()) {
            Long l = stack.pop();
            ObjectCache.get(l, true).setColor(BLACK, true);
            Iterator<Long> childAddresses = AnyPersistent.getChildAddressIterator(l);
            while (childAddresses.hasNext()) {
                long childAddr = childAddresses.next();
                AnyPersistent child = ObjectCache.get(childAddr, true);
                child.incRefCount();
                if (child.getColor() != BLACK) {
                    stack.push(childAddr);
                }
            }
        }
    }

    private static void collectCandidates() {
        PersistentConcurrentHashMapInternal.EntryIterator iter = candidatesSet.iter();
        while (iter.hasNext()) {
            PersistentConcurrentHashMapInternal.NodeLL node = iter.next();
            Long l = node.getKey();
            iter.remove();
            Deque<Long> stack = new ArrayDeque<>();
            stack.push(l);
            collectWhite(stack);
        }
    }

    private static void collectWhite(Deque<Long> stack) {
        while (!stack.isEmpty()) {
            Long l = stack.pop();
            if (!freedCandidates.contains(l)) {
                AnyPersistent obj = ObjectCache.get(l, true);
                if (obj.getColor() == WHITE && !candidatesSet.containsKey(l)) {
                    obj.setColor(BLACK, true);
                    Iterator<Long> childAddresses = AnyPersistent.getChildAddressIterator(l);
                    while (childAddresses.hasNext()) {
                        long childAddr = childAddresses.next();
                        stack.push(childAddr);
                    }
                    PersistentObject.free(l);
                    freedCandidates.add(l);
                    root.removeFromAllObjects(l);
                }
            }
        }
    }

    static void addCandidate(AnyPersistent obj) {
        long addr = obj.getPointer().addr();
        Transaction.run(() -> {
            if (obj.getColor() != PURPLE) {
                if (processing.get() == true) {
                    stashObjColorChange(addr, PURPLE);
                } else {
                    obj.setColor(PURPLE);
                }
                ((XRoot)(PersistentMemoryProvider.getDefaultProvider().getHeap().getRoot())).addToCandidates(addr);
            }
        });
    }

    static void addCandidate(long addr) {
        addCandidate(ObjectCache.get(addr, true));
    }

    static synchronized void removeFromCandidates(long addr) {
        ((XRoot)(PersistentMemoryProvider.getDefaultProvider().getHeap().getRoot())).removeFromCandidates(addr);
    }

    static boolean isProcessing() { return processing.get(); }
    static void stashObjColorChange(long addr, byte color) {
        synchronized(colorChangesWhileCollecting) {
            colorChangesWhileCollecting.put(addr, color);
        }
    }

    static synchronized boolean isCandidate(long addr) {
        return candidatesSet.containsKey(addr);
    }
}
