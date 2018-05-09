/* 
 * Copyright (c) 2017 Intel Corporation. 
 *
 * Includes code from java.util.concurrent.ConcurrentSkipListMap.java
*/

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package lib.util.persistent;

import lib.util.persistent.types.*;
import lib.util.persistent.types.ObjectType;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.StampedLock;
import java.util.function.IntConsumer;
import java.util.Map;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.Spliterator;
import java.util.Comparator;
import java.util.Collections;
import java.util.AbstractCollection;
import java.util.Scanner;
import java.io.IOException;
import java.io.File;
import java.util.function.BiFunction;

public class PersistentFPTree2<K extends AnyPersistent, V extends AnyPersistent> extends PersistentObject implements ConcurrentNavigableMap<K,V>, PersistentSortedMap<K,V> {
	private Node<K,V> root;
	private static final ObjectField<PersistentLeaf> HEAD_LEAF = new ObjectField<>(PersistentLeaf.class);
	private static final ObjectField<PersistentArrayList> LEAF_ARRAY = new ObjectField<>(PersistentArrayList.class);

	private static final IntField P_MAX_LEAF_KEYS = new IntField();
	private static final IntField P_MAX_INTERNAL_KEYS = new IntField();
	private static final ObjectType<PersistentFPTree2> TYPE = ObjectType.fromFields(PersistentFPTree2.class, HEAD_LEAF, LEAF_ARRAY, P_MAX_LEAF_KEYS, P_MAX_INTERNAL_KEYS);

	private final int MAX_INTERNAL_KEYS;
	private final int MID_INTERNAL_KEYS;
	private final int MAX_LEAF_KEYS;
	private final int MIN_LEAF_KEYS;

	private final StampedLock rootLock;
	private final Comparator<? super K> comparator;
	private LeafNode<K,V> headLeafNode;
	
	private ConcurrentNavigableMap<K,V> descendingMap;

	private static final int[] PEARSON_LOOKUP_TABLE = { 110, 228, 235, 91, 67, 211, 45, 46, 79, 23, 118, 48, 32, 208,
			251, 0, 255, 128, 174, 238, 94, 27, 13, 121, 66, 168, 165, 125, 25, 194, 4, 90, 47, 30, 242, 133, 218, 7,
			203, 114, 231, 180, 96, 248, 214, 249, 122, 75, 163, 41, 88, 243, 221, 76, 89, 29, 145, 119, 22, 14, 206,
			131, 53, 141, 50, 106, 24, 140, 149, 19, 9, 212, 123, 81, 177, 62, 224, 200, 254, 15, 134, 185, 100, 151,
			158, 95, 11, 63, 21, 233, 204, 127, 210, 92, 154, 198, 172, 56, 197, 186, 152, 226, 55, 97, 217, 193, 213,
			115, 236, 175, 86, 139, 202, 18, 39, 156, 232, 98, 60, 245, 70, 189, 35, 20, 12, 157, 34, 199, 38, 16, 6,
			161, 171, 3, 184, 124, 147, 37, 42, 207, 234, 93, 167, 190, 205, 28, 135, 77, 148, 143, 10, 117, 237, 138,
			112, 223, 8, 150, 136, 183, 176, 179, 191, 101, 105, 43, 103, 195, 219, 192, 132, 246, 126, 58, 73, 244, 26,
			83, 49, 69, 178, 74, 253, 169, 201, 120, 64, 17, 111, 164, 216, 239, 108, 146, 80, 225, 144, 129, 84, 78,
			107, 181, 2, 247, 40, 196, 82, 153, 57, 230, 71, 44, 99, 113, 5, 160, 182, 36, 188, 51, 155, 162, 1, 33,
			142, 102, 166, 85, 215, 187, 31, 116, 137, 220, 68, 252, 54, 240, 209, 104, 222, 159, 227, 52, 87, 59, 250,
			61, 109, 170, 65, 229, 241, 130, 173, 72 };

	public PersistentFPTree2(int maxInternalKeys, int maxLeafKeys, Comparator<? super K> comparator) {
		super(TYPE);
		if (maxInternalKeys <= 0) throw new IllegalArgumentException("Number of internal keys must  be > 0");
		if (maxLeafKeys <= 0) throw new IllegalArgumentException("Number of leaf keys must be > 0");
		MAX_INTERNAL_KEYS = maxInternalKeys;
		MID_INTERNAL_KEYS = (MAX_INTERNAL_KEYS + 1) / 2;
		MAX_LEAF_KEYS = maxLeafKeys;
		MIN_LEAF_KEYS = (MAX_LEAF_KEYS + 1) / 2;
		this.rootLock = new StampedLock();
		this.comparator = comparator;
		setIntField(P_MAX_LEAF_KEYS, MAX_LEAF_KEYS);
		setIntField(P_MAX_INTERNAL_KEYS, MAX_INTERNAL_KEYS);
		initialize();
	}

	private void initialize() {
		PersistentLeaf<K,V> headLeaf = new PersistentLeaf<>(MAX_LEAF_KEYS + 1);
		headLeafNode = new LeafNode<K,V>(headLeaf);
		//root = headLeafNode;
        root = new InternalNode<K,V>();
        headLeafNode.parent = (InternalNode<K, V>)root;
        ((InternalNode<K, V>)root).children.set(0, headLeafNode);

		setObjectField(HEAD_LEAF, headLeaf);
		// setObjectField(LEAF_ARRAY, new PersistentArrayList<PersistentLeaf<K,V>>(500000));
	}

	public PersistentFPTree2(int maxInternalKeys, int maxLeafKeys) {
		this(maxInternalKeys, maxLeafKeys, null);
	}

	public PersistentFPTree2() {
		this(8, 64, null);
	}

	public PersistentFPTree2(ObjectPointer<? extends PersistentFPTree2> p) {
		super(p);

		MAX_INTERNAL_KEYS = getIntField(P_MAX_INTERNAL_KEYS);
		MID_INTERNAL_KEYS = (MAX_INTERNAL_KEYS + 1) / 2;
		MAX_LEAF_KEYS = getIntField(P_MAX_LEAF_KEYS);
		MIN_LEAF_KEYS = (MAX_LEAF_KEYS + 1) / 2;
		this.rootLock = new StampedLock();
		this.comparator = null; // how to persist the compartor?

		// System.out.println("starting reconstruction");
		long start = System.nanoTime();
		//parallelTouch();
		reconstructTree();
		float telap = (System.nanoTime() - start) * 1e-9f;
		int size = this.size();
		// System.out.println("reconstructed FPTree size = " + size + ", time to reconstruct = " + telap + " sec.");
		// Trace.trace(true, "Reconstruction rate for FPTree2 is: " + size / (telap) + " entries / sec.");
	}

	static class Runner extends Thread {
		int id;
		IntConsumer f;

		public Runner(int id, IntConsumer f) {
			this.id = id;
			this.f = f;
		}

		public void run() {
			f.accept(id);
		}
	}

