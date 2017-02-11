/* Copyright (C) 2016-17  Intel Corporation
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

package lib.persistent;

import java.util.Set;
import java.util.Map;
import java.util.Collection;
import java.util.AbstractMap;
import java.util.AbstractCollection;
import java.util.SortedMap;
import java.util.Comparator;
import java.util.Iterator;
import java.util.AbstractSet;
import java.util.NoSuchElementException;
import java.lang.IllegalStateException;
import java.lang.IllegalArgumentException;
import java.lang.UnsupportedOperationException;
import java.lang.NullPointerException;
import java.util.function.BiFunction;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.nio.charset.*;

public class PersistentTreeMap<K extends Persistent, V extends Persistent> extends AbstractMap<K, V> implements Persistent, SortedMap<K, V> {

    private long offset;

    static {
        System.loadLibrary("Persistent");
        Util.openPool();
    }

    synchronized static PersistentTreeMap fromOffset(long offset) {
        return new PersistentTreeMap(offset);
    }

    synchronized static boolean isValidOffset(long offset) {
        return nativeCheckTreeMapExists(offset);
    }

    public synchronized long getOffset() {
        return this.offset;
    }

    public PersistentTreeMap() {
        synchronized (PersistentTreeMap.class) {
            this.offset = nativeCreateTreeMap();
            ObjectDirectory.registerObject(this);
        }
    }

    private PersistentTreeMap(long offset) {
        synchronized (PersistentTreeMap.class) {
            this.offset = offset;
            ObjectDirectory.registerObject(this);
        }
    }

    public synchronized V put(K key, V val) {
        if (key == null) throw new NullPointerException();
        else if (val == null) return nativePut(getOffset(), key.getOffset(), 0);
        else return nativePut(getOffset(), key.getOffset(), val.getOffset());
    }

    public synchronized V remove(Object key) {
        if (key == null)
            throw new NullPointerException();
        if (!(key instanceof Persistent)) return null;

        return nativeRemove(getOffset(), ((Persistent)key).getOffset());
    }

    public synchronized V get(Object key) {
        if (key == null)
            throw new NullPointerException();
        Entry<K, V> entry = getEntry(key);
        return entry == null ? null : entry.getValue();
    }

    public synchronized int size() {
        return nativeSize(getOffset());
    }

    public synchronized Set<Map.Entry<K, V>> entrySet() {
        return new EntrySet();
    }

    public synchronized K firstKey() {
        long offset = nativeGetFirstNode(getOffset());
        if (offset == 0)
            throw new NoSuchElementException();
        else
            return nativeGetNodeKey(getOffset(), offset);
    }

    public synchronized K lastKey() {
        long offset = nativeGetLastNode(getOffset());
        if (offset == 0)
            throw new NoSuchElementException();
        else
            return nativeGetNodeKey(getOffset(), offset);
    }

    public synchronized SortedMap<K, V> headMap(K toKey) {
        if (toKey == null)
            throw new NullPointerException();
        return new SubMap(null, toKey, true, false, true, false);
    }

    public synchronized SortedMap<K, V> subMap(K fromKey, K toKey) {
        if (toKey == null || fromKey == null)
            throw new NullPointerException();
        return new SubMap(fromKey, toKey, true, false, false, false);
    }

    public synchronized SortedMap<K, V> tailMap(K fromKey) {
        if (fromKey == null)
            throw new NullPointerException();
        return new SubMap(fromKey, null, true, true, false, true);
    }

    // natural ordering
    public synchronized Comparator<? super K> comparator() { return null; }

    public synchronized void clear() {
        nativeClear(getOffset());
    }

    public synchronized void putAll(Map<? extends K, ? extends V> m) {
        if (m == null)
            throw new NullPointerException();
        int size = m.size();
        long[] keys = new long[size];
        long[] values = new long[size];
        int i = 0;
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
            keys[i] = e.getKey().getOffset();
            values[i] = e.getValue().getOffset();
            i++;
        }
        nativePutAll(getOffset(), keys, values, size);
    }

    @Override
    public synchronized int hashCode() {
        return super.hashCode();
    }

    @Override
    public synchronized String toString() {
        return super.toString();
    }

    @Override
    public synchronized Set<K> keySet() {
        return super.keySet();
    }

    @Override
    public synchronized Collection<V> values() {
        return super.values();
    }

    @Override
    public synchronized V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return super.compute(key, remappingFunction);
    }

    @Override
    public synchronized V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        return super.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public synchronized V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return super.computeIfPresent(key, remappingFunction);
    }

    @Override
    public synchronized boolean containsKey(Object key) {
        return super.containsKey(key);
    }

    @Override
    public synchronized boolean containsValue(Object value) {
        return super.containsValue(value);
    }

    @Override
    public synchronized boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public synchronized void forEach(BiConsumer<? super K, ? super V> action) {
        super.forEach(action);
    }

    @Override
    public synchronized V getOrDefault(Object key, V defaultValue) {
        return super.getOrDefault(key, defaultValue);
    }

    @Override
    public synchronized boolean isEmpty() {
        return super.isEmpty();
    }

    @Override
    public synchronized V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        return super.merge(key, value, remappingFunction);
    }

    @Override
    public synchronized V putIfAbsent(K key, V value) {
        return super.putIfAbsent(key, value);
    }

    @Override
    public synchronized boolean remove(Object key, Object value) {
        return super.remove(key, value);
    }

    @Override
    public synchronized V replace(K key, V value) {
        return super.replace(key, value);
    }

    @Override
    public synchronized boolean replace(K key, V oldValue, V newValue) {
        return super.replace(key, oldValue, newValue);
    }

    @Override
    public synchronized void replaceAll(BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        throw new UnsupportedOperationException();
    }

    private synchronized native long nativeCreateTreeMap();
    private synchronized static native boolean nativeCheckTreeMapExists(long offset);
    private synchronized native V nativePut(long mapOffset, long keyOffset, long valOffset);
    private synchronized native long nativeGet(long mapOffset, long keyOffset);             // gets the whole node
    private synchronized native V nativeRemove(long mapOffset, long keyOffset);
    private synchronized native int nativeSize(long mapOffset);
    private synchronized native long nativeGetFirstNode(long mapOffset);
    private synchronized native long nativeGetLastNode(long mapOffset);
    private synchronized native long nativeGetSuccessor(long mapOffset, long keyOffset);
    private synchronized native long nativeGetPredecessor(long mapOffset, long keyOffset);
    private synchronized native K nativeGetNodeKey(long mapOffset, long nodeOffset);
    private synchronized native V nativeGetNodeValue(long mapOffset, long nodeOffset);
    private synchronized native void nativeClear(long mapOffset);
    private synchronized native void nativePutAll(long mapOffset, long[] keys, long[] values, int size);
    private synchronized native void nativeRemoveAll(long mapOffset, long[] keys, int size);

    private synchronized final Entry<K, V> getFirstEntry() {
        long offset = nativeGetFirstNode(getOffset());
        return nodeOffsetToEntry(offset);
    }

    private synchronized final Entry<K, V> getLastEntry() {
        long offset = nativeGetLastNode(getOffset());
        return nodeOffsetToEntry(offset);
    }

    synchronized final Entry<K, V> getEntry(Object key) {
        if (!(key instanceof Persistent)) return null;
        long offset = nativeGet(getOffset(), ((Persistent)key).getOffset());
        return nodeOffsetToEntry(offset);
    }

    private synchronized final void deleteEntry(Object key) {
        remove(key);
    }

    private synchronized final Entry<K, V> successor(Entry<K, V> t) {
        if (t == null) {
            return null;
        } else {
            long offset = nativeGetSuccessor(getOffset(), t.getKey().getOffset());
            return nodeOffsetToEntry(offset);
        }
    }

    private synchronized final Entry<K, V> predecessor(Entry<K, V> t) {
        if (t == null) {
            return null;
        } else {
            long offset = nativeGetPredecessor(getOffset(), t.getKey().getOffset());
            return nodeOffsetToEntry(offset);
        }
    }

    @SuppressWarnings("unchecked")
    private synchronized Entry<K, V> nodeOffsetToEntry(long offset) {
        return offset == 0 ? null : new Entry(nativeGetNodeKey(getOffset(), offset),
                                              nativeGetNodeValue(getOffset(), offset));
    }

    private synchronized PersistentByteBuffer byteBufferFromOffset(long bufOffset) {
        return bufOffset == 0 ? null : new PersistentByteBuffer(bufOffset);
    }

    final static synchronized boolean valEquals(Object o1, Object o2) {
        return (o1 == null ? o2 == null : o1.equals(o2));
    }

    // Smallest entry larger than the given key (or equal, if inclusive)
    private synchronized Map.Entry<K, V> getCeilingEntry(K key, boolean inclusive) {
        if (inclusive) {
            Entry<K, V> e = getEntry(key);
            if (e != null) return e;
        }

        for (Map.Entry<K, V> e : entrySet()) {
            @SuppressWarnings("unchecked") Comparable<? super K> ek = (Comparable<? super K>)(e.getKey());
            if (ek.compareTo(key) > 0)  // only test for greater; equality returns earlier
                return e;
        }

        return null;   // key is greater than everything in map
    }

    // Largest entry smaller than the given key (or equal, if inclusive)
    private synchronized Map.Entry<K, V> getFloorEntry(K key, boolean inclusive) {
        if (inclusive) {
            Entry<K, V> e = getEntry(key);
            if (e != null) return e;
        }

        Map.Entry<K, V> prev = null;
        for (Map.Entry<K, V> e : entrySet()) {
            @SuppressWarnings("unchecked") Comparable<? super K> ek = (Comparable<? super K>)(e.getKey());
            if (ek.compareTo(key) >= 0)  // if cur == key, return previous one, since non-inclusive
                return prev;    // could be null, which handles special case of smallest entry too big
            prev = e;
        }

        return null;  // should never get here
    }

    static final class Entry<K, V> implements Map.Entry<K, V> {

        K key;
        V value;

        Entry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public synchronized K getKey() {
            return key;
        }

        @Override
        public synchronized V getValue() {
            return value;
        }

        @Override
        public synchronized V setValue(V value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public synchronized boolean equals(Object o) {
            if (!(o instanceof Entry))
                return false;
            Entry e = (Entry)o;
            return valEquals(key, e.getKey()) && valEquals(value, e.getValue());
        }

        @Override
        public synchronized int hashCode() {
            int keyHash = (key == null) ? 0 : key.hashCode();
            int valHash = (value == null) ? 0 : value.hashCode();
            return keyHash ^ valHash;
        }

        @Override
        public synchronized String toString() {
            return key + " = " + value;
        }
    }

    class EntrySet extends AbstractSet<Map.Entry<K, V>> {

        boolean fromStart;
        boolean toEnd;
        K firstKey;
        K lastKey;

        public EntrySet(boolean fromStart, boolean toEnd,
                        K firstKey, K lastKey) {
            this.fromStart = fromStart;
            this.toEnd = toEnd;
            this.firstKey = firstKey;
            this.lastKey = lastKey;
        }

        public EntrySet() {
            this(true, true, null, null);
        }

        public EntrySet(K firstKey, K lastKey) {
            this(false, false, firstKey, lastKey);
        }

        public Iterator<Map.Entry<K, V>> iterator() {
            synchronized(PersistentTreeMap.this) {
                Entry<K, V> firstEntry, lastEntry;
                if (firstKey == null) {
                    firstEntry = fromStart ? PersistentTreeMap.this.getFirstEntry() : null;
                } else {
                    firstEntry = PersistentTreeMap.this.getEntry(firstKey);
                }
                if (lastKey == null) {
                    lastEntry = toEnd ? PersistentTreeMap.this.getLastEntry() : null;
                } else {
                    lastEntry = PersistentTreeMap.this.getEntry(lastKey);
                }
                return new EntryIterator(firstEntry, lastEntry);
            }
        }

        public boolean contains(Object o) {
            synchronized(PersistentTreeMap.this) {
                if (!(o instanceof Entry)) return false;
                Entry entry = (Entry)o;
                if (!inRange(entry.getKey())) return false;
                Object value = entry.getValue();
                Entry<K, V> p = getEntry(entry.getKey());
                return p != null && valEquals(p.getValue(), value);
            }
        }

        public int size() {
            synchronized(PersistentTreeMap.this) {
                int size = 0;
                Iterator<Map.Entry<K, V>> it = iterator();
                while (it.hasNext()) {
                    size++;
                    Entry<K, V> e = (Entry<K, V>)it.next();
                }
                return size;
            }
        }

        public boolean remove(Object o) {
            synchronized(PersistentTreeMap.this) {
                throw new UnsupportedOperationException();
            }
        }

        public boolean removeAll(Collection<?> c) {
            synchronized(PersistentTreeMap.this) {
                throw new UnsupportedOperationException();
            }
        }

        private boolean inRange(Object key) {
            synchronized(PersistentTreeMap.this) {
                if (largerThanFirst(key) && smallerThanLast(key)) {
                    return true;
                } else {
                    return false;
                }
            }
        }

        private boolean largerThanFirst(Object key) {
            synchronized(PersistentTreeMap.this) {
                @SuppressWarnings("unchecked") Comparable<? super K> k = (Comparable<? super K>)key;
                if (fromStart)
                    return true;
                if (k.compareTo(firstKey) >= 0)
                    return true;
                return false;
            }
        }

        private boolean smallerThanLast(Object key) {
            synchronized(PersistentTreeMap.this) {
                @SuppressWarnings("unchecked") Comparable<? super K> k = (Comparable<? super K>)key;
                if (toEnd)
                    return true;
                if (k.compareTo(lastKey) <= 0)
                    return true;
                return false;
            }
        }
    }

    class EntryIterator extends PrivateEntryIterator<Map.Entry<K, V>> {

        EntryIterator(Entry<K, V> first, Entry<K, V> last) {
            super(first, last);
        }

        @Override
        public Entry<K, V> next() {
            synchronized(PersistentTreeMap.this) {
                return nextEntry();
            }
        }
    }

    abstract class PrivateEntryIterator<T> implements Iterator<T> {

        Entry<K, V> next;
        Entry<K, V> lastReturned;
        Entry<K, V> last;
        Entry<K, V> first;

        PrivateEntryIterator(Entry<K, V> first, Entry<K, V> last) {
            this.lastReturned = null;
            this.next = first;
            this.first = first;
            this.last = last;
        }

        @Override
        public final boolean hasNext() {
            synchronized(PersistentTreeMap.this) {
                return next != null;
            }
        }

        final Entry<K, V> nextEntry() {
            synchronized(PersistentTreeMap.this) {
                Entry<K, V> e = next;
                if (e == null)
                    throw new NoSuchElementException();
                if (e.equals(last)) {    // if current == last, don't go any further
                    next = null;
                } else {
                    next = PersistentTreeMap.this.successor(e);
                }
                lastReturned = e;
                return e;
            }
        }

        @Override
        public void remove() {
            synchronized(PersistentTreeMap.this) {
                throw new UnsupportedOperationException();
            }
        }

        @Override
        public void forEachRemaining(Consumer<? super T> action) {
            synchronized(PersistentTreeMap.this) {
                throw new UnsupportedOperationException();
            }
        }
    }

    final class SubMap extends AbstractMap<K, V> implements SortedMap<K, V> {

        private K firstKey;
        private K lastKey;
        private boolean loInclusive;
        private boolean hiInclusive;
        private boolean fromStart;
        private boolean toEnd;

        public SubMap(K firstKey, K lastKey,
                      boolean loInclusive, boolean hiInclusive, boolean fromStart, boolean toEnd) {
            this.firstKey = firstKey;
            this.lastKey = lastKey;
            this.loInclusive = loInclusive;
            this.hiInclusive = hiInclusive;
            this.fromStart = fromStart;
            this.toEnd = toEnd;
        }

        public V put(K key, V val) {
            synchronized(PersistentTreeMap.this) {
                if (inRange(key))
                    return PersistentTreeMap.this.put(key, val);
                else
                    throw new IllegalArgumentException("Key out of range");
            }
        }

        public V remove(Object key) {
            synchronized(PersistentTreeMap.this) {
                if (inRange(key))
                    return PersistentTreeMap.this.remove(key);
                else
                    return null;
            }
        }

        public V get(Object key) {
            synchronized(PersistentTreeMap.this) {
                if (inRange(key))
                    return PersistentTreeMap.this.get(key);
                else
                    return null;
            }
        }

        public Set<Map.Entry<K, V>> entrySet() {
            synchronized(PersistentTreeMap.this) {
                return new EntrySet(firstKey(), lastKey());
            }
        }

        public K firstKey() {
            synchronized(PersistentTreeMap.this) {
                K firstEntryKey = PersistentTreeMap.this.getFirstEntry().getKey();
                if (fromStart) {
                    return inRange(firstEntryKey) ? firstEntryKey : null;
                } else {
                    Map.Entry<K, V> e = PersistentTreeMap.this.getCeilingEntry(firstKey, loInclusive);
                    return (e != null && inRange(e.getKey())) ? e.getKey() : null;
                }
            }
        }

        public K lastKey() {
            synchronized(PersistentTreeMap.this) {
                K lastEntryKey = PersistentTreeMap.this.getLastEntry().getKey();
                if (toEnd) {
                    return inRange(lastEntryKey) ? lastEntryKey : null;
                } else {
                    Map.Entry<K, V> e = PersistentTreeMap.this.getFloorEntry(lastKey, hiInclusive);
                    return (e != null && inRange(e.getKey())) ? e.getKey() : null;
                }
            }
        }

        public Comparator<? super K> comparator() {
            synchronized(PersistentTreeMap.this) {
                return null;
            }
        }

        public SortedMap<K, V> headMap(K toKey) {
            synchronized(PersistentTreeMap.this) {
                if (toKey == null)
                    throw new NullPointerException();
                if (inRange(toKey, true)) {
                    return new SubMap(firstKey, toKey, loInclusive, false, fromStart, false);
                } else {
                    throw new IllegalArgumentException("Key out of range");
                }
            }
        }

        public SortedMap<K, V> subMap(K fromKey, K toKey) {
            synchronized(PersistentTreeMap.this) {
                if (toKey == null || fromKey == null)
                    throw new NullPointerException();
                if (inRange(toKey, true) && inRange(fromKey, true)) {
                    return new SubMap(fromKey, toKey, true, false, false, false);
                } else {
                    throw new IllegalArgumentException("Key out of range");
                }
            }
        }

        public SortedMap<K, V> tailMap(K fromKey) {
            synchronized(PersistentTreeMap.this) {
                if (fromKey == null)
                    throw new NullPointerException();
                if (inRange(fromKey, true)) {
                    return new SubMap(fromKey, lastKey, loInclusive, hiInclusive, false, toEnd);
                } else {
                    throw new IllegalArgumentException("Key out of range");
                }
            }
        }

        @Override
        public String toString() {
            synchronized(PersistentTreeMap.this) {
                return super.toString();
            }
        }

        @Override
        public void clear() {
            synchronized(PersistentTreeMap.this) {
                int size = size();
                long[] keys = new long[size];
                int i = 0;
                for (Map.Entry<? extends K, ? extends V> e : entrySet()) {
                    keys[i] = e.getKey().getOffset();
                    i++;
                }
                PersistentTreeMap.this.nativeRemoveAll(getOffset(), keys, size);
            }
        }

        @Override
        public Set<K> keySet() {
            synchronized(PersistentTreeMap.this) {
                return super.keySet();
            }
        }

        @Override
        public Collection<V> values() {
            synchronized(PersistentTreeMap.this) {
                return super.values();
            }
        }

        @Override
        public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
            synchronized(PersistentTreeMap.this) {
                return super.compute(key, remappingFunction);
            }
        }

        @Override
        public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
            synchronized(PersistentTreeMap.this) {
                return super.computeIfAbsent(key, mappingFunction);
            }
        }

        @Override
        public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
            synchronized(PersistentTreeMap.this) {
                return super.computeIfPresent(key, remappingFunction);
            }
        }

        @Override
        public boolean containsKey(Object key) {
            synchronized(PersistentTreeMap.this) {
                return super.containsKey(key);
            }
        }

        @Override
        public boolean containsValue(Object value) {
            synchronized(PersistentTreeMap.this) {
                return super.containsValue(value);
            }
        }

        @Override
        public boolean equals(Object o) {
            synchronized(PersistentTreeMap.this) {
                return super.equals(o);
            }
        }

        @Override
        public void forEach(BiConsumer<? super K, ? super V> action) {
            synchronized(PersistentTreeMap.this) {
                super.forEach(action);
            }
        }

        @Override
        public V getOrDefault(Object key, V defaultValue) {
            synchronized(PersistentTreeMap.this) {
                return super.getOrDefault(key, defaultValue);
            }
        }

        @Override
        public int hashCode() {
            synchronized(PersistentTreeMap.this) {
                return super.hashCode();
            }
        }

        @Override
        public boolean isEmpty() {
            synchronized(PersistentTreeMap.this) {
                return super.isEmpty();
            }
        }

        @Override
        public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
            synchronized(PersistentTreeMap.this) {
                return super.merge(key, value, remappingFunction);
            }
        }

        @Override
        public V putIfAbsent(K key, V value) {
            synchronized(PersistentTreeMap.this) {
                return super.putIfAbsent(key, value);
            }
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
            synchronized(PersistentTreeMap.this) {
                for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
                    if (!inRange(e.getKey()))
                        throw new IllegalArgumentException("Key out of range");
                }
                PersistentTreeMap.this.putAll(m);
            }
        }

        @Override
        public boolean remove(Object key, Object value) {
            synchronized(PersistentTreeMap.this) {
                return super.remove(key, value);
            }
        }

        @Override
        public V replace(K key, V value) {
            synchronized(PersistentTreeMap.this) {
                return super.replace(key, value);
            }
        }

        @Override
        public boolean replace(K key, V oldValue, V newValue) {
            synchronized(PersistentTreeMap.this) {
                return super.replace(key, oldValue, newValue);
            }
        }

        @Override
        public void replaceAll(BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
            synchronized(PersistentTreeMap.this) {
                throw new UnsupportedOperationException();
            }
        }

        @Override
        public int size() {
            synchronized(PersistentTreeMap.this) {
                return super.size();
            }
        }

        private boolean inRange(Object key) {
            synchronized(PersistentTreeMap.this) {
                return inRange(key, false);
            }
        }

        private boolean inRange(Object key, boolean alwaysInclusive) {
            synchronized(PersistentTreeMap.this) {
                if (largerThanFirst(key, alwaysInclusive) && smallerThanLast(key, alwaysInclusive)) {
                    return true;
                } else {
                    return false;
                }
            }
        }

        private boolean largerThanFirst(Object key, boolean alwaysInclusive) {
            synchronized(PersistentTreeMap.this) {
                @SuppressWarnings("unchecked") Comparable<? super K> k = (Comparable<? super K>)key;
                if (fromStart)
                    return true;
                if ((loInclusive || alwaysInclusive) && k.compareTo(firstKey) == 0)
                    return true;
                if (k.compareTo(firstKey) > 0)
                    return true;
                return false;
            }
        }

        private boolean smallerThanLast(Object key, boolean alwaysInclusive) {
            synchronized(PersistentTreeMap.this) {
                @SuppressWarnings("unchecked") Comparable<? super K> k = (Comparable<? super K>)key;
                if (toEnd)
                    return true;
                if ((hiInclusive || alwaysInclusive) && k.compareTo(lastKey) == 0)
                    return true;
                if (k.compareTo(lastKey) < 0)
                    return true;
                return false;
            }
        }

        private boolean smallerThanFirst(Object key) {
            synchronized(PersistentTreeMap.this) {
                @SuppressWarnings("unchecked") Comparable<? super K> k = (Comparable<? super K>)key;
                if (fromStart)
                    return false;
                if (k.compareTo(firstKey) == 0)
                    return !loInclusive;
                if (k.compareTo(firstKey) > 0)
                    return false;
                return true;
            }
        }

        private boolean largerThanLast(Object key) {
            synchronized(PersistentTreeMap.this) {
                @SuppressWarnings("unchecked") Comparable<? super K> k = (Comparable<? super K>)key;
                if (toEnd)
                    return false;
                if (k.compareTo(lastKey) == 0)
                    return !hiInclusive;
                if (k.compareTo(lastKey) < 0)
                    return false;
                return true;
            }
        }
    }
}
