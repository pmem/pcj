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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.Spliterator;
import java.util.AbstractSet;
import java.util.AbstractCollection;
import java.util.function.Consumer;
import java.util.function.BiFunction;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.NoSuchElementException;

public class PersistentSIHashMap<K extends AnyPersistent, V extends AnyPersistent> extends PersistentObject implements PersistentMap<K, V> {
    static final int INITIAL_SIZE_POWER = 4;
    static final int DEFAULT_INITIAL_CAPACITY = 1 << INITIAL_SIZE_POWER; // aka 16
    static final int MAXIMUM_CAPACITY = 1 << 30;
    static final int DEFAULT_RESIZE_THRESHOLD = 8;

    static final long ERROR_VALUE_CONST = -1L;

    private Set<Map.Entry<K, V>> entrySet;
    private Set<K> keySet;
    private Collection<V> values;

    private static Statics statics;
    private static final IntField RESIZE_THRESHOLD = new IntField();
    private static final ObjectField<Table> TABLE = new ObjectField<>();
    public static final ObjectType<PersistentSIHashMap> TYPE = ObjectType.withFields(PersistentSIHashMap.class, RESIZE_THRESHOLD, TABLE);

    static {
        statics = ObjectDirectory.get("PersistentSIHashMap_statics", Statics.class);
        if (statics == null)
            ObjectDirectory.put("PersistentSIHashMap_statics", statics = new Statics());
    }

    public static final class Statics extends PersistentObject {
        private static final ObjectField<PersistentLong> ERROR_VALUE = new ObjectField<>();
        public static final ObjectType<Statics> TYPE = ObjectType.withFields(Statics.class, ERROR_VALUE);

        public Statics() {
            super(TYPE);
            setObjectField(ERROR_VALUE, new PersistentLong(ERROR_VALUE_CONST));
        }

        public Statics (ObjectPointer<Statics> p) { super(p); }

        public PersistentLong errorValue() { return getObjectField(ERROR_VALUE); }
    }

    static final class Table extends PersistentObject {
        private static final ObjectField<PersistentArrayList<PersistentArray<Node>>> ARRAY = new ObjectField<>();
        private static final IntField CAPACITY = new IntField();
        private static final IntField INITIAL_CAPACITY = new IntField();
        private static final ObjectType<Table> TYPE = ObjectType.withFields(Table.class, ARRAY, CAPACITY, INITIAL_CAPACITY);

        AtomicBoolean resizing = new AtomicBoolean(false);
        ExecutorService executor = Executors.newSingleThreadExecutor();

        Table() {
            this(DEFAULT_INITIAL_CAPACITY);
        }

        Table(int initialCapacity) {
            super(TYPE);

            PersistentArrayList<PersistentArray<Node>> tableArray = new PersistentArrayList<>();
            setObjectField(ARRAY, tableArray);

            addSlots(initialCapacity);
            setInitialCapacity(initialCapacity);
        }

        Table(ObjectPointer<Table> p) {
            super(p);
        }

        boolean setResizingIfFalse() { return this.resizing.compareAndSet(false, true); }
        boolean getResizing() { return this.resizing.get(); }
        void clearResizing() { this.resizing.set(false); }

        void setInitialCapacity(int cap) { setIntField(INITIAL_CAPACITY, cap); }
        int getInitialCapacity() { return getIntField(INITIAL_CAPACITY); }

        void setCapacity(int cap) { setIntField(CAPACITY, cap); }
        int getCapacity() { return getIntField(CAPACITY); }

        @SuppressWarnings("unchecked")
        PersistentArrayList<PersistentArray<Node>> getTableArray() { return (PersistentArrayList<PersistentArray<Node>>)getObjectField(ARRAY); }

        void setSlot(int index, Node sentinel) {
            Transaction.run(() -> {
                int initialCapacity = getInitialCapacity();
                int slotIndex = Math.max(0, Integer.SIZE - Integer.numberOfLeadingZeros(index) - INITIAL_SIZE_POWER);
                int diff;
                if ((diff = slotIndex - (getTableArray().size() - 1)) > 0) {
                    int startingSize = (getTableArray().size() == 1) ? initialCapacity : ((getTableArray().get(getTableArray().size() - 1).length()) << 1);
                    for (int i = 0; i < diff; i++) {
                        addSlots(startingSize << i);
                    }
                }
                (getTableArray().get(slotIndex)).set((index < initialCapacity) ? index : (index - Integer.highestOneBit(index)), sentinel);
            });
        }

