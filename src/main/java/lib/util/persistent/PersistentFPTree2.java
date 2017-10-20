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
import lib.util.persistent.types.ObjectType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.StampedLock;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.Comparator;

public class PersistentFPTree2<K extends AnyPersistent, V extends AnyPersistent> extends PersistentObject implements PersistentSortedMap <K, V> {
	private Node<K, V> root;
	private static final ObjectField<PersistentLeaf> HEAD_LEAF = new ObjectField<>(PersistentLeaf.class);
	private static final IntField P_MAX_LEAF_KEYS = new IntField();
	private static final IntField P_MAX_INTERNAL_KEYS = new IntField();
	private static final ObjectType<PersistentFPTree2> TYPE = ObjectType.fromFields(PersistentFPTree2.class, HEAD_LEAF, P_MAX_LEAF_KEYS, P_MAX_INTERNAL_KEYS);

	private final int MAX_INTERNAL_KEYS;
	private final int MID_INTERNAL_KEYS;
	private final int MAX_LEAF_KEYS;
	private final int MIN_LEAF_KEYS;

	private final StampedLock rootLock;
	private final Comparator<? super K> comparator;

	private static final int[] PEARSON_LOOKUP_TABLE = {
			110, 228, 235, 91, 67, 211, 45, 46, 79, 23, 118, 48, 32, 208, 251, 0, 255, 128, 174, 238, 94, 27, 13, 121, 66, 168, 165, 125, 25, 
			194, 4, 90, 47, 30, 242, 133, 218, 7,203, 114, 231, 180, 96, 248, 214, 249, 122, 75, 163, 41, 88, 243, 221, 76, 89, 29, 145, 119, 
			22, 14, 206, 131, 53, 141, 50, 106, 24, 140, 149, 19, 9, 212, 123, 81, 177,62, 224, 200, 254, 15, 134, 185, 100, 151, 158, 95, 11, 
			63, 21, 233, 204, 127, 210, 92, 154, 198, 172, 56, 197, 186, 152, 226, 55, 97, 217, 193, 213, 115, 236, 175, 86,139, 202, 18, 39, 
			156, 232, 98, 60, 245, 70, 189, 35, 20, 12, 157, 34, 199, 38, 16, 6, 161, 171, 3, 184, 124, 147, 37, 42, 207, 234, 93, 167, 190, 
			205, 28, 135, 77, 148, 143, 10, 117, 237, 138, 112, 223, 8, 150, 136, 183, 176, 179, 191, 101, 105, 43, 103, 195, 219, 192, 132, 
			246, 126, 58, 73, 244, 26, 83, 49, 69, 178, 74, 253, 169, 201, 120, 64, 17, 111, 164, 216, 239, 108, 146, 80, 225, 144, 129, 84, 
			78, 107, 181, 2, 247, 40, 196, 82, 153, 57, 230, 71, 44, 99, 113, 5, 160, 182, 36, 188, 51, 155, 162,1, 33, 142, 102, 166, 85, 
			215, 187, 31, 116, 137, 220, 68, 252, 54, 240, 209, 104, 222, 159, 227, 52, 87, 59, 250, 61, 109, 170, 65, 229, 241, 130, 173, 72
	};

