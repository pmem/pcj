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
import java.util.concurrent.atomic.*;
import java.lang.StringBuilder;
import java.util.Collection;
import java.util.Queue;
import java.util.NoSuchElementException;
import java.util.ArrayList;
import java.util.Iterator;

public class PersistentLinkedQueue <E extends AnyPersistent> extends PersistentObject implements Iterable<E> {	

	private static final ObjectField<PersistentAtomicReference> HEAD = new ObjectField<>(PersistentAtomicReference.class);
	private static final ObjectField<PersistentAtomicReference> TAIL = new ObjectField<>(PersistentAtomicReference.class);
	public static final ObjectType<PersistentLinkedQueue> TYPE = ObjectType.withFields(PersistentLinkedQueue.class, HEAD, TAIL);

	public PersistentLinkedQueue() {
		this(TYPE);
	}

	protected PersistentLinkedQueue(ObjectType<? extends PersistentLinkedQueue> type) {
		super(type);
		Transaction.run(() -> {
			PersistentNode<E> sentinel = new PersistentNode<>();
			setHeadAndTail(sentinel, sentinel);
		});
	}

	public PersistentLinkedQueue(Collection<? extends E> c) {
		super(TYPE);
		Transaction.run(() -> {
			PersistentNode<E> h = null, t = null;
			for (E e : c) {
				checkNotNull(e);
				PersistentNode<E> newNode = new PersistentNode<E>(e);
				if (h == null) h = t = newNode;
				else {
					t.setNextNode(newNode);
					t = newNode;
				}
			}
			if (h == null) h = t = new PersistentNode<E>();
			setHeadAndTail(h, t);
		});
	}

	public PersistentLinkedQueue(ObjectPointer<? extends PersistentLinkedQueue> p) {super(p);}

	private void setHeadAndTail(PersistentNode<E> h, PersistentNode<E> t) {
		Transaction.run(() -> {
			setHead(new PersistentAtomicReference<PersistentNode<E>>(h));
			setTail(new PersistentAtomicReference<PersistentNode<E>>(t));
		});
	}
	private void setHead(PersistentAtomicReference<PersistentNode<E>> first) {
		setObjectField(HEAD, first);
	}

	private void setTail(PersistentAtomicReference<PersistentNode<E>> last) {
		setObjectField(TAIL, last);
	}

	@SuppressWarnings("unchecked")
	private PersistentNode<E> getHead() {
		return (PersistentNode<E>)getObjectField(HEAD).get();
	}

	@SuppressWarnings("unchecked")
	private PersistentAtomicReference<PersistentNode<E>> getHeadRef() {
		return getObjectField(HEAD);
	}


	@SuppressWarnings("unchecked")
	private PersistentNode<E> getTail() {
		return (PersistentNode<E>)getObjectField(TAIL).get();
	}

	@SuppressWarnings("unchecked")
	private boolean casHead(PersistentNode<E> expect, PersistentNode<E> update) {
		return getObjectField(HEAD).compareAndSet(expect, update);
	}

	@SuppressWarnings("unchecked")
	private boolean casTail(PersistentNode<E> expect, PersistentNode<E> update) {
		return getObjectField(TAIL).compareAndSet(expect, update);
	}

	//enqueue
	public boolean add(E e) {
		return offer(e);
	}

	//enqueue
	public boolean offer(E e) {
		checkNotNull(e);
		final PersistentNode<E> newNode = new PersistentNode<E>(e);

		for (PersistentNode<E> t = getTail(), p = t;;) {
			PersistentNode<E> q = p.getNext();
			if (q == null) {
				if (p.casNext(null, newNode)) {
					if(p != t) casTail(t, newNode);  
					return true;
				}
			}
			else if (p == q) p = (t != (t = getTail())) ? t : getHead();
			else p = (p != t && t != (t = getTail())) ? t : q;
		}
	}

	final void updateHead(PersistentAtomicReference<PersistentNode<E>> hd, PersistentNode<E> p) {
		PersistentNode<E> h = hd.get();
		if (h != p && casHead(h, p)) h.setNext(hd);
	}