        Node getSlot(int index) {
            int slotIndex = Math.max(0, Integer.SIZE - Integer.numberOfLeadingZeros(index) - INITIAL_SIZE_POWER);
            return getTableArray().get(slotIndex).get((index < getInitialCapacity()) ? index : (index - Integer.highestOneBit(index)));
        }

        void addSlots(int size) {
            Transaction.run(() -> {
                PersistentArray<Node> sentinelArray = new PersistentArray<>(size);
                getTableArray().add(sentinelArray);
                // debug();
                setIntField(CAPACITY, getIntField(CAPACITY) + size);
            });
        }

        void resize() {
            if (!getResizing() && setResizingIfFalse()) {
                try {
                // executor.submit(() -> {
                    addSlots(getCapacity());
                // });
                } finally {
                    clearResizing();
                }
            }
        }

        void debug() {
            for (int i = 0; i < getTableArray().size(); i++) {
                PersistentArray<Node> slots = getTableArray().get(i);
                System.out.println("Slots at index " + i);
                for (int j = 0; j < slots.length(); j++) {
                    System.out.println("    Slot at index " + j + ": " + slots.get(j));
                }
            }
        }
    }

    static final class Node<K extends AnyPersistent, V extends AnyPersistent> extends PersistentObject implements Map.Entry<K, V> {
        private static final LongField HASH = new LongField();
        private static final ObjectField<AnyPersistent> KEY = new ObjectField<>();
        private static final ObjectField<AnyPersistent> VALUE = new ObjectField<>();
        private static final ObjectField<Node> NEXT = new ObjectField<>();
        private static final ObjectType<Node> TYPE = ObjectType.withFields(Node.class, HASH, KEY, VALUE, NEXT);

        Node(long hash, K key, V value, Node next) {
            super(TYPE);
            setLongField(HASH, hash);
            setObjectField(KEY, key);
            setObjectField(VALUE, value);
            setNext(next);
        }

        Node(ObjectPointer<Node> p) { super(p); }

        @SuppressWarnings("unchecked")
        public final K getKey() { return (K)getObjectField(KEY); }
        @SuppressWarnings("unchecked")
        public final V getValue() { return (V)getObjectField(VALUE); }

        @SuppressWarnings("unchecked")
        final Node<K, V> getNext() { return (Node<K, V>)getObjectField(NEXT); }
        final void setNext(Node next) { setObjectField(NEXT, next); }

        final long getHash() { return getLongField(HASH); }

        public final String toString() {
            return "0x" + Long.toHexString(getHash()) + ", (" + getKey() + " = " + getValue() + ")";
        }

        public final int hashCode() {
            return getKey().hashCode() ^ getValue().hashCode();
        }

        public final V setValue(V newValue) {
            Box<V> ret = new Box<>();
            Transaction.run(() -> {
                V oldValue = getValue();
                setObjectField(VALUE, newValue);
                ret.set(oldValue);
            });
            return ret.get();
        }

        public final boolean equals(Object o) {
            if (o == this) return true;
            if (o instanceof Map.Entry) {
                Map.Entry<?,?> e = (Map.Entry<?,?>)o;
                return Util.synchronizedBlock(this, () -> {
                    if (getKey().equals(e.getKey()) && getValue().equals(e.getValue())) return true;
                    else return false;
                });
            }
            return false;
        }

        final boolean isSentinel() { return (getHash() & 0x1) == 0; }
    }

