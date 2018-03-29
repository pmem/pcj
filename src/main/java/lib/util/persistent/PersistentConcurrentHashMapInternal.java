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

import lib.util.persistent.types.*;
import lib.util.persistent.spi.*;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.Collections;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import lib.xpersistent.UncheckedPersistentMemoryRegion;
// A long-to-long concurrent hashmap; no negative values are allowed.

public class PersistentConcurrentHashMapInternal {
    static final int INITIAL_SIZE_POWER = 14;
    static final int DEFAULT_INITIAL_CAPACITY = 1 << INITIAL_SIZE_POWER; // aka 16
    static final int MAXIMUM_CAPACITY = 1 << 30;
    static final int DEFAULT_RESIZE_THRESHOLD = 8;
    static final long TOMBSTONE_NODE_VALUE = -1L;
    static final long MARKER_NODE_VALUE = -2L;
    static final long SENTINEL_NODE_VALUE = -3L;
    static final long ERROR_RETURN_VALUE = -4L;

    // murmur3 hash constants
    static final int HASH_SEED = 1318007700;
    static final int c1 = 0xcc9e2d51;
    static final int c2 = 0x1b873593;
    static final int r1 = 15;
    static final int r2 = 13;
    static final int m = 5;
    static final int n = 0xe6546b64;

    private static final PersistentHeap heap = PersistentMemoryProvider.getDefaultProvider().getHeap();
    //private int size;

    private NodeLL head;
    private Table table;
    private int resizeThreshold;

    // All nodes are sorted based on the reversed version of the hash of the key (stored as the HASH field)
    public static class NodeLL {
        private static final long HASH_OFFSET = 0L;
        private static final long KEY_OFFSET = 4L;
        private static final long VALUE_OFFSET = 12L;
        private static final long NEXT_OFFSET = 20L;
        private static final long NODE_SIZE = 28L;

        private MemoryRegion reg;

        public NodeLL(int hash, long key, long value, NodeLL next) {
            this.reg = heap.allocateRegion(NODE_SIZE);
            reg.putRawInt(HASH_OFFSET, hash);
            reg.putRawLong(KEY_OFFSET, key);
            reg.putRawLong(VALUE_OFFSET, value);
            reg.putRawLong(NEXT_OFFSET, next == null ? 0 : next.addr());
            reg.flush(NODE_SIZE);
            if (Config.ENABLE_ALLOC_STATS) Stats.current.allocStats.update(NodeLL.class.getName(), NODE_SIZE,  16 + 64, 1);  // uncomment for allocation stats
        }

        protected NodeLL(long addr) {
            if (Config.ENABLE_ALLOC_STATS) Stats.current.allocStats.update(NodeLL.class.getName() + "<rctor>", 0, 16 + 64, 1);  // uncomment for allocation stats
            this.reg = new UncheckedPersistentMemoryRegion(addr);
        }

        public static NodeLL copyOf(NodeLL old) {
            if (old == null) return null;
            return new NodeLL(old.addr());
        }

        public final void changeAddr(long addr) { ((UncheckedPersistentMemoryRegion)this.reg).addr(addr); }
        public final long getKey() { return this.reg.getLong(KEY_OFFSET); }
        public final long getValue() { return this.reg.getLong(VALUE_OFFSET); }

        public final NodeLL getNext() { return this.reg.getLong(NEXT_OFFSET) == 0 ? null : new NodeLL(this.reg.getLong(NEXT_OFFSET)); }
        public final void getNext(NodeLL next) {
            if (getNextAddr() == 0) {
                next = null;
            } else {
                next.changeAddr(this.reg.getLong(NEXT_OFFSET));
            }
        }
        public final long getNextAddr() { return this.reg.getLong(NEXT_OFFSET); }
        final void setNextAddr(long nextAddr) { Transaction.run(() -> { this.reg.putLong(NEXT_OFFSET, nextAddr); }); }

        public final void setNext(NodeLL next) { Transaction.run(() -> { this.reg.putLong(NEXT_OFFSET, next == null? 0 : next.addr()); }); }

        public final int getHash() { return this.reg.getInt(HASH_OFFSET); }

        public final String toString() {
            return "(0x" + Integer.toHexString(getHash()) + ", 0x" + Long.toHexString(getKey()) + " = " + getValue() + "), Next addr 0x" + Long.toHexString(getNextAddr());
        }

