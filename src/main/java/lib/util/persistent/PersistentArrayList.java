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
import java.util.Collection;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.function.Consumer;
import java.util.NoSuchElementException;
import java.util.ConcurrentModificationException;
import java.util.Objects;

public class PersistentArrayList<T extends PersistentObject> extends PersistentObject implements Iterable<T> {
    static final int DEFAULT_CAPACITY = 10;
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
    protected int modCount = 0;
    private static Statics statics;

    private static final IntField SIZE = new IntField();
    private static final ObjectField<PersistentArray> ELEMENTDATA = new ObjectField<>(PersistentArray.class);
    public static final ObjectType<PersistentArrayList> TYPE = ObjectType.fromFields(PersistentArrayList.class, SIZE, ELEMENTDATA);

    static {
        statics = ObjectDirectory.get("PersistentArrayList_statics", Statics.class);
        if (statics == null)
            ObjectDirectory.put("PersistentArrayList_statics", statics = new Statics());
    }
    
    public static class Statics extends PersistentObject {
        private static final ObjectField<PersistentArray> EMPTY_ELEMENTDATA = new ObjectField<>(PersistentArray.class);
        private static final ObjectField<PersistentArray> DEFAULTCAPACITY_EMPTY_ELEMENTDATA = new ObjectField<>(PersistentArray.class);
        public static final ObjectType<Statics> TYPE = ObjectType.fromFields(Statics.class, EMPTY_ELEMENTDATA, DEFAULTCAPACITY_EMPTY_ELEMENTDATA);

        public Statics() { 
            super(TYPE); 
            setObjectField(EMPTY_ELEMENTDATA, new PersistentArray(0));
            setObjectField(DEFAULTCAPACITY_EMPTY_ELEMENTDATA, new PersistentArray(0));
        }

        protected Statics (ObjectPointer<? extends Statics> p) { super(p); }

        public PersistentArray emptyArray() { return getObjectField(EMPTY_ELEMENTDATA); }

        public PersistentArray defaultCapacityEmptyArray() { return getObjectField(DEFAULTCAPACITY_EMPTY_ELEMENTDATA); }

    }   

    private void size(int size){
        setIntField(SIZE, size);
    }

    private static PersistentArray emptyArray(){
        return statics.emptyArray();
    }

    private static PersistentArray defaultCapacityEmptyArray(){
        return statics.defaultCapacityEmptyArray();
    }

    private void setDataArray(PersistentArray a){
        setObjectField(ELEMENTDATA, a);
    }

    public PersistentArray getDataArray(){
        return getObjectField(ELEMENTDATA);
    }

    private boolean isEmptyArray(){
        return getDataArray().is(emptyArray()); 
    }

    private boolean isDefaultCapacityEmptyArray(){
        return getDataArray().is(defaultCapacityEmptyArray());
    }

    public PersistentArrayList(int initialCapacity){
        super(TYPE);
        if (initialCapacity > 0){
            setDataArray(new PersistentArray(initialCapacity));
        } else if (initialCapacity == 0) {
            setDataArray(emptyArray());
        } else {
            throw new IllegalArgumentException("Illegal Capacity: "+initialCapacity);
        }
    }

    public PersistentArrayList() {
        super(TYPE);
        setDataArray(defaultCapacityEmptyArray());
    }
    
    protected PersistentArrayList(ObjectType<? extends PersistentArrayList> type) {
        super(type);
        setDataArray(defaultCapacityEmptyArray());
    }
    
    //public PersistentArrayList(Collection<? extends PersistentObject> c) {
    @SafeVarargs
    @SuppressWarnings("unchecked")
    public PersistentArrayList(T... ts) {
        super(TYPE);
        if (ts.length > 0){
            setDataArray(new PersistentArray(ts));
            size(ts.length);
        } else {
            setDataArray(emptyArray());
        }
    }

    protected PersistentArrayList(ObjectPointer<? extends PersistentArrayList> p) { super(p); }

