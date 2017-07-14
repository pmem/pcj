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

import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;

class MemoryRegionCollectionTest {
    public static void main(String[] args) {
        Heap h = Heap.getHeap("/mnt/mem/persistent_pool");
        HashMap<MemoryRegion, Integer> hm = new HashMap<>();
        for (int i = 0; i < 10; i++) {
            hm.put(h.allocateRawMemoryRegion(10), i);
        }
        assert(hm.size() == 10);
        for (int i = 0; i < 10; i++) {
            hm.put(h.allocateTransactionalMemoryRegion(10), i);
        }
        assert(hm.size() == 20);
        for (int i = 0; i < 10; i++) {
            hm.put(h.allocateFlushableMemoryRegion(10), i);
        }
        assert(hm.size() == 30);
        for (Map.Entry<MemoryRegion, Integer> e : hm.entrySet()) {
            // System.out.println(e.getKey().addr() + ", " + e.getKey().getClass() + " --> " + e.getValue());
            h.freeRegion(e.getKey());
        }

        TreeMap<MemoryRegion, Integer> tm = new TreeMap<>();
        for (int i = 0; i < 10; i++) {
            tm.put(h.allocateRawMemoryRegion(10), i);
        }
        assert(tm.size() == 10);
        for (int i = 0; i < 10; i++) {
            tm.put(h.allocateTransactionalMemoryRegion(10), i);
        }
        assert(tm.size() == 20);
        for (int i = 0; i < 10; i++) {
            tm.put(h.allocateFlushableMemoryRegion(10), i);
        }
        assert(tm.size() == 30);
        for (Map.Entry<MemoryRegion, Integer> e : tm.entrySet()) {
            // System.out.println(e.getKey().addr() + ", " + e.getKey().getClass() + " --> " + e.getValue());
            h.freeRegion(e.getKey());
        }
        System.out.println("=================================All MemoryRegionCollection tests passed!=================================");
    }
}