	//dequeue 
	public E poll() {
		restartFromHead:
			for (;;) {
				PersistentAtomicReference<PersistentNode<E>> hd = getHeadRef();
				for (PersistentNode<E> h = hd.get(), p = h, q;;) {            			
					E item = p.getItem();
					if (item != null && p.casItem(item, null)) {
						if (p != h) updateHead(hd, ((q = p.getNext()) != null) ? q : p);
						return item;
					}
					else if ((q = p.getNext()) == null) {
						updateHead(hd, p);
						return null;
					}
					else if (p == q) continue restartFromHead;

					else p = q;
				}
			}
	}

	public E peek() {
		restartFromHead:
			for (;;) {
				PersistentAtomicReference<PersistentNode<E>> hd = getHeadRef();
				for (PersistentNode<E> h = hd.get(), p = h, q;;) {
					E item = p.getItem();
					if (item != null || (q = p.getNext()) == null) {
						updateHead(hd, p);
						return item;
					}
					else if (p == q) continue restartFromHead;
					else p = q;
				}
			}
	}

	//Returns the first actual (non-header) node on list
	PersistentNode<E> first() {
		restartFromHead:
			for (;;) {
				PersistentAtomicReference<PersistentNode<E>> hd = getHeadRef();
				for (PersistentNode<E> h = hd.get(), p = h, q;;) {
					boolean hasItem = (p.getItem() != null);
					if (hasItem || (q = p.getNext()) == null) {
						updateHead(hd, p);
						return hasItem ? p : null;
					}
					else if (p == q) continue restartFromHead;
					else p = q;
				}
			}
	}

	public E element() {
		E x = peek();
		if(x != null) return x;
		else throw new NoSuchElementException();		
	}

	//dequeue
	public E remove() {
		E x = poll();
		if(x != null) return x;
		else throw new NoSuchElementException();
	}

	final PersistentNode<E> succ(PersistentNode<E> p) {
		PersistentNode<E> next = p.getNext();
		return (p == next) ? getHead() : next;
	}

	public boolean remove(Object o) {
		if(o == null) return false;
		PersistentNode<E> pred = null;
		for(PersistentNode<E> p = first(); p != null; p = succ(p)) {
			E item = p.getItem();
			if(item != null && o.equals(item) && p.casItem(item, null)) {
				PersistentNode<E> next = succ(p);
				if(pred != null && next != null) pred.casNext(p, next);
				return true;
			}
			pred = p;
		}
		return false;
	}

	public boolean contains(Object o) {
		if (o == null) return false;
		for (PersistentNode<E> p = first(); p != null; p = succ(p)) {
			E item = p.getItem();
			if (item != null && o.equals(item)) return true;
		}
		return false;
	}

	public void clear() {
		while(poll() != null);
	}

	public int size() {
		int count = 0;
		for(PersistentNode<E> p = first(); p != null; p = succ(p)) 
			if(p.getItem() != null) 
				if(++count == Integer.MAX_VALUE) break;
		return count;
	}

	public boolean isEmpty() {
		return first() == null;
	}

	@SuppressWarnings("unchecked")
	public <T> T[] toArray(T[] a) {
		int k = 0;
		PersistentNode<E> p;
		for(p = first(); p != null && k < a.length; p = succ(p)) {
			E item = p.getItem();
			if(item != null) a[k++] = (T)item;
		}
		if(p == null) {
			if(k < a.length) a[k] = null;
			return a;
		}

		ArrayList<E> al = new ArrayList<E>();
		for(PersistentNode<E> q = first(); q != null; q = succ(q)) {
			E item = q.getItem();
			if(item != null) al.add(item);
		}
		return al.toArray(a);
	}

	public Object[] toArray() {
		ArrayList<E> al = new ArrayList<E>();
		for(PersistentNode<E> p = first(); p != null; p = succ(p)) {
			E item = p.getItem();
			if(item != null) al.add(item);
		}
		return al.toArray();		
	}

	public boolean addAll(Collection<? extends E> c) {
		if (c == this) throw new IllegalArgumentException();

		PersistentNode<E> beginningOfTheEnd = null, last = null;
		for (E e : c) {
			checkNotNull(e);
			PersistentNode<E> newNode = new PersistentNode<E>(e);
			if (beginningOfTheEnd == null) beginningOfTheEnd = last = newNode;
			else {
				last.setNextNode(newNode);
				last = newNode;
			}
		}
		if (beginningOfTheEnd == null) return false;

		for (PersistentNode<E> t = getTail(), p = t;;) {
			PersistentNode<E> q = p.getNext();
			if (q == null) {
				if (p.casNext(null, beginningOfTheEnd)) {
					if (!casTail(t, last)) {
						t = getTail();
						if (last.getNext() == null) casTail(t, last);
					}
					return true;
				}
			}
			else if (p == q) p = (t != (t = getTail())) ? t : getHead();
			else p = (p != t && t != (t = getTail())) ? t : q;
		}
	}	

