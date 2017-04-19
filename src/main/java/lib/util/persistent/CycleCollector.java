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
            Deque<PersistentObject> stack = new ArrayDeque<PersistentObject>();
            stack.push(PersistentObject.getObjectAtAddress(l));
            scan(stack);
        }
        collectCandidates();
        root.clearCandidates();
    }

    private static void markCandidates() {
        Iterator<Long> it = candidatesSet.iterator();
        while (it.hasNext()) {
            Long l = it.next();
            PersistentObject ref = PersistentObject.getObjectAtAddress(l);
            if (ref.refColor() == PURPLE) {
                Deque<PersistentObject> stack = new ArrayDeque<PersistentObject>();
                stack.push(ref);
                markGrey(stack);
            } else {
                it.remove();
                if (ref.refColor() == BLACK && ref.getRefCount() == 0) {
                    ref.free();
                    root.removeFromAllObjects(ref.getPointer().region().addr());
                }
            }
        }
    }

    private static void markGrey(Deque<PersistentObject> stack) {
        while (!stack.isEmpty()) {
            PersistentObject ref = (PersistentObject)stack.pop();
            if (ref.refColor() != GREY) {
                forEachChild(ref, (PersistentObject child) -> {
                    stack.push(child);
                });
                ref.refColor(GREY);
            }
        }
    }

    private static void scan(Deque<PersistentObject> stack) {
        while (!stack.isEmpty()) {
            PersistentObject ref = (PersistentObject)stack.pop();
            if (ref.refColor() == GREY) {
                if (ref.getRefCount() > 0) {
                    Deque<PersistentObject> scanBlackStack = new ArrayDeque<PersistentObject>();
                    scanBlackStack.push(ref);
                    scanBlack(scanBlackStack);
                } else {
                    forEachChild(ref, (PersistentObject child) -> stack.push(child));
                    ref.refColor(WHITE);
                }
            }
        }
    }

    private static void scanBlack(Deque<PersistentObject> stack) {
        while (!stack.isEmpty()) {
            PersistentObject ref = stack.pop();
            ref.refColor(BLACK);
            forEachChild(ref, (PersistentObject child) -> {
                child.speculativeIncRefCountBy(1);
                if (child.refColor() != BLACK) {
                    stack.push(child);
                }
            });
        }
    }

    private static void collectCandidates() {
        Iterator<Long> it = candidatesSet.iterator();
        while (it.hasNext()) {
            PersistentObject ref = PersistentObject.getObjectAtAddress(it.next());
            it.remove();
            Deque<PersistentObject> stack = new ArrayDeque<PersistentObject>();
            stack.push(ref);
            collectWhite(stack);
        }
    }

    private static void collectWhite(Deque<PersistentObject> stack) {
        while (!stack.isEmpty()) {
            PersistentObject ref = stack.pop();
            if (ref.refColor() == WHITE && !candidatesSet.contains(new Long(ref.getPointer().region().addr()))) {
                ref.refColor(BLACK);
                forEachChild(ref, (PersistentObject child) -> stack.push(child));
                ref.free();
                root.removeFromAllObjects(ref.getPointer().region().addr());
            }
        }
    }

    static synchronized void candidate(PersistentObject ref) {
        if (ref.refColor() != PURPLE) {
            ref.refColor(PURPLE);
            candidatesSet.add(ref.getPointer().region().addr());
            ((XRoot)(PersistentMemoryProvider.getDefaultProvider().getHeap().getRoot())).addToCandidates(ref.getPointer().region().addr());
        }
    }

    static synchronized boolean isCandidate(long addr) {
        return candidatesSet.contains(new Long(addr));
    }

    static synchronized void markBlack(PersistentObject ref) {
        ref.refColor(BLACK);
    }

    static interface Update {
        public void run(PersistentObject child);
    }

    static synchronized void forEachChild(PersistentObject ref, Update update) {
        if (!(ref.getPointer().type() instanceof ArrayType)) {
            for (int i = 0; i < ref.getPointer().type().fieldCount(); i++) {
                if (!(((ObjectType<?>)(ref.getPointer().type())).getTypes().get(i) instanceof ObjectType || ((ObjectType<?>)(ref.getPointer().type())).getTypes().get(i) == Types.OBJECT)) continue;
                if (ref.getLong(ref.getPointer().type().getOffset(i)) == 0) continue;
                PersistentObject child = PersistentObject.getObjectAtAddress(ref.getLong(ref.getPointer().type().getOffset(i)));
                if (child != null) {
                    update.run(child);
                }
            }
        } else {
            AbstractPersistentImmutableArray arr = (AbstractPersistentImmutableArray)ref;
            if (((ArrayType)arr.getPointer().type()).getElementType() == Types.OBJECT) {
                for (int j = 0; j < arr.length(); j++) {
                    long target = arr.getLong(arr.elementOffset(j));
                    if (target != 0) {
                        PersistentObject child = PersistentObject.getObjectAtAddress(target);
                        if (child != null) {
                            update.run(child);
                        }
                    }
                }
            }
        }
    }
}