	@SuppressWarnings("unchecked")
	private void parallelTouch() {
		System.out.println("PT: 1");
		final Object[] leafArray = getObjectField(LEAF_ARRAY).toArray();
		System.out.println("PT: 2");
		final int N_THREADS = 1;
		Thread[] threads = new Thread[N_THREADS];

		//long start = System.nanoTime();
		for (int j = 0; j < threads.length; j++) {
			//int id = j;
			threads[j] = new Runner(j, (int id)-> {
				int L = leafArray.length/N_THREADS;
				int begin = id * L;
				int end = (id + 1) * L;
				for(int i = begin; i < end; i++) {
					PersistentLeaf<K,V> leaf = (PersistentLeaf<K,V>) leafArray[i];
					//leaf.getNext();
					System.out.println("PT: before " + id);
					for(int s = 0; s < MAX_LEAF_KEYS; s++) {
						PersistentLeafSlot<K,V> slot = leaf.getSlot(s);
						if(slot != null) slot.getKey();
					}
					System.out.println("PT: after" + id);
				}
			});
		}

		for (int j = 0; j < threads.length; j++) threads[j].start();
		try {
			for (int j = 0; j < threads.length; j++) threads[j].join();
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
		/*double telapsed = (double) (System.nanoTime() - start) * 1e-9;
		System.out.println("PARALLEL TOUCH (time) with N= " + N_THREADS + " is " + telapsed);*/
	}

	final boolean reconstruct = true;
	private void reconstructTree() {
		final PersistentLeaf<K, V> firstLeaf = getFirstNonEmptyLeaf(getHeadLeaf());
		if(firstLeaf == null) {
			PersistentLeaf<K, V> headLeaf = new PersistentLeaf<>(MAX_LEAF_KEYS + 1);
			setObjectField(HEAD_LEAF, headLeaf);
			headLeafNode = new LeafNode<K, V>(headLeaf);
            root = new InternalNode<K,V>();
            headLeafNode.parent = (InternalNode<K, V>)root;
            ((InternalNode<K, V>)root).children.set(0, headLeafNode);
			return;
		}

		//Transaction.run(() -> {
		setObjectField(HEAD_LEAF, firstLeaf);
		final PersistentLeaf<K, V> secondLeaf = firstLeaf.getNextNonEmpty();
		firstLeaf.setNext(secondLeaf);
		if(secondLeaf == null) {
            headLeafNode = new LeafNode<K, V>(firstLeaf, reconstruct);
            root = new InternalNode<K,V>();
            headLeafNode.parent = (InternalNode<K, V>)root;
            ((InternalNode<K, V>)root).children.set(0, headLeafNode);
        }
		else {
			root = new InternalNode<K, V>();
			final LeafNode<K, V> firstLeafNode = new LeafNode<K, V>(firstLeaf, reconstruct);
			final LeafNode<K, V> secondLeafNode = new LeafNode<K, V>(secondLeaf, reconstruct);
			headLeafNode = firstLeafNode;
			setLinks(firstLeafNode, secondLeafNode);
			buildNewRootNode((InternalNode<K, V>) root, firstLeafNode, secondLeafNode, firstLeafNode.highKey);
			addLeavesToTheRightOf(secondLeafNode);
		}
		//});
	}

	private PersistentLeaf<K, V> getFirstNonEmptyLeaf(PersistentLeaf<K, V> leaf) {
		if(leaf == null) return null;
		while(leaf.getNext() != null) {
			if(!leaf.isEmpty()) return leaf;
			leaf = leaf.getNext();
		}
		if (!leaf.isEmpty()) return leaf;
		else return null;
	}

	private void addLeavesToTheRightOf(LeafNode<K, V> startNode) {
		LeafNode<K, V> leafNode, nextLeafNode = startNode;
		PersistentLeaf<K, V> leaf = startNode.leaf;
		while(leaf.getNextNonEmpty() != null) {
			leafNode = nextLeafNode;
			leaf = leaf.getNextNonEmpty();
			leafNode.leaf.setNext(leaf);
			nextLeafNode = new LeafNode<K, V>(leaf, reconstruct);
			setLinks(leafNode, nextLeafNode);
			addNewLeafNode(leafNode, nextLeafNode);
		}
		nextLeafNode.leaf.setNext(null);
		nextLeafNode.next = null;
	}
	
	private void setLinks(LeafNode<K, V> left, LeafNode<K, V> right) {
		left.next = right;
		right.prev = left;
	}

	private void addNewLeafNode(LeafNode<K, V> leafNode, LeafNode<K, V> nextLeafNode) {
		rippleSplitHigherInternalNodes(leafNode.parent);
		updateParent(leafNode.parent, leafNode, nextLeafNode, leafNode.highKey);
		//nextLeafNode.parent.printInternal();
	}

	private void rippleSplitHigherInternalNodes(InternalNode<K, V> internalNode) {
		InternalNode<K, V> parent = internalNode.parent;
		if(parent != null) rippleSplitHigherInternalNodes(parent);

		if(internalNode.needToSplit) {
			if(parent == null) root = parent = new InternalNode<K, V>();
			splitInternalNodeAndUpdateParent(parent, internalNode);
		}
	}

	@SuppressWarnings("unchecked")
	private /* synchronized */ PersistentLeaf<K,V> getHeadLeaf() {
		return (PersistentLeaf<K,V>) getObjectField(HEAD_LEAF);
	}

	@SuppressWarnings("unchecked")
	private /* synchronized */ PersistentArrayList<PersistentLeaf<K,V>> leafArray() {
		return (PersistentArrayList<PersistentLeaf<K,V>>) getObjectField(LEAF_ARRAY);
	}

	// begin Map interface methods

	@Override
	public int size() {
		long count = 0;
		for(LeafNode<K,V> n = headLeafNode; n != null; n = n.next){
			long stamp = n.readLock();
			count += n.keycount;
			n.unlock(stamp);
		}
		return (count >= Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) count;
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean containsKey(Object key) {
		return doGet((K)key) != null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean containsValue(Object value) {
		for(PersistentLeaf leaf = getHeadLeaf(); leaf != null; leaf = leaf.getNext()) {
			for(int slot = 0; slot <= MAX_LEAF_KEYS; slot++) {
				V val = ((leaf.getSlot(slot) != null)? (V) leaf.getSlot(slot).getValue() : null);
				if(val != null && ((V) value).equals(val)) return true;
			}
		}
		return false;
	}

	@Override
	@SuppressWarnings("unchecked")
	public V get(Object key) {
		return doGet((K) key);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <L, K extends ComparableWith<L>> V get(L sisterKey, Class<K> cls) {
		return doGet(sisterKey);
	}

	public V putIfAbsent(K key, V value) {
		return doPut(key, value, true);
	}

	@Override
	@SuppressWarnings("unchecked")
	public V remove(Object key) {
		if (key == null) throw new NullPointerException();
		return doRemove((K) key, null);
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean remove(Object key, Object value) {
		if (key == null) throw new NullPointerException();
		return (V)value != null && doRemove((K)key, (V)value) != null;
	}

	public V replace(K key, V value) {
		if (key == null || value == null) throw new NullPointerException();
		return doReplace(key, null, value);
	}

	public boolean replace(K key, V oldValue, V newValue) {
		if (key == null || oldValue == null || newValue == null) throw new NullPointerException();
		return doReplace(key, oldValue, newValue) == oldValue;
	}

	public boolean equals(AnyPersistent o) {
        if (o == this)
            return true;
        if (!(o instanceof PersistentFPTree2))
            return false;
        PersistentFPTree2<?,?> m = (PersistentFPTree2<?,?>) o;
        try {
            for (Map.Entry<K,V> e : this.entrySet())
                if (! e.getValue().equals(m.get(e.getKey())))
                    return false;
            for (Map.Entry<? extends AnyPersistent,? extends AnyPersistent> e : m.entrySet()) {
                AnyPersistent k = e.getKey();
                AnyPersistent v = e.getValue();
                if (k == null || v == null || !v.equals(get(k)))
                    return false;
            }
            return true;
        } catch (ClassCastException unused) {
            return false;
        } catch (NullPointerException unused) {
            return false;
        }
    }

	
	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		Transaction.run(() -> {
			for (Map.Entry<? extends K, ? extends V> e : m.entrySet())
				put(e.getKey(), e.getValue());
		});
	}

	@Override
	public void clear() {
		initialize();
	}

	public ConcurrentNavigableMap<K,V> headMap(K toKey, boolean inclusive) {
		if (toKey == null) throw new NullPointerException();
		return new SubMap<K,V>(this, null, false, toKey, inclusive, false);
	}

	public ConcurrentNavigableMap<K,V> tailMap(K fromKey, boolean inclusive) {
		if (fromKey == null) throw new NullPointerException();
		return new SubMap<K,V>(this, fromKey, inclusive, null, false, false);
	}

	public ConcurrentNavigableMap<K,V> subMap(K fromKey, K toKey) {
		return subMap(fromKey, true, toKey, false);
	}

	public ConcurrentNavigableMap<K,V> headMap(K toKey) {
		return headMap(toKey, false);
	}

	public ConcurrentNavigableMap<K,V> tailMap(K fromKey) {
		return tailMap(fromKey, true);
	}

	@Override
	public ConcurrentNavigableMap<K,V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
		if (fromKey == null || toKey == null) throw new NullPointerException();
		return new SubMap<K,V>(this, fromKey, fromInclusive, toKey, toInclusive, false);
	}

	@Override
	public ConcurrentNavigableMap<K,V> descendingMap() {
		ConcurrentNavigableMap<K,V> dm = descendingMap;
        return (dm != null) ? dm : (descendingMap = new SubMap<K,V> (this, null, false, null, false, true));
	}

	@Override
	public NavigableSet<K> descendingKeySet() {
		return descendingMap().navigableKeySet();
	}

	@Override
	public NavigableSet<K> keySet() {
		return new KeySet<K>(this);
	}

	@Override
	public NavigableSet<K> navigableKeySet() {
		return new KeySet<K>(this);
	}

	@Override
	public Collection<V> values() {
		return new Values<V>(this);
	}

	@Override
	public Set<java.util.Map.Entry<K,V>> entrySet() {
		return new EntrySet<K,V>(this);
	}

	@Override
	public K firstKey() {
		return doGetFirstOrLast(true, false, false).getKey();
	}

	@Override
	public K lastKey() {
		return doGetFirstOrLast(false, true, false).getKey();
	}

	public Map.Entry<K,V> firstEntry() {
		return doGetFirstOrLast(true, true, false);
	}

	public Map.Entry<K,V> lastEntry() {
		return doGetFirstOrLast(false, true, false);
	}

	public Map.Entry<K,V> pollFirstEntry() {
		return doGetFirstOrLast(true, true, true);
	}

	public Map.Entry<K,V> pollLastEntry() {
		return doGetFirstOrLast(false, true, true);
	}

	public K lowerKey(K key) {
		Map.Entry<K,V> e = getNeighbourEntry(key, false, true, false);
		return (e == null) ? null : e.getKey();
	}

	public K higherKey(K key) {
		Map.Entry<K,V> e = getNeighbourEntry(key, false, false, false);
		return (e == null) ? null : e.getKey();
	}

	public K floorKey(K key) {
		Map.Entry<K,V> e = getNeighbourEntry(key, false, false, true);
		return (e == null) ? null : e.getKey();
	}

	public K ceilingKey(K key) {
		Map.Entry<K,V> e = getNeighbourEntry(key, false, true, true);
		return (e == null) ? null : e.getKey();
	}

	public Map.Entry<K,V> lowerEntry(K key) {
		return getNeighbourEntry(key, true, true, false);
	}

	public Map.Entry<K,V> higherEntry(K key) {
		return getNeighbourEntry(key, true, false, false);
	}

	public Map.Entry<K,V> floorEntry(K key) {
		return getNeighbourEntry(key, true, false, true);
	}

	public Map.Entry<K,V> ceilingEntry(K key) {
		return getNeighbourEntry(key, true, true, true);
	}

	@Override
	public Comparator<? super K> comparator() {
		return this.comparator;
	}

	// end of interface methods

	private int compareKeys(K k1, K k2) {
		if (k1 == null)
			throw new NullPointerException("Key 1 is null");
		if (k2 == null)
			throw new NullPointerException("Key 2 is null");
		return compare(k1, k2);
	}

	@SuppressWarnings("unchecked")
	private final int compare(Object k1, Object k2) {
		if (k1 instanceof AnyPersistent)
			return (comparator == null) ? ((Comparable<? super K>) k1).compareTo((K) k2) : comparator.compare((K) k1, (K) k2);
			else
				return ((ComparableWith) k2).compareWith(k1) * -1;
	}

	private final class KeyRangeBox {
		final K lo;
		final K hi;
		final boolean loInclusive;
		final boolean hiInclusive;
		long count;

		public KeyRangeBox(K lowKey, boolean fromInclusive, K highKey, boolean toInclusive){
			this.lo = lowKey;
			this.hi = highKey;
			this.loInclusive = fromInclusive;
			this.hiInclusive = toInclusive;
			this.count = 0;
		}

/*		private boolean checkBounds(K key) {
			if (key == null) throw new NullPointerException("Null key");
			if(lowKey == null && highKey == null) return true;
			boolean lowOK, highOK;
			if(lowKey == null) lowOK = true;
			else {
				int l = compareKeys(key, lowKey);
				lowOK = (l > 0) || (l >= 0 && fromInclusive);
			}
			
			if(highKey == null) highOK = true;
			else {
				int h = compareKeys(key, highKey);
				highOK = (h < 0) || (h <= 0 && toInclusive);
			}
			return (lowOK && highOK);
		}*/
		
		boolean tooLow(K key) {
			int c;
			return (lo != null && ((c = compareKeys(key, lo)) < 0 || (c == 0 && !loInclusive)));
		}

		boolean tooHigh(K key) {
			int c;
			return (hi != null && ((c = compareKeys(key, hi)) > 0 || (c == 0 && !hiInclusive)));
		}

		boolean checkBounds(K key) {
			return !tooLow(key) && !tooHigh(key);
		}
		
		public boolean isAboveHighKey(K key) {
			if(key == null) throw new NullPointerException();
			int h = compareKeys(key, hi);
			return !((h < 0) || (h <= 0 && hiInclusive));
		}
	}


	public void clear(K lowKey, boolean fromInclusive, K highKey, boolean toInclusive) {
		//doBulkRemove(new KeyRangeBox(lowKey, fromInclusive, highKey, toInclusive));
		navigateAndApply(new KeyRangeBox(lowKey, fromInclusive, highKey, toInclusive), this::bulkRemoveInLeaf, true);
	}

	private int size(K lowKey, boolean fromInclusive, K highKey, boolean toInclusive) {
		KeyRangeBox kRange = new KeyRangeBox(lowKey, fromInclusive, highKey, toInclusive);
		navigateAndApply(kRange, this::countSize, false);
		long count = kRange.count;
		return (count >= Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) count;
	}


	@SuppressWarnings("unchecked")
	private void navigateAndApply(KeyRangeBox kRange, BiFunction<LeafNode<K,V>, KeyRangeBox, Boolean> func, boolean isWriteLock) {
		Node<K,V> parent, child;
		long stampParent, stampChild, stamp = 0L;
		long stampRoot = rootLock.readLock();
		try {
			parent = root;
			stampParent = parent.applyLock(isWriteLock);
		} finally {
			rootLock.unlock(stampRoot);
		}

		try {
			while (!parent.isLeaf) {
				child = ((InternalNode<K,V>) parent).getChild(kRange.lo);
				stampChild = child.applyLock(isWriteLock);
				parent.unlock(stampParent);
				parent = child;
				stampParent = stampChild;
			}

			LeafNode<K,V> leafNode = (LeafNode<K,V>) parent;
			while (func.apply(leafNode, kRange)) {
				leafNode = leafNode.next;
				try {
					stamp = leafNode.applyLock(isWriteLock);
					parent.unlock(stampParent);
					stampParent = stamp;
					parent = leafNode;
				}
				catch(Exception ex) {
					leafNode.unlock(stamp);
				}
			}
		} finally {
			parent.unlock(stampParent);
		}
	}

	/*@SuppressWarnings("unchecked")
	private void doBulkRemove(KeyRangeBox kRange) {
		Node<K,V> parent, child;
		long stampParent, stampChild, stamp = 0L;
		long stampRoot = rootLock.readLock();
		try {
			parent = root;
			stampParent = parent.deleteLock();
		} finally {
			rootLock.unlock(stampRoot);
		}

		try {
			while (!parent.isLeaf) {
				child = ((InternalNode<K,V>) parent).getChild(kRange.lowKey);
				stampChild = child.deleteLock();
				parent.unlock(stampParent);
				parent = child;
				stampParent = stampChild;
			}

			LeafNode<K,V> leafNode = (LeafNode<K,V>) parent;
			while (bulkRemoveInLeaf(leafNode, kRange)) {
				leafNode = leafNode.next;
				try {
					stamp = leafNode.deleteLock();
					parent.unlock(stampParent);
					stampParent = stamp;
					parent = leafNode;
				}
				catch(Exception ex) {
					leafNode.unlock(stamp);
				}
			}
		} finally {
			parent.unlock(stampParent);
		}
	}
	 */

	private boolean countSize(LeafNode<K,V> leafNode, KeyRangeBox kRange) {
		boolean reachedEnd = false;
		for (int slot = 0; (slot <= MAX_LEAF_KEYS) && leafNode.keycount > 0; slot++) {
			K key = leafNode.keys.get(slot);
			if (key == null ) continue;
			if (kRange.checkBounds(key)) kRange.count++;
			else if (kRange.isAboveHighKey(key)) reachedEnd = true;
		}
		if(!reachedEnd) return leafNode.next != null;
		else return false;
	}

	private boolean bulkRemoveInLeaf(LeafNode<K,V> leafNode, KeyRangeBox kRange) {
		boolean reachedEnd = false;
		for (int slot = 0; (slot <= MAX_LEAF_KEYS) && leafNode.keycount > 0; slot++) {
			K key = leafNode.keys.get(slot);
			if (key == null ) continue;
			if (kRange.checkBounds(key)) {
				final int slotDel = slot;
				Transaction.run(() -> {
					leafNode.leaf.setSlot(slotDel, null);
					if (leafNode.keycount == 1) leafNode.leaf.setIsEmpty(true);
				});
				leafNode.hashes.set(slot, 0);
				leafNode.keys.set(slot, null);
				leafNode.keycount--;
			}
			else if (kRange.isAboveHighKey(key)) reachedEnd = true;
		}
		if(!reachedEnd) return leafNode.next != null;
		else return false;
	}


	@SuppressWarnings("unchecked")
	private V doRemove(K key, V value) {
		Node<K,V> parent, child;
		long stampParent, stampChild;
		long stampRoot = rootLock.readLock();
		try {
			parent = root;
			stampParent = parent.deleteLock();
		} finally {
			rootLock.unlock(stampRoot);
		}

		try {
			while (!parent.isLeaf) {
				child = ((InternalNode<K,V>) parent).getChild(key);
				stampChild = child.deleteLock();
				parent.unlock(stampParent);
				parent = child;
				stampParent = stampChild;
			}
			return removeInLeaf((LeafNode<K,V>) parent, key, value);
		} finally {
			parent.unlock(stampParent);
		}
	}

	private V removeInLeaf(LeafNode<K,V> leafNode, K key, V value) {
		final int hash = generateHash(key);
		for (int slot = 0; slot <= MAX_LEAF_KEYS; slot++) {
			if (leafNode.hashes.get(slot) == hash) {
				if (key.equals(leafNode.keys.get(slot))) {
					V val = leafNode.leaf.getSlot(slot).getValue();
					final int slotToRemove = slot;
					if(value != null && value != val) return null;
					else {
						Transaction.run(() -> {
							leafNode.leaf.setSlot(slotToRemove, null);
							if (leafNode.keycount == 1) leafNode.leaf.setIsEmpty(true);
						});
						leafNode.hashes.set(slot, 0);
						leafNode.keys.set(slot, null);
						leafNode.keycount--;
						return val;
					}
				}
			}
		}
		return null;
	}

	private void getSubMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
	}

	@SuppressWarnings("unchecked")
	private Map.Entry<K,V> getNeighbourEntry(Object key, boolean wantValue, boolean isSmallerThan, boolean inclusive) {
		Node<K,V> parent, child;
		K k = null;
		long stampParent, stampChild, stampLeaf = -1;
		long stampRoot = rootLock.readLock();
		try {
			parent = root;
			stampParent = parent.readLock();
		} finally {
			rootLock.unlock(stampRoot);
		}

		try {
			while (!parent.isLeaf) {
				child = ((InternalNode<K,V>) parent).getChild(key);
				stampChild = child.readLock();
				parent.unlock(stampParent);
				parent = child;
				stampParent = stampChild;
			}
			LeafNode<K,V> leafNode = (LeafNode<K,V>) parent;
			while(leafNode != null) {
				if(isSmallerThan) {
					k = findLowerOrCeilKey(leafNode, key, inclusive);
					leafNode = leafNode.prev;
				}
				else {
					k = findHigherOrFloorKey(leafNode, key, inclusive);
					leafNode = leafNode.next;
				}

				if(k != null || leafNode == null) break;
				try {
					stampLeaf = leafNode.readLock();
				}
				finally {
					parent.unlock(stampParent);
					stampParent = stampLeaf;
					parent = leafNode;
				}
			}
			return ((LeafNode<K,V>) parent).getEntry(k, wantValue);

		} finally {
			parent.unlock(stampParent);
		}
	}


	K findHigherOrFloorKey(LeafNode<K,V> leafNode, Object key, final boolean wantFloor) {
		if(leafNode.keycount == 0) return null;
		ArrayList<K> keys = sortedKeyList(leafNode, false);
		//System.out.println(keys.toString());
		int i; boolean found = false;
		for(i = 0; i < keys.size(); i++) {
			int cmp = compare(key, keys.get(i));
			if((wantFloor &&  cmp <= 0) || (!wantFloor && cmp < 0)) {
				found = true;
				break;
			}
		}
		return (found ? keys.get(i) : null);
	}

	K findLowerOrCeilKey(LeafNode<K,V> leafNode, Object key, final boolean wantCeil) {
		if(leafNode.keycount == 0) return null;
		ArrayList<K> keys = sortedKeyList(leafNode, true);
		//System.out.println(keys.toString());
		int i; boolean found = false;
		for(i = 0; i < keys.size(); i++) {
			int cmp = compare(key, keys.get(i));
			if((wantCeil &&  cmp >= 0) || (!wantCeil && cmp > 0)) {
				found = true;
				break;
			}
		}
		return (found ? keys.get(i) : null);
	}

	@SuppressWarnings("unchecked")
	private Map.Entry<K,V> doGetFirstOrLast(boolean wantFirst, boolean wantValue, boolean doRemove) {
		Node<K,V> parent, child;
		long stampParent, stampChild;
		long stampRoot = rootLock.readLock();
		try {
			parent = root;
			stampParent = parent.readLock();
		} finally {
			rootLock.unlock(stampRoot);
		}

		try {
			while (!parent.isLeaf) {
				child = ((InternalNode<K,V>) parent).get(wantFirst);
				stampChild = child.readLock();
				parent.unlock(stampParent);
				parent = child;
				stampParent = stampChild;
			}

			LeafNode<K,V> leafNode = (LeafNode<K,V>) parent;
			K k = sortedKeyList(leafNode, false).get((wantFirst ? 0 : leafNode.keycount-1));
			if(doRemove) return new AbstractMap.SimpleImmutableEntry<K,V>(k, removeInLeaf(leafNode, k, null));
			else return leafNode.getEntry(k, wantValue);
		} finally {
			parent.unlock(stampParent);
		}
	}

	@SuppressWarnings("unchecked")
	private LeafNode<K,V> getLeafNode(Object key) {
		Node<K,V> parent, child;
		long stampParent, stampChild;
		long stampRoot = rootLock.readLock();
		try {
			parent = root;
			stampParent = parent.readLock();
		} finally {
			rootLock.unlock(stampRoot);
		}

		try {
			while (!parent.isLeaf) {
				child = ((InternalNode<K,V>) parent).getChild(key);
				stampChild = child.readLock();
				parent.unlock(stampParent);
				parent = child;
				stampParent = stampChild;
			}
			return ((LeafNode<K,V>)parent);
		} finally {
			parent.unlock(stampParent);
		}
	}

	@SuppressWarnings("unchecked")
	private PersistentLeaf<K,V> getPersistentLeaf(Object key) {
		Node<K,V> parent, child;
		long stampParent, stampChild;
		long stampRoot = rootLock.readLock();
		try {
			parent = root;
			stampParent = parent.readLock();
		} finally {
			rootLock.unlock(stampRoot);
		}

		try {
			while (!parent.isLeaf) {
				child = ((InternalNode<K,V>) parent).getChild(key);
				stampChild = child.readLock();
				parent.unlock(stampParent);
				parent = child;
				stampParent = stampChild;
			}
			//return ((LeafNode<K,V>) parent).getValue(generateHash(key), key);
			return ((LeafNode<K,V>)parent).leaf;
		} finally {
			parent.unlock(stampParent);
		}
	}

	@SuppressWarnings("unchecked")
	private V doGet(Object key) {
		Node<K,V> parent, child;
		long stampParent, stampChild;
		long stampRoot = rootLock.readLock();
		try {
			parent = root;
			stampParent = parent.readLock();
		} finally {
			rootLock.unlock(stampRoot);
		}

		try {
			while (!parent.isLeaf) {
				child = ((InternalNode<K,V>) parent).getChild(key);
				stampChild = child.readLock();
				parent.unlock(stampParent);
				parent = child;
				stampParent = stampChild;
			}
			return ((LeafNode<K,V>) parent).getValue(generateHash(key), key);
		} finally {
			parent.unlock(stampParent);
		}
	}

	public V put(K key, V value) {
		return doPut(key, value, false);
	}

	public V doPut(K key, V value, boolean putOnlyIfAbsent) {
		Node<K,V> parent, child;
		long stampParent, stampChild;
		long stampRoot = rootLock.writeLock();
        final boolean innerTX = Transaction.isTransactionActive();
		try {
			parent = root;
			stampParent = parent.writeLock();
			if (parent.needToSplit) {
				InternalNode<K,V> newRoot = new InternalNode<K,V>();
				//splitChildAndUpdateParent(newRoot, parent);
				splitInternalNodeAndUpdateParent(newRoot, (InternalNode<K,V>)parent);
				root = newRoot;
				parent.unlock(stampParent);
				parent = newRoot;
				stampParent = parent.writeLock();
			}
		} finally {
			rootLock.unlock(stampRoot);
		}

		try {
            //while(!parent.isLeaf && !(child = ((InternalNode<K,V>) parent).getChild(key)).isLeaf) {
            while (!parent.isLeaf) {
				child = ((InternalNode<K,V>) parent).getChild(key);
                if (innerTX && child.isLeaf) break;
				stampChild = child.writeLock();
				try {
					if (child.needToSplit) {
						Node<K,V> newChild = splitChildAndUpdateParent((InternalNode<K,V>) parent, child, key);
						if (newChild != child) {
							child.unlock(stampChild);
							child = newChild;
							stampChild = child.writeLock();
						}
					}
				} finally {
					parent.unlock(stampParent);
					parent = child;
					stampParent = stampChild;
				}
			}
            if (innerTX){
                Box<LeafNode<K,V>> otherLeaf = new Box<>();
                Box<Node<K,V>> Leaf = new Box<>();
                Box<V> ret = new Box<V>();
                final InternalNode<K,V> tparent = (InternalNode<K,V>)parent;
                final long stampedParent = stampParent;
                Transaction.run(()->{
                    Node<K,V> leaf = tparent.getChild(key);
                    Leaf.set(leaf);
                    long stampedChild = leaf.writeLock();
                        if(leaf.needToSplit) {
                            Node<K, V> newChild = splitChildAndUpdateParent(tparent, leaf, key);
                            //Node<K, V> newChild = splitLeafNodeAndUpdateParent(tparent, (LeafNode<K,V>)leaf, key);
                            if(newChild != leaf) {
                                otherLeaf.set((LeafNode<K,V>)leaf);
                                // dont unclock until after TX
                                // leaf.unlock(stampedChild); 
                                leaf = newChild;
                                Leaf.set(leaf);
                                stampedChild = leaf.writeLock();
                            } else {
                                otherLeaf.set(((LeafNode<K,V>)leaf).next);
                                //paranoid locking no one should be able to see this node
                                otherLeaf.get().writeLock(); 
                            }
                        }
                    V v = putInLeaf((LeafNode<K, V>) leaf, key, value, putOnlyIfAbsent, stampedChild, true);
                    ret.set(v);
                },()->{
                    tparent.unlock(stampedParent);
                    if(otherLeaf.get() != null) otherLeaf.get().tryUnlockWrite();
                },()->{
                    LeafNode<K,V> leaf = (LeafNode<K,V>)Leaf.get();
                    tparent.unlock(stampedParent);
                    if(otherLeaf.get() != null) otherLeaf.get().tryUnlockWrite();
                    if(leaf != null) leaf.tryUnlockWrite();
                });
                return ret.get();
                }
            else {
                return putInLeaf((LeafNode<K, V>) parent, key, value, putOnlyIfAbsent, stampParent, false);
            }
		} finally {
		//	if (!innerTX) parent.unlock(stampParent);
		}
	}

	private V putInLeaf(LeafNode<K,V> leafNode, K key, V value, boolean putOnlyIfAbsent, long stampLock, boolean inner) {
		final int hash = generateHash(key);
		// scan for empty/matching slots
		int lastEmptySlot = -1;
		for (int slot = MAX_LEAF_KEYS; slot >= 0; slot--) {
			int slotHash = leafNode.hashes.get(slot);
			if (slotHash == 0) {
				lastEmptySlot = slot;
			} else if (slotHash == hash) {
				if (key.equals(leafNode.keys.get(slot))) {
					V oldValue = leafNode.leaf.getSlot(slot).getValue();
					if (putOnlyIfAbsent == false) {
						final int slotToPut = slot;
						Transaction.run(() -> {
							leafNode.leaf.getSlot(slotToPut).setValue(value);
							if (leafNode.keycount == 0)
								leafNode.leaf.setIsEmpty(false);
						}, ()->{
                            leafNode.unlock(stampLock);
                        }, ()-> {
                           if (inner) leafNode.unlock(stampLock);
                        });
					} else {
                        leafNode.unlock(stampLock);
                    }
					return oldValue;
				}
			}
		}

		final int slot = lastEmptySlot;
		if (slot >= 0) {
            final int oldCount = leafNode.keycount;
			Transaction.run(() -> {
				leafNode.leaf.setSlot(slot, new PersistentLeafSlot<K,V>(hash, key, value));
				if (leafNode.keycount == 0) leafNode.leaf.setIsEmpty(false);
            }, () -> {
			leafNode.hashes.set(slot, hash);
			leafNode.keys.set(slot, key);
			if ((++leafNode.keycount) == MAX_LEAF_KEYS + 1) leafNode.needToSplit = true;
                leafNode.unlock(stampLock);
            }, ()-> {
                if (inner) leafNode.unlock(stampLock);
			});
		} else {
			throw new IllegalStateException("Leaf full while trying to insert key: " + key);
		}
		return null;
	}

	private V doReplace(K key, V oldValue, V newValue) {
		Node<K,V> parent, child;
		long stampParent, stampChild;
		long stampRoot = rootLock.writeLock();
		try {
			parent = root;
			stampParent = parent.writeLock();
			if (parent.needToSplit) {
				InternalNode<K,V> newRoot = new InternalNode<K,V>();
				//splitChildAndUpdateParent(newRoot, parent);
				splitInternalNodeAndUpdateParent(newRoot, (InternalNode<K,V>)parent);
				root = newRoot;
				parent.unlock(stampParent);
				parent = newRoot;
				stampParent = parent.writeLock();
			}
		} finally {
			rootLock.unlock(stampRoot);
		}

		try {
			while (!parent.isLeaf) {
				child = ((InternalNode<K,V>) parent).getChild(key);
				stampChild = child.writeLock();
				try {
					if (child.needToSplit) {
						Node<K,V> newChild = splitChildAndUpdateParent((InternalNode<K,V>) parent, child, key);
						if (newChild != child) {
							child.unlock(stampChild);
							child = newChild;
							stampChild = child.writeLock();
						}
					}
				} finally {
					parent.unlock(stampParent);
					parent = child;
					stampParent = stampChild;
				}
			}
			return replaceInLeaf((LeafNode<K,V>) parent, key, oldValue, newValue);
		} finally {
			parent.unlock(stampParent);
		}
	}

	private V replaceInLeaf(LeafNode<K,V> leafNode, K key, V oldValue, V newValue) {
		final int hash = generateHash(key);
		// scan for empty/matching slots
		for (int slot = MAX_LEAF_KEYS; slot >= 0; slot--) {
			int slotHash = leafNode.hashes.get(slot);
			if (slotHash == hash) {
				if (key.equals(leafNode.keys.get(slot))) {
					V value = leafNode.leaf.getSlot(slot).getValue();
					if (oldValue == null || (oldValue != null && oldValue == value)) {
						final int slotToPut = slot;
						Transaction.run(() -> {
							leafNode.leaf.getSlot(slotToPut).setValue(value);
							if (leafNode.keycount == 0)
								leafNode.leaf.setIsEmpty(false);
						});
					}
					return value;
				}
			}
		}
		return null;
	}

	private Node<K,V> splitChildAndUpdateParent(InternalNode<K,V> parent, Node<K,V> child, K key) {
		if (child.isLeaf)
			return splitLeafNodeAndUpdateParent(parent, (LeafNode<K,V>) child, key);
		else
			splitInternalNodeAndUpdateParent(parent, (InternalNode<K,V>) child);
		    return parent.getChild(key);
	}

	private Node<K,V> splitLeafNodeAndUpdateParent(InternalNode<K,V> parent, LeafNode<K,V> leafNode, K key) {
		return splitLeafNodeAndUpdateParent(parent, leafNode, key, getSplitKey(leafNode));
	}

	private LeafNode<K,V> splitLeafNodeAndUpdateParent(InternalNode<K,V> parent, LeafNode<K,V> leafNode, K key, K splitKey) {
		PersistentLeaf<K,V> newLeaf = new PersistentLeaf<>(MAX_LEAF_KEYS + 1);
		LeafNode<K,V> newLeafNode = new LeafNode<>(newLeaf);
          
		Transaction.run(() -> {
			for (int slot = 0; slot <= MAX_LEAF_KEYS; slot++) {
				if (compareKeys(splitKey, leafNode.keys.get(slot)) < 0) {
					newLeaf.setSlot(slot, leafNode.leaf.getSlot(slot));
					leafNode.leaf.setSlot(slot, null);
				}
			}
			newLeafNode.leaf.setNext(leafNode.leaf.getNext());
			leafNode.leaf.setNext(newLeafNode.leaf);
			newLeafNode.leaf.setIsEmpty(false);
			leafNode.leaf.setIsEmpty(false);
			// leafArray().add(newLeaf);
		}, ()->{

		updateParent(parent, leafNode, newLeafNode, splitKey);
            
        }, ()->{ 
		    for (int slot = 0; slot <= MAX_LEAF_KEYS; slot++) {
			    if (newLeafNode.hashes.get(slot) != 0) {
                    leafNode.hashes.set(slot, newLeafNode.hashes.get(slot)); 
                    leafNode.keys.set(slot, newLeafNode.keys.get(slot)); 
				    leafNode.keycount++;
			    }
		    }
            if(leafNode.next == newLeafNode){
                leafNode.next = newLeafNode.next;
		        if(newLeafNode.next != null) newLeafNode.next.prev = leafNode;
            }
		    leafNode.needToSplit = true;
        });

		for (int slot = 0; slot <= MAX_LEAF_KEYS; slot++) {
			if (compareKeys(splitKey, leafNode.keys.get(slot)) < 0) {
				newLeafNode.hashes.set(slot, leafNode.hashes.get(slot));
				newLeafNode.keys.set(slot, leafNode.keys.get(slot));
				leafNode.hashes.set(slot, 0);
				leafNode.keys.set(slot, null);
				leafNode.keycount--;
				newLeafNode.keycount++;
			}
		}
		newLeafNode.parent = leafNode.parent = parent;
		newLeafNode.prev = leafNode;
		newLeafNode.next = leafNode.next;
		leafNode.next = newLeafNode;
		if(newLeafNode.next != null) newLeafNode.next.prev = newLeafNode;
		newLeafNode.needToSplit = leafNode.needToSplit = false;

		return (compareKeys(key, splitKey) <=0) ? leafNode : newLeafNode;
	}

	private void buildNewRootNode(InternalNode<K,V> root, Node<K,V> leftChild, Node<K,V> rightChild, K splitKey) {
		root.keys.set(0, splitKey);
		root.keycount = 1;
		leftChild.parent = rightChild.parent = root;
		root.children.set(0, leftChild);
		root.children.set(1, rightChild);
	}

	private void updateParent(InternalNode<K,V> parent, Node<K,V> child, Node<K,V> newChild, K splitKey) {
		if (parent.keycount == 0) {
			//buildNewRootNode(parent, child, newChild, splitKey);
		    parent.keys.set(0, splitKey);
		    parent.keycount = 1;
		    child.parent = newChild.parent = parent;
		    parent.children.set(0, child);
		    parent.children.set(1, newChild);
        }
		else {
			final int keycount = parent.keycount;
			int idx = 0;
			for (; idx < keycount; idx++)
				if (compareKeys(splitKey, parent.keys.get(idx)) <= 0)
					break;
			for (int i = keycount - 1; i >= idx; i--)
				parent.keys.set(i + 1, parent.keys.get(i));
			for (int i = keycount; i > idx; i--)
				parent.children.set(i + 1, parent.children.get(i));
			parent.keys.set(idx, splitKey);
			parent.children.set(idx + 1, newChild);
			parent.keycount = keycount + 1;
			newChild.parent = child.parent = parent;
			if (parent.keycount == MAX_INTERNAL_KEYS + 1)
				parent.needToSplit = true;
		}
	}

	private Node<K,V> splitInternalNodeAndUpdateParent(InternalNode<K,V> parent, InternalNode<K,V> child) {

		InternalNode<K,V> newChild = new InternalNode<>();
		// newChild.parent = child.parent = parent;
		newChild.needToSplit = child.needToSplit = false;
		final int splitIdx = MID_INTERNAL_KEYS + 1;
		final int keycount = child.keycount;
		for (int i = splitIdx; i < keycount; i++) {
			newChild.keys.set(i - splitIdx, child.keys.get(i));
			child.keys.set(i, null);
		}

		for (int i = splitIdx; i < keycount + 1; i++) {
			newChild.children.set(i - splitIdx, child.children.get(i));
			newChild.children.get(i - splitIdx).parent = newChild;
			child.children.set(i, null);
		}

		K splitKey = child.keys.get(MID_INTERNAL_KEYS);
		child.keycount = MID_INTERNAL_KEYS;
		newChild.keycount = MAX_INTERNAL_KEYS - MID_INTERNAL_KEYS;
		updateParent(parent, child, newChild, splitKey);
		return newChild;
	}

	private ArrayList<K> sortedKeyList(LeafNode<K,V> leafNode, boolean doReverse) {
		ArrayList<K> keys = new ArrayList<>(MAX_LEAF_KEYS + 1);
		for (int slot = 0; slot <= MAX_LEAF_KEYS; slot++) {
			K key = leafNode.keys.get(slot);
			if(key != null) keys.add(key);
		}
		Collections.sort(keys, (doReverse? Collections.reverseOrder(this.comparator) : this.comparator));
		return keys;
	}

	@SuppressWarnings("unchecked")
	private K getSplitKey(LeafNode<K,V> leafNode) {
		return sortedKeyList(leafNode, false).get(MIN_LEAF_KEYS - 1);
	}

	// Modified Pearson hashing algorithm from pmemkv
	// (https://github.com/pmem/pmemkv/blob/master/src/pmemkv.cc)
	/*
	 * public int pearsonHash(K key) { String skey; if(key instanceof
	 * PersistentInteger) skey = Integer.toString(((PersistentInteger)
	 * key).intValue()); else skey = ((PersistentString) key).toString(); return
	 * computePearsonHash(skey.getBytes()); }
	 */

	public int computePearsonHash(byte[] data) {
		int hash = data.length;
		for (int i = data.length; i > 0;) {
			hash = PEARSON_LOOKUP_TABLE[hash ^ data[--i]];
		}
		hash = (hash == 0) ? 1 : hash;
		return hash;
	}

	public int generateHash(Object key) {
		return computePearsonHash(Integer.toString(key.hashCode()).getBytes());
	}

	public boolean verifyNext() {
		LeafNode<K,V> cursor = headLeafNode;
		boolean ans = true;
		while (cursor != null) {
			if(ans == false) break;
			ans = ans && verifyLeafNode(cursor);
			cursor = cursor.next;
		}
		return ans;
	}

	public boolean verifyLeafNode(LeafNode<K,V> cursor) {
		boolean consistent = true;
		for (int i = 0; i < this.MAX_LEAF_KEYS + 1; i++) {
			if(consistent == false) break;
			K key = cursor.keys.get(i);
			if(key == null) consistent = (cursor.leaf.getSlot(i) == null);
			else consistent = (cursor.leaf.getSlot(i).getKey() == key);
		}
		return consistent;
	}

	public void printLeaves() {
		PersistentLeaf<K,V> cursor = getHeadLeaf();
		StringBuilder sb = new StringBuilder("HEAD-->");
		while (cursor != null) {
			printLeaf(cursor, sb);
			sb.append("-->");
			cursor = cursor.getNext();
		}
		sb.append("NULL");
		System.out.println(sb.toString());
	}

	private void printLeaf(PersistentLeaf<K,V> leaf) {
		System.out.println(printLeaf(leaf, new StringBuilder("")).toString());
	}

	private StringBuilder printLeaf(PersistentLeaf<K,V> cursor, StringBuilder sb) {
		sb.append("[");
		for (int i = 0; i < this.MAX_LEAF_KEYS + 1; i++) {
			PersistentLeafSlot<K,V> slot = cursor.getSlot(i);
			if (slot != null) {
				sb.append("" + slot.getValue().toString() + "");
			} else {
				sb.append("X");
			}
			if (i != this.MAX_LEAF_KEYS)
				sb.append(", ");
		}
		sb.append("]");
		return sb;
	}

	public void verifyDelete(String msg) {
		if (verifyDelete())
			System.out.println(msg + "REMOVE VERIFICATION: SUCCESS");
	}

	public boolean verifyDelete() {
		PersistentLeaf<K,V> cursor = getHeadLeaf();
		while (cursor != null) {
			for (int i = 0; i < this.MAX_LEAF_KEYS + 1; i++) {
				PersistentLeafSlot<K,V> slot = cursor.getSlot(i);
				if (slot != null)
					throw new IllegalStateException("REMOVE FAILED");
			}
			cursor = cursor.getNext();
		}
		return true;
	}

	/*	public void randomlyDeleteLeaves() {
		PersistentLeaf<K,V> cursor = getHeadLeaf();
		Random rndb = new Random();
		while (cursor != null) {
			boolean val = rndb.nextBoolean();
			if (val == true) {
				cursor.setIsEmpty(true);
				for (int i = 0; i < MAX_LEAF_KEYS + 1; i++) {
					cursor.setSlot(i, null);
				}
			}
			cursor = cursor.getNext();
		}
	}
	 */
	public void randomlyDeleteLeaves() {
		LeafNode<K,V> cursor = headLeafNode;
		Random rndb = new Random();
		while (cursor != null) {
			boolean val = rndb.nextBoolean();
			if (val == true) {
				cursor.keycount = 0;
				cursor.leaf.setIsEmpty(true);
				for (int i = 0; i < MAX_LEAF_KEYS + 1; i++) {
					cursor.leaf.setSlot(i, null);
					cursor.keys.set(i, null);
					cursor.hashes.set(i, 0);
				}
			}
			cursor = cursor.next;
		}
	}


	public HashMap<K,V> getHashMap() {
		PersistentLeaf<K,V> cursor = getHeadLeaf();
		HashMap<K,V> map = new HashMap<>();
		while (cursor != null) {
			if (!cursor.isEmpty()) {
				for (int i = 0; i < MAX_LEAF_KEYS + 1; i++) {
					PersistentLeafSlot<K,V> slot = cursor.getSlot(i);
					if (slot != null)
						map.put(slot.getKey(), slot.getValue());
				}
			}
			cursor = cursor.getNext();
		}
		return map;
	}

	private abstract class Node<K,V> {
		protected InternalNode<K,V> parent;
		protected AtomicReferenceArray<K> keys;
		protected int keycount;
		protected final boolean isLeaf;
		protected boolean needToSplit;

		public Node(int capacity, boolean isLeaf) {
			this.keys = new AtomicReferenceArray<K>(capacity);
			this.isLeaf = isLeaf;
			this.keycount = 0;
			this.needToSplit = false;
			this.parent = null;
		}

		public abstract long readLock();

		public abstract long writeLock();

		public abstract long deleteLock();

		public abstract long applyLock(boolean isWrite);

		public abstract void unlock(long stamp);
	}

	private class InternalNode<K,V> extends Node<K,V> {
		private AtomicReferenceArray<Node<K,V>> children;
		private final StampedLock nodeLock;
		//private final InternalNode<K,V> next;

		public InternalNode() {
			super(MAX_INTERNAL_KEYS + 1, false);
			this.keycount = 0;
			this.children = new AtomicReferenceArray<>(MAX_INTERNAL_KEYS + 2);
			this.nodeLock = new StampedLock();
			if (Config.ENABLE_ALLOC_STATS) Stats.current.allocStats.update(InternalNode.class.getName(), 0, 16 + 8 + 8 * 8 + 8 + 30, 1);   // uncomment for allocation stats
		}

		@SuppressWarnings("unchecked")
		public Node<K,V> getChild(Object key) {
			int idx;
			for (idx = 0; idx < keycount; idx++)
				if (compare(key, keys.get(idx)) <= 0)
					break;
			return children.get(idx);
		}

		public Node<K,V> get(boolean wantFirst) {
			if(wantFirst) return children.get(0);
			else return children.get(this.keycount);
		}

		public void printInternal() {
			StringBuilder sb = new StringBuilder("[");
			for (int idx = 0; idx < keycount; idx++)
				sb.append(keys.get(idx) + ",");
			System.out.println(sb.append("]").toString());
		}

		public long readLock() {
			return nodeLock.readLock();
		}

		public long writeLock() {
			return nodeLock.writeLock();
		}

		public long deleteLock() {
			return nodeLock.readLock();
		}

		public long applyLock(boolean isWrite) {
			if(isWrite) return nodeLock.writeLock();
			return nodeLock.readLock();
		}

		public void unlock(long stamp) {
			nodeLock.unlock(stamp);
		}
	}

	private class LeafNode<K extends AnyPersistent, V extends AnyPersistent> extends Node<K,V> {
		private AtomicIntegerArray hashes;
		private PersistentLeaf<K,V> leaf;
		private final StampedLock nodeLock;
		private K highKey;
		private LeafNode<K,V> next;
		private LeafNode<K,V> prev;

		public LeafNode(PersistentLeaf<K,V> leaf, boolean doReconstruction) {
			super(MAX_LEAF_KEYS + 1, true);
			this.leaf = leaf;
			this.hashes = new AtomicIntegerArray(MAX_LEAF_KEYS + 1);
			this.nodeLock = new StampedLock();
			this.highKey = null;
			this.next = null;
			this.prev = null;
			if (doReconstruction) reconstructVolatileLeafNode();
		}

		public LeafNode(PersistentLeaf<K,V> leaf) {
			this(leaf, false);
		}

		@SuppressWarnings("unchecked")
		private void reconstructVolatileLeafNode() {
			boolean isFull = true;
			//Box<Boolean> isFullBox = new Box(true);
			//Transaction.run(() -> {
			for (int slot = 0; slot <= MAX_LEAF_KEYS; slot++) {
				if (leaf.getSlot(slot) != null) {
					this.hashes.set(slot, leaf.getSlot(slot).getHash());
					K key = leaf.getSlot(slot).getKey();
					//this.hashes.set(slot, generateHash(key));
					this.keys.set(slot, key);
					this.keycount++;
					if (highKey == null)
						highKey = key;
					else if (compare(key, highKey) > 0)
						highKey = key;
				} else
					isFull = false;

			}
			//});
			this.needToSplit = isFull;
		}

		public long readLock() {
			return nodeLock.readLock();
		}

		public long writeLock() {
			return nodeLock.writeLock();
		}

		public long deleteLock() {
			return nodeLock.writeLock();
		}

		public long applyLock(boolean isWrite) {
			if(isWrite) return nodeLock.writeLock();
			return nodeLock.readLock();
		}

		public void unlock(long stamp) {
			nodeLock.unlock(stamp);
		}

        public void tryUnlockWrite() {
            nodeLock.tryUnlockWrite();
        }

		public LeafNode<K,V> getNextNonEmpty() {
			LeafNode<K,V> leafNode = this;
			if (leafNode.next == null) return null;
			else leafNode = leafNode.next;
			while (leafNode.next != null) {
				if (leafNode.keycount != 0) return leafNode;
				leafNode = leafNode.next;
			}
			if (leafNode.keycount == 0) return null;
			else return leafNode;
		}

		@SuppressWarnings("unchecked")
		public V getValue(int hash, Object key) {
			for (int slot = 0; slot <= MAX_LEAF_KEYS; slot++) {
				if (hashes.get(slot) == hash) {
					if (compare(key, keys.get(slot)) == 0)
						return leaf.getSlot(slot).getValue();
				}
			}
			return null;
		}

		private V getValue(K key) {
			return getValue(generateHash(key), key);
		}

		public Map.Entry<K,V> getEntry(K key, boolean wantValue) {
			if(key == null) return null;
			return new AbstractMap.SimpleImmutableEntry<K,V>(key, (wantValue? getValue(key) : null));
		}


		public void printKeys() {
			StringBuilder sb = new StringBuilder("KEYS => [");
			for (int slot = 0; slot <= MAX_LEAF_KEYS; slot++) {
				String temp = (keys.get(slot) == null) ? "X" : keys.get(slot).toString();
				sb.append(/* hashes.get(slot) + "->" + */ temp + ",");
			}
			sb.append("]");
			System.out.println(sb.toString());
		}
	}

	public static final class PersistentLeaf<K extends AnyPersistent, V extends AnyPersistent>
	extends PersistentObject {
		// private static final FinalObjectField<PersistentArray> SLOTS_ARRAY =
		// new FinalObjectField<>(PersistentArray.class);
		private static final ObjectField<PersistentArray> SLOTS_ARRAY = new ObjectField<>(PersistentArray.class);
		private static final ObjectField<PersistentLeaf> NEXT = new ObjectField<>(PersistentLeaf.class);
		private static final ObjectField<PersistentBoolean> IS_EMPTY = new ObjectField<>(PersistentBoolean.class);
		private static final ObjectType<PersistentLeaf> TYPE = ObjectType.fromFields(PersistentLeaf.class, SLOTS_ARRAY,
				NEXT, IS_EMPTY);

		/*
		 * public PersistentLeaf(int size) { super(TYPE, (PersistentLeaf self)
		 * -> { self.initObjectField(SLOTS_ARRAY, new
		 * PersistentArray<PersistentLeafSlot<K,V>>(size));
		 * self.setObjectField(NEXT, null); self.setObjectField(IS_EMPTY, new
		 * PersistentBoolean(true)); }); }
		 */

		public PersistentLeaf(int size) {
			super(TYPE);
			setObjectField(SLOTS_ARRAY, new PersistentArray<PersistentLeafSlot<K,V>>(size));
			setObjectField(NEXT, null);
			setObjectField(IS_EMPTY, new PersistentBoolean(true));
		}

		public PersistentLeaf(ObjectPointer<? extends PersistentLeaf> p) {
			super(p);
		}

		@SuppressWarnings("unchecked")
		private /* synchronized */ PersistentArray<PersistentLeafSlot<K,V>> getSlotsArray() {
			return (PersistentArray<PersistentLeafSlot<K,V>>) getObjectField(SLOTS_ARRAY);
		}

		public /* synchronized */ PersistentLeafSlot<K,V> getSlot(int idx) {
			PersistentArray<PersistentLeafSlot<K,V>> slots = getSlotsArray();
			return slots.get(idx);
		}

		public void setSlot(int idx, PersistentLeafSlot<K,V> slot) {
			PersistentArray<PersistentLeafSlot<K,V>> slots = getSlotsArray();
			slots.set(idx, slot);
		}

		@SuppressWarnings("unchecked")
		public PersistentLeaf<K,V> getNext() {
			return getObjectField(NEXT);
		}

		public void setNext(PersistentLeaf<K,V> next) {
			setObjectField(NEXT, next);
		}

		public void setIsEmpty(boolean status) {
			setObjectField(IS_EMPTY, new PersistentBoolean(status));
		}

		public boolean isEmpty() {
			return getObjectField(IS_EMPTY).booleanValue();
		}

		public PersistentLeaf<K,V> getNextNonEmpty() {
			PersistentLeaf<K,V> leaf = this;
			if (leaf.getNext() == null)
				return null;
			else
				leaf = leaf.getNext();
			// leaf.printLeaf("1st next:");
			while (leaf.getNext() != null) {
				if (!leaf.isEmpty())
					return leaf;
				leaf = leaf.getNext();
				// leaf.printLeaf("next: ");
			}
			if (leaf.isEmpty())
				return null;
			else
				return leaf;
		}

		/*
		 * private void printLeaf(String s) { StringBuilder sb = new
		 * StringBuilder(s); sb.append(" ["); int size =
		 * getIntField(P_MAX_LEAF_KEYS) + 1; for(int i = 0; i < size; i++) {
		 * PersistentLeafSlot<K,V> slot = getSlot(i); if(slot!= null) {
		 * sb.append("" + slot.getValue().toString() + ""); } else {
		 * sb.append("X"); } if(i != size) sb.append(", "); } sb.append("]");
		 * System.out.println(sb.toString()); }
		 */
	}

	public static final class PersistentLeafSlot<K extends AnyPersistent, V extends AnyPersistent>
	extends PersistentObject {
		private static final FinalIntField HASH = new FinalIntField();
		private static final FinalObjectField<AnyPersistent> KEY = new FinalObjectField<>();
		private static final ObjectField<AnyPersistent> VALUE = new ObjectField<>();
		private static final ObjectType<PersistentLeafSlot> TYPE = ObjectType.fromFields(PersistentLeafSlot.class, HASH,
				KEY, VALUE);

		public PersistentLeafSlot(int hash, K key, V value) {
			super(TYPE, (PersistentLeafSlot self) -> {
				self.initIntField(HASH, hash);
				self.initObjectField(KEY, key);
				self.setObjectField(VALUE, value);
			});
		}
		/*
		 * private static final IntField HASH = new IntField(); private static
		 * final ObjectField<AnyPersistent> KEY = new ObjectField<>(); private
		 * static final ObjectField<AnyPersistent> VALUE = new ObjectField<>();
		 * private static final ObjectType<PersistentLeafSlot> TYPE =
		 * ObjectType.fromFields(PersistentLeafSlot.class, HASH, KEY, VALUE);
		 * 
		 * public PersistentLeafSlot(int hash, K key, V value) { super(TYPE);
		 * setIntField(HASH, hash); setObjectField(KEY, key);
		 * setObjectField(VALUE, value); }
		 */

		public PersistentLeafSlot(ObjectPointer<? extends PersistentLeafSlot> p) {
			super(p);
		}

		private /* synchronized */ int getHash() {
			return getIntField(HASH);
		}

		@SuppressWarnings("unchecked")
		private /* synchronized */ K getKey() {
			return (K) getObjectField(KEY);
		}

		@SuppressWarnings("unchecked")
		private /* synchronized */ V getValue() {
			return (V) getObjectField(VALUE);
		}

		private void setValue(V value) {
			setObjectField(VALUE, value);
		}
	}

	/* ---------------- Iterators -------------- */

	abstract class Iter<T> implements Iterator<T> {
		PersistentLeaf<K,V> currentLeaf;
		ArrayList<AbstractMap.SimpleImmutableEntry<K,V>> currentLeafEntries;
		int currentLeafIdx;

		Iter() {
			currentLeaf = getFirstNonEmptyLeaf(getHeadLeaf());
			if (currentLeaf != null)
				currentLeafEntries = new ArrayList<>(MAX_LEAF_KEYS + 1);
			scanAndAdvanceLeaf();
		}

		public final boolean hasNext() {
			if (currentLeaf == null)
				return false;
			else
				return !(currentLeafIdx >= currentLeafEntries.size() && currentLeaf.getNextNonEmpty() == null);
		}

		public Map.Entry<K,V> baseNext() {
			if (!this.hasNext())
				throw new NoSuchElementException();
			if (currentLeafIdx >= currentLeafEntries.size()) {
				currentLeaf = currentLeaf.getNextNonEmpty();
				scanAndAdvanceLeaf();
			}
			return currentLeafEntries.get(currentLeafIdx++);
		}

		protected final void scanAndAdvanceLeaf() {
			currentLeafIdx = 0;
			while (currentLeaf != null && !sortedKeyList(currentLeaf)) {
				currentLeaf = currentLeaf.getNextNonEmpty();
			}
		}

		private boolean sortedKeyList(PersistentLeaf<K,V> leaf) {
			currentLeafEntries.clear();
			for (int slot = 0; slot <= MAX_LEAF_KEYS; slot++) {
				PersistentLeafSlot<K,V> pSlot = leaf.getSlot(slot);
				if (pSlot != null)
					currentLeafEntries
					.add(new AbstractMap.SimpleImmutableEntry<K,V>(pSlot.getKey(), pSlot.getValue()));
			}
			Collections.sort(currentLeafEntries, new Comparator<AbstractMap.SimpleImmutableEntry<K,V>>() {
				public int compare(AbstractMap.SimpleImmutableEntry<K,V> e1,
						AbstractMap.SimpleImmutableEntry<K,V> e2) {
					return compareKeys(e1.getKey(), e2.getKey());
				}
			});

			return currentLeafEntries.size() > 0;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	final class ValueIterator extends Iter<V> {
		public V next() {
			return baseNext().getValue();
		}
	}

	final class KeyIterator implements Iterator<K> {
		LeafNode<K,V> currentLeafNode;
		ArrayList<K> currentLeafEntries;
		int currentLeafIdx;

		public KeyIterator() {
			currentLeafNode = firstNonEmpty();
			currentLeafEntries = new ArrayList<>(MAX_LEAF_KEYS + 1);
			currentLeafIdx = 0;
			scanAndAdvanceLeaf();
		}

		public LeafNode<K,V> firstNonEmpty() {
			LeafNode<K,V> cursor = headLeafNode;
			if (cursor == null) return null;

			while(cursor.keycount == 0 && cursor.next != null) {
				cursor = cursor.next;
			}
			return cursor;
		}

		protected final void scanAndAdvanceLeaf() {
			currentLeafIdx = 0;
			while (currentLeafNode != null && !sortedKeyList(currentLeafNode)) {
				currentLeafNode = currentLeafNode.getNextNonEmpty();
			}
		}

		private boolean sortedKeyList(LeafNode<K,V> leafNode) {
			currentLeafEntries.clear();
			long stamp = leafNode.readLock();
			for (int slot = 0; slot <= MAX_LEAF_KEYS; slot++) {
				K key = leafNode.keys.get(slot);
				if (key != null) currentLeafEntries.add(key);
			}
			leafNode.unlock(stamp);
			Collections.sort(currentLeafEntries, comparator);

			return currentLeafEntries.size() > 0;
		}

		public boolean hasNext() {
			if (currentLeafNode == null) return false;
			else return !(currentLeafIdx >= currentLeafEntries.size() && currentLeafNode.getNextNonEmpty() == null);
		}

		public K next() {
			if (!this.hasNext()) throw new NoSuchElementException();
			if (currentLeafIdx >= currentLeafEntries.size()) {
				currentLeafNode = currentLeafNode.getNextNonEmpty();
				scanAndAdvanceLeaf();
			}
			return currentLeafEntries.get(currentLeafIdx++);
		}
	}

	final class EntryIterator extends Iter<Map.Entry<K,V>> {
		public Map.Entry<K,V> next() {
			return baseNext();
		}
	}

	Iterator<K> keyIterator() {
		return new KeyIterator();
	}

	Iterator<V> valueIterator() {
		return new ValueIterator();
	}

	Iterator<Map.Entry<K,V>> entryIterator() {
		return new EntryIterator();
	}

	private static final <E> List<E> toList(Collection<E> c) {
		// Using size() here would be a pessimization.
		ArrayList<E> list = new ArrayList<E>();
		for (E e : c)
			list.add(e);
		return list;
	}

	static final class KeySet<E extends AnyPersistent> extends AbstractSet<E> implements NavigableSet<E> {
		final ConcurrentNavigableMap<E, ? extends AnyPersistent> m;

		KeySet(ConcurrentNavigableMap<E, ? extends AnyPersistent> map) {
			m = map;
		}

		public int size() {
			return m.size();
		}

		public boolean isEmpty() {
			return m.isEmpty();
		}

		public boolean contains(Object o) {
			return m.containsKey(o);
		}

		public boolean remove(Object o) {
			return m.remove(o) != null;
		}

		public void clear() {
			m.clear();
		}

		public E lower(E e) {
			return m.lowerKey(e);
		}

		public E floor(E e) {
			return m.floorKey(e);
		}

		public E ceiling(E e) {
			return m.ceilingKey(e);
		}

		public E higher(E e) {
			return m.higherKey(e);
		}

		public Comparator<? super E> comparator() {
			return m.comparator();
		}

		public E first() {
			return m.firstKey();
		}

		public E last() {
			return m.lastKey();
		}

		public E pollFirst() {
			Map.Entry<E, ?> e = m.pollFirstEntry();
			return (e == null) ? null : e.getKey();
		}

		public E pollLast() {
			Map.Entry<E, ?> e = m.pollLastEntry();
			return (e == null) ? null : e.getKey();
		}

		@SuppressWarnings("unchecked")
		public Iterator<E> iterator() {
			if (m instanceof PersistentFPTree2)
				return ((PersistentFPTree2<E, AnyPersistent>) m).keyIterator();
			else
		    // throw new UnsupportedOperationException(); // return
			    return ((PersistentFPTree2.SubMap<E,AnyPersistent>)m).keyIterator();
		}

		public boolean equals(Object o) {
			if (o == this)
				return true;
			if (!(o instanceof Set))
				return false;
			Collection<?> c = (Collection<?>) o;
			try {
				return containsAll(c) && c.containsAll(this);
			} catch (ClassCastException unused) {
				return false;
			} catch (NullPointerException unused) {
				return false;
			}
		}

		public Object[] toArray() {
			return toList(this).toArray();
		}

		public <T> T[] toArray(T[] a) {
			return toList(this).toArray(a);
		}

		public Iterator<E> descendingIterator() {
			return descendingSet().iterator();
		}

		public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
			return new KeySet<E>(m.subMap(fromElement, fromInclusive, toElement, toInclusive));
		}

		public NavigableSet<E> headSet(E toElement, boolean inclusive) {
			return new KeySet<E>(m.headMap(toElement, inclusive));
		}

		public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
			return new KeySet<E>(m.tailMap(fromElement, inclusive));
		}

		public NavigableSet<E> subSet(E fromElement, E toElement) {
			return subSet(fromElement, true, toElement, false);
		}

		public NavigableSet<E> headSet(E toElement) {
			return headSet(toElement, false);
		}

		public NavigableSet<E> tailSet(E fromElement) {
			return tailSet(fromElement, true);
		}

		public NavigableSet<E> descendingSet() {
			return new KeySet<E>(m.descendingMap());
		}

		@SuppressWarnings("unchecked")
		public Spliterator<E> spliterator() {
			throw new UnsupportedOperationException();
		}
	}

	static final class Values<E extends AnyPersistent> extends AbstractCollection<E> {
		final ConcurrentNavigableMap<?, E> m;

		Values(ConcurrentNavigableMap<?, E> map) {
			m = map;
		}

		@SuppressWarnings("unchecked")
		public Iterator<E> iterator() {
			if (m instanceof PersistentFPTree2)
				return ((PersistentFPTree2<?, E>) m).valueIterator();
			else
				return ((SubMap<?,E>)m).valueIterator();
				//throw new UnsupportedOperationException();
		}

		public boolean isEmpty() {
			return m.isEmpty();
		}

		public int size() {
			return m.size();
		}

		public boolean contains(Object o) {
			return m.containsValue(o);
		}

		public void clear() {
			m.clear();
		}

		public Object[] toArray() {
			return toList(this).toArray();
		}

		public <T> T[] toArray(T[] a) {
			return toList(this).toArray(a);
		}

		@SuppressWarnings("unchecked")
		public Spliterator<E> spliterator() {
			throw new UnsupportedOperationException();
		}
	}

	static final class EntrySet<K1 extends AnyPersistent, V1 extends AnyPersistent> extends AbstractSet<Map.Entry<K1, V1>> {
		final ConcurrentNavigableMap<K1, V1> m;

		EntrySet(ConcurrentNavigableMap<K1, V1> map) {
			m = map;
		}

		@SuppressWarnings("unchecked")
		public Iterator<Map.Entry<K1, V1>> iterator() {
			if (m instanceof PersistentFPTree2)
				return ((PersistentFPTree2<K1, V1>) m).entryIterator();
			else
				return ((SubMap<K1,V1>)m).entryIterator();
				//throw new UnsupportedOperationException();
		}

		public boolean contains(Object o) {
			if (!(o instanceof Map.Entry))
				return false;
			Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
			V1 v = m.get(e.getKey());
			return v != null && v.equals(e.getValue());
		}

		public boolean remove(Object o) {
			if (!(o instanceof Map.Entry))
				return false;
			Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
			return m.remove(e.getKey(), e.getValue());
		}

		public boolean isEmpty() {
			return m.isEmpty();
		}

		public int size() {
			return m.size();
		}

		public void clear() {
			m.clear();
		}

		public boolean equals(Object o) {
			if (o == this)
				return true;
			if (!(o instanceof Set))
				return false;
			Collection<?> c = (Collection<?>) o;
			try {
				return containsAll(c) && c.containsAll(this);
			} catch (ClassCastException unused) {
				return false;
			} catch (NullPointerException unused) {
				return false;
			}
		}

		public Object[] toArray() {
			return toList(this).toArray();
		}

		public <T> T[] toArray(T[] a) {
			return toList(this).toArray(a);
		}

		@SuppressWarnings("unchecked")
		public Spliterator<Map.Entry<K1, V1>> spliterator() {
			if (m instanceof PersistentFPTree2)
				// return ((PersistentFPTree2<K1,V1>)m).entrySpliterator();
				throw new UnsupportedOperationException();
			else
				// return (Spliterator<Map.Entry<K1,V1>>)
				// ((SubMap<K1,V1>)m).entryIterator();
				throw new UnsupportedOperationException();
		}
	}

	/* SubMap Implementation */
	private static final class SubMap<K extends AnyPersistent,V extends AnyPersistent> implements ConcurrentNavigableMap<K,V> {
		private final PersistentFPTree2<K,V> m;
		private final K lo;
		private final K hi;
		private final boolean loInclusive;
		private final boolean hiInclusive;
		private final boolean isDescending;

		private transient KeySet<K> keySetView;
		private transient Set<Map.Entry<K,V>> entrySetView;
		private transient Collection<V> valuesView;

		SubMap(PersistentFPTree2<K,V> map, K fromKey, boolean fromInclusive, K toKey, boolean toInclusive, boolean isDescending) {
			if (fromKey != null && toKey != null && map.compareKeys(fromKey, toKey) > 0) throw new IllegalArgumentException("inconsistent range");
			this.m = map;
			this.lo = fromKey;
			this.hi = toKey;
			this.loInclusive = fromInclusive;
			this.hiInclusive = toInclusive;
			this.isDescending = isDescending;
		}

		private int compareKeys(K k1, K k2) {
			return m.compareKeys(k1, k2);
		}
		
		boolean tooLow(K key) {
			int c;
			return (lo != null && ((c = compareKeys(key, lo)) < 0 || (c == 0 && !loInclusive)));
		}

		boolean tooHigh(K key) {
			int c;
			return (hi != null && ((c = compareKeys(key, hi)) > 0 || (c == 0 && !hiInclusive)));
		}

		boolean inBounds(K key) {
			return !tooLow(key) && !tooHigh(key);
		}

		@SuppressWarnings("unchecked")
		private boolean inBounds(Object key) { return inBounds((K) key); }

		private void checkKeyBounds(K key) {
			if(!inBounds(key)) throw new IllegalArgumentException("Key out of range");
		}

		public boolean containsKey(Object key) {
			return inBounds(key) && m.containsKey(key);
		}

		public V get(Object key) {
			return (!inBounds(key)) ? null : m.get(key);
		}

		public V put(K key, V value) {
			checkKeyBounds(key);
			return m.put(key, value);
		}

		public V putIfAbsent(K key, V value) {
			checkKeyBounds(key);
			return m.putIfAbsent(key, value);
		}

		public V remove(Object key) {
			return (!inBounds(key)) ? null : m.remove(key);
		}

		public boolean remove(Object key, Object value) {
			return inBounds(key) && m.remove(key, value);
		}

		public boolean replace(K key, V oldValue, V newValue) {
			checkKeyBounds(key);
			return m.replace(key, oldValue, newValue);
		}

		public V replace(K key, V value) {
			checkKeyBounds(key);
			return m.replace(key, value);
		}

		public Comparator<? super K> comparator() {
			Comparator<? super K> cmp = m.comparator();
			if (isDescending) return Collections.reverseOrder(cmp);
			else return cmp;
		}

		public void clear() {
			m.clear(lo, loInclusive, hi, hiInclusive);
		}
		
		public void putAll(Map<? extends K,? extends V> map) {
			throw new UnsupportedOperationException();
		}

		public boolean containsValue(Object value) {
			throw new UnsupportedOperationException(); 
		}

		public boolean isEmpty() {
			return size() == 0;
		}

		public int size() {
			return m.size(lo, loInclusive, hi, hiInclusive);
		}
		//
		public NavigableSet<K> keySet() {
			KeySet<K> ks = keySetView;
			return (ks != null) ? ks : (keySetView = new KeySet<K>(this));
		}

		public NavigableSet<K> navigableKeySet() {
			KeySet<K> ks = keySetView;
			return (ks != null) ? ks : (keySetView = new KeySet<K>(this));
		}

		public Collection<V> values() {
			Collection<V> vs = valuesView;
			return (vs != null) ? vs : (valuesView = new Values<V>(this));
		}

		public Set<Map.Entry<K,V>> entrySet() {
			Set<Map.Entry<K,V>> es = entrySetView;
			return (es != null) ? es : (entrySetView = new EntrySet<K,V>(this));
		}

		@Override
		public ConcurrentNavigableMap<K,V> headMap(K toKey) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ConcurrentNavigableMap<K,V> headMap(K toKey, boolean inclusive) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ConcurrentNavigableMap<K,V> tailMap(K fromKey) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ConcurrentNavigableMap<K,V> tailMap(K fromKey, boolean inclusive) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ConcurrentNavigableMap<K,V> subMap(K fromKey, K toKey) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ConcurrentNavigableMap<K,V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ConcurrentNavigableMap<K,V> descendingMap() {
			throw new UnsupportedOperationException();
		}

		@Override
		public NavigableSet<K> descendingKeySet() {
			throw new UnsupportedOperationException();
		}
		
		private K lowestKey() {
			if (lo == null) return m.firstKey();
			else return loInclusive ? m.ceilingKey(lo) : m.higherKey(lo); //else return m.ceilingKey(lo);
		}
		
		private K highestKey() {
			if (hi == null) return m.lastKey();
			else return hiInclusive ? m.floorKey(hi) : m.lowerKey(hi); //else return m.floorKey(hi);
		}
		
		private Map.Entry<K,V> lowestEntry() {
			if (lo == null) return m.firstEntry();
			else return m.ceilingEntry(lo);
		}
		
		private Map.Entry<K,V> highestEntry() {
			if (hi == null) return m.lastEntry();
			else return m.floorEntry(hi);
		}
		
		private Map.Entry<K,V> removeLowest() {
			if (lo == null) return m.pollFirstEntry();
			else throw new UnsupportedOperationException();
		}
		
		private Map.Entry<K,V> removeHighest() {
			if (hi == null) return m.pollLastEntry();
			else throw new UnsupportedOperationException();
		}
		
        public K firstKey() {
            return isDescending ? highestKey() : lowestKey();
        }

        public K lastKey() {
            return isDescending ? lowestKey() : highestKey();
        }
        
		public Map.Entry<K,V> firstEntry() {
            return isDescending ? highestEntry() : lowestEntry();
        }

        public Map.Entry<K,V> lastEntry() {
            return isDescending ? lowestEntry() : highestEntry();
        }

        public Map.Entry<K,V> pollFirstEntry() {
            return isDescending ? removeHighest() : removeLowest();
        }

        public Map.Entry<K,V> pollLastEntry() {
            return isDescending ? removeLowest() : removeHighest();
        }
		
		public K lowerKey(K key) {
			throw new UnsupportedOperationException();
		}

		public K higherKey(K key) {
			throw new UnsupportedOperationException();
		}

		public Map.Entry<K,V> ceilingEntry(K key) {
			throw new UnsupportedOperationException();
		}

		public K ceilingKey(K key) {
			throw new UnsupportedOperationException();
		}

		public Map.Entry<K,V> lowerEntry(K key) {
			throw new UnsupportedOperationException();
		}

		public Map.Entry<K,V> floorEntry(K key) {
			throw new UnsupportedOperationException();
		}

		public K floorKey(K key) {
			throw new UnsupportedOperationException();
		}

		public Map.Entry<K,V> higherEntry(K key) {
			throw new UnsupportedOperationException();
		}

         /* ---------------- Iterators -------------- */

        abstract class SubMapIter<T> implements Iterator<T> {
            PersistentLeaf<K,V> currentLeaf;
            ArrayList<AbstractMap.SimpleImmutableEntry<K,V>> currentLeafEntries;
            K lastKey;
            int currentLeafIdx;

            SubMapIter() {
                K key = firstKey();
                lastKey = lastKey();
                currentLeaf = m.getPersistentLeaf(key);
                if (currentLeaf != null)
                    currentLeafEntries = new ArrayList<>(m.MAX_LEAF_KEYS + 1);
                scanAndAdvanceLeaf();
                //find first key
                for (int i=0; i<currentLeafEntries.size(); i++) {
                    if (compareKeys(key,currentLeafEntries.get(i).getKey()) <=0) {
                        currentLeafIdx = i;
                        break;
                    }
                }
			}

            public final boolean hasNext() {
                if (currentLeafIdx >= currentLeafEntries.size()) {
                    currentLeaf = currentLeaf.getNextNonEmpty();
                    scanAndAdvanceLeaf();
                }
                if (currentLeaf == null)
                    return false;
                else
                    return !((compareKeys(lastKey,currentLeafEntries.get(currentLeafIdx).getKey()) < 0) || (currentLeafIdx >= currentLeafEntries.size() && currentLeaf.getNextNonEmpty() == null));
            }

            public Map.Entry<K,V> baseNext() {
                if (!this.hasNext())
                    throw new NoSuchElementException();
                return currentLeafEntries.get(currentLeafIdx++);
            }

            protected final void scanAndAdvanceLeaf() {
                currentLeafIdx = 0;
                while (currentLeaf != null && !sortedKeyList(currentLeaf)) {
                    currentLeaf = currentLeaf.getNextNonEmpty();
                }
            }

            private boolean sortedKeyList(PersistentLeaf<K,V> leaf) {
                currentLeafEntries.clear();
                for (int slot = currentLeafIdx; slot <= m.MAX_LEAF_KEYS; slot++) {
                    PersistentLeafSlot<K,V> pSlot = leaf.getSlot(slot);
                    if (pSlot != null)
                        currentLeafEntries
                        .add(new AbstractMap.SimpleImmutableEntry<K,V>(pSlot.getKey(), pSlot.getValue()));
                }
                Collections.sort(currentLeafEntries, new Comparator<AbstractMap.SimpleImmutableEntry<K,V>>() {
                    public int compare(AbstractMap.SimpleImmutableEntry<K,V> e1,
                            AbstractMap.SimpleImmutableEntry<K,V> e2) {
                        return compareKeys(e1.getKey(), e2.getKey());
                    }
                });

                return currentLeafEntries.size() > 0;
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        }

        final class ValueIterator extends SubMapIter<V> {
            public V next() {
                return baseNext().getValue();
            }
        }

        final class KeyIterator implements Iterator<K> {
            PersistentFPTree2<K,V>.LeafNode<K,V> currentLeafNode;
            ArrayList<K> currentLeafEntries;
            K lastKey;
            int currentLeafIdx;

            public KeyIterator() {
                K key = firstKey();
                lastKey = lastKey();
                currentLeafNode = m.getLeafNode(key);
                currentLeafEntries = new ArrayList<>(m.MAX_LEAF_KEYS + 1);
                scanAndAdvanceLeaf();
                for (int i=0; i<currentLeafEntries.size(); i++) {
                    if (compareKeys(key,currentLeafEntries.get(i)) <=0) {
                        currentLeafIdx = i;
                        break;
                    }
                }    
            }

            public PersistentFPTree2<K,V>.LeafNode<K,V> firstNonEmpty() {
                PersistentFPTree2<K,V>.LeafNode<K,V> cursor = m.headLeafNode;
                if (cursor == null) return null;

                while(cursor.keycount == 0 && cursor.next != null) {
                    cursor = cursor.next;
                }
                return cursor;
            }

            protected final void scanAndAdvanceLeaf() {
                currentLeafIdx = 0;
                while (currentLeafNode != null && !sortedKeyList(currentLeafNode)) {
                    currentLeafNode = currentLeafNode.getNextNonEmpty();
                }
            }

            private boolean sortedKeyList(PersistentFPTree2<K,V>.LeafNode<K,V> leafNode) {
                currentLeafEntries.clear();
                long stamp = leafNode.readLock();
                for (int slot = 0; slot <= m.MAX_LEAF_KEYS; slot++) {
                    K key = leafNode.keys.get(slot);
                    if (key != null) currentLeafEntries.add(key);
                }
                leafNode.unlock(stamp);
                Collections.sort(currentLeafEntries, m.comparator);

                return currentLeafEntries.size() > 0;
            }

            public boolean hasNext() {
                if (currentLeafIdx >= currentLeafEntries.size()) {
                    currentLeafNode = currentLeafNode.getNextNonEmpty();
                    scanAndAdvanceLeaf();
                }
                if (currentLeafNode == null) return false;
                else return !((compareKeys(lastKey,currentLeafEntries.get(currentLeafIdx)) < 0) || (currentLeafIdx >= currentLeafEntries.size() && currentLeafNode.getNextNonEmpty() == null));
            }

            public K next() {
                if (!this.hasNext()) throw new NoSuchElementException();
                return currentLeafEntries.get(currentLeafIdx++);
            }
	    }


        final class EntryIterator extends SubMapIter<Map.Entry<K,V>> {
            public Map.Entry<K,V> next() {
                return baseNext();
            }
        }

        Iterator<K> keyIterator() {
            return new KeyIterator();
        }

        Iterator<V> valueIterator() {
            return new ValueIterator();
        }

        Iterator<Map.Entry<K,V>> entryIterator() {
            return new EntryIterator();
        }   
	}

}
