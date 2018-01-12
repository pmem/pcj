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

package examples.array;

import lib.llpl.*;

public class IntArray {
    static final int HEADER_SIZE = 4;

    public static void main(String[] args) {
        int size = 10;
        IntArray ia = new IntArray(size);
        for (int i = 0; i < size; i++) {
            assert(ia.get(i) == 0);
        }
        ia.set(5, 10);
        ia.set(7, 20);
        assert(ia.size() == 10);
        for (int i = 0; i < size; i++) {
            // System.out.format("i = %d, ia[i] = %d\n", i, ia.get(i));
            if (i == 5) assert(ia.get(i) == 10);
            else if (i == 7) assert(ia.get(i) == 20);
            else assert(ia.get(i) == 0);
        }
        assert(ia.size() == 10);
        boolean caught = false;
        try {
            ia.set(10, 100);
        } catch (ArrayIndexOutOfBoundsException e) {
            caught = true;
        }
        assert(caught);
    }

    private static Heap h = Heap.getHeap("/mnt/mem/persistent_pool", 2147483648L);
    MemoryRegion<Flushable> region;

    public static IntArray fromAddress(long addr) {
        MemoryRegion<Flushable> region = h.memoryRegionFromAddress(Flushable.class, addr);
        if (!region.isFlushed()) {     // simple consistency scheme: if not consistent, throw away, return null
            h.freeMemoryRegion(region);
            return null;
        } else {
            return new IntArray(region);
        }
    }

    public IntArray(int size) {
        this.region = (MemoryRegion<Flushable>)h.allocateMemoryRegion(Flushable.class, HEADER_SIZE + Integer.BYTES * size);
        this.region.putInt(0, size);
    }

    private IntArray(MemoryRegion<Flushable> region) {
        this.region = region;
    }

    public void set(int index, int value) {
        if (index < 0 || index >= size()) {
            throw new ArrayIndexOutOfBoundsException();
        }
        region.putInt(HEADER_SIZE + Integer.BYTES * index, value);
        region.flush();
    }

    public int get(int index) {
        if (index < 0 || index >= size()) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return region.getInt(HEADER_SIZE + Integer.BYTES * index);
    }

    public int size() {
        return region.getInt(0);
    }

    public long addr() {
        return region.addr();
    }
}