        public final int hashCode() {
            return Long.hashCode(getKey()) ^ Long.hashCode(getValue());
        }

        public final long setValue(long newValue) {
            long oldValue = getValue();
            Transaction.run(() -> { this.reg.putLong(VALUE_OFFSET, newValue); });
            return oldValue;
        }

        public final boolean equals(Object o) {
            if (o == this) return true;
            if (o instanceof NodeLL) {
                NodeLL e = (NodeLL)o;
                if (getKey() == e.getKey() && getValue() == e.getValue()) return true;
            }
            return false;
        }

        public final long addr() { return this.reg.addr(); }
        public final void free() {
            Transaction.run(() -> {
                heap.freeRegion(this.reg);
            }, () -> {
                ((UncheckedPersistentMemoryRegion)this.reg).addr(0);
            });
        }

        public boolean isSentinel() { return ((this.getHash() & 0x1) == 0); }
    }

    final class Slot {
        NodeLL sentinel;
        AnyPersistent lock;

        Slot() {
            this.sentinel = null;
            this.lock = AnyPersistent.asLock();
            if (Config.ENABLE_ALLOC_STATS) Stats.current.allocStats.update(Slot.class.getName(), 0,  32, 1);  // uncomment for allocation stats
        }

        AnyPersistent getLock() {
            return this.lock;
        }

        void setSentinel(NodeLL sentinel) {
            this.sentinel = sentinel;
        }

        NodeLL getSlotSentinel() {
            return this.sentinel;
        }

        @Override
        public String toString() {
            return (getSlotSentinel() == null) ? "(null)" : getSlotSentinel().toString();
        }
    }

    final class Table {
        CopyOnWriteArrayList<Slot[]> tableList;
        AtomicInteger capacity;
        AtomicBoolean resizing;
        int initialCapacity;

        Table(int initialCapacity, NodeLL headSentinel) {
            this.tableList = new CopyOnWriteArrayList<Slot[]>();
            this.initialCapacity = initialCapacity;
            this.capacity = new AtomicInteger(0);

            addSlots(initialCapacity);
            ((tableList.get(0))[0]).setSentinel(headSentinel == null ? new NodeLL(makeSentinelKey(0), 0, SENTINEL_NODE_VALUE, null) : headSentinel);
            this.resizing = new AtomicBoolean(false);
            if (Config.ENABLE_ALLOC_STATS) Stats.current.allocStats.update(Table.class.getName(), 0,  28 + capacity.get() * 40, 1);  // uncomment for allocation stats
        }

        Table(int initialCapacity) {
            this(initialCapacity, null);
        }

        void setSlot(int index, Slot slot) {
            int slotIndex = Math.max(0, Integer.SIZE - Integer.numberOfLeadingZeros(index) - INITIAL_SIZE_POWER);
            if (slotIndex >= tableList.size()) expandToSlot(slotIndex);
            // System.out.println("setslot " + index + ", " + slot);
            // System.out.println("will be looking at slot[" + ((index < initialCapacity) ? index : index - Integer.highestOneBit(index)) + "]");
            (tableList.get(slotIndex))[(index < initialCapacity) ? index : (index - Integer.highestOneBit(index))] = slot;
        }

        Slot getSlot(int index, boolean rebuilding) {
            int slotIndex = Math.max(0, Integer.SIZE - Integer.numberOfLeadingZeros(index) - INITIAL_SIZE_POWER);
            if (slotIndex >= tableList.size()) {
                expandToSlot(slotIndex);
            }
            // System.out.println("Looking for slot for index " + index + ", slotIndex " + slotIndex);
            // System.out.println("will be looking at slot[" + (index - Integer.highestOneBit(index)) + "]");
            return (tableList.get(slotIndex))[(index < initialCapacity) ? index : (index - Integer.highestOneBit(index))];
        }

        void expandToSlot(int slotIndex) {
            int diff = slotIndex - (tableList.size() - 1);
            int startingSize = (tableList.size() == 1) ? this.initialCapacity : ((tableList.get(tableList.size() - 1).length) << 1);
            // System.out.println("starting size is " + startingSize);
            for (int i = 0; i < diff; i++) {
                addSlots(startingSize << i);
            }
        }

        void setSentinelAtSlot(int index, NodeLL sentinel) {
            getSlot(index, false).setSentinel(sentinel);
        }