    final class EntrySet extends AbstractSet<Map.Entry<K, V>> {
        public final int size() { return PersistentSIHashMap.this.size(); }
        public final void clear() { /* NIY */ }
        public final Iterator<Map.Entry<K, V>> iterator() {
            return new EntryIterator();
        }
        public final boolean contains(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<?,?> e = (Map.Entry<?,?>)o;
            Object key = e.getKey();
            @SuppressWarnings("unchecked") Object value = PersistentSIHashMap.this.get((K)key);
            return value != null && value.equals(e.getValue());
        }
        public final boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }
        public final Spliterator<Map.Entry<K, V>> spliterator() {
            throw new UnsupportedOperationException();
        }
        public synchronized final void forEach(Consumer<? super Map.Entry<K, V>> action) {
            if (action == null)
                throw new NullPointerException();
            Transaction.run(() -> {
                @SuppressWarnings("unchecked") Node<K, V> curr = getHead();
                while (curr != null) {
                    if (curr.getValue() != null && !(curr.isSentinel()))
                        action.accept(curr);
                    curr = curr.getNext();
                }
            });
        }
    }

    final class Values extends AbstractCollection<V> {
        public synchronized final int size() { return PersistentSIHashMap.this.size(); }
        public final void clear() { /* NIY */ }
        public final Iterator<V> iterator() { return new ValueIterator(); }
        public synchronized final boolean contains(Object o) { return containsValue(o); }
        public final Spliterator<V> spliterator() {
            throw new UnsupportedOperationException();
        }
        public synchronized final void forEach(Consumer<? super V> action) {
            if (action == null)
                throw new NullPointerException();
            Transaction.run(() -> {
                @SuppressWarnings("unchecked") Node<K, V> curr = getHead();
                while (curr != null) {
                    if (curr.getValue() != null && !curr.isSentinel())
                        action.accept(curr.getValue());
                    curr = curr.getNext();
                }
            });
        }
    }

    final class KeySet extends AbstractSet<K> {
        public synchronized final int size() { return PersistentSIHashMap.this.size(); }
        public final void clear() { /* NIY */ }
        public final Iterator<K> iterator() { return new KeyIterator(); }
        public synchronized final boolean contains(Object o) { return containsKey(o); }
        public final Spliterator<K> spliterator() {
            throw new UnsupportedOperationException();
        }
        public synchronized final void forEach(Consumer<? super K> action) {
            if (action == null)
                throw new NullPointerException();
            Transaction.run(() -> {
                @SuppressWarnings("unchecked") Node<K, V> curr = getHead();
                while (curr != null) {
                    if (curr.getValue() != null && !(curr.isSentinel()))
                        action.accept(curr.getKey());
                    curr = curr.getNext();
                }
            });
        }
    }

    final class EntryIterator extends HashIterator implements Iterator<Map.Entry<K, V>> {
        public final Map.Entry<K, V> next() { return nextNode(); }
    }

    final class KeyIterator extends HashIterator implements Iterator<K> {
        public final K next() { return nextNode().getKey(); }
    }

    final class ValueIterator extends HashIterator implements Iterator<V> {
        public final V next() { return nextNode().getValue(); }
    }

    abstract class HashIterator {
        Node<K, V> next, lastReturned;
        V nextValue;

        @SuppressWarnings("unchecked")
        HashIterator() {
            next = getHead();
            while (next != null) {
                AnyPersistent x = next.getValue();
                if (x != null && x != next && !(next.isSentinel())) {
                    @SuppressWarnings("unchecked") V vv = (V)x;
                    nextValue = vv;
                    break;
                }
                next = next.getNext();
            }
        }

        public final boolean hasNext() {
            return next != null;
        }

        final Node<K, V> nextNode() {
            if (next == null) throw new NoSuchElementException();
            Node<K, V> e = next, lastReturned = e;
            while ((next = next.getNext()) != null) {
                AnyPersistent x = next.getValue();
                if (x != null && !(next.isSentinel())) {
                    @SuppressWarnings("unchecked") V vv = (V)x;
                    nextValue = vv;
                    break;
                }
            }
            return e;
        }

        public final void remove() {
            Node<K, V> p = lastReturned;
            if (p != null) {
                lastReturned = null;
                PersistentSIHashMap.this.remove(p.getKey());
            }
        }
    }

    public PersistentSIHashMap() {
        this(DEFAULT_INITIAL_CAPACITY, DEFAULT_RESIZE_THRESHOLD);
    }

    public PersistentSIHashMap(int initialCapacity, int resizeThreshold) {
        super(TYPE);
        setObjectField(TABLE, new Table(initialCapacity));
        setIntField(RESIZE_THRESHOLD, resizeThreshold);
        setNode(0, new Node<K, V>(0, null, null, null));
    }

    public PersistentSIHashMap(ObjectPointer<? extends PersistentSIHashMap> p) {
        super(p);
    }

    private Table getTable() { return getObjectField(TABLE); }
    private int getResizeThreshold() { return getIntField(RESIZE_THRESHOLD); }

    public V put(K key, V value) {
        return doPut(key, value, false);
    }

    public V putIfAbsent(K key, V value) {
        return doPut(key, value, true);
    }

    @SuppressWarnings("unchecked")
    private V doPut(K key, V value, boolean onlyIfAbsent) {
        if (key == null || value == null) throw new NullPointerException();
        final int hash = hash(key);
        Box<Object[]> ret = new Box<>();

        while (true) {
            int slot = hash % getTable().getCapacity();
            final Node<K, V> sentinel = getSentinel(slot);
            // System.out.println("table length is " + getTable().getCapacity() + ", slot is " + slot);
            Transaction.run(() -> {
                // System.out.println("thread " + Thread.currentThread().getId() + " attempting to insert " + key + " from slot " + slot);
                ret.set(addOrUpdateNode(sentinel, hash, key, value, onlyIfAbsent));
            }, sentinel);

            if (ret.get()[0] != statics.errorValue()) break;
        }
        // System.out.println("thread " + Thread.currentThread().getId() + " succeeded in inserting " + key);

        // System.out.format("while inserting (%d, %d), traversed %d nodes\n", key, value, ret[1]);
        if ((Long)((ret.get())[1]) > getResizeThreshold() && getTable().getCapacity() < MAXIMUM_CAPACITY && getTable().getResizing() == false) {
        //if (((float)size() / (float)(table.length()) > LINKED_LIST_THRESHOLD)) {
            // System.out.println("insertion of key " + key + " triggered resize");
            getTable().resize();
        }
        //return ret[0];
        return (V)((ret.get())[0]);
    }

    private Object[] addOrUpdateNode(Node<K, V> sentinel, long hash, K key, V value, boolean onlyIfAbsent) {
        // System.out.println("inserting (" + key + ", " + value + " from sentinel " + sentinel);
        long sortedHash = makeRegularKey(hash);
        Node<K, V> curr = sentinel;
        Node<K, V> next = curr.getNext();
        long count = 0;
        while (true) {
            if (next != null) {
                int c;
                long nextHash = next.getHash();
                // System.out.println("comparing newNode's hash " + Long.toHexString(sortedHash) + ", next hash " + Long.toHexString(nextHash));
                if ((c = Long.compareUnsigned(sortedHash, nextHash)) > 0) {
                    if ((nextHash & 0x1) == 0) {    // only sentinel nodes have even hashes
                        // System.out.println("Insertion stepping over sentinel!");
                        Object[] ret = {statics.errorValue(), 0};
                        return ret;
                    }
                    curr = next;
                    next = next.getNext();
                    count++;
                    continue;
                } else if (c == 0) {
                    if (!(next.getKey().equals(key))) {
                        curr = next;
                        next = next.getNext();
                        count++;
                        continue;
                    } else {
                        if (onlyIfAbsent) {    // do nothing if present
                            Object[] ret = {next.getValue(), count};
                            return ret;
                        } else {
                            Object prevValue = next.getValue();
                            next.setValue(value);
                            Object[] ret = {prevValue, count};
                            return ret;
                        }
                    }
                } else break;
            } else break;
        }
        Node<K, V> newNode = new Node<>(sortedHash, key, value, next);
        curr.setNext(newNode);
        Object[] ret = {null, count};
        return ret;
    }

    public V get(Object key) {
        if (key == null) throw new NullPointerException();
        int hash = hash(key);
        while (true) {
            int slot = hash % getTable().getCapacity();
            Node sentinel = getSentinel(slot);
            Object ret = Util.synchronizedBlock(sentinel, () -> {
                return getValueFromSentinel(sentinel, hash, key);
            });
            if (ret != statics.errorValue()) {
                @SuppressWarnings("unchecked") V vv = (V)ret;
                return vv;
            }
            // @SuppressWarnings("unchecked")
            // if (ret == null || !(ret instanceof PersistentLong) || (PersistentLong)ret != statics.errorValue()) return (V)ret;
        }
    }

    public <L, K extends EquatesWith<L>> V get(L key, Class<K> cls) {
        if (key == null) throw new NullPointerException();
        int hash = hash(key);
        while (true) {
            int slot = hash % getTable().getCapacity();
            Node sentinel = getSentinel(slot);
            Object ret = Util.synchronizedBlock(sentinel, () -> {
                return getValueFromSentinel(sentinel, hash, key, cls);
            });
            if (ret != statics.errorValue()) {
                @SuppressWarnings("unchecked") V vv = (V)ret;
                return vv;
            }
            // @SuppressWarnings("unchecked")
            // if (ret == null || !(ret instanceof PersistentLong) || (PersistentLong)ret != statics.errorValue()) return (V)ret;
        }
    }

    private Object getValueFromSentinel(Node sentinel, long hash, Object key) {
        // System.out.println("looking for key " + key + ", hash " + Long.toHexString(hash) + ", sentinel " + sentinel);
        long sortedHash = makeRegularKey(hash);
        Node curr = sentinel;
        while (true) {
            if (curr != null) {
                int c;
                // System.out.println("sortedHash " + Long.toHexString(sortedHash) + ", curr.getHash() = " + Long.toHexString(curr.getHash()));
                long currHash = curr.getHash();
                if ((c = Long.compareUnsigned(sortedHash, currHash)) > 0) {
                    if (curr != sentinel && (currHash & 0x1) == 0) {    // only sentinel nodes have even hashes
                        // System.out.println("Retrieval stepping over sentinel!");
                        return statics.errorValue();
                    }
                    curr = curr.getNext();
                    // System.out.println("too small, next");
                    continue;
                } else if (c == 0) {
                    if (!(key.equals(curr.getKey()))) {
                        curr = curr.getNext();
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
        return null;    // not found
    }

    @SuppressWarnings("unchecked")
    private Object getValueFromSentinel(Node sentinel, long hash, Object key, Class<?> cls) {
        // System.out.println("looking for key " + key + ", hash " + Long.toHexString(hash) + ", sentinel " + sentinel);
        long sortedHash = makeRegularKey(hash);
        Node curr = sentinel;
        while (true) {
            if (curr != null) {
                int c;
                // System.out.println("sortedHash " + Long.toHexString(sortedHash) + ", curr.getHash() = " + Long.toHexString(curr.getHash()));
                long currHash = curr.getHash();
                if ((c = Long.compareUnsigned(sortedHash, currHash)) > 0) {
                    if (curr != sentinel && (currHash & 0x1) == 0) {    // only sentinel nodes have even hashes
                        // System.out.println("Retrieval stepping over sentinel!");
                        return statics.errorValue();
                    }
                    curr = curr.getNext();
                    // System.out.println("too small, next");
                    continue;
                } else if (c == 0) {
                    K curKey = (K)curr.getKey();
                    if (curKey instanceof EquatesWith) {
                        if (!((EquatesWith)curKey).equatesWith(key)) {
                            curr = curr.getNext();
                            continue;
                        } else {
                            return curr.getValue();
                        }
                    } else if (!(key.equals(curr.getKey()))) {
                        curr = curr.getNext();
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
        return null;    // not found
    }

    public V remove(Object key) {
        return doRemove(key);
    }

    private V doRemove(Object key) {
        if (key == null) throw new NullPointerException();
        int hash = hash(key);
        Box<Object> ret = new Box<>();
        while (true) {
            int slot = hash % getTable().getCapacity();
            Node sentinel = getSentinel(slot);
            Transaction.run(() -> {
                ret.set(removeNodeFromSentinel(sentinel, hash, key));
            }, sentinel);
            if (ret.get() != statics.errorValue()) break;
        }
        @SuppressWarnings("unchecked") V vv = (V)ret.get();
        return vv;
    }

    private Object removeNodeFromSentinel(Node sentinel, long hash, Object key) {
        long sortedHash = makeRegularKey(hash);
        Node curr = sentinel;
        Node next = curr.getNext();
        while (true) {
            if (next != null) {
                int c;
                long nextHash = next.getHash();
                if ((c = Long.compareUnsigned(sortedHash, nextHash)) > 0) {
                    if ((nextHash & 0x1) == 0) {    // only sentinel nodes have even hashes
                        // System.out.println("Deletion stepping over sentinel!");
                        return statics.errorValue();
                    }
                    curr = next;
                    next = next.getNext();
                    continue;
                } else if (c == 0) {
                    if (!(key.equals(next.getKey()))) {
                        curr = next;
                        next = next.getNext();
                        continue;
                    } else {
                        @SuppressWarnings("unchecked")
                        V prevValue = (V)next.getValue();
                        curr.setNext(next.getNext());
                        return prevValue;
                    }
                } else break;
            } else break;
        }
        return null;    // not found
    }

    private Node<K, V> getSentinel(int slot) {
        String hexSlot = Long.toHexString(slot);
        // System.out.println("thread " + Thread.currentThread().getId() + " getSentinel 0x" + hexSlot);
        // debugFromHead();
        Node<K, V> sentinel = getNode(slot);
        if (sentinel != null) {
            // System.out.println("thread " + Thread.currentThread().getId() + " found sentinel at slot 0x" + hexSlot + ", returning");
            return sentinel;
        }

        long sentinelKey = makeSentinelKey((long)slot);
        final Box<Node<K, V>> ret = new Box<>();
        final int parentSlot = findParentSlot(slot);
        final Node<K, V> parentSentinel = getSentinel(parentSlot);

        // System.out.println("thread " + Thread.currentThread().getId() + " trying to lock parentSlot 0x" + Long.toHexString(parentSlot));
        Transaction.run(() -> {
            // System.out.println("thread " + Thread.currentThread().getId() + " locked parentSlot 0x" + Long.toHexString(parentSlot));
            Node<K, V> curr = parentSentinel, next = curr.getNext();
            while (true) {
                if (next != null) {
                    int c;
                    // System.out.println("thread " + Thread.currentThread().getId() + " curr is " + curr + ", next is " + next);
                    // System.out.println("thread " + Thread.currentThread().getId() + " 0x" + Long.toHexString(sentinelKey));
                    // System.out.println("thread " + Thread.currentThread().getId() + " next hash 0x" + Long.toHexString(next.getHash()));
                    long nextHash = next.getHash();
                    if ((c = Long.compareUnsigned(sentinelKey, nextHash)) > 0) {
                        if ((nextHash & 0x1) == 0) {
                            int slotToAcquire = (int)(Long.reverse(nextHash));
                            // System.out.println("thread " + Thread.currentThread().getId() + " encountered new slot while walking to insert sentinel, acquiring lock on slot 0x" + Integer.toHexString(slotToAcquire));
                            Transaction.run(() -> {}, getNode(slotToAcquire));
                            // System.out.println("thread " + Thread.currentThread().getId() + " acquired lock on slot 0x" + Integer.toHexString(slotToAcquire));
                        }
                        curr = next;
                        next = next.getNext();
                        continue;
                    } else if (c == 0) {
                        // System.out.println("thread " + Thread.currentThread().getId() + " just got slot 0x" + hexSlot);
                        ret.set(next);
                        setNode(slot, next);
                        break;
                    } else break;
                } else break;
            }
            if (ret.get() == null) {
                final Node<K, V> nextF = next, currF = curr;
                // Transaction.runOuter(() -> {
                    Node<K, V> newSentinel = new Node<K, V>(sentinelKey, null, null, nextF);
                    ret.set(newSentinel);
                    currF.setNext(newSentinel);
                // });
                setNode(slot, curr.getNext());
                // System.out.println("thread " + Thread.currentThread().getId() + " inserted new slot 0x" + hexSlot + ", curr is " + curr + ", new node is " + ret.get());
            }
        }, parentSentinel);

        return ret.get();
    }

    public int size() {
        Node curr = getHead();
        int size = 0;
        while (curr != null) {
            if (!curr.isSentinel()) size++;
            curr = curr.getNext();
        }
        return size;
    }

    private int hash(Object key) {
        if (key instanceof EquatesWith)
            return Math.abs(((EquatesWith)key).equivalentHash());
        else
            return Math.abs(key.hashCode());
    }

    private long makeSentinelKey(long key) {
        return Long.reverse(key);
    }

    private long makeRegularKey(long key) {
        return Long.reverse(key | 0x8000_0000_0000_0000L);
    }

    private int findParentSlot(int slot) {
        return slot - Integer.highestOneBit(slot);
    }

    private void setNode(int index, Node<K, V> node) {
        getTable().setSlot(index, node);
    }

    @SuppressWarnings("unchecked")
    private Node<K, V> getNode(int index) {
        return (Node<K, V>)getTable().getSlot(index);
    }

    public Set<Map.Entry<K, V>> entrySet() {
        Set<Map.Entry<K, V>> es;
        return (es = entrySet) == null ? (entrySet = new EntrySet()) : es;
    }

    public Set<K> keySet() {
        Set<K> ks;
        return (ks = keySet) == null ? (keySet = new KeySet()) : ks;
    }

    public Collection<V> values() {
        Collection<V> vs;
        return (vs = values) == null ? (values = new Values()) : vs;
    }

    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    public boolean containsValue(Object value) {
        Node node = getHead();
        while (node != null) {
            AnyPersistent x = node.getValue();
            if (x != null && !(node.isSentinel())) {
                @SuppressWarnings("unchecked") V vv = (V)x;
                if (x.equals(value))
                    return true;
            }
            node = node.getNext();
        }
        return false;
    }

    public boolean isEmpty() { return size() == 0; }

    public void putAll(Map<? extends K, ? extends V> m) {
        Transaction.run(() -> {
            for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
                put(e.getKey(), e.getValue());
            }
        });
    }

    public void clear() {
        Transaction.run(() -> {
            Node sentinel = getHead(), next = sentinel.getNext();
            while (next != null) {
                if (next.isSentinel()) {
                    sentinel = next;
                    Transaction.run(() -> {});
                    next = sentinel.getNext();
                } else {
                    sentinel.setNext(next.getNext());
                    next = next.getNext();
                }
            }
        }, getHead());
    }

    public boolean equals(Object o) {
        if (o != this) {
            if (!(o instanceof Map)) return false;
            Map<?,?> that = (Map<?,?>)o;
            Node<K, V> curr = getHead();
            while (curr != null) {
                if (!curr.isSentinel()) {
                    V thisVal = curr.getValue();
                    Object thatVal = that.get(curr.getKey());
                    if (thatVal == null || (thatVal != thisVal || !thatVal.equals(thisVal))) return false;
                }
                curr = curr.getNext();
            }
            for (Map.Entry<?,?> e : that.entrySet()) {
                Object thatKey, thatVal, thisVal;
                if ((thatKey = e.getKey()) == null ||
                    (thatVal = e.getValue()) == null ||
                    (thisVal = this.get(thatKey)) == null ||
                    (thatVal != thisVal && !thatVal.equals(thisVal)))
                    return false;
            }
        }
        return true;
    }

    public int hashCode() {
        int h = 0;
        Node<K, V> curr = getHead();
        while (curr != null) {
            if (!curr.isSentinel()) {
                h += curr.hashCode();
            }
            curr = curr.getNext();
        }
        return h;
    }

    private Node<K, V> getHead() { return getNode(0); }

    /*public V getOrDefault(Object key, V defaultValue) {
        V v;
        return (v = get(key)) == null ? defaultValue : v;
    }

    public void forEach(BiConsumer<? super K, ? super V> action) {
        Transaction.run(() -> {
            Node<K, V> curr = getHead();
            while (curr != null) {
                if (curr.isSentinel()) Transaction.run(() -> {}, curr);
                else {
                    action.accept(curr.getKey(), curr.getValue());
                }
                curr = curr.getNext();
            }
        });
    }*/

    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        Transaction.run(() -> {
            Node<K, V> curr = getHead();
            while (curr != null) {
                if (curr.isSentinel()) Transaction.run(() -> {}, curr);
                else {
                    V oldVal = curr.getValue();
                    V newVal = function.apply(curr.getKey(), curr.getValue());
                    if (newVal == null) throw new NullPointerException();
                    else curr.setValue(newVal);
                }
                curr = curr.getNext();
            }
        });
    }

    /*public boolean remove(Object key, Object value) {
    }

    public boolean replace(K key, V value, V newValue) {}
    public V replace(K key, V value) {}
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {}
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {}
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {}
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {}*/

    public void debugFromHead() {
        Node<K, V> curr = getHead();
        StringBuilder sb = new StringBuilder();
        long prevHash = 0L;
        while (curr != null) {
            if (Long.compareUnsigned(curr.getHash(), prevHash) < 0) System.out.println("ERROR in map: hash out of order: previous hash 0x" + Long.toHexString(prevHash) + ", current hash 0x" + Long.toHexString(curr.getHash()));
            prevHash = curr.getHash();
            sb.append("(node: (hash 0x" + Long.toHexString(curr.getHash()) + ", key " + curr.getKey() + ", value " + curr.getValue() + ")->\n");
            curr = curr.getNext();
        }
        sb.append("(null)");
        System.out.println(sb.toString());
    }
}