    @SuppressWarnings("unchecked")
    public synchronized void trimToSize() {
        Transaction.run(() -> {
            int size = size();
            int length = getDataArray().length();
        modCount++;
            if (size < length) {
                if (size == 0)
                    setDataArray(emptyArray());
                else
                    setDataArray(PersistentArrays.copyOfRange(getDataArray(), 0 ,size));
            }
        });
    }

    public void ensureCapacity(int minCapacity) {
        int minExpand = (!isDefaultCapacityEmptyArray())        
            ? 0
            : DEFAULT_CAPACITY;

        if (minCapacity > minExpand) {
            ensureExplicitCapacity(minCapacity);
        }
    }

    private void ensureCapacityInternal(int minCapacity) {
        if (isDefaultCapacityEmptyArray()) {
            minCapacity = Math.max(DEFAULT_CAPACITY, minCapacity); 
        }

        ensureExplicitCapacity(minCapacity);
    }

    private void ensureExplicitCapacity(int minCapacity) {
        modCount++;
        if (minCapacity - getDataArray().length() > 0) {
            grow(minCapacity);
        }
    }

    @SuppressWarnings("unchecked")
    private synchronized void grow(int minCapacity) {
        int oldCapacity = getDataArray().length();
        int newCapacity = oldCapacity + (oldCapacity >> 1);
        if (newCapacity - minCapacity < 0)
            newCapacity = minCapacity;
        if (newCapacity - MAX_ARRAY_SIZE > 0)
            newCapacity = hugeCapacity(minCapacity);
        setDataArray(PersistentArrays.copyOf(getDataArray(), newCapacity));
    }

    private static int hugeCapacity(int minCapacity) {
        if (minCapacity < 0)
            throw new OutOfMemoryError();
        return (minCapacity > MAX_ARRAY_SIZE) ?
            Integer.MAX_VALUE :
            MAX_ARRAY_SIZE;
    }

    public synchronized int size(){
        return getIntField(SIZE); 
    }

    public synchronized boolean isEmpty(){
        return size() == 0; 
    }

    public boolean contains(Object o) {
        return indexOf(o) >= 0;
    }

    public synchronized int indexOf(Object o) {
        if (o == null) {
            for (int i = 0; i < size(); i++)
                if (getDataArray().get(i)==null)
                    return i;
        } else {
            for (int i = 0; i < size(); i++)
                if (o.equals(getDataArray().get(i)))
                    return i;
        }
        return -1;
    }

    public synchronized int lastIndexOf(Object o) {
        if (o == null) {
            for (int i = size()-1; i >= 0; i--)
                if (getDataArray().get(i)==null)
                    return i;
        } else {
            for (int i = size()-1; i >= 0; i--)
                if (o.equals(getDataArray().get(i)))
                    return i;
        }
        return -1;
    }

    @SuppressWarnings("unchecked")
    public synchronized <T extends PersistentObject> T[] toArray(T[] a) {
        if (a.length < size())
            return (T[]) (PersistentArrays.copyOf(getDataArray(), size())).toArray(a);
        a = (T[]) (PersistentArrays.copyOf(getDataArray(),size())).toArray(a);
        if (a.length > size())
            a[size()] = null;
        return a;
    }

    @SuppressWarnings("unchecked")
    synchronized T elementData(int index){
        return (T) getDataArray().get(index);
    }

    
    @SuppressWarnings("unchecked")
    public synchronized T get(int index){
        rangeCheck(index);
        return elementData(index);
    }

    @SuppressWarnings("unchecked")
    synchronized void setElementData(int index, T element){
        Transaction.run(() -> {
            getDataArray().set(index, element);
        });
    }

    public synchronized T set(int index, T element) {
        rangeCheck(index);
        final Box<T> oldValue = new Box<>();
        Transaction.run(() -> { 
            oldValue.set(elementData(index));
            setElementData(index, element);
        });
        return oldValue.get();
    }