        int getCapacity() { return this.capacity.get(); }
        boolean casCapacity(int expected, int newCapacity) { return this.capacity.compareAndSet(expected, newCapacity); }

        boolean isResizing() { return this.resizing.get(); }
        boolean setResizingIfFalse() { return this.resizing.compareAndSet(false, true); }
        void clearResizing() { this.resizing.set(false); }

        void addSlots(int size) {
            Slot[] newArray = new Slot[size];
            for (int i = 0; i < newArray.length; i++) {
                newArray[i] = new Slot();
            }
            this.tableList.add(newArray);
            this.capacity.addAndGet(size);
        }

        void debug(boolean verbose) {
            for (int i = 0; i < tableList.size(); i++) {
                Slot[] slots = tableList.get(i);
                System.out.println("Slots at index " + i);
                if (verbose) {
                    for (int j = 0; j < slots.length; j++) {
                        System.out.println("    Slot at index " + j + ": " + slots[j]);
                    }
                }
            }
        }
    }

    public class EntryIterator implements Iterator<NodeLL> {
        NodeLL prev;
        NodeLL next;
        NodeLL lastReturned;

        public EntryIterator() {
            prev = new NodeLL(head.addr());
            next = new NodeLL(head.addr());
            lastReturned = new NodeLL(0);
            while (next != null && next.getValue() == SENTINEL_NODE_VALUE) {
                if (next.getNextAddr() == 0) {
                    next = null;
                } else {
                    next.changeAddr(next.getNextAddr());
                }
            }
        }

        public boolean hasNext() {
            return next != null;
        }

        public NodeLL next() {
            //debugFromHead();
            if (lastReturned.addr() == 0) {
                lastReturned.changeAddr(next.addr());
            }
            lastReturned.changeAddr(next == null ? 0 : next.addr());
            //System.out.println("lastReturned is " + lastReturned);
            //System.out.println("prev is " + prev);
            while (prev.getNextAddr() != lastReturned.addr() && prev.getNextAddr() != 0) {
                prev.changeAddr(prev.getNextAddr());
                //System.out.println("prev is " + prev);
            }
            //System.out.println("next starts at " + next);
            do {
                if (next.getNextAddr() == 0) {
                    next = null;
                } else {
                    next.changeAddr(next.getNextAddr());
                    //System.out.println("next is now " + next);
                }
            } while (next != null && next.getValue() == SENTINEL_NODE_VALUE);
            return lastReturned;
        }

        public void remove() {
            if (lastReturned == null) return;
            //PersistentConcurrentHashMapInternal.this.remove(lastReturned.getKey());
            //System.out.println("removing " + lastReturned);
            prev.setNextAddr(lastReturned.getNextAddr());
            lastReturned.free();
            //System.out.println("after remove: prev is " + prev);
        }
    }

    public PersistentConcurrentHashMapInternal() {
        this.table = new Table(DEFAULT_INITIAL_CAPACITY);
        this.resizeThreshold = DEFAULT_RESIZE_THRESHOLD;
        this.head = getNode(0);
        //this.size = 0;
    }

    public PersistentConcurrentHashMapInternal(long addr) {
        this(addr, false);
    }

    public PersistentConcurrentHashMapInternal(long addr, boolean rebuild) {
        this.head = new NodeLL(addr);
        this.resizeThreshold = DEFAULT_RESIZE_THRESHOLD;
        if (rebuild) {
            this.table = new Table(DEFAULT_INITIAL_CAPACITY, this.head);
            NodeLL curr = this.head;
            while (curr != null) {
                if (curr.isSentinel()) {
                    this.table.getSlot(Integer.reverse(curr.getHash()), true).setSentinel(curr);
                }
                curr = curr.getNext();
            }
        }
        //this.size = size();
    }

    private int getCapacity() { return this.table.getCapacity(); }

    public long put(long key, long value) {
        return doPut(key, value, false, false);
    }

    public long increment(long key) {
        return doPut(key, 1, false, true);    // default value is 1
    }

