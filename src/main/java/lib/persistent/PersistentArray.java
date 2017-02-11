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

package lib.persistent;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.lang.UnsupportedOperationException;
import java.util.function.Consumer;

public class PersistentArray<E extends Persistent> implements Iterable, Persistent {

    static {
        System.loadLibrary("Persistent");
        Util.openPool();
    }

    private long offset;

    public PersistentArray(long size) {
        synchronized(PersistentArray.class) {
            this.offset = Aggregation.nativeAggregate(this.getClass().getName(), size);
            ObjectDirectory.registerObject(this);
        }
    }

    @SuppressWarnings("unchecked")
    public synchronized E get(long index) {
        if (index < 0 || index >= size()) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return (E)Aggregation.nativeGetField(this.getOffset(), index);
    }

    public synchronized void set(long index, E value) {
        Transaction.run(() -> {
            if (index < 0 || index >= size()) {
                throw new ArrayIndexOutOfBoundsException();
            }
            Aggregation.nativeSetField(this.getOffset(), index, value.getOffset());
        });
    }

    public synchronized long size() {
        return Aggregation.nativeGetFieldCount(this.offset);
    }

    public synchronized Iterator<E> iterator() {
        return new ArrayIterator();
    }

    public synchronized String toString() {
        long iMax = this.size() - 1L;
        if (iMax == -1) return "[]";

        StringBuilder b = new StringBuilder();
        b.append('[');
        for (long i = 0; ; i++) {
            if (this.get(i) != null)
                b.append(this.get(i).toString());
            else
                b.append("NULL");
            if (i == iMax)
                return b.append(']').toString();
            b.append(", ");
        }
    }

    public synchronized int hashCode() {
        int result = 1;

        for (long i = 0; i < this.size(); i++)
            result = 31 * result + (this.get(i) == null ? 0 : this.get(i).hashCode());

        return result;
    }

    public synchronized boolean equals(Object obj) {
        if (!(obj instanceof PersistentArray)) return false;

        PersistentArray that = (PersistentArray)obj;
        if (that.size() != this.size()) return false;

        for (long i = 0; i < this.size(); i++)
            if (!(this.get(i).equals(that.get(i)))) return false;

        return true;
    }

    public synchronized long getOffset() {
        return this.offset;
    }

    private class ArrayIterator implements Iterator<E> {

        long index;

        @Override
        public final boolean hasNext() {
            synchronized(PersistentArray.this) {
                return index != size();
            }
        }

        @Override
        public final E next() {
            synchronized(PersistentArray.this) {
                E next = get(index);
                index++;
                return next;
            }
        }

        @Override
        public void remove() {
            synchronized(PersistentArray.this) {
                throw new UnsupportedOperationException();
            }
        }

        @Override
        public void forEachRemaining(Consumer<? super E> action) {
            synchronized(PersistentArray.this) {
                throw new UnsupportedOperationException();
            }
        }
    }
}

