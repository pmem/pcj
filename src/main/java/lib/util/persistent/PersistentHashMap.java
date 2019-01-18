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

import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.Iterator;
import java.util.AbstractSet;
import java.util.AbstractCollection;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.function.Consumer;
import lib.util.persistent.types.*;

public class PersistentHashMap<K extends AnyPersistent, V extends AnyPersistent> extends PersistentObject {
    static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; // aka 16
    static final int MAXIMUM_CAPACITY = 1 << 30;
    static final float DEFAULT_LOAD_FACTOR = 0.75f;

    private static IntField SIZE = new IntField();
    private static IntField THRESHOLD = new IntField();
    private static FloatField LOAD_FACTOR = new FloatField();
    private static ObjectField<PersistentArray> TABLE = new ObjectField<>(PersistentArray.class);
    public static final ObjectType<PersistentHashMap> TYPE = ObjectType.withFields(PersistentHashMap.class, SIZE, THRESHOLD, LOAD_FACTOR, TABLE);


    public static class Node<K extends AnyPersistent, V extends AnyPersistent> extends PersistentObject implements Map.Entry<K, V> {
        private static final IntField HASH = new IntField();
        private static final ObjectField<AnyPersistent> KEY = new ObjectField<>();
        private static final ObjectField<AnyPersistent> VALUE = new ObjectField<>();
        private static final ObjectField<Node> NEXT = new ObjectField<>();
        public static final ObjectType<Node> TYPE = ObjectType.withFields(Node.class, HASH, KEY, VALUE, NEXT);

        public Node(int hash, K key, V value, Node<K, V> next) {
            super(TYPE);
            setIntField(HASH, hash);
            setObjectField(KEY, key);
            setObjectField(VALUE, value);
            setNext(next);
        }

        protected Node(ObjectPointer<? extends Node> p) { super(p); }

        @SuppressWarnings("unchecked")
        public synchronized final K getKey() { return (K)getObjectField(KEY); }
        @SuppressWarnings("unchecked")
        public synchronized final V getValue() { return (V)getObjectField(VALUE); }

        @SuppressWarnings("unchecked")
        public synchronized final Node<K, V> getNext() { return (Node<K, V>)getObjectField(NEXT); }
        public synchronized final void setNext(Node<K, V> next) { setObjectField(NEXT, next); }

        public synchronized final int getHash() { return getIntField(HASH); }

        public synchronized final String toString() {
            return ((K)getKey()).toString() + " = " + ((V)getValue()).toString();
        }

        public synchronized final int hashCode() {
            return getKey().hashCode() ^ getValue().hashCode();
        }

        public synchronized final V setValue(V newValue) {
            V oldValue = getValue();
            setObjectField(VALUE, newValue);
            return oldValue;
        }

        public synchronized final boolean equals(Object o) {
            if (o == this) return true;
            if (o instanceof Map.Entry) {
                Map.Entry<?,?> e = (Map.Entry<?,?>)o;
                if (getKey().equals(e.getKey()) &&
                    getValue().equals(e.getValue()))
                    return true;
            }
            return false;
        }
    }