    @SuppressWarnings("unchecked")
    private long doPut(long key, long value, boolean onlyIfAbsent, boolean increment) {
        final int hash = hash(key);
        long[] ret;

        while (true) {
            int slot = hash % table.getCapacity();
            // System.out.println("table length is " + table.length() + ", slot is " + slot);
            ret = Transaction.run(() -> {
                NodeLL sentinel = getSentinel(slot);
                // System.out.println("thread " + Thread.currentThread().getId() + " attempting to insert " + key + " from slot " + slot);
                return addOrUpdateNode(sentinel, hash, key, value, onlyIfAbsent, increment);
            }, table.getSlot(slot, false).getLock());

            if (ret[0] != ERROR_RETURN_VALUE) break;
        }
        // System.out.println("thread " + Thread.currentThread().getId() + " succeeded in inserting " + key);

        // System.out.format("while inserting (%d, %d), traversed %d nodes\n", key, value, ret[1]);
        if (ret[1] > this.resizeThreshold && table.getCapacity() < MAXIMUM_CAPACITY && table.isResizing() == false) {
            // System.out.println("insertion of key " + key + " triggered resize");
            resize();
        }
        return ret[0];
    }

    public long get(long key) {
        int hash = hash(key);
        while (true) {
            int slot = hash % table.getCapacity();
            long ret = Util.synchronizedBlock(table.getSlot(slot, false).getLock(), () -> {
                NodeLL sentinel = getSentinel(slot);
                return getValueFromSentinel(sentinel, hash, key);
            });
            if (ret != ERROR_RETURN_VALUE) return ret;
        }
    }

    public long remove(long key) {
        return doRemove(key, false);
    }

    public long decrement(long key) {
        return doRemove(key, true);
    }

    private long doRemove(long key, boolean decrement) {
        int hash = hash(key);
        long ret;
        while (true) {
            int slot = hash % table.getCapacity();
            ret = Transaction.run(() -> {
                NodeLL sentinel = getSentinel(slot);
                return removeNodeFromSentinel(sentinel, hash, key, decrement);
            }, table.getSlot(slot, false).getLock());
            if (ret != ERROR_RETURN_VALUE) break;
        }
        return ret;
    }

    public int size() {
        NodeLL curr = head;
        int size = 0;
        while (curr != null) {
            if (curr.getValue() != SENTINEL_NODE_VALUE) size++;
            curr = curr.getNext();
        }
        return size;
    }

    private void resize() {
        if (!table.setResizingIfFalse()) return;    // another thread is resizing
        table.addSlots(table.getCapacity());
        table.clearResizing();
    }

    private long[] addOrUpdateNode(NodeLL sentinel, int hash, long key, long value, boolean onlyIfAbsent, boolean increment) {
        // System.out.println("inserting node from sentinel " + sentinel);
        int sortedHash = makeRegularKey(hash);
        NodeLL curr = NodeLL.copyOf(sentinel);
        NodeLL next = curr.getNext();
        // System.out.println("curr is " + curr + ", next is " + next);
        long count = 0;
        while (true) {
            if (next != null) {
                int c;
                int nextHash = next.getHash();
                // System.out.println("comparing newNode's hash " + Integer.toHexString(sortedHash) + ", next hash " + Integer.toHexString(nextHash));
                if ((c = Integer.compareUnsigned(sortedHash, nextHash)) > 0) {
                    if ((nextHash & 0x1) == 0) {    // only sentinel nodes have even hashes
                        // System.out.println("Insertion stepping over sentinel!");
                        long[] ret = {ERROR_RETURN_VALUE, 0};
                        return ret;
                    }
                    // System.out.println("curr is " + curr + ", next is " + next);
                    curr.getNext(curr);
                    if (next.getNextAddr() == 0) {
                        next = null;
                    } else {
                        next.getNext(next);
                    }
                    // System.out.println("curr is " + curr + ", next is " + next);
                    count++;
                    continue;
                } else if (c == 0) {
                    if (next.getKey() != key) {
                        curr.getNext(curr);
                        if (next.getNextAddr() == 0) {
                            next = null;
                        } else {
                            next.getNext(next);
                        }
                        count++;
                        continue;
                    } else {
                        if (onlyIfAbsent) {    // do nothing if present
                            long[] ret = {next.getValue(), count};
                            return ret;
                        } else {
                            if (increment) {
                                long prevValue = next.getValue();
                                next.setValue(prevValue + 1);
                                long[] ret = {prevValue, count};
                                return ret;
                            } else {
                                long prevValue = next.getValue();
                                next.setValue(value);
                                long[] ret = {prevValue, count};
                                return ret;
                            }
                        }
                    }
                } else break;
            } else break;
        }
        NodeLL newNode = new NodeLL(sortedHash, key, value, null);
        /*if (((++size) % 100) == 0) {
            System.out.println("Current size of map at addr " + addr() + " is " + size);
        }*/
        newNode.setNext(next);
        curr.setNext(newNode);
        long[] ret = {-1, count};
        return ret;
    }