    public synchronized boolean add(T t) {
        Transaction.run(() -> {
            ensureCapacityInternal(size() + 1); 
            setElementData(size(), t);
            size(size()+1);
        });
        return true;
    }

     @SuppressWarnings("unchecked")
     public synchronized void add(int index, T element) {
        rangeCheckForAdd(index);
        Transaction.run(() -> {
            ensureCapacityInternal(size() + 1); 
            PersistentArrays.ArrayCopy(getDataArray(), index, getDataArray(), index + 1,
                        size() - index);
            setElementData(index, element);
            size(size()+1);
        });
    }

    @SuppressWarnings("unchecked")
    public synchronized T remove(int index) {
        rangeCheck(index);

        modCount++;
        final Box<T> oldValue = new Box<>(); 
        Transaction.run(() -> {
            oldValue.set(elementData(index));

            int numMoved = size() - index - 1;
            if (numMoved > 0)
                PersistentArrays.ArrayCopy(getDataArray(), index+1, getDataArray(), index,
                             numMoved);
                size(size()-1);
                setElementData(size(), null); // clear to let GC do its work
        });

        return oldValue.get();
    }
    
    public synchronized boolean remove(Object o) {
        if (o == null) {
            for (int index = 0; index < size(); index++)
                if (elementData(index).equals(null)) {
                    fastRemove(index);
                    return true;
                }
        } else {
            for (int index = 0; index < size(); index++)
                if (o.equals(elementData(index))) {
                    fastRemove(index);
                    return true;
                }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private synchronized void fastRemove(int index) {
        modCount++;
        Transaction.run(() -> {
            int numMoved = size() - index - 1;
            if (numMoved > 0)
                PersistentArrays.ArrayCopy(getDataArray(), index+1, getDataArray(), index,
                             numMoved);
            size(size()-1);
            setElementData(size(), null); // clear to let GC do its work
        });
    } 

    public synchronized void clear() {
        modCount++;
        Transaction.run(() -> {
            // clear to let GC do its work
            for (int i = 0; i < size(); i++)
                setElementData(i, null);

            size(0);
        });
    }

    @SuppressWarnings("unchecked")
    public synchronized boolean addAll(Collection<? extends T> c) {
        final Box<Integer> numNew = new Box<>();
        Transaction.run(() -> {
            PersistentObject[] a = c.toArray(new PersistentObject[c.size()]);
            numNew.set(a.length);
            ensureCapacityInternal(size() + numNew.get());  // Increments modCount
            getDataArray().insert(size(), a);
            size(size()+numNew.get());
        });
        return numNew.get() != 0;
    }
  
    @SuppressWarnings("unchecked")
    public synchronized boolean addAll(int index, Collection<? extends T> c) {
        rangeCheckForAdd(index);

        final Box<Integer> numNew = new Box<>();
        Transaction.run(() -> {
            PersistentObject[] a = c.toArray(new PersistentObject[c.size()]);
            numNew.set(a.length);
            ensureCapacityInternal(size() + numNew.get());  // Increments modCount

            int numMoved = size() - index;
            if (numMoved > 0) 
                PersistentArrays.ArrayCopy(getDataArray(), index, getDataArray(), index + numNew.get(), numMoved);

            getDataArray().insert(index, a);
            size(size() + numNew.get());
        });
        return numNew.get() != 0;
    }

    @SuppressWarnings("unchecked")
    protected synchronized void removeRange(int fromIndex, int toIndex) {
        Transaction.run(() -> {
            modCount++;
            int numMoved = size() - toIndex;
            PersistentArrays.ArrayCopy(getDataArray(), toIndex, getDataArray(), fromIndex,
                         numMoved);

            // clear to let GC do its work
            int newSize = size() - (toIndex-fromIndex);
            for (int i = newSize; i < size(); i++) {
                setElementData(i, null);
            }
            size(newSize);
        });
    }

    private void rangeCheck(int index) {
        if (index >= size())
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
    }

    private void rangeCheckForAdd(int index) {
        if (index > size() || index < 0)
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
    }

    private String outOfBoundsMsg(int index) {
        return "Index: "+index+", Size: "+size();
    }

    public ListIterator<T> listIterator(int index) {
        if (index < 0 || index > size())
            throw new IndexOutOfBoundsException("Index: "+index);
        return new ListItr(index);
    }

    public ListIterator<T> listIterator() {
        return new ListItr(0);
    }

    public Iterator<T> iterator() {
        return new Itr();
    }

    public synchronized String toString() {
        Iterator<T> it = iterator();
        if (! it.hasNext())
            return "[]";

        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (;;) {
            T t = it.next();
            sb.append(t == this ? "(this Collection)" : t);
            if (! it.hasNext())
                return sb.append(']').toString();
            sb.append(',').append(' ');
        }
    }

    private class ListItr extends Itr implements ListIterator<T> {
        ListItr(int index) {
            super();
            cursor = index;
        }

        public boolean hasPrevious() {
            return cursor != 0;
        }

        public int nextIndex() {
            return cursor;
        }

        public int previousIndex() {
            return cursor - 1;
        }

        @SuppressWarnings("unchecked")
        public T previous() {
            checkForComodification();
            int i = cursor - 1;
            if (i < 0)
                throw new NoSuchElementException();
            PersistentObject[] elementData = PersistentArrayList.this.getDataArray().toArray(new PersistentObject[0]);
            if (i >= elementData.length)
                throw new ConcurrentModificationException();
            cursor = i;
            return (T) elementData[lastRet = i];
        }

        public void set(T t) {
            if (lastRet < 0)
                throw new IllegalStateException();
            checkForComodification();

            try {
                PersistentArrayList.this.set(lastRet, t);
            } catch (IndexOutOfBoundsException ex) {
                throw new ConcurrentModificationException();
            }
        }

        public void add(T t) {
            checkForComodification();

            try {
                int i = cursor;
                PersistentArrayList.this.add(i, t);
                cursor = i + 1;
                lastRet = -1;
                expectedModCount = modCount;
            } catch (IndexOutOfBoundsException ex) {
                throw new ConcurrentModificationException();
            }
        }    
    }

    private class Itr implements Iterator<T>{
        int cursor;         //index of next element to return
        int lastRet = -1;  //index of last element returned; -1 if no such       
        int expectedModCount = modCount;

        public boolean hasNext() {
            return cursor != size();
        }

        @SuppressWarnings("unchecked")
        public T next() {
            checkForComodification();
            int i = cursor;
            if (i >= size())
                throw new NoSuchElementException();
            PersistentObject[] elementData = PersistentArrayList.this.getDataArray().toArray(new PersistentObject[0]);
            if (i >= elementData.length)
                throw new ConcurrentModificationException();
            cursor = i + 1;
            return (T) elementData[lastRet = i];
        }

        public void remove() {
            if (lastRet < 0)
                throw new IllegalStateException();
            checkForComodification();

            try {
                PersistentArrayList.this.remove(lastRet);
                cursor = lastRet;
                lastRet = -1;
                expectedModCount = modCount;
            } catch (IndexOutOfBoundsException ex) {
                throw new ConcurrentModificationException();
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public void forEachRemaining(Consumer<? super T> consumer) {
            Objects.requireNonNull(consumer);
            final int size = PersistentArrayList.this.size();
            int i = cursor;
            if (i >= size()) {
                return;
            }
            final PersistentObject[] elementData = PersistentArrayList.this.getDataArray().toArray(new PersistentObject[0]);
            if (i >= elementData.length) {
                throw new ConcurrentModificationException();
            }
            while (i != size && modCount == expectedModCount) {
                consumer.accept((T) elementData[i++]);
            }
            // update once at end of iteration to reduce heap write traffic
            cursor = i;
            lastRet = i - 1;
            checkForComodification();
        }

        final void checkForComodification() {
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
        }

    }

    
}
