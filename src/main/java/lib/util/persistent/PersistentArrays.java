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

import lib.xpersistent.*;
import lib.util.persistent.spi.PersistentMemoryProvider;
import lib.util.persistent.types.Types;

public class PersistentArrays{
    private static XHeap heap = (XHeap) PersistentMemoryProvider.getDefaultProvider().getHeap();
    private PersistentArrays(){}

    @SuppressWarnings("unchecked")
    public static <T extends AnyPersistent> PersistentArray<T> copyOf(PersistentArray<T> original, int newLength) {
//        return (PersistentArray<T>) copyOf(original, newLength, original.getClass());
        if (original.equals(null))
            throw new NullPointerException("Cannot copy Null Array");
        if (newLength < 0)
            throw new IllegalArgumentException(newLength + " <  0");

        final Box<PersistentArray<T>> copy = new Box<>();
        Transaction.run(() ->{
            copy.set(new PersistentArray<>(newLength));
            int size = Math.min(newLength, original.length());
            for (int i = 0; i < size; i++){
                copy.get().set(i, (T)original.get(i));
            }
        });
        return copy.get();
    }

    @SuppressWarnings("unchecked")
    public static <T extends AnyPersistent> PersistentArray<T> copyOfRange(PersistentArray<T> original, int from, int to) {
        //return copyOfRange(original, from, to, (Class<? extends T>) original.getClass());
        int newLength = to - from;
        if (original.equals(null))
            throw new NullPointerException("Cannot copy Null Array");
        if (newLength < 0)
            throw new IllegalArgumentException(from + " < " + to);

        final Box<PersistentArray<T>> copy = new Box<>();
        Transaction.run(() ->{
            copy.set(new PersistentArray<>(newLength));
            int size = Math.min(newLength, original.length()-from);
            for (int i = 0; i < size; i++){
                copy.get().set(i, (T)original.get(from+i));
            }
        });
        return copy.get();
    }

     @SuppressWarnings("unchecked")
    public static synchronized <T extends AnyPersistent> void ArrayCopy(PersistentArray<T> src, int srcPos, PersistentArray<T> dest, int destPos, int length){
        if (src.equals(null) || dest.equals(null))
            throw new NullPointerException("Cannot copy to or from Null Array");
        if (srcPos < 0 || destPos < 0 || length < 0)
            throw new IndexOutOfBoundsException("Index out of bounds");
        if (srcPos+length > ((AbstractPersistentArray)src).length())
            throw new IndexOutOfBoundsException("Index out of bounds: src Array");
        if (destPos+length > ((AbstractPersistentArray)dest).length())
            throw new IndexOutOfBoundsException("Index out of bounds: dest Array");

        //special overlap case. Start copying from end
        Transaction.run(() -> {
            if (src.equals(dest) && srcPos < destPos && destPos <= srcPos + length -1) {
                for (int i=length-1; i>=0; i--) {
                    dest.set(destPos+i, src.get(srcPos+i));
                }
            } else {
                for (int i=0; i<length; i++) {
                    dest.set(destPos+i, src.get(srcPos+i));
                }
            }
        });
    }

    public static void toByteArray(AnyPersistent src, byte[] dest, int length) {
        toByteArray(src, 0, dest, 0, length);
    }

    public static void toByteArray(AnyPersistent src, int srcIndex, byte[] dest, int offset, int length) {
        Util.synchronizedBlock(src, () -> {
            MemoryRegion from = src.region();
            heap.memcpy(from, ((AbstractPersistentArray)src).elementOffset(srcIndex), dest, offset, length);
        });
    }

    public static void fromByteArray(byte[] src, int offset, AnyPersistent dest, int destIndex, int length) {
        Util.synchronizedBlock(dest, () -> {
            MemoryRegion to = dest.region();
            heap.memcpy(src, offset, to, ((AbstractPersistentArray)dest).elementOffset(destIndex), length);
        });
    }

    static void toPersistentByteArray(AnyPersistent src, int srcIndex, AnyPersistent dest, int destIndex, int length) {
        MemoryRegion to = dest.region();
        MemoryRegion from = src.region();
        heap.memcpy(from, ((AbstractPersistentArray)src).elementOffset(srcIndex), to, ((AbstractPersistentArray)dest).elementOffset(destIndex), length);
    }
}