    private long getValueFromSentinel(NodeLL sentinel, int hash, long key) {
        // System.out.println("looking for key " + key + ", hash " + Long.toHexString(hash) + ", sentinel " + sentinel);
        int sortedHash = makeRegularKey(hash);
        NodeLL curr = NodeLL.copyOf(sentinel);
        while (true) {
            if (curr != null) {
                int c;
                // System.out.println("sortedHash " + Long.toHexString(sortedHash) + ", curr.getHash() = " + Long.toHexString(curr.getHash()));
                int currHash = curr.getHash();
                if ((c = Integer.compareUnsigned(sortedHash, currHash)) > 0) {
                    if (curr != sentinel && (currHash & 0x1) == 0) {    // only sentinel nodes have even hashes
                        // System.out.println("Retrieval stepping over sentinel!");
                        return ERROR_RETURN_VALUE;
                    }
                    if (curr.getNextAddr() == 0) {
                        curr = null;
                    } else {
                        curr.getNext(curr);
                    }
                    // System.out.println("too small, next");
                    continue;
                } else if (c == 0) {
                    if (curr.getKey() != key) {
                        if (curr.getNextAddr() == 0) {
                            curr = null;
                        } else {
                            curr.getNext(curr);
                        }
                        // System.out.println("key mismatch, next");
                        continue;
                    } else {
                        // System.out.println("found");
                        return curr.getValue();
                    }
                } else {
                    // System.out.println("too big, break");
                    break;
                }
            } else {
                // System.out.println("curr null, break");
                break;
            }
        }
        return -1;    // not found
    }

    private long removeNodeFromSentinel(NodeLL sentinel, int hash, long key, boolean decrement) {
        int sortedHash = makeRegularKey(hash);
        NodeLL curr = NodeLL.copyOf(sentinel);
        NodeLL next = curr.getNext();
        while (true) {
            if (next != null) {
                int c;
                int nextHash = next.getHash();
                if ((c = Integer.compareUnsigned(sortedHash, nextHash)) > 0) {
                    if ((nextHash & 0x1) == 0) {    // only sentinel nodes have even hashes
                        // System.out.println("Deletion stepping over sentinel!");
                        return ERROR_RETURN_VALUE;
                    }
                    curr.getNext(curr);
                    if (next.getNextAddr() == 0) {
                        next = null;
                    } else {
                        next.getNext(next);
                    }
                    continue;
                } else if (c == 0) {
                    if (next.getKey() != key) {
                        curr.getNext(curr);
                        if (next.getNextAddr() == 0) {
                            next = null;
                        } else {
                            next.getNext(next);
                        }
                        continue;
                    } else {
                        long prevValue = next.getValue();
                        if (decrement && prevValue != 1) {
                            next.setValue(prevValue - 1);
                            return prevValue;
                        }
                        curr.setNext(next.getNext());
                        next.free();
                        /*if (((--size) % 100) == 0) {
                            System.out.println("Current size of map at addr " + addr() + " is " + size);
                        }*/
                        return prevValue;
                    }
                } else break;
            } else break;
        }
        return -1;    // not found
    }

    // murmur3 hash
    private int hash(long key) {
        /*int h = HASH_SEED;
        int k1 = (int)(key & 0xffffffffL);
        int k2 = (int)((key & 0xffffffff00000000L) >>> 32);

        k1 *= c1;
        k1 = Integer.rotateLeft(k1, r1);
        k1 *= c2;
        h ^= k1;
        h = Integer.rotateLeft(h, r2);
        h *= (m + n);

        k2 *= c1;
        k2 = Integer.rotateLeft(k2, r1);
        k2 *= c2;
        h ^= k2;
        h = Integer.rotateLeft(h, r2);
        h *= (m + n);

        h ^= 64;   // len
        h ^= (h >> 16);
        h *= 0x853bca6b;
        h ^= (h >> 13);
        h *= 0xc2b2ae35;
        h ^= (h >> 16);

        return Math.abs(h);*/
        return Integer.hashCode((int)(key >>> 8));
    }

