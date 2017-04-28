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
import lib.util.persistent.types.ObjectType;
import lib.util.persistent.types.ArrayType;
import lib.util.persistent.types.Types;
import lib.xpersistent.XRoot;
import lib.xpersistent.XHeap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Deque;
import java.util.ArrayDeque;

public class CycleCollector {

    public static final byte BLACK = 0;
    public static final byte PURPLE = 1;
    public static final byte GREY = 2;
    public static final byte WHITE = 3;

    static HashSet<Long> candidatesSet = new HashSet<>();
    static XHeap heap;
    static XRoot root;

    public static synchronized HashSet<Long> getCandidates() { return candidatesSet; }

    public static synchronized void collect() {
        heap = ((XHeap)(PersistentMemoryProvider.getDefaultProvider().getHeap()));
        root = ((XRoot)(heap.getRoot()));
        markCandidates();
        for (Long l : candidatesSet) {
            Deque<Long> stack = new ArrayDeque<>();
            stack.push(l);
            scan(stack);
        }
        collectCandidates();
        root.clearCandidates();
    }

    private static void markCandidates() {
        Iterator<Long> it = candidatesSet.iterator();
        while (it.hasNext()) {
            Long l = it.next();
            if (PersistentObject.getColor(l) == PURPLE) {
                Deque<Long> stack = new ArrayDeque<>();
                stack.push(l);
                markGrey(stack);
            } else {
                it.remove();
                if (PersistentObject.getColor(l) == BLACK && PersistentObject.getRefCount(l) == 0) {
                    PersistentObject.free(l);
                    root.removeFromAllObjects(l);
                }
            }
        }
    }

    private static void markGrey(Deque<Long> stack) {
        while (!stack.isEmpty()) {
            Long l = stack.pop();
            if (PersistentObject.getColor(l) != GREY) {
                Iterator<Long> childAddresses = PersistentObject.getChildAddressIterator(heap.regionFromAddress(l));
                while (childAddresses.hasNext()) {
                    long childAddr = childAddresses.next();
                    PersistentObject.decRefCount(childAddr);
                    stack.push(childAddr);
                }
                PersistentObject.setColor(l, GREY);
            }
        }
    }

    private static void scan(Deque<Long> stack) {
        while (!stack.isEmpty()) {
            Long l = stack.pop();
            if (PersistentObject.getColor(l) == GREY) {
                if (PersistentObject.getRefCount(l) > 0) {
                    Deque<Long> scanBlackStack = new ArrayDeque<>();
                    scanBlackStack.push(l);
                    scanBlack(scanBlackStack);
                } else {
                    Iterator<Long> childAddresses = PersistentObject.getChildAddressIterator(heap.regionFromAddress(l));
                    while (childAddresses.hasNext()) {
                        long childAddr = childAddresses.next();
                        stack.push(childAddr);
                    }
                    PersistentObject.setColor(l, WHITE);
                }
            }
        }
    }

    private static void scanBlack(Deque<Long> stack) {
        while (!stack.isEmpty()) {
            Long l = stack.pop();
            PersistentObject.setColor(l, BLACK);
            Iterator<Long> childAddresses = PersistentObject.getChildAddressIterator(heap.regionFromAddress(l));
            while (childAddresses.hasNext()) {
                long childAddr = childAddresses.next();
                PersistentObject.incRefCount(childAddr);
                if (PersistentObject.getColor(childAddr) != BLACK) {
                    stack.push(childAddr);
                }
            }
        }
    }

    private static void collectCandidates() {
        Iterator<Long> it = candidatesSet.iterator();
        while (it.hasNext()) {
            Long l = it.next();
            it.remove();
            Deque<Long> stack = new ArrayDeque<>();
            stack.push(l);
            collectWhite(stack);
        }
    }

    private static void collectWhite(Deque<Long> stack) {
        while (!stack.isEmpty()) {
            Long l = stack.pop();
            if (PersistentObject.getColor(l) == WHITE && !candidatesSet.contains(l)) {
                PersistentObject.setColor(l, BLACK);
                Iterator<Long> childAddresses = PersistentObject.getChildAddressIterator(heap.regionFromAddress(l));
                while (childAddresses.hasNext()) {
                    long childAddr = childAddresses.next();
                    stack.push(childAddr);
                }
                PersistentObject.free(l);
                root.removeFromAllObjects(l);
            }
        }
    }

    static synchronized void addCandidate(long addr) {
        if (PersistentObject.getColor(addr) != PURPLE) {
            PersistentObject.setColor(addr, PURPLE);
            candidatesSet.add(addr);
            ((XRoot)(PersistentMemoryProvider.getDefaultProvider().getHeap().getRoot())).addToCandidates(addr);
        }
    }

    static synchronized boolean isCandidate(long addr) {
        return candidatesSet.contains(new Long(addr));
    }
}
