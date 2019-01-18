/*
 * Copyright (c) 2017 Intel Corporation.  Persistent memory version
*/

/*
 * Copyright (c) 2003, 2013, Oracle and/or its affiliates. All rights reserved.
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

package lib.util.persistent;

import lib.util.persistent.types.*;
import lib.util.persistent.types.Types;
import lib.util.persistent.types.ArrayType;
import lib.util.persistent.types.ReferenceArrayType;
import java.util.Collection;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.function.Consumer;
import java.util.NoSuchElementException;
import java.util.ConcurrentModificationException;
import java.util.Objects;
import java.util.*;
import static lib.util.persistent.Util.*;

public class PersistentArrayList<T extends AnyPersistent> extends PersistentObject implements Iterable<T> {
    static final int DEFAULT_CAPACITY = 10;
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
    protected int modCount = 0;
    private static Statics statics;

    private static final IntField SIZE = new IntField();
    private static final ObjectField<PersistentArray> ELEMENTDATA = new ObjectField<>(PersistentArray.class);
    public static final ObjectType<PersistentArrayList> TYPE = ObjectType.withFields(PersistentArrayList.class, SIZE, ELEMENTDATA);

    static {
        statics = ObjectDirectory.get("PersistentArrayList_statics", Statics.class);
        if (statics == null)
            ObjectDirectory.put("PersistentArrayList_statics", statics = new Statics());
    }

    public static class Statics extends PersistentImmutableObject {
        private static final ObjectField<PersistentArray> EMPTY_ELEMENTDATA = new ObjectField<>(PersistentArray.class);
        private static final ObjectField<PersistentArray> DEFAULTCAPACITY_EMPTY_ELEMENTDATA = new ObjectField<>(PersistentArray.class);
        public static final ObjectType<Statics> TYPE = ObjectType.withFields(Statics.class, EMPTY_ELEMENTDATA, DEFAULTCAPACITY_EMPTY_ELEMENTDATA);

        public Statics() {
            super(TYPE, (Statics obj) -> {
                obj.initObjectField(EMPTY_ELEMENTDATA, new PersistentArray(0));
                obj.initObjectField(DEFAULTCAPACITY_EMPTY_ELEMENTDATA, new PersistentArray(0));
            });
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

    private PersistentArray getDataArray(){
        return getObjectField(ELEMENTDATA);
    }

    private boolean isEmptyArray(){
        return getDataArray().is(emptyArray());
    }

    private boolean isDefaultCapacityEmptyArray(){
        return getDataArray().is(defaultCapacityEmptyArray());
    }

    public PersistentArrayList(int initialCapacity){
        this(TYPE, initialCapacity);
    }

    public PersistentArrayList() {
        this(TYPE);
    }

    protected PersistentArrayList(ObjectType<? extends PersistentArrayList> type) {
        super(type);
        setDataArray(defaultCapacityEmptyArray());
    }

    protected PersistentArrayList(ObjectType<? extends PersistentArrayList> type, int initialCapacity) {
        super(type);
        if (initialCapacity > 0){
            setDataArray(new PersistentArray(initialCapacity));
        } else if (initialCapacity == 0) {
            setDataArray(emptyArray());
        } else {
            throw new IllegalArgumentException("Illegal Capacity: "+initialCapacity);
        }
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    public PersistentArrayList(T... ts) {
        this(TYPE, ts);
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    public PersistentArrayList(ObjectType<? extends PersistentArrayList> type, T... ts) {
        super(type);
        if (ts.length > 0){
            setDataArray(new PersistentArray(ts));
            size(ts.length);
        } else {
            setDataArray(emptyArray());
        }
    }

    protected PersistentArrayList(ObjectPointer<? extends PersistentArrayList> p) { super(p); }

    @SuppressWarnings("unchecked")
    public void trimToSize() {
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
        }, this);
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
    private void grow(int minCapacity) {
        Util.synchronizedBlock(this, () -> {
            int oldCapacity = getDataArray().length();
            int newCapacity = oldCapacity + (oldCapacity >> 1);
            if (newCapacity - minCapacity < 0)
                newCapacity = minCapacity;
            if (newCapacity - MAX_ARRAY_SIZE > 0)
                newCapacity = hugeCapacity(minCapacity);
            setDataArray(PersistentArrays.copyOf(getDataArray(), newCapacity));
        });
    }

    private static int hugeCapacity(int minCapacity) {
        if (minCapacity < 0)
            throw new OutOfMemoryError();
        return (minCapacity > MAX_ARRAY_SIZE) ?
            Integer.MAX_VALUE :
            MAX_ARRAY_SIZE;
    }

    public int size(){
        return Util.synchronizedBlock(this, () -> {
            return getIntField(SIZE);
        });
    }

    public boolean isEmpty(){
        return Util.synchronizedBlock(this, () -> {
            return size() == 0;
        });
    }

    public boolean contains(Object o) {
        return indexOf(o) >= 0;
    }

    public int indexOf(Object o) {
        return Util.synchronizedBlock(this, () -> {
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
        });
    }

    public int lastIndexOf(Object o) {
        return Util.synchronizedBlock(this, () -> {
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
        });
    }

    @SuppressWarnings("unchecked")
    public Object[] toArray() {
        AnyPersistent[] all = getDataArray().toArray();
        AnyPersistent[] ans = new AnyPersistent[size()];
        System.arraycopy(all, 0, ans, 0, ans.length);
        return ans;
    }

    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        Object[] oa = toArray();
        int size = oa.length;
        if (a.length < size) return (T[]) Arrays.copyOf(oa, size, a.getClass());
        System.arraycopy(oa, 0, a, 0, oa.length);
        if (a.length > size)
            a[size] = null;
        return a;
    }

    @SuppressWarnings("unchecked")
    T elementData(int index){
        return Util.synchronizedBlock(this, () -> {
            return (T) getDataArray().get(index);
        });
    }


    @SuppressWarnings("unchecked")
    public T get(int index){
        return Util.synchronizedBlock(this, () -> {
            rangeCheck(index);
            return elementData(index);
        });
    }

    @SuppressWarnings("unchecked")
    void setElementData(int index, T element){
        Transaction.run(() -> {
                getDataArray().set(index, element);
        }, this);
    }

    public T set(int index, T element) {
        return Util.synchronizedBlock(this, () -> {
            rangeCheck(index);
            final Box<T> oldValue = new Box<>();
            Transaction.run(() -> {
                oldValue.set(elementData(index));
                setElementData(index, element);
            });
            return oldValue.get();
        });
    }

    public boolean add(T t) {
        Transaction.run(() -> {
            ensureCapacityInternal(size() + 1);
            setElementData(size(), t);
            size(size()+1);
        }, this);
        return true;
    }

     @SuppressWarnings("unchecked")
     public void add(int index, T element) {
        rangeCheckForAdd(index);
        Transaction.run(() -> {
            ensureCapacityInternal(size() + 1);
            PersistentArrays.ArrayCopy(getDataArray(), index, getDataArray(), index + 1,
                        size() - index);
            setElementData(index, element);
            size(size()+1);
        }, this);
    }

    @SuppressWarnings("unchecked")
    public T remove(int index) {
        return Util.synchronizedBlock(this, () -> {
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
        });
    }

    public boolean remove(Object o) {
        return Util.synchronizedBlock(this, () -> {
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
        });
    }

    @SuppressWarnings("unchecked")
    private void fastRemove(int index) {
        Util.synchronizedBlock(this, () -> {
            modCount++;
            Transaction.run(() -> {
                int numMoved = size() - index - 1;
                if (numMoved > 0)
                    PersistentArrays.ArrayCopy(getDataArray(), index+1, getDataArray(), index,
                                 numMoved);
                size(size()-1);
                setElementData(size(), null); // clear to let GC do its work
            });
        });
    }

    public void clear() {
        Util.synchronizedBlock(this, () -> {
            modCount++;
            Transaction.run(() -> {
                // clear to let GC do its work
                for (int i = 0; i < size(); i++)
                    setElementData(i, null);

                size(0);
            });
        });
    }

    @SuppressWarnings("unchecked")
    public boolean addAll(Collection<? extends T> c) {
        final Box<Integer> numNew = new Box<>();
        Transaction.run(() -> {
            AnyPersistent[] a = c.toArray(new AnyPersistent[c.size()]);
            numNew.set(a.length);
            ensureCapacityInternal(size() + numNew.get());  // Increments modCount
            getDataArray().insert(size(), a);
            size(size()+numNew.get());
        }, this);
        return numNew.get() != 0;
    }

    @SuppressWarnings("unchecked")
    public boolean addAll(int index, Collection<? extends T> c) {
        rangeCheckForAdd(index);

        final Box<Integer> numNew = new Box<>();
        Transaction.run(() -> {
            AnyPersistent[] a = c.toArray(new AnyPersistent[c.size()]);
            numNew.set(a.length);
            ensureCapacityInternal(size() + numNew.get());  // Increments modCount

            int numMoved = size() - index;
            if (numMoved > 0)
                PersistentArrays.ArrayCopy(getDataArray(), index, getDataArray(), index + numNew.get(), numMoved);

            getDataArray().insert(index, a);
            size(size() + numNew.get());
        }, this);
        return numNew.get() != 0;
    }

    @SuppressWarnings("unchecked")
    protected void removeRange(int fromIndex, int toIndex) {
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
        }, this);
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

    public String toString() {
        return Util.synchronizedBlock(this, () -> {
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
        });
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
            AnyPersistent[] elementData = PersistentArrayList.this.getDataArray().toArray(new AnyPersistent[0]);
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
        final AnyPersistent[] snapshot;

        public boolean hasNext() {
            return cursor != size();
        }

        @SuppressWarnings("unchecked")
        public Itr() {
            snapshot = PersistentArrayList.this.toArray(new AnyPersistent[0]);
        }

        @SuppressWarnings("unchecked")
        public T next() {
            checkForComodification();
            int i = cursor;
            if (i >= size())
                throw new NoSuchElementException();
            //AnyPersistent[] elementData = PersistentArrayList.this.getDataArray().toArray(new AnyPersistent[0]);
            if (i >= snapshot.length)
                throw new ConcurrentModificationException();
            cursor = i + 1;
            //return (T) elementData[lastRet = i];
            return (T) snapshot[lastRet = i];
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
            //final AnyPersistent[] elementData = PersistentArrayList.this.getDataArray().toArray(new AnyPersistent[0]);
            if (i >= snapshot.length) {
                throw new ConcurrentModificationException();
            }
            while (i != size && modCount == expectedModCount) {
                consumer.accept((T) snapshot[i++]);
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