    private NodeLL getSentinel(int slot) {
        // String hexSlot = Long.toHexString(slot);
        // System.out.println("thread " + Thread.currentThread().getId() + " getSentinel 0x" + hexSlot);
        // debugFromHead();
        NodeLL sentinel = NodeLL.copyOf(getNode(slot));
        if (sentinel != null) {
            // System.out.println("thread " + Thread.currentThread().getId() + " found sentinel at slot 0x" + hexSlot + ", returning");
            return sentinel;
        }

        int sentinelKey = makeSentinelKey(slot);
        final int parentSlotIndex = findParentSlot(slot);
        final Slot parentSlot = table.getSlot(parentSlotIndex, false);
        final NodeLL parentSentinel = getSentinel(parentSlotIndex);        // guaranteed not to be null
        // System.out.println("thread " + Thread.currentThread().getId() + " trying to lock parentSlot 0x" + Long.toHexString(parentSlotIndex));
        return Transaction.run(() -> {
            // System.out.println("thread " + Thread.currentThread().getId() + " locked parentSlot 0x" + Long.toHexString(parentSlotIndex));
            NodeLL curr = NodeLL.copyOf(parentSentinel), next = curr.getNext(), ret = null;
            while (true) {
                if (next != null) {
                    int c;
                    // System.out.println("thread " + Thread.currentThread().getId() + " curr is " + curr + ", next is " + next);
                    // System.out.println("thread " + Thread.currentThread().getId() + " 0x" + Long.toHexString(sentinelKey));
                    // System.out.println("thread " + Thread.currentThread().getId() + " next addr 0x" + Long.toHexString(next.addr()));
                    // System.out.println("thread " + Thread.currentThread().getId() + " next hash 0x" + Long.toHexString(next.getHash()));
                    int nextHash = next.getHash();
                    if ((c = Integer.compareUnsigned(sentinelKey, nextHash)) > 0) {
                        if ((nextHash & 0x1) == 0) {
                            int slotToAcquire = Integer.reverse(nextHash);
                            // System.out.println("thread " + Thread.currentThread().getId() + " encountered new slot while walking to insert sentinel, acquiring lock on slot 0x" + Integer.toHexString(slotToAcquire));
                            Transaction.run(() -> {}, table.getSlot(slotToAcquire, false).getLock());
                            // System.out.println("walking over sentinel when inserting new sentinel!!");
                            // System.out.println("thread " + Thread.currentThread().getId() + " acquired lock on slot 0x" + Integer.toHexString(slotToAcquire));
                        }
                        // curr = next;
                        // next = next.getNext();
                        // curr.changeAddr(next.addr());
                        // next.changeAddr(next.getNextAddr());
                    curr.getNext(curr);
                    if (next.getNextAddr() == 0) {
                        next = null;
                    } else {
                        next.getNext(next);
                    }
                        continue;
                    } else if (c == 0) {
                        // System.out.println("thread " + Thread.currentThread().getId() + " just got slot 0x" + hexSlot);
                        ret = next;
                        setNodeIfNull(slot, next);
                        break;
                    } else break;
                } else break;
            }
            if (ret == null) {
                final NodeLL nextF = next, currF = curr;
                Box<NodeLL> retBox = new Box<>();
                Transaction.runOuter(() -> {
                    NodeLL newSentinel = new NodeLL(sentinelKey, slot, SENTINEL_NODE_VALUE, nextF);
                    retBox.set(newSentinel);
                    currF.setNext(newSentinel);
                });
                ret = retBox.get();
                setNodeIfNull(slot, curr.getNext());
                // System.out.println("thread " + Thread.currentThread().getId() + " inserted new slot 0x" + hexSlot + ", curr is " + curr + ", new node is " + curr.getNext());
            }
            return ret;
        }, parentSlot.getLock());
    }

    private int makeSentinelKey(int key) {
        return Integer.reverse(key);
    }

    private int makeRegularKey(int key) {
        return Integer.reverse(key | 0x8000_0000);
    }

    // Per paper: unset the most significant bit that is set
    private int findParentSlot(int slot) {
        return slot - Integer.highestOneBit(slot);
        /*long sentinelHash = (long)makeSentinelKey(slot) & 0xffffffffL;
        long decrement = (long)(1 << (32 - Integer.numberOfTrailingZeros(table.getCapacity()))) & 0xffffffffL;
        long decrementedHash = sentinelHash;
        // System.out.println("sentinelHash is " + Long.toHexString(sentinelHash) + ", decrement is " + Long.toHexString(decrement));
        while (true) {
            decrementedHash = decrementedHash - decrement;
            // System.out.println("decrementedHash is " + Long.toHexString(decrementedHash));
            int parentSlot = Integer.reverse((int)decrementedHash);
            // System.out.println("parent slot is " + parentSlot);
            if (getNode(parentSlot) != null) return parentSlot;
        }*/
    }