	public PersistentFPTree2(int maxInternalKeys, int maxLeafKeys, Comparator<? super K> comparator) {
		super(TYPE);
		if(maxInternalKeys <= 0) throw new IllegalArgumentException("Number of internal keys must  be > 0");
		if(maxLeafKeys <= 0) throw new IllegalArgumentException("Number of leaf keys must be > 0");

		MAX_INTERNAL_KEYS = maxInternalKeys;
		MID_INTERNAL_KEYS = (MAX_INTERNAL_KEYS + 1)/2;
		MAX_LEAF_KEYS = maxLeafKeys;
		MIN_LEAF_KEYS = (MAX_LEAF_KEYS + 1)/2;

		PersistentLeaf<K, V> headLeaf = new PersistentLeaf<>(MAX_LEAF_KEYS + 1);
		root = new LeafNode<K, V>(headLeaf);
		setIntField(P_MAX_LEAF_KEYS, MAX_LEAF_KEYS);
		setIntField(P_MAX_INTERNAL_KEYS, MAX_INTERNAL_KEYS);
		setObjectField(HEAD_LEAF, headLeaf);
		this.rootLock = new StampedLock();
		this.comparator = comparator;
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
		MID_INTERNAL_KEYS = (MAX_INTERNAL_KEYS + 1)/2;
		MAX_LEAF_KEYS = getIntField(P_MAX_LEAF_KEYS);
		MIN_LEAF_KEYS = (MAX_LEAF_KEYS + 1)/2;
		this.rootLock = new StampedLock();
		this.comparator = null; // how to persist the comparitor?
		long start = System.nanoTime();
		reconstructTree();
		long telap = System.nanoTime() - start;
		Trace.trace("Reconstruction time (secs) for FPTree2 is: " + (telap * 1e-9));
	}


	/*private void reconstructTree() {
		LeafNode<K, V> leafNode, nextLeafNode;
		boolean doReconstruction = true;
		PersistentLeaf<K, V> leaf = getFirstNonEmptyLeaf(getHeadLeaf());
		if(leaf == null) {
			PersistentLeaf<K, V> headLeaf = new PersistentLeaf<>(MAX_LEAF_KEYS + 1);
			setObjectField(HEAD_LEAF, headLeaf);
			root = new LeafNode<K, V>(headLeaf);
			return;
		}
		else {
			setObjectField(HEAD_LEAF, leaf);
		}

		leafNode = new LeafNode<K, V>(leaf, doReconstruction);
		leaf = leaf.getNextNonEmpty();
		//only 1 leaf node
		if(leaf == null) {
			root = leafNode;
			leafNode.leaf.setNext(null); // transaction
		}
		else {
			root = new InternalNode<K, V>();
			nextLeafNode = new LeafNode<K, V>(leaf, doReconstruction);
			leafNode.leaf.setNext(nextLeafNode.leaf);
			buildNewRootNode((InternalNode<K, V>) root, leafNode, nextLeafNode, leafNode.highKey);
			addLeavesFromRight(leaf, leafNode, nextLeafNode);
		}
	}*/

	final boolean reconstruct = true;
	private void reconstructTree() {
		final PersistentLeaf<K, V> firstLeaf = getFirstNonEmptyLeaf(getHeadLeaf());
		if(firstLeaf == null) {
			PersistentLeaf<K, V> headLeaf = new PersistentLeaf<>(MAX_LEAF_KEYS + 1);
			setObjectField(HEAD_LEAF, headLeaf);
			root = new LeafNode<K, V>(headLeaf);
			return;
		}

		//Transaction.run(() -> {
		setObjectField(HEAD_LEAF, firstLeaf);
		final PersistentLeaf<K, V> secondLeaf = firstLeaf.getNextNonEmpty();
		firstLeaf.setNext(secondLeaf);
		if(secondLeaf == null) root = new LeafNode<K, V>(firstLeaf, reconstruct);
		else {
			root = new InternalNode<K, V>();
			final LeafNode<K, V> firstLeafNode = new LeafNode<K, V>(firstLeaf, reconstruct);
			final LeafNode<K, V> secondLeafNode = new LeafNode<K, V>(secondLeaf, reconstruct);
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
			addNewLeafNode(leafNode, nextLeafNode);
		}
		leaf.setNext(null);
	}

	private void addNewLeafNode(LeafNode<K, V> leafNode, LeafNode<K, V> nextLeafNode) {
		rippleSplitHigerInternalNodes(leafNode.parent);
		updateParent(leafNode.parent, leafNode, nextLeafNode, leafNode.highKey);
		//nextLeafNode.parent.printInternal();
	}

