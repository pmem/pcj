/* Copyright (C) 2016  Intel Corporation
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
import java.util.function.Function;
import java.nio.charset.*;

public class PersistentSortedMap extends AbstractMap<PersistentByteBuffer, PersistentByteBuffer> implements Persistent, SortedMap<PersistentByteBuffer, PersistentByteBuffer> {

    private long offset;

    static {
        System.loadLibrary("PersistentSortedMap");
        nativeOpenPool();
    }

    synchronized static PersistentSortedMap fromOffset(long offset) {
        return new PersistentSortedMap(offset);
    }

    synchronized static boolean isValidOffset(long offset) {
        return nativeCheckSortedMapExists(offset);
    }

    public synchronized long getOffset() {
        return this.offset;
    }

    public PersistentSortedMap() {
        synchronized (PersistentSortedMap.class) {
            this.offset = nativeCreateSortedMap();
            ObjectDirectory.registerObject(this);
        }
    }

    private PersistentSortedMap(long offset) {
        synchronized (PersistentSortedMap.class) {
            this.offset = offset;
            ObjectDirectory.registerObject(this);
        }
    }

    public synchronized PersistentByteBuffer put(PersistentByteBuffer key, PersistentByteBuffer val) {
        PersistentByteBuffer ret;
        if (key == null)
            throw new NullPointerException();
        else if (val == null)
            ret = byteBufferFromOffset(nativePut(getOffset(), key.getOffset(), 0));
        else ret = byteBufferFromOffset(nativePut(getOffset(), key.getOffset(), val.getOffset()));
        return ret;
    }

    public synchronized PersistentByteBuffer remove(Object key) {
        if (key == null)
            throw new NullPointerException();
        if (!(key instanceof PersistentByteBuffer)) return null;

        PersistentByteBuffer ret = byteBufferFromOffset(nativeRemove(getOffset(), ((PersistentByteBuffer)key).getOffset()));
        return ret;
    }

    public synchronized PersistentByteBuffer get(Object key) {
        if (key == null)
            throw new NullPointerException();
        Entry entry = getEntry(key);
        return entry == null ? null : entry.getValue();
    }

    public synchronized int size() {
        return nativeSize(getOffset());
    }

    public synchronized Set<Map.Entry<PersistentByteBuffer, PersistentByteBuffer>> entrySet() {
        return new EntrySet();
    }

    public synchronized PersistentByteBuffer firstKey() {
        long offset = nativeGetFirstNode(getOffset());
        if (offset == 0)
            throw new NoSuchElementException();
        else
            return byteBufferFromOffset(nativeGetNodeKey(getOffset(), offset));
    }

    public synchronized PersistentByteBuffer lastKey() {
        long offset = nativeGetLastNode(getOffset());
        if (offset == 0)
            throw new NoSuchElementException();
        else
            return byteBufferFromOffset(nativeGetNodeKey(getOffset(), offset));
    }

    public synchronized SortedMap<PersistentByteBuffer, PersistentByteBuffer> headMap(PersistentByteBuffer toKey) {
        if (toKey == null)
            throw new NullPointerException();
        return new SubMap(null, toKey, true, false, true, false);
    }

    public synchronized SortedMap<PersistentByteBuffer, PersistentByteBuffer> subMap(PersistentByteBuffer fromKey, PersistentByteBuffer toKey) {
        if (toKey == null || fromKey == null)
            throw new NullPointerException();
        return new SubMap(fromKey, toKey, true, false, false, false);
    }

    public synchronized SortedMap<PersistentByteBuffer, PersistentByteBuffer> tailMap(PersistentByteBuffer fromKey) {
        if (fromKey == null)
            throw new NullPointerException();
        return new SubMap(fromKey, null, true, true, false, true);
    }

    // natural ordering
    public synchronized Comparator<? super PersistentByteBuffer> comparator() { return null; }

    public synchronized void clear() {
        nativeClear(getOffset());
    }

    public synchronized void putAll(Map<? extends PersistentByteBuffer, ? extends PersistentByteBuffer> m) {
        if (m == null)
            throw new NullPointerException();
        int size = m.size();
        long[] keys = new long[size];
        long[] values = new long[size];
        int i = 0;
        for (Map.Entry<? extends PersistentByteBuffer, ? extends PersistentByteBuffer> e : m.entrySet()) {
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
    public synchronized Set<PersistentByteBuffer> keySet() {
        return super.keySet();
    }

    @Override
    public synchronized Collection<PersistentByteBuffer> values() {
        return super.values();
    }

    @Override
    public synchronized PersistentByteBuffer compute(PersistentByteBuffer key, BiFunction<? super PersistentByteBuffer, ? super PersistentByteBuffer, ? extends PersistentByteBuffer> remappingFunction) {
        return super.compute(key, remappingFunction);
    }

    @Override
    public synchronized PersistentByteBuffer computeIfAbsent(PersistentByteBuffer key, Function<? super PersistentByteBuffer, ? extends PersistentByteBuffer> mappingFunction) {
        return super.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public synchronized PersistentByteBuffer computeIfPresent(PersistentByteBuffer key, BiFunction<? super PersistentByteBuffer, ? super PersistentByteBuffer, ? extends PersistentByteBuffer> remappingFunction) {
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
    public synchronized void forEach(BiConsumer<? super PersistentByteBuffer, ? super PersistentByteBuffer> action) {
        super.forEach(action);
    }

    @Override
    public synchronized PersistentByteBuffer getOrDefault(Object key, PersistentByteBuffer defaultValue) {
        return super.getOrDefault(key, defaultValue);
    }

    @Override
    public synchronized boolean isEmpty() {
        return super.isEmpty();
    }

    @Override
    public synchronized PersistentByteBuffer merge(PersistentByteBuffer key, PersistentByteBuffer value, BiFunction<? super PersistentByteBuffer, ? super PersistentByteBuffer, ? extends PersistentByteBuffer> remappingFunction) {
        return super.merge(key, value, remappingFunction);
    }

    @Override
    public synchronized PersistentByteBuffer putIfAbsent(PersistentByteBuffer key, PersistentByteBuffer value) {
        return super.putIfAbsent(key, value);
    }

    @Override
    public synchronized boolean remove(Object key, Object value) {
        return super.remove(key, value);
    }

    @Override
    public synchronized PersistentByteBuffer replace(PersistentByteBuffer key, PersistentByteBuffer value) {
        return super.replace(key, value);
    }

    @Override
    public synchronized boolean replace(PersistentByteBuffer key, PersistentByteBuffer oldValue, PersistentByteBuffer newValue) {
        return super.replace(key, oldValue, newValue);
    }

    @Override
    public synchronized void replaceAll(BiFunction<? super PersistentByteBuffer, ? super PersistentByteBuffer, ? extends PersistentByteBuffer> remappingFunction) {
        throw new UnsupportedOperationException();
    }

    private synchronized static native void nativeOpenPool();
    private synchronized static native long nativeCreateSortedMap();
    private synchronized static native boolean nativeCheckSortedMapExists(long offset);
    private synchronized native long nativePut(long mapOffset, long keyOffset, long valOffset);
    private synchronized native long nativeGet(long mapOffset, long keyOffset);             // gets the whole node
    private synchronized native long nativeRemove(long mapOffset, long keyOffset);
    private synchronized native int nativeSize(long mapOffset);
    private synchronized native long nativeGetFirstNode(long mapOffset);
    private synchronized native long nativeGetLastNode(long mapOffset);
    private synchronized native long nativeGetSuccessor(long mapOffset, long keyOffset);
    private synchronized native long nativeGetPredecessor(long mapOffset, long keyOffset);
    private synchronized native long nativeGetNodeKey(long mapOffset, long nodeOffset);
    private synchronized native long nativeGetNodeValue(long mapOffset, long nodeOffset);
    private synchronized native void nativeClear(long mapOffset);
    private synchronized native void nativePutAll(long mapOffset, long[] keys, long[] values, int size);
    private synchronized native void nativeRemoveAll(long mapOffset, long[] keys, int size);

    private synchronized final Entry getFirstEntry() {
        long offset = nativeGetFirstNode(getOffset());
        return nodeOffsetToEntry(offset);
    }

    private synchronized final Entry getLastEntry() {
        long offset = nativeGetLastNode(getOffset());
        return nodeOffsetToEntry(offset);
    }

    synchronized final Entry getEntry(Object key) {
        if (!(key instanceof PersistentByteBuffer)) return null;
        long offset = nativeGet(getOffset(), ((PersistentByteBuffer)key).getOffset());
        return nodeOffsetToEntry(offset);
    }

    private synchronized final void deleteEntry(Object key) {
        remove(key);
    }

    private synchronized final Entry successor(Entry t) {
        if (t == null) {
            return null;
        } else {
            long offset = nativeGetSuccessor(getOffset(), t.getKey().getOffset());
            return nodeOffsetToEntry(offset);
        }
    }

    private synchronized final Entry predecessor(Entry t) {
        if (t == null) {
            return null;
        } else {
            long offset = nativeGetPredecessor(getOffset(), t.getKey().getOffset());
            return nodeOffsetToEntry(offset);
        }
    }

    private synchronized Entry nodeOffsetToEntry(long offset) {
        return offset == 0 ? null : new Entry(byteBufferFromOffset(nativeGetNodeKey(getOffset(), offset)),
                                              byteBufferFromOffset(nativeGetNodeValue(getOffset(), offset)));
    }

    private synchronized PersistentByteBuffer byteBufferFromOffset(long bufOffset) {
        return bufOffset == 0 ? null : new PersistentByteBuffer(bufOffset);
    }

    final static synchronized boolean valEquals(Object o1, Object o2) {
        return (o1 == null ? o2 == null : o1.equals(o2));
    }

    // Smallest entry larger than the given key (or equal, if inclusive)
    private synchronized Entry getCeilingEntry(PersistentByteBuffer key, boolean inclusive) {
        if (inclusive) {
            Entry e = getEntry(key);
            if (e != null) return e;
        }

        for (Map.Entry<PersistentByteBuffer, PersistentByteBuffer> entry : entrySet()) {
            if (entry.getKey().compareTo(key) > 0)  // only test for greater; equality returns earlier
                return (Entry)entry;
        }

        return null;   // key is greater than everything in map
    }

    // Largest entry smaller than the given key (or equal, if inclusive)
    private synchronized Entry getFloorEntry(PersistentByteBuffer key, boolean inclusive) {
        if (inclusive) {
            Entry e = getEntry(key);
            if (e != null) return e;
        }

        Entry prev = null;
        for (Map.Entry<PersistentByteBuffer, PersistentByteBuffer> entry : entrySet()) {
            if (entry.getKey().compareTo(key) >= 0)  // if cur == key, return previous one, since non-inclusive
                return prev;    // could be null, which handles special case of smallest entry too big
            prev = (Entry)entry;
        }

        return null;  // should never get here
    }

    static final class Entry implements Map.Entry<PersistentByteBuffer, PersistentByteBuffer> {

        PersistentByteBuffer key;
        PersistentByteBuffer value;

        Entry(PersistentByteBuffer key, PersistentByteBuffer value) {
            this.key = key;
            this.value = value;
        }

        public synchronized PersistentByteBuffer getKey() {
            return key;
        }

        public synchronized PersistentByteBuffer getValue() {
            return value;
        }

        public synchronized PersistentByteBuffer setValue(PersistentByteBuffer value) {
            throw new UnsupportedOperationException();
        }

        public synchronized boolean equals(Object o) {
            if (!(o instanceof Entry))
                return false;
            Entry e = (Entry) o;
            return valEquals(key, e.getKey()) && valEquals(value, e.getValue());
        }

        public synchronized int hashCode() {
            int keyHash = (key == null) ? 0 : key.hashCode();
            int valHash = (value == null) ? 0 : value.hashCode();
            return keyHash ^ valHash;
        }

        public synchronized String toString() {
            return key + " = " + value;
        }
    }

    class EntrySet extends AbstractSet<Map.Entry<PersistentByteBuffer, PersistentByteBuffer>> {

        boolean fromStart;
        boolean toEnd;
        PersistentByteBuffer firstKey;
        PersistentByteBuffer lastKey;

        public EntrySet(boolean fromStart, boolean toEnd,
                        PersistentByteBuffer firstKey, PersistentByteBuffer lastKey) {
            this.fromStart = fromStart;
            this.toEnd = toEnd;
            this.firstKey = firstKey;
            this.lastKey = lastKey;
        }

        public EntrySet() {
            this(true, true, null, null);
        }

        public EntrySet(PersistentByteBuffer firstKey, PersistentByteBuffer lastKey) {
            this(false, false, firstKey, lastKey);
        }

        public Iterator<Map.Entry<PersistentByteBuffer, PersistentByteBuffer>> iterator() {
            synchronized(PersistentSortedMap.this) {
                Entry firstEntry, lastEntry;
                if (firstKey == null) {
                    firstEntry = fromStart ? PersistentSortedMap.this.getFirstEntry() : null;
                } else {
                    firstEntry = PersistentSortedMap.this.getEntry(firstKey);
                }
                if (lastKey == null) {
                    lastEntry = toEnd ? PersistentSortedMap.this.getLastEntry() : null;
                } else {
                    lastEntry = PersistentSortedMap.this.getEntry(lastKey);
                }
                return new EntryIterator(firstEntry, lastEntry);
            }
        }

        public boolean contains(Object o) {
            synchronized(PersistentSortedMap.this) {
                if (!(o instanceof Entry))
                    return false;
                Entry entry = (Entry) o;
                if (!inRange(entry.getKey())) return false;
                PersistentByteBuffer value = entry.getValue();
                Entry p = getEntry(entry.getKey());
                return p != null && valEquals(p.getValue(), value);
            }
        }

        public int size() {
            synchronized(PersistentSortedMap.this) {
                int size = 0;
                Iterator<Map.Entry<PersistentByteBuffer, PersistentByteBuffer>> it = iterator();
                while (it.hasNext()) {
                    size++;
                    Entry e = (Entry)it.next();
                }
                return size;
            }
        }

        public boolean remove(Object o) {
            synchronized(PersistentSortedMap.this) {
                throw new UnsupportedOperationException();
            }
        }

        public boolean removeAll(Collection<?> c) {
            synchronized(PersistentSortedMap.this) {
                throw new UnsupportedOperationException();
            }
        }

        private boolean inRange(PersistentByteBuffer key) {
            synchronized(PersistentSortedMap.this) {
                if (largerThanFirst(key) && smallerThanLast(key)) {
                    return true;
                } else {
                    return false;
                }
            }
        }

        private boolean largerThanFirst(PersistentByteBuffer key) {
            synchronized(PersistentSortedMap.this) {
                if (fromStart)
                    return true;
                if (key.compareTo(firstKey) >= 0)
                    return true;
                return false;
            }
        }

        private boolean smallerThanLast(PersistentByteBuffer key) {
            synchronized(PersistentSortedMap.this) {
                if (toEnd)
                    return true;
                if (key.compareTo(lastKey) <= 0)
                    return true;
                return false;
            }
        }
    }

    class EntryIterator extends PrivateEntryIterator<Map.Entry<PersistentByteBuffer, PersistentByteBuffer>> {

        EntryIterator(Entry first, Entry last) {
            super(first, last);
        }

        public Entry next() {
            synchronized(PersistentSortedMap.this) {
                return nextEntry();
            }
        }
    }

    abstract class PrivateEntryIterator<T> implements Iterator<T> {

        Entry next;
        Entry lastReturned;
        Entry last;
        Entry first;

        PrivateEntryIterator(Entry first, Entry last) {
            this.lastReturned = null;
            this.next = first;
            this.first = first;
            this.last = last;
        }

        public final boolean hasNext() {
            synchronized(PersistentSortedMap.this) {
                return next != null;
            }
        }

        final Entry nextEntry() {
            synchronized(PersistentSortedMap.this) {
                Entry e = next;
                if (e == null)
                    throw new NoSuchElementException();
                if (e.equals(last)) {    // if current == last, don't go any further
                    next = null;
                } else {
                    next = PersistentSortedMap.this.successor(e);
                }
                lastReturned = e;
                return e;
            }
        }

        public void remove() {
            synchronized(PersistentSortedMap.this) {
                throw new UnsupportedOperationException();
            }
        }
    }

    final class SubMap extends AbstractMap<PersistentByteBuffer, PersistentByteBuffer> implements SortedMap<PersistentByteBuffer, PersistentByteBuffer> {

        private PersistentByteBuffer firstKey;
        private PersistentByteBuffer lastKey;
        private boolean loInclusive;
        private boolean hiInclusive;
        private boolean fromStart;
        private boolean toEnd;

        public SubMap(PersistentByteBuffer firstKey, PersistentByteBuffer lastKey,
                      boolean loInclusive, boolean hiInclusive, boolean fromStart, boolean toEnd) {
            this.firstKey = firstKey;
            this.lastKey = lastKey;
            this.loInclusive = loInclusive;
            this.hiInclusive = hiInclusive;
            this.fromStart = fromStart;
            this.toEnd = toEnd;
        }

        public PersistentByteBuffer put(PersistentByteBuffer key, PersistentByteBuffer val) {
            synchronized(PersistentSortedMap.this) {
                if (inRange(key))
                    return PersistentSortedMap.this.put(key, val);
                else
                    throw new IllegalArgumentException("Key out of range");
            }
        }

        public PersistentByteBuffer remove(Object key) {
            synchronized(PersistentSortedMap.this) {
                if (!(key instanceof PersistentByteBuffer))
                    throw new IllegalArgumentException("Expecting a PersistentByteBuffer");
                PersistentByteBuffer pbbKey = (PersistentByteBuffer)key;
                if (inRange(pbbKey))
                    return PersistentSortedMap.this.remove(pbbKey);
                else
                    return null;
            }
        }

        public PersistentByteBuffer get(Object key) {
            synchronized(PersistentSortedMap.this) {
                if (!(key instanceof PersistentByteBuffer))
                    throw new IllegalArgumentException("Expecting a PersistentByteBuffer");
                PersistentByteBuffer pbbKey = (PersistentByteBuffer)key;
                if (inRange(pbbKey))
                    return PersistentSortedMap.this.get(pbbKey);
                else
                    return null;
            }
        }

        public Set<Map.Entry<PersistentByteBuffer, PersistentByteBuffer>> entrySet() {
            synchronized(PersistentSortedMap.this) {
                return new EntrySet(firstKey(), lastKey());
            }
        }

        public PersistentByteBuffer firstKey() {
            synchronized(PersistentSortedMap.this) {
                PersistentByteBuffer firstEntryKey = PersistentSortedMap.this.getFirstEntry().getKey();
                if (fromStart) {
                    return inRange(firstEntryKey) ? firstEntryKey : null;
                } else {
                    PersistentSortedMap.Entry e = PersistentSortedMap.this.getCeilingEntry(firstKey, loInclusive);
                    return (e != null && inRange(e.getKey())) ? e.getKey() : null;
                }
            }
        }

        public PersistentByteBuffer lastKey() {
            synchronized(PersistentSortedMap.this) {
                PersistentByteBuffer lastEntryKey = PersistentSortedMap.this.getLastEntry().getKey();
                if (toEnd) {
                    return inRange(lastEntryKey) ? lastEntryKey : null;
                } else {
                    PersistentSortedMap.Entry e = PersistentSortedMap.this.getFloorEntry(lastKey, hiInclusive);
                    return (e != null && inRange(e.getKey())) ? e.getKey() : null;
                }
            }
        }

        public Comparator<? super PersistentByteBuffer> comparator() {
            synchronized(PersistentSortedMap.this) {
                return null;
            }
        }

        public SortedMap<PersistentByteBuffer, PersistentByteBuffer> headMap(PersistentByteBuffer toKey) {
            synchronized(PersistentSortedMap.this) {
                if (toKey == null)
                    throw new NullPointerException();
                if (inRange(toKey, true)) {
                    return new SubMap(firstKey, toKey, loInclusive, false, fromStart, false);
                } else {
                    throw new IllegalArgumentException("Key out of range");
                }
            }
        }

        public SortedMap<PersistentByteBuffer, PersistentByteBuffer> subMap(PersistentByteBuffer fromKey, PersistentByteBuffer toKey) {
            synchronized(PersistentSortedMap.this) {
                if (toKey == null || fromKey == null)
                    throw new NullPointerException();
                if (inRange(toKey, true) && inRange(fromKey, true)) {
                    return new SubMap(fromKey, toKey, true, false, false, false);
                } else {
                    throw new IllegalArgumentException("Key out of range");
                }
            }
        }

        public SortedMap<PersistentByteBuffer, PersistentByteBuffer> tailMap(PersistentByteBuffer fromKey) {
            synchronized(PersistentSortedMap.this) {
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
            synchronized(PersistentSortedMap.this) {
                return super.toString();
            }
        }

        @Override
        public void clear() {
            synchronized(PersistentSortedMap.this) {
                int size = size();
                long[] keys = new long[size];
                int i = 0;
                for (Map.Entry<? extends PersistentByteBuffer, ? extends PersistentByteBuffer> e : entrySet()) {
                    keys[i] = e.getKey().getOffset();
                    i++;
                }
                PersistentSortedMap.this.nativeRemoveAll(getOffset(), keys, size);
            }
        }

        @Override
        public Set<PersistentByteBuffer> keySet() {
            synchronized(PersistentSortedMap.this) {
                return super.keySet();
            }
        }

        @Override
        public Collection<PersistentByteBuffer> values() {
            synchronized(PersistentSortedMap.this) {
                return super.values();
            }
        }

        @Override
        public PersistentByteBuffer compute(PersistentByteBuffer key, BiFunction<? super PersistentByteBuffer, ? super PersistentByteBuffer, ? extends PersistentByteBuffer> remappingFunction) {
            synchronized(PersistentSortedMap.this) {
                return super.compute(key, remappingFunction);
            }
        }

        @Override
        public PersistentByteBuffer computeIfAbsent(PersistentByteBuffer key, Function<? super PersistentByteBuffer, ? extends PersistentByteBuffer> mappingFunction) {
            synchronized(PersistentSortedMap.this) {
                return super.computeIfAbsent(key, mappingFunction);
            }
        }

        @Override
        public PersistentByteBuffer computeIfPresent(PersistentByteBuffer key, BiFunction<? super PersistentByteBuffer, ? super PersistentByteBuffer, ? extends PersistentByteBuffer> remappingFunction) {
            synchronized(PersistentSortedMap.this) {
                return super.computeIfPresent(key, remappingFunction);
            }
        }

        @Override
        public boolean containsKey(Object key) {
            synchronized(PersistentSortedMap.this) {
                return super.containsKey(key);
            }
        }

        @Override
        public boolean containsValue(Object value) {
            synchronized(PersistentSortedMap.this) {
                return super.containsValue(value);
            }
        }

        @Override
        public boolean equals(Object o) {
            synchronized(PersistentSortedMap.this) {
                return super.equals(o);
            }
        }

        @Override
        public void forEach(BiConsumer<? super PersistentByteBuffer, ? super PersistentByteBuffer> action) {
            synchronized(PersistentSortedMap.this) {
                super.forEach(action);
            }
        }

        @Override
        public PersistentByteBuffer getOrDefault(Object key, PersistentByteBuffer defaultValue) {
            synchronized(PersistentSortedMap.this) {
                return super.getOrDefault(key, defaultValue);
            }
        }

        @Override
        public int hashCode() {
            synchronized(PersistentSortedMap.this) {
                return super.hashCode();
            }
        }

        @Override
        public boolean isEmpty() {
            synchronized(PersistentSortedMap.this) {
                return super.isEmpty();
            }
        }

        @Override
        public PersistentByteBuffer merge(PersistentByteBuffer key, PersistentByteBuffer value, BiFunction<? super PersistentByteBuffer, ? super PersistentByteBuffer, ? extends PersistentByteBuffer> remappingFunction) {
            synchronized(PersistentSortedMap.this) {
                return super.merge(key, value, remappingFunction);
            }
        }

        @Override
        public PersistentByteBuffer putIfAbsent(PersistentByteBuffer key, PersistentByteBuffer value) {
            synchronized(PersistentSortedMap.this) {
                return super.putIfAbsent(key, value);
            }
        }

        @Override
        public void putAll(Map<? extends PersistentByteBuffer, ? extends PersistentByteBuffer> m) {
            synchronized(PersistentSortedMap.this) {
                for (Map.Entry<? extends PersistentByteBuffer, ? extends PersistentByteBuffer> e : m.entrySet()) {
                    if (!inRange(e.getKey()))
                        throw new IllegalArgumentException("Key out of range");
                }
                PersistentSortedMap.this.putAll(m);
            }
        }

        @Override
        public boolean remove(Object key, Object value) {
            synchronized(PersistentSortedMap.this) {
                return super.remove(key, value);
            }
        }

        @Override
        public PersistentByteBuffer replace(PersistentByteBuffer key, PersistentByteBuffer value) {
            synchronized(PersistentSortedMap.this) {
                return super.replace(key, value);
            }
        }

        @Override
        public boolean replace(PersistentByteBuffer key, PersistentByteBuffer oldValue, PersistentByteBuffer newValue) {
            synchronized(PersistentSortedMap.this) {
                return super.replace(key, oldValue, newValue);
            }
        }

        @Override
        public void replaceAll(BiFunction<? super PersistentByteBuffer, ? super PersistentByteBuffer, ? extends PersistentByteBuffer> remappingFunction) {
            synchronized(PersistentSortedMap.this) {
                throw new UnsupportedOperationException();
            }
        }

        @Override
        public int size() {
            synchronized(PersistentSortedMap.this) {
                return super.size();
            }
        }

        private boolean inRange(PersistentByteBuffer key) {
            synchronized(PersistentSortedMap.this) {
                return inRange(key, false);
            }
        }

        private boolean inRange(PersistentByteBuffer key, boolean alwaysInclusive) {
            synchronized(PersistentSortedMap.this) {
                if (largerThanFirst(key, alwaysInclusive) && smallerThanLast(key, alwaysInclusive)) {
                    return true;
                } else {
                    return false;
                }
            }
        }

        private boolean largerThanFirst(PersistentByteBuffer key, boolean alwaysInclusive) {
            synchronized(PersistentSortedMap.this) {
                if (fromStart)
                    return true;
                if ((loInclusive || alwaysInclusive) && key.compareTo(firstKey) == 0)
                    return true;
                if (key.compareTo(firstKey) > 0)
                    return true;
                return false;
            }
        }

        private boolean smallerThanLast(PersistentByteBuffer key, boolean alwaysInclusive) {
            synchronized(PersistentSortedMap.this) {
                if (toEnd)
                    return true;
                if ((hiInclusive || alwaysInclusive) && key.compareTo(lastKey) == 0)
                    return true;
                if (key.compareTo(lastKey) < 0)
                    return true;
                return false;
            }
        }

        private boolean smallerThanFirst(PersistentByteBuffer key) {
            synchronized(PersistentSortedMap.this) {
                if (fromStart)
                    return false;
                if (key.compareTo(firstKey) == 0)
                    return !loInclusive;
                if (key.compareTo(firstKey) > 0)
                    return false;
                return true;
            }
        }

        private boolean largerThanLast(PersistentByteBuffer key) {
            synchronized(PersistentSortedMap.this) {
                if (toEnd)
                    return false;
                if (key.compareTo(lastKey) == 0)
                    return !hiInclusive;
                if (key.compareTo(lastKey) < 0)
                    return false;
                return true;
            }
        }
    }
}