	public String toString() {
		StringBuilder sb = new StringBuilder();
		if(isEmpty()) sb.append("Empty Queue");
		else {
			PersistentNode<E> cursor = first();
			do {
				String item = (cursor.getItem() == null)? "(null)" : cursor.getItem().toString();
				sb.append(item);
				sb.append("-->");
				cursor = cursor.getNext();
			} while(cursor != null);
			sb.append("NULL");    			
		}
		return sb.toString();
	}

	private static void checkNotNull(Object v) {
		if (v == null) throw new NullPointerException();
	}

	public static class PersistentNode<E extends AnyPersistent> extends PersistentObject {
		private static final ObjectField<PersistentAtomicReference> ITEM = new ObjectField<>(PersistentAtomicReference.class);
		private static final ObjectField<PersistentAtomicReference> NEXT = new ObjectField<>(PersistentAtomicReference.class);
		public static final ObjectType<PersistentNode> TYPE = ObjectType.withFields(PersistentNode.class, ITEM, NEXT);

		protected PersistentNode(ObjectType<? extends PersistentNode> type, E item, PersistentAtomicReference<PersistentNode<E>> next) {
			super(type);
			Transaction.run(() -> {
				setObjectField(ITEM, new PersistentAtomicReference<E>(item));
				setObjectField(NEXT, next);
			});
		}

		public PersistentNode(E item, PersistentAtomicReference<PersistentNode<E>> next) {
			this(TYPE, item, next); 
		}

		public PersistentNode(E item) {
			this(TYPE, item, new PersistentAtomicReference<PersistentNode<E>>());
		}

		public PersistentNode() {
			this(TYPE, null, new PersistentAtomicReference<PersistentNode<E>>());
		}

		public PersistentNode(ObjectPointer<? extends PersistentNode> p) {super(p);}

		@SuppressWarnings("unchecked")
		public E getItem() {
			return (E)getObjectField(ITEM).get();
		}

		@SuppressWarnings("unchecked")
		public void removeItem() {
			getObjectField(ITEM).set(null);
		}

		@SuppressWarnings("unchecked")
		public PersistentNode<E> getNext() {
			return (PersistentNode<E>)getObjectField(NEXT).get();
		}

		private void setNext(PersistentAtomicReference<PersistentNode<E>> next) {
			setObjectField(NEXT, next);
		}

		public void setNextNode(PersistentNode<E> node) {
			setObjectField(NEXT, new PersistentAtomicReference<PersistentNode<E>>(node));
		}

		@SuppressWarnings("unchecked")
		public boolean casNext(PersistentNode<E> expect, PersistentNode<E> update) {
			return getObjectField(NEXT).compareAndSet(expect, update);
		}

		@SuppressWarnings("unchecked")
		public boolean casItem(E expect, E update) {
			return getObjectField(ITEM).compareAndSet(expect, update);
		}
	}

	public Iterator<E> iterator() {
		return new Itr();
	}

	private class Itr implements Iterator<E> {
		private PersistentNode<E> nextNode;
		private E nextItem;
		private PersistentNode<E> lastRet;

		Itr() {
			advance();
		}

		private E advance() {
			lastRet = nextNode;
			E x = nextItem;

			PersistentNode<E> pred, p;
			if (nextNode == null) {
				p = first();
				pred = null;
			} else {
				pred = nextNode;
				p = succ(nextNode);
			}

			for (;;) {
				if (p == null) {
					nextNode = null;
					nextItem = null;
					return x;
				}
				E item = p.getItem();
				if (item != null) {
					nextNode = p;
					nextItem = item;
					return x;
				} else {
					PersistentNode<E> next = succ(p);
					if (pred != null && next != null) pred.casNext(p, next);
					p = next;
				}
			}
		}

		public boolean hasNext() {
			return nextNode != null;
		}

		public E next() {
			if(nextNode == null) throw new NoSuchElementException();
			return advance();

		}

		public void remove() {
			PersistentNode<E> l = lastRet;
			if(l == null) throw new IllegalStateException();
			l.removeItem();
			lastRet = null;
		}
	}

}