    private NodeLL setNodeIfNull(int index, NodeLL node) {
        Slot slot = table.getSlot(index, false);
        // System.out.println("thread " + Thread.currentThread().getId() + " to set sentinel for index 0x" + Long.toHexString(index));
        NodeLL ret;
        // Transaction.run(() -> {
            if (slot.getSlotSentinel() == null) {
                // System.out.println("thread " + Thread.currentThread().getId() + " null, so sentinel is now " + node);
                table.setSentinelAtSlot(index, node);
                ret = node;
            } else {
                // System.out.println("thread " + Thread.currentThread().getId() + " not null, old sentinel is " + slot.getSlotSentinel());
                ret = slot.getSlotSentinel();
            }
        // }, slot.getLock());
        return ret;
    }

    private NodeLL getNode(int index) {
        Slot slot = table.getSlot(index, false);
        return slot.getSlotSentinel();
    }

    public void debugFromHead() {
        NodeLL curr = new NodeLL(head.addr());
        System.out.println("curr is " + curr);
        StringBuilder sb = new StringBuilder();
        int prevHash = 0;
        while (curr.addr() != 0) {
            if (Integer.compareUnsigned(curr.getHash(), prevHash) < 0) System.out.println("ERROR in map: hash out of order: previous hash 0x" + Integer.toHexString(prevHash) + ", current hash 0x" + Integer.toHexString(curr.getHash()));
            prevHash = curr.getHash();
            sb.append("(node @ " + Long.toHexString(curr.addr()) + ": hash 0x" + Integer.toHexString(curr.getHash()) + ", key 0x" + Long.toHexString(curr.getKey()) + ", value " + curr.getValue() + ", next addr 0x" + Long.toHexString(curr.getNextAddr()) + ")->\n");
            curr.changeAddr(curr.getNextAddr());
        }
        sb.append("(null)");
        System.out.println(sb.toString());
    }

    public void debugTable() { table.debug(false); }
    public void debugTable(boolean verbose) { table.debug(verbose); }

    public long addr() { return head.addr(); }

    public EntryIterator iter() { return new EntryIterator(); }

    public void delete(boolean removeHead) {
        NodeLL curr = new NodeLL(head.getNextAddr());
        long nextAddr;
        while (curr.addr() != 0) {
            nextAddr = curr.getNextAddr();
            Transaction.run(() -> {
                head.setNextAddr(curr.getNextAddr());
                curr.free();
            });
            curr.changeAddr(nextAddr);
        }
        if (removeHead) deleteHead();
    }

    public void delete() {
        delete(true);
    }

    public void deleteHead() {
        head.free();
    }

    public boolean containsKey(long key) {
        EntryIterator iter = iter();
        while (iter.hasNext()) {
            NodeLL curr = iter.next();
            if (curr.getKey() == key) return true;
        }
        return false;
    }

    /*static class ElemAndSentinel implements Comparable<ElemAndSentinel> {
        long[] arr;
        ElemAndSentinel(long sentinelHash, long elem) {
            this.arr = new long[2];
            this.arr[0] = sentinelHash;
            this.arr[1] = elem;
        }
        public int compareTo(ElemAndSentinel that) {
            return Long.compareUnsigned(this.arr[0], that.arr[0]);
        }
    }

    public void forEachInSentinelOrder(List<Long> list, Consumer<Long> task) {
        ArrayList<ElemAndSentinel> sortingList = new ArrayList<>();
        while (!table.setResizingIfFalse());    // proceed when we are not resizing
        for (Long l : list) {
            int sentinelSlot = hash(l) % table.getCapacity();
            long sentinelHash = (long)makeSentinelKey(sentinelSlot) & 0xffffffffL;
            sortingList.add(new ElemAndSentinel(sentinelHash, l));
        }
        Collections.sort(sortingList);
        for (ElemAndSentinel e : sortingList) {
            // System.out.println("original value " + e.arr[1] + ", key is " + Long.toHexString(e.arr[0]));
            task.accept(e.arr[1]);
            debugFromHead();
        }
    }*/
}