    final class EntrySet extends AbstractSet<Map.Entry<K, V>> {
        public synchronized final int size() { return PersistentHashMap.this.size(); }
        public final void clear() { /* NIY */ }
        public final Iterator<Map.Entry<K, V>> iterator() {
            return new EntryIterator();
        }
        public synchronized final boolean contains(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<?,?> e = (Map.Entry<?,?>)o;
            Object key = e.getKey();
            Node<K, V> candidate = getNode(hash(key), key);
            return candidate != null && candidate.equals(e);
        }
        public final boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }
        public final Spliterator<Map.Entry<K, V>> spliterator() {
            throw new UnsupportedOperationException();
        }
        public synchronized final void forEach(Consumer<? super Map.Entry<K, V>> action) {
            PersistentArray<Node<K, V>> tab;
            if (action == null)
                throw new NullPointerException();
            if (size() > 0 && (tab = table()) != null) {
                Transaction.run(() -> {
                    for (int i = 0; i < table().length(); i++) {
                        for (Node<K, V> e = tab.get(i); e != null; e = e.getNext())
                            action.accept(e);
                    }
                });
            }
        }
    }

    final class Values extends AbstractCollection<V> {
        public synchronized final int size() { return PersistentHashMap.this.size(); }
        public final void clear() { /* NIY */ }
        public final Iterator<V> iterator() { return new ValueIterator(); }
        public synchronized final boolean contains(Object o) { return containsValue(o); }
        public final Spliterator<V> spliterator() {
            throw new UnsupportedOperationException();
        }
        public synchronized final void forEach(Consumer<? super V> action) {
            PersistentArray<Node<K, V>> tab;
            if (action == null)
                throw new NullPointerException();
            if (size() > 0 && (tab = table()) != null) {
                Transaction.run(() -> {
                    for (int i = 0; i < tab.length(); i++) {
                        for (Node<K, V> e = tab.get(i); e != null; e = e.getNext())
                            action.accept(e.getValue());
                    }
                });
            }
        }
    }

    final class KeySet extends AbstractSet<K> {
        public synchronized final int size() { return PersistentHashMap.this.size(); }
        public final void clear() { /* NIY */ }
        public final Iterator<K> iterator() { return new KeyIterator(); }
        public synchronized final boolean contains(Object o) { return containsKey(o); }
        public final Spliterator<K> spliterator() {
            throw new UnsupportedOperationException();
        }
        public synchronized final void forEach(Consumer<? super K> action) {
            PersistentArray<Node<K, V>> tab;
            if (action == null)
                throw new NullPointerException();
            if (size() > 0 && (tab = table()) != null) {
                Transaction.run(() -> {
                    for (int i = 0; i < tab.length(); i++) {
                        for (Node<K, V> e = tab.get(i); e != null; e = e.getNext())
                            action.accept(e.getKey());
                    }
                });
            }
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
        Node<K, V> next;
        Node<K, V> current;
        int index;

        HashIterator() {
            PersistentArray<Node<K, V>> t = table();
            current = next = null;
            index = 0;
            if (t != null && size() > 0) {
                do {} while (index < t.length() && (next = t.get(index++)) == null);
            }
        }

        public final boolean hasNext() {
            return next != null;
        }

        final Node<K, V> nextNode() {
            PersistentArray<Node<K, V>> t = table();
            Node<K, V> e = next;
            if (e == null)
                throw new NoSuchElementException();
            if ((next = (current = e).getNext()) == null && (t = table()) != null) {
                do {} while (index < t.length() && (next = t.get(index++)) == null);
            }
            return e;
        }

        public final void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public PersistentHashMap() {
        this(TYPE);
    }

    protected PersistentHashMap(ObjectType<? extends PersistentHashMap> type) {
        super(type);
        loadFactor(DEFAULT_LOAD_FACTOR);
    }

    protected PersistentHashMap(ObjectPointer<? extends PersistentHashMap> p) { super(p); }

    public synchronized int size() { return getIntField(SIZE); }

    private void size(int size) { setIntField(SIZE, size); }
    private int threshold() { return getIntField(THRESHOLD); }
    private void threshold(int threshold) { setIntField(THRESHOLD, threshold); }
    private float loadFactor() { return getFloatField(LOAD_FACTOR); }
    private void loadFactor(float loadFactor) { setFloatField(LOAD_FACTOR, loadFactor); }

    @SuppressWarnings("unchecked")
    private PersistentArray<Node<K, V>> table() { return (PersistentArray<Node<K, V>>)getObjectField(TABLE); }
    private void table(PersistentArray<Node<K, V>> table) { setObjectField(TABLE, table); }

    private Set<Map.Entry<K, V>> entrySet;
    private Set<K> keySet;
    private Collection<V> values;

    public synchronized V put(K key, V value) {
        final Box<V> ret = new Box<>();
        Transaction.run(() -> {
            PersistentArray<Node<K, V>> tab;
            Node<K, V> p;
            int n, i;
            int hash = hash(key);

            if ((tab = table()) == null || (n = tab.length()) == 0)
                n = (tab = resize()).length();
            if ((p = tab.get(i = (n - 1) & hash)) == null)
                tab.set(i, new Node<>(hash, key, value, null));
            else {
                Node<K, V> e;
                K k;

                if (p.getHash() == hash &&
                    ((k = p.getKey()) == key || (key != null && key.equals(k))))
                    e = p;
                else {
                    for (int binCount = 0; ; ++binCount) {
                        if ((e = p.getNext()) == null) {
                            p.setNext(new Node<>(hash, key, value, null));
                            break;
                        }
                        if (e.getHash() == hash &&
                            ((k = e.getKey()) == key || (key != null && key.equals(k))))
                            break;
                        p = e;
                    }
                }
                if (e != null) {  // existing mapping for key
                    V oldValue = e.setValue(value);
                    ret.set(oldValue);
                }
            }
            if (ret.get() == null) {
                size(size()+1);
                if (size() > threshold())
                    resize();
            }
        });
        return ret.get();
    }

    public synchronized V get(Object key) {
        Node<K, V> e;
        return (e = getNode(hash(key), key)) == null ? null : e.getValue();
    }

    public synchronized V remove(Object key) {
        Node<K, V> e;
        return (e = removeNode(hash(key), key, null, false, true)) == null ? null : e.getValue();
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

    public synchronized boolean containsKey(Object key) {
        return getNode(hash(key), key) != null;
    }

    public synchronized boolean containsValue(Object value) {
        PersistentArray<Node<K, V>> tab;
        V v;
        if ((tab = table()) != null && size() > 0) {
            for (int i = 0; i < tab.length(); i++) {
                for (Node<K, V> e = tab.get(i); e != null; e = e.getNext()) {
                    if ((v = e.getValue()) == value ||
                        (value != null && value.equals(v)))
                        return true;
                }
            }
        }
        return false;
    }

    final synchronized PersistentArray<Node<K, V>> resize() {
        final Box<PersistentArray<Node<K, V>>> ret = new Box<>();
        Transaction.run(() -> {
            PersistentArray<Node<K, V>> oldTab = table();
            int oldCap = (oldTab == null) ? 0 : oldTab.length();
            int oldThr = threshold();
            int newCap = 0, newThr = 0;
            if (oldCap > 0) {
                if (oldCap >= MAXIMUM_CAPACITY) {
                    threshold(Integer.MAX_VALUE);
                    ret.set(oldTab);
                } else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY &&
                            oldCap >= DEFAULT_INITIAL_CAPACITY) {
                    newThr = oldThr << 1;
                }
            } else if (oldThr > 0) {
                newCap = oldThr;
            } else {
                newCap = DEFAULT_INITIAL_CAPACITY;
                newThr = (int)(DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
            }
            if (ret.get() == null) {
                if (newThr == 0) {
                    float ft = (float)newCap * loadFactor();
                    newThr = (newCap < MAXIMUM_CAPACITY && ft < (float)MAXIMUM_CAPACITY ? (int)ft : Integer.MAX_VALUE);
                }
                threshold(newThr);
                @SuppressWarnings({"rawtypes", "unchecked"})
                    PersistentArray<Node<K, V>> newTab = new PersistentArray<Node<K, V>>(newCap);
                table(newTab);
                if (oldTab != null) {
                    for (int j = 0; j < oldCap; ++j) {
                        Node<K, V> e;
                        if ((e = oldTab.get(j)) != null) {
                            oldTab.set(j, null);
                            if (e.getNext() == null)
                                newTab.set((e.getHash() & (newCap - 1)), e);
                            else {
                                Node<K, V> loHead = null, loTail = null;
                                Node<K, V> hiHead = null, hiTail = null;
                                Node<K, V> next;
                                do {
                                    next = e.getNext();
                                    if ((e.getHash() & oldCap) == 0) {
                                        if (loTail == null)
                                            loHead = e;
                                        else
                                            loTail.setNext(e);
                                        loTail = e;
                                    } else {
                                        if (hiTail == null)
                                            hiHead = e;
                                        else
                                            hiTail.setNext(e);
                                        hiTail = e;
                                    }
                                } while ((e = next) != null);
                                if (loTail != null) {
                                    loTail.setNext(null);
                                    newTab.set(j, loHead);
                                }
                                if (hiTail != null) {
                                    hiTail.setNext(null);
                                    newTab.set(j + oldCap, hiHead);
                                }
                            }
                        }
                    }
                }
                ret.set(newTab);
            }
        });
        return ret.get();
    }

    final synchronized Node<K, V> getNode(int hash, Object key) {
        PersistentArray<Node<K, V>> tab;
        Node<K, V> first, e;
        int n;
        K k;

        if ((tab = table()) != null && (n = tab.length()) > 0 &&
            (first = tab.get((n - 1) & hash)) != null) {
            if (first.getHash() == hash &&
                ((k = first.getKey()) == key || (key != null && key.equals(k))))
                return first;
            if ((e = first.getNext()) != null) {
                do {
                    if (e.getHash() == hash &&
                        ((k = e.getKey()) == key || (key != null && key.equals(k))))
                        return e;
                } while ((e = e.getNext()) != null);
            }
        }
        return null;
    }

    final synchronized Node<K, V> removeNode(int hash, Object key, Object value, boolean matchValue, boolean movable) {
        final Box<Node<K, V>> ret = new Box<>();
        Transaction.run(() -> {
            PersistentArray<Node<K, V>> tab;
            Node<K, V> p;
            int n, index;
            if ((tab = table()) != null && (n = tab.length()) > 0 &&
                (p = tab.get(index = (n - 1) & hash)) != null) {
                Node<K, V> node = null, e;
                K k;
                V v;
                if (p.getHash() == hash &&
                    ((k = p.getKey()) == key || (key != null && key.equals(k))))
                    node = p;
                else if ((e = p.getNext()) != null) {
                    do {
                        if (e.getHash() == hash &&
                            ((k = e.getKey()) == key ||
                             (key != null && key.equals(k)))) {
                            node = e;
                            break;
                        }
                        p = e;
                    } while ((e = e.getNext()) != null);
                }
                if (node != null && (!matchValue || (v = node.getValue()) == value ||
                                     (value != null && value.equals(v)))) {
                    if (node == p)
                        tab.set(index, node.getNext());
                    else
                        p.setNext(node.getNext());
                    size(size()-1);
                    ret.set(node);
                }
            }
        });
        return ret.get();
    }

    static final int hash(Object key) {
        int h;
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }
}
