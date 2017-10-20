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

import lib.util.persistent.types.Types;
import lib.util.persistent.types.ArrayType;
import lib.util.persistent.types.ObjectType;
import lib.util.persistent.types.ValueType;
import java.lang.reflect.Array;
import java.util.Arrays;
import lib.xpersistent.*;
import lib.util.persistent.spi.PersistentMemoryProvider;

public class PersistentArray<T extends AnyPersistent> extends AbstractPersistentArray {
    private static final ArrayType<PersistentArray> TYPE = new ArrayType<>(PersistentArray.class, Types.OBJECT);

    public static <A extends PersistentArray, T extends AnyPersistent> ArrayType<A> typeForClasses(Class<A> arrayClass, Class<T> elementClass) {
        return new ArrayType<>(arrayClass, Types.typeForClass(elementClass));
    }

    @SuppressWarnings("unchecked")
    public static <T extends AnyPersistent> PersistentArray<T> forElementClass(Class<T> elementClass, int size) {
        PersistentArray<T> ans = null;
        ObjectType ot = Types.objectTypeForClass(elementClass);
        if (ot.isValueBased()) {
            ans = new PersistentValueArray<T>(elementClass, size);
        }
        else { 
            ans = new PersistentArray<T>(size);
        }
        return ans;
    }

    public PersistentArray(int size) {
        super(TYPE, size);
    }


    public PersistentArray(Class<? extends AnyPersistent> elementClass, int size) {
        super(new ArrayType<PersistentArray>(PersistentArray.class, Types.typeForClass(elementClass)), size);
    }

    protected PersistentArray(ArrayType<? extends PersistentArray<T>> type, int size) {
        super(type, size);
    }

    @SafeVarargs
    public PersistentArray(T... ts) {
        this(ts.length);
        for (int i = 0; i < ts.length; i++) setObjectElement(i, ts[i]);
    }

    protected PersistentArray(ObjectPointer<? extends PersistentArray<T>> pointer) {
        super(pointer);
    }

    @SuppressWarnings("unchecked")
    public T get(int index) {
        return (T)getObjectElement(index);
    }

    public void set(int index, T value) {
        setObjectElement(index, value);
    }
    
    @SafeVarargs
    public final void insert(int index, T... ts) {
        for (int i = index; i < index + ts.length; i++) setObjectElement(i, ts[i-index]);
    }

    @SuppressWarnings("unchecked")
    public T[] toArray(T[] a) {
        int len = length();
	if (a.length < len) a = Arrays.copyOf(a,len);
        for (int i = 0; i < len; i++) a[i] = get(i);
        return a;
    }

    public synchronized String toString() {
        int iMax = this.length() - 1;
        if (iMax == -1) return "[]";

        StringBuilder b = new StringBuilder();
        b.append('[');
        for (int i = 0; ; i++) {
            if (this.get(i) != null)
                b.append(this.get(i).toString());
            else
                b.append("NULL");
            if (i == iMax)
                return b.append(']').toString();
            b.append(", ");
        }
    }

    /*@SuppressWarnings("unchecked")
    public static synchronized void ArrayCopy(PersistentObject src, int srcPos, PersistentObject dest, int destPos, int length){
        if (src.equals(null) || dest.equals(null)) 
            throw new NullPointerException("Cannot copy to or from Null Array");
        if (!(src instanceof AbstractPersistentArray)) //might just need to be AbstractPersistentArray
            throw new ArrayStoreException("src is not an Array");
        if (!(dest instanceof AbstractPersistentArray)) //might just need to be AbstractPersistentArray
            throw new ArrayStoreException("dest is not an Array");
        if (!src.getType().equals(dest.getType())) 
            throw new ArrayStoreException("src and dest Arrays are of different types");
        if (srcPos < 0 || destPos < 0 || length < 0) 
            throw new IndexOutOfBoundsException("Index out of bounds");
        if (srcPos+length > ((AbstractPersistentArray)src).length())
            throw new IndexOutOfBoundsException("Index out of bounds: src Array");
        if (destPos+length > ((AbstractPersistentArray)dest).length())
            throw new IndexOutOfBoundsException("Index out of bounds: dest Array");
        
        XHeap heap = (XHeap) PersistentMemoryProvider.getDefaultProvider().getHeap();
        MemoryRegion from = src.getPointer().region();
        MemoryRegion to = dest.getPointer().region();
        long elemLength = Types.OBJECT.getSize();

        //special overlap case. Start copying from end
        if (src.equals(dest) && srcPos < destPos && destPos <= srcPos + length -1) {
            for (int i=length-1; i>=0; i--) {
                heap.memcpy(from, ((AbstractPersistentArray)src).elementOffset(srcPos+i), to, ((AbstractPersistentArray)dest).elementOffset(destPos+i), elemLength);  
            }
        } else {
            for (int i=0; i<length; i++) {
                heap.memcpy(from, ((AbstractPersistentArray)src).elementOffset(srcPos+i), to, ((AbstractPersistentArray)dest).elementOffset(destPos+i), elemLength);  
            }
        }
    }*/
}
