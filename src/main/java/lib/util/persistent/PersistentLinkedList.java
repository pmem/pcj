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
import java.lang.StringBuilder;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class PersistentLinkedList<E extends AnyPersistent> extends PersistentObject implements Iterable<E> {
	
    private static final IntField SIZE = new IntField();
    private static final ObjectField<PersistentEntry> HEADER = new ObjectField<>(PersistentEntry.class);
    private static final ObjectType<PersistentLinkedList> TYPE = ObjectType.withFields(PersistentLinkedList.class, SIZE, HEADER);
    private Class<E> elementClass; 
	
    public PersistentLinkedList() {
        super(TYPE);
        setSize(0);
        setHeader(new PersistentEntry<>(null, null));
    }    

    // protected PersistentLinkedList(ObjectType<? extends PersistentLinkedList> type) {
    //     super(type);
    //     setSize(0);
    //     setHeader(new PersistentEntry<E>(null, null, null));
    // }
	
	protected PersistentLinkedList(ObjectPointer<? extends PersistentLinkedList> p) {super(p);}
	
	public synchronized void add(E element) {
		Transaction.run(() -> {	
			if(size() == 0) getHeader().setElement(element);
	        else {
	        	PersistentEntry<E> last = traverseToEntryAt(size()-1);
			    last.setNext(new PersistentEntry<E>(element, null));
	        }
			incrementSize();
		});
	}
	
	public synchronized void insert(int index, E element) {
		if (index < 0 || index > size()) throw new IndexOutOfBoundsException("Correct range of index is [0,Size]");		
		
		Transaction.run(() -> {	
			if(index == 0) setHeader(new PersistentEntry<E>(element, getHeader()));
			else {
				PersistentEntry<E> prev = traverseToEntryAt(index-1);
				PersistentEntry<E> newEntry = new PersistentEntry<E>(element, prev.getNext());
				prev.setNext(newEntry);
			}
			incrementSize();
		});
	}
	
	public synchronized void remove(int index) {
		if (index < 0 || index >= size()) throw new IndexOutOfBoundsException("Correct range of index is [0,Size)");

        Transaction.run(() -> {			
			
			PersistentEntry<E> toDelete;
			if(index == 0) {
				toDelete = getHeader();
				setHeader(toDelete.getNext());				
			}
			else {		
				PersistentEntry<E> prev = traverseToEntryAt(index-1);
				toDelete = prev.getNext();
				prev.setNext(prev.getNext().getNext());					
			}
			toDelete.setElement(null);
			toDelete.setNext(null);
			decrementSize();
		});
	}
	
	public synchronized void clear() {
		Transaction.run(() -> {		
			PersistentEntry<E> cursor = getHeader();
			for(int i=0; i < size(); i++) {
				PersistentEntry<E> current = cursor;
				cursor = cursor.getNext();
				current.setElement(null);
				current.setNext(null);
			}
			setSize(0);
		});
	}
	

	public synchronized E get(int index) {
		if (index < 0 || index >= size()) throw new IndexOutOfBoundsException("Correct range of index is [0,Size)");
				
		PersistentEntry<E> cursor = traverseToEntryAt(index);
		return cursor.getElement();
	}
	
	public synchronized void set(int index, E element) {
		if (index < 0 || index >= size()) throw new IndexOutOfBoundsException("Correct range of index is [0,Size)");
		
		Transaction.run(() -> {		
			PersistentEntry<E> cursor = traverseToEntryAt(index);
			cursor.setElement(element);
		});
	}
	
	public synchronized int size() {
		return getIntField(SIZE);
	}
	
	private synchronized void setSize(int size) {
		setIntField(SIZE, size);
	}
	
	private synchronized void incrementSize() {
		setIntField(SIZE, size() + 1);
	}
	
	private synchronized void decrementSize() {
		setIntField(SIZE, size() - 1);
	}
	
	@SuppressWarnings("unchecked")
	private synchronized PersistentEntry<E> getHeader() {
		return getObjectField(HEADER);
	}
	
	@SuppressWarnings("unchecked")
	private synchronized void setHeader(PersistentEntry<E> newHeader) {
		setObjectField(HEADER, newHeader);
	}
	
	private synchronized PersistentEntry<E> traverseToEntryAt(int index) {
		PersistentEntry<E> cursor = getHeader();
		for(int i = 0; i < index; i++) cursor = cursor.getNext();
		return cursor;
	}
	
	public synchronized String toString() {
		StringBuilder sb = new StringBuilder();
		PersistentEntry<E> cursor = getHeader();
		if(size() > 0) {
			for(int i = 0; i < size(); i++) {
				sb.append(cursor.getElement().toString());
				sb.append("-->");
				cursor = cursor.getNext();
			}
		}
		sb.append("NULL");
		return sb.toString();
	}
	
	public static class PersistentEntry<E extends AnyPersistent> extends PersistentObject {
				
		// private static final ObjectField<AnyPersistent> ELEMENT = new ObjectField<>();
		private static final GenericField<AnyPersistent> ELEMENT = new GenericField<>();
		private static final ObjectField<PersistentEntry> NEXT = new ObjectField<>(PersistentEntry.class);
	    public static final ObjectType<PersistentEntry> TYPE = ObjectType.withFields(PersistentEntry.class, ELEMENT, NEXT);
	   		    
		public PersistentEntry(E element, PersistentEntry<E> next) {
			super(TYPE);
			setElement(element);
            setNext(next);
		}
		
		public PersistentEntry(ObjectPointer<? extends PersistentEntry> p) {super(p);}
		
		@SuppressWarnings("unchecked")
		private synchronized E getElement() {
			return (E)getObjectField(ELEMENT);
		}
		
		private synchronized void setElement(E element) {
			setObjectField(ELEMENT, element);
		}
		
		@SuppressWarnings("unchecked")
		private synchronized PersistentEntry<E> getNext() {
			return getObjectField(NEXT);
		}
		
		private synchronized void setNext(PersistentEntry<E> next) {
			setObjectField(NEXT, next);
		}
	}
	
	public synchronized Iterator<E> iterator() {
		return new PersistentListIterator(size());
	}
	
	private class PersistentListIterator implements Iterator<E> {
		private int cursor;
		private final int size;
		
		public PersistentListIterator(int size) {
			this.size = size;
			this.cursor = 0;
		}
		
		public boolean hasNext() {
			return cursor < size;
		}
		
		public E next() {
			if(!this.hasNext()) throw new NoSuchElementException();			
			return get(cursor++);
		}
		
		public void remove() {
            throw new UnsupportedOperationException();
        }
	}
}