	private void rippleSplitHigerInternalNodes(InternalNode<K, V> internalNode) {
		InternalNode<K, V> parent = internalNode.parent;
		if(parent != null) rippleSplitHigerInternalNodes(parent);

		if(internalNode.needToSplit) {
			if(parent == null) root = parent = new InternalNode<K, V>();
			splitInternalNodeAndUpdateParent(parent, internalNode);
		}
	}


	@SuppressWarnings("unchecked")
	private /*synchronized*/ PersistentLeaf<K, V> getHeadLeaf() {
		return (PersistentLeaf<K, V>) getObjectField(HEAD_LEAF);
	}


	@Override
	public int size() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isEmpty() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsKey(Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsValue(Object value) {
		throw new UnsupportedOperationException();
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

	@Override
	@SuppressWarnings("unchecked")
	public V remove(Object key) {
		return doRemove((K) key);
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		throw new UnsupportedOperationException();		
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();		
	}

	@Override
	public Set<K> keySet() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Collection<V> values() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		throw new UnsupportedOperationException();
	}

	@Override
	public K lastKey() {
		throw new UnsupportedOperationException();
	}

	@Override
	public K firstKey() {
		throw new UnsupportedOperationException();
	}

	@Override
	public SortedMap<K,V> headMap(K toKey) {
		throw new UnsupportedOperationException();
	}

	@Override
	public SortedMap<K,V> tailMap(K fromKey) {
		throw new UnsupportedOperationException();
	}

	@Override
	public SortedMap<K,V> subMap(K fromKey, K toKey) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Comparator<? super K> comparator() {
		return this.comparator;
	}

	private int compareKeys(K k1, K k2) {
		if(k1 == null) throw new NullPointerException("Key 1 is null");
		if(k2 == null) throw new NullPointerException("Key 2 is null");
		return compare(k1, k2);
	}

	@SuppressWarnings("unchecked")
	private final int compare(Object k1, Object k2) {
		if(k1 instanceof AnyPersistent) return (comparator == null) ? ((Comparable<? super K>)k1).compareTo((K)k2) : comparator.compare((K)k1, (K)k2);
		else return ((ComparableWith) k2).compareWith(k1) * -1;
	}

	@SuppressWarnings("unchecked")
	private V doRemove(K key) {
		Node<K, V> parent, child;
		long stampParent, stampChild;
		long stampRoot = rootLock.readLock();
		try {
			parent = root; 
			stampParent = parent.deleteLock();
		}
		finally {
			rootLock.unlock(stampRoot);
		}

		try {
			while(!parent.isLeaf) {
				child = ((InternalNode<K, V>) parent).getChild(key);  
				stampChild = child.deleteLock();
				parent.unlock(stampParent); 
				parent = child;
				stampParent = stampChild;
			} 
			return removeInLeaf((LeafNode<K, V>) parent, key); 
		} 
		finally {
			parent.unlock(stampParent); 
		}
	}

	private V removeInLeaf(LeafNode<K, V> leafNode, K key) {
		final int hash = generateHash(key);
		for(int slot = 0; slot <= MAX_LEAF_KEYS; slot++) {
			if(leafNode.hashes.get(slot) == hash) {
				if(key.equals(leafNode.keys.get(slot))) {
					leafNode.hashes.set(slot, 0);
					leafNode.keys.set(slot, null);
					V val = leafNode.leaf.getSlot(slot).getValue();
					final int slotToRemove = slot;
					Transaction.run(() -> {
						leafNode.leaf.setSlot(slotToRemove, null);
						if(leafNode.keycount == 1) leafNode.leaf.setIsEmpty(true);
					});
					leafNode.keycount--;
					return val;
				}
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private V doGet(Object key) {
		Node<K, V> parent, child;
		long stampParent, stampChild;
		long stampRoot = rootLock.readLock();
		try {
			parent = root; 
			stampParent = parent.readLock();
		}
		finally {
			rootLock.unlock(stampRoot);
		}

		try {
			while(!parent.isLeaf) {
				child = ((InternalNode<K, V>) parent).getChild(key);  
				stampChild = child.readLock();
				parent.unlock(stampParent); 
				parent = child;
				stampParent = stampChild;
			} 
			return ((LeafNode<K, V>) parent).getValue(generateHash(key), key); 
		} 
		finally {
			parent.unlock(stampParent); 
		}
	}

	public V put(K key, V value) {
		Node<K, V> parent, child; 
		long stampParent, stampChild;
		long stampRoot = rootLock.writeLock();
		try {
			parent = root; 
			stampParent = parent.writeLock();
			if(parent.needToSplit) {
				InternalNode<K, V> newRoot =  new InternalNode<K, V>();
				splitChildAndUpdateParent(newRoot, parent);
				root = newRoot;
				parent.unlock(stampParent);
				parent = newRoot;
				stampParent = parent.writeLock();
			}
		}
		finally {
			rootLock.unlock(stampRoot);
		}

		try {
			while(!parent.isLeaf) {
				child = ((InternalNode<K, V>) parent).getChild(key);  
				stampChild = child.writeLock();
				try {
					if(child.needToSplit) {
						Node<K, V> newChild = splitChildAndUpdateParent((InternalNode<K, V>) parent, child, key);  
						if(newChild != child) {
							child.unlock(stampChild);
							child = newChild;
							stampChild = child.writeLock();
						} 
					}
				} 
				finally {
					parent.unlock(stampParent); 
					parent = child;
					stampParent = stampChild;
				}
			} 
			return putInLeaf((LeafNode<K, V>) parent, key, value);
		} 
		finally {
			parent.unlock(stampParent); 
		}
	}

	private V putInLeaf(LeafNode<K, V> leafNode, K key, V value) {
		final int hash = generateHash(key);
		// scan for empty/matching slots
		int lastEmptySlot = -1;
		for (int slot = MAX_LEAF_KEYS; slot >= 0 ; slot-- ) {
			int slotHash = leafNode.hashes.get(slot);
			if (slotHash == 0) {
				lastEmptySlot = slot;
			}
			else if (slotHash == hash) {
				if (key.equals(leafNode.keys.get(slot))) {
					V oldValue = leafNode.leaf.getSlot(slot).getValue();
					final int slotToPut = slot;
					Transaction.run(() -> {
						leafNode.leaf.getSlot(slotToPut).setValue(value);
						if(leafNode.keycount == 0) leafNode.leaf.setIsEmpty(false);
					});
					return oldValue;
				}
			}
		}

		final int slot = lastEmptySlot;
		if (slot >= 0) {
			Transaction.run(() -> {
				leafNode.leaf.setSlot(slot, new PersistentLeafSlot<K, V>(hash, key, value));
				if(leafNode.keycount == 0) leafNode.leaf.setIsEmpty(false);
			});
			leafNode.hashes.set(slot, hash);
			leafNode.keys.set(slot, key);
			if((++leafNode.keycount) == MAX_LEAF_KEYS + 1) leafNode.needToSplit = true;
		}
		else {
			throw new IllegalStateException("Leaf full while trying to insert key: " + key);
		}
		return null;
	}

	private Node<K, V> splitChildAndUpdateParent(InternalNode<K, V> parent, Node<K, V> child, K key) {
		splitChildAndUpdateParent(parent, child);
		return parent.getChild(key);
	}

	private void  splitChildAndUpdateParent(InternalNode<K, V> parent, Node<K, V> child) {
		if(child.isLeaf) splitLeafNodeAndUpdateParent(parent, (LeafNode<K, V>) child);
		else splitInternalNodeAndUpdateParent(parent, (InternalNode<K, V>) child);
	}

	private Node<K, V> splitLeafNodeAndUpdateParent(InternalNode<K, V> parent, LeafNode<K, V> leafNode) {
		return splitLeafNodeAndUpdateParent(parent, leafNode, getSplitKey(leafNode));
	}

	private LeafNode<K, V> splitLeafNodeAndUpdateParent(InternalNode<K, V> parent, LeafNode<K, V> leafNode, K splitKey) {
		PersistentLeaf<K, V> newLeaf = new PersistentLeaf<>(MAX_LEAF_KEYS + 1);
		LeafNode<K, V> newLeafNode = new LeafNode<>(newLeaf);

		Transaction.run(() -> {
			for (int slot = 0; slot <= MAX_LEAF_KEYS; slot++) {
				if(compareKeys(splitKey, leafNode.keys.get(slot)) < 0) {
					newLeaf.setSlot(slot, leafNode.leaf.getSlot(slot));
					leafNode.leaf.setSlot(slot, null);
				}
			}
			newLeafNode.leaf.setNext(leafNode.leaf.getNext());
			leafNode.leaf.setNext(newLeafNode.leaf);
			newLeafNode.leaf.setIsEmpty(false);
			leafNode.leaf.setIsEmpty(false);
		});

		for (int slot = 0; slot <= MAX_LEAF_KEYS; slot++) {
			if(compareKeys(splitKey, leafNode.keys.get(slot)) < 0) {
				newLeafNode.hashes.set(slot, leafNode.hashes.get(slot));
				newLeafNode.keys.set(slot, leafNode.keys.get(slot));
				leafNode.hashes.set(slot, 0);
				leafNode.keys.set(slot, null);
				leafNode.keycount--;
				newLeafNode.keycount++;
			}
		}
		newLeafNode.parent = leafNode.parent = parent;
		updateParent(parent, leafNode, newLeafNode, splitKey);
		newLeafNode.needToSplit = leafNode.needToSplit = false;
		return newLeafNode;
	}

	private void buildNewRootNode(InternalNode<K, V> root, Node<K, V> leftChild, Node<K, V> rightChild, K splitKey) {
		root.keys.set(0, splitKey);
		root.keycount = 1;
		leftChild.parent = rightChild.parent = root;
		root.children.set(0, leftChild);
		root.children.set(1, rightChild);
	}


	private void updateParent(InternalNode<K, V> parent, Node<K, V> child, Node<K, V> newChild, K splitKey) {
		if(parent.keycount == 0) buildNewRootNode(parent, child, newChild, splitKey);
		else {
			final int keycount = parent.keycount;
			int idx = 0;
			for( ; idx < keycount; idx++) if(compareKeys(splitKey, parent.keys.get(idx)) <= 0) break;
			for(int i = keycount - 1; i >= idx; i--) parent.keys.set(i + 1, parent.keys.get(i));
			for(int i = keycount; i > idx; i--) parent.children.set(i + 1, parent.children.get(i));
			parent.keys.set(idx, splitKey);
			parent.children.set(idx + 1, newChild);
			parent.keycount = keycount + 1;
			newChild.parent = child.parent = parent;
			if(parent.keycount == MAX_INTERNAL_KEYS + 1) parent.needToSplit = true;
		}
	}

	private Node<K, V> splitInternalNodeAndUpdateParent(InternalNode<K, V> parent, InternalNode<K, V> child) {

		InternalNode<K, V> newChild = new InternalNode<>();
		//newChild.parent = child.parent = parent;
		newChild.needToSplit = child.needToSplit = false;
		final int splitIdx = MID_INTERNAL_KEYS + 1;
		final int keycount = child.keycount;
		for(int i = splitIdx; i < keycount; i++) {
			newChild.keys.set(i - splitIdx, child.keys.get(i));
			child.keys.set(i, null);
		}

		for(int i = splitIdx; i < keycount + 1; i++) {
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

	@SuppressWarnings("unchecked")
	private K getSplitKey(LeafNode<K, V> leafNode) {
		Object[] keys = new Object[MAX_LEAF_KEYS + 1];
		for (int slot = 0; slot <= MAX_LEAF_KEYS; slot++) {
			keys[slot] = leafNode.keys.get(slot);
		}
		Arrays.sort(keys);
		return (K) keys[MIN_LEAF_KEYS - 1];
	}

	// Modified Pearson hashing algorithm from pmemkv (https://github.com/pmem/pmemkv/blob/master/src/pmemkv.cc)
	/*public int pearsonHash(K key) {
		String skey;
		if(key instanceof PersistentInteger) skey = Integer.toString(((PersistentInteger) key).intValue());
		else skey = ((PersistentString) key).toString();
		return computePearsonHash(skey.getBytes());
	}*/

	public int computePearsonHash(byte[] data) {
		int hash = data.length;
		for(int i = data.length; i > 0; ) {
			hash = PEARSON_LOOKUP_TABLE[hash ^ data[--i]];
		}
		hash = (hash == 0) ? 1 : hash;
		return hash;
	}

	public int generateHash(Object key) {
		return computePearsonHash(Integer.toString(key.hashCode()).getBytes());
	}

	public void printLeaves() {
		PersistentLeaf<K, V> cursor = getHeadLeaf();
		StringBuilder sb = new StringBuilder("HEAD-->");
		while(cursor != null) {
			printLeaf(cursor, sb);
			sb.append("-->");
			cursor = cursor.getNext();
		}
		sb.append("NULL");
		System.out.println(sb.toString());
	}

	private void printLeaf(PersistentLeaf<K, V> leaf) {
		System.out.println(printLeaf(leaf, new StringBuilder("")).toString());
	}

	private StringBuilder printLeaf(PersistentLeaf<K, V> cursor, StringBuilder sb) {
		sb.append("[");
		for(int i = 0; i < this.MAX_LEAF_KEYS + 1; i++) {
			PersistentLeafSlot<K, V> slot = cursor.getSlot(i);
			if(slot!= null) {
				sb.append("" + slot.getValue().toString() + "");
			}
			else {
				sb.append("X");
			}
			if(i != this.MAX_LEAF_KEYS) sb.append(", ");
		}
		sb.append("]");
		return sb;
	}


	public void verifyDelete(String msg) {
		if(verifyDelete()) System.out.println(msg + "REMOVE VERIFICATION: SUCCESS");
	}

	public boolean verifyDelete(){
		PersistentLeaf<K, V> cursor = getHeadLeaf();
		while(cursor != null) {
			for(int i = 0; i < this.MAX_LEAF_KEYS + 1; i++) {
				PersistentLeafSlot<K, V> slot = cursor.getSlot(i);
				if(slot != null) throw new IllegalStateException("REMOVE FAILED");
			}
			cursor = cursor.getNext();
		}
		return true;
	}

	public void randomlyDeleteLeaves(){
		PersistentLeaf<K, V> cursor = getHeadLeaf();
		Random rndb = new Random();
		while(cursor != null) {
			boolean val = rndb.nextBoolean();
			if(val == true) {
				cursor.setIsEmpty(true);
				for(int i = 0; i < MAX_LEAF_KEYS + 1; i++) {cursor.setSlot(i, null);}
			}
			cursor = cursor.getNext();
		}
	}

	public HashMap<K, V> getHashMap() {
		PersistentLeaf<K, V> cursor = getHeadLeaf();
		HashMap<K, V> map = new HashMap<>();
		while(cursor != null) {
			if(!cursor.isEmpty()) {
				for(int i = 0; i < MAX_LEAF_KEYS + 1; i++) {
					PersistentLeafSlot<K, V> slot = cursor.getSlot(i);
					if(slot != null) map.put(slot.getKey(), slot.getValue());
				}
			}
			cursor = cursor.getNext();
		}
		return map;
	}

	private abstract class Node<K, V> {
		protected InternalNode<K, V> parent;
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
		public abstract void unlock(long stamp);
	}

	private class InternalNode<K, V> extends Node<K, V> {
		private AtomicReferenceArray<Node<K, V>> children;
		private final StampedLock nodeLock;

		public InternalNode(){
			super(MAX_INTERNAL_KEYS + 1, false);
			this.keycount = 0;
			this.children = new AtomicReferenceArray<>(MAX_INTERNAL_KEYS + 2);
			this.nodeLock = new StampedLock();
		}

		@SuppressWarnings("unchecked")
		public Node<K, V> getChild(Object key) {
			int idx;
			for(idx = 0; idx < keycount; idx++) 
				if(compare(key, keys.get(idx)) <= 0) break;
			return children.get(idx);
		}

		public void printInternal() {
			StringBuilder sb = new StringBuilder("[");
			for(int idx = 0; idx < keycount; idx++) sb.append(keys.get(idx) + ",");
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

		public void unlock(long stamp) {
			nodeLock.unlock(stamp);
		}
	}

	private class LeafNode<K extends AnyPersistent, V extends AnyPersistent> extends Node<K, V> {
		private AtomicIntegerArray hashes;
		private PersistentLeaf<K, V> leaf;
		private final StampedLock nodeLock;
		private K highKey;

		public LeafNode(PersistentLeaf<K, V> leaf, boolean doReconstruction) {
			super(MAX_LEAF_KEYS + 1, true);
			this.leaf = leaf;
			this.hashes = new AtomicIntegerArray(MAX_LEAF_KEYS + 1);
			this.nodeLock = new StampedLock();
			this.highKey = null;
			if(doReconstruction) reconstructVolatileLeafNode();
		}

		public LeafNode(PersistentLeaf<K, V> leaf) {
			this(leaf, false);
		}

		@SuppressWarnings("unchecked")
		private void reconstructVolatileLeafNode() {
			boolean isFull = true;
			for(int slot = 0; slot <= MAX_LEAF_KEYS; slot++) {
				if(leaf.getSlot(slot) != null) {
					this.hashes.set(slot, leaf.getSlot(slot).getHash());
					K key = leaf.getSlot(slot).getKey();
					this.keys.set(slot, key);
					this.keycount++;
					if(highKey == null) highKey = key;
					else if (compare(key, highKey) > 0) highKey = key;
				}
				else isFull = false;
				this.needToSplit = isFull;
			}
		}

		@SuppressWarnings("unchecked")
		public V getValue(int hash, Object key) {
			for(int slot = 0; slot <= MAX_LEAF_KEYS; slot++) {
				if(hashes.get(slot) == hash) {
					if(compare(key, keys.get(slot)) == 0) return leaf.getSlot(slot).getValue();
				}
			}
			return null;
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

		public void unlock(long stamp) {
			nodeLock.unlock(stamp);
		}

		public void printKeys() {
			StringBuilder sb = new StringBuilder("KEYS => [");
			for(int slot = 0; slot <= MAX_LEAF_KEYS; slot++) {
				String temp = (keys.get(slot) == null)? "X" : keys.get(slot).toString();
				sb.append(/*hashes.get(slot) + "->" + */ temp + ",");
			}
			sb.append("]");
			System.out.println(sb.toString());
		}
	}

	public static final class PersistentLeaf<K extends AnyPersistent, V extends AnyPersistent> extends PersistentObject {
		private static final ObjectField<PersistentArray> SLOTS_ARRAY = new ObjectField<>(PersistentArray.class);
		private static final ObjectField<PersistentLeaf> NEXT = new ObjectField<>(PersistentLeaf.class);
		private static final ObjectField<PersistentBoolean> IS_EMPTY = new ObjectField<>(PersistentBoolean.class);
		private static final ObjectType<PersistentLeaf> TYPE = ObjectType.fromFields(PersistentLeaf.class, SLOTS_ARRAY, NEXT, IS_EMPTY);

		public PersistentLeaf(int size) {
			super(TYPE);
			setObjectField(SLOTS_ARRAY, new PersistentArray<PersistentLeafSlot<K, V>>(size));
			setObjectField(NEXT, null);
			setObjectField(IS_EMPTY, new PersistentBoolean(true));
		}

		public PersistentLeaf(ObjectPointer<? extends PersistentLeaf> p) {super(p);}

		@SuppressWarnings("unchecked")
		private /*synchronized*/ PersistentArray<PersistentLeafSlot<K, V>> getSlotsArray(){
			return (PersistentArray<PersistentLeafSlot<K, V>>) getObjectField(SLOTS_ARRAY);
		}

		public /*synchronized*/ PersistentLeafSlot<K, V> getSlot(int idx) {
			PersistentArray<PersistentLeafSlot<K, V>> slots = getSlotsArray();
			return slots.get(idx);
		}

		public void setSlot(int idx, PersistentLeafSlot<K, V> slot) {
			PersistentArray<PersistentLeafSlot<K, V>> slots = getSlotsArray();
			slots.set(idx, slot);
		}

		@SuppressWarnings("unchecked")
		public PersistentLeaf<K, V> getNext() {
			return getObjectField(NEXT);
		}

		public void setNext(PersistentLeaf<K, V> next) {
			setObjectField(NEXT, next);
		}

		public void setIsEmpty(boolean status) {
			setObjectField(IS_EMPTY, new PersistentBoolean(status));
		}

		public boolean isEmpty() {
			return getObjectField(IS_EMPTY).booleanValue();
		}

		public PersistentLeaf<K, V> getNextNonEmpty() {
			PersistentLeaf<K, V> leaf = this;
			if(leaf.getNext() == null) return null;
			else leaf = leaf.getNext();
			//leaf.printLeaf("1st next:");
			while(leaf.getNext() != null) {
				if(!leaf.isEmpty()) return leaf;
				leaf = leaf.getNext();
				//leaf.printLeaf("next: ");
			}
			if(leaf.isEmpty()) return null;
			else return leaf;
		}

		/*private void printLeaf(String s) {
			StringBuilder sb = new StringBuilder(s);
			sb.append(" [");
			int size = getIntField(P_MAX_LEAF_KEYS) + 1;
			for(int i = 0; i < size; i++) {
				PersistentLeafSlot<K, V> slot = getSlot(i);
				if(slot!= null) {
					sb.append("" + slot.getValue().toString() + "");
				}
				else {
					sb.append("X");
				}
				if(i != size) sb.append(", ");
			}
			sb.append("]");
			System.out.println(sb.toString());
		}*/
	}

	public static final class PersistentLeafSlot<K extends AnyPersistent, V extends AnyPersistent> extends PersistentObject {
		private static final IntField HASH = new IntField();
		private static final ObjectField<AnyPersistent> KEY = new ObjectField<>();
		private static final ObjectField<AnyPersistent> VALUE = new ObjectField<>();
		private static final ObjectType<PersistentLeafSlot> TYPE = ObjectType.fromFields(PersistentLeafSlot.class, HASH, KEY, VALUE);

		public PersistentLeafSlot(int hash, K key, V value) {
			super(TYPE);
			setIntField(HASH, hash);
			setObjectField(KEY, key);
			setObjectField(VALUE, value);
		}

		public PersistentLeafSlot(ObjectPointer<? extends PersistentLeafSlot> p) {super(p);}

		private /*synchronized*/ int getHash() {
			return getIntField(HASH);
		}

		@SuppressWarnings("unchecked")
		private /*synchronized*/ K getKey() {
			return (K) getObjectField(KEY);
		}

		@SuppressWarnings("unchecked")
		private /*synchronized*/ V getValue() {
			return (V) getObjectField(VALUE);
		}

		private void setValue(V value) {
			setObjectField(VALUE, value);
		}
	}

}


