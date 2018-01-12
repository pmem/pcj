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

package lib.llpl;

public class ReallocateTest {
    public static void main(String[] args) {
        Heap h = Heap.getHeap("/mnt/mem/persistent_pool2", 2147483648L);

        MemoryRegion<Raw> mr = h.allocateMemoryRegion(Raw.class, 500000000L);
        mr.putByte(1000000, (byte)0x50);
        mr = h.reallocateMemoryRegion(Raw.class, mr, 1000000000L);
        assert(mr.getByte(1000000) == (byte)0x50);

        boolean caught = false;
        try {
            h.reallocateMemoryRegion(Raw.class, mr, 3000000000L);
        } catch (PersistenceException e) {
            caught = true;
        }
        assert(caught);

        mr = h.reallocateMemoryRegion(Raw.class, mr, 500000000L);

        MemoryRegion<Raw> mr2 = h.allocateMemoryRegion(Raw.class, 1000000000L);
        caught = false;
        try {
            h.reallocateMemoryRegion(Raw.class, mr2, 1500000000L);
        } catch (PersistenceException e) {
            caught = true;
        }
        assert(caught);

        System.out.println("=================================All Reallocate tests passed=================================");
    }
}