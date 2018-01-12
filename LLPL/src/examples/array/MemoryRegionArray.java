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

public class MemoryRegionArray {
    static final int HEADER_SIZE = 4;

    public static void main(String[] args) {
        int size = 10;
        MemoryRegionArray mra = new MemoryRegionArray(size);
        for (int i = 0; i < size; i++) {
            assert(mra.get(i) == null);
        }
        MemoryRegion<Transactional> mr1 = h.allocateMemoryRegion(Transactional.class, 10);
        mr1.putLong(0, 0xcafe);
        MemoryRegion<Transactional> mr2 = h.allocateMemoryRegion(Transactional.class, 20);
        mr2.putLong(0, 0xbeef);

        mra.set(5, mr1);
        mra.set(7, mr2);
        assert(mra.size() == 10);

        for (int i = 0; i < size; i++) {
            if (i == 5) assert(mra.get(i) != null && mra.get(i).addr() == mr1.addr() && mra.get(i).getLong(0) == 0xcafe);
            else if (i == 7) assert(mra.get(i) != null && mra.get(i).addr() == mr2.addr() && mra.get(i).getLong(0) == 0xbeef);
            else assert(mra.get(i) == null);
        }
        assert(mra.size() == 10);
        boolean caught = false;
        try {
            mra.set(10, h.allocateMemoryRegion(Transactional.class, 5));
        } catch (ArrayIndexOutOfBoundsException e) {
            caught = true;
        }
        assert(caught);
    }

    private static Heap h = Heap.getHeap("/mnt/mem/persistent_pool", 2147483648L);
    MemoryRegion<Transactional> region;

    public static MemoryRegionArray fromAddress(long addr) {
        MemoryRegion<Transactional> region = h.memoryRegionFromAddress(Transactional.class, addr);
        return new MemoryRegionArray(region);
    }

    public MemoryRegionArray(int size) {
        this.region = h.allocateMemoryRegion(Transactional.class, HEADER_SIZE + Long.BYTES * size);
        this.region.putInt(0, size);
    }

    private MemoryRegionArray(MemoryRegion<Transactional> region) {
        this.region = region;
    }

    public void set(int index, MemoryRegion<Transactional> value) {
        if (index < 0 || index >= size()) {
            throw new ArrayIndexOutOfBoundsException();
        }
        region.putLong(HEADER_SIZE + Long.BYTES * index, value == null ? 0 : value.addr());
    }

    public MemoryRegion<Transactional> get(int index) {
        if (index < 0 || index >= size()) {
            throw new ArrayIndexOutOfBoundsException();
        }
        long addr = region.getInt(HEADER_SIZE + Long.BYTES * index);
        return addr == 0 ? null : h.memoryRegionFromAddress(Transactional.class, addr);
    }

    public int size() {
        return region.getInt(0);
    }

    public long addr() {
        return region.addr();
    }
}
