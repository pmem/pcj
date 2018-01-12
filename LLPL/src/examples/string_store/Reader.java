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

package examples.string_store;

import lib.llpl.*;
import java.io.Console;

public class Reader {
    public static void main(String[] args) {
        Heap h = Heap.getHeap("/mnt/mem/persistent_pool", 2147483648L);

        Console c = System.console();
        if (c == null) {
            System.out.println("No console.");
            System.exit(1);
        }

        long rootAddr = h.getRoot();
        if (rootAddr == 0) {
            System.out.println("No string found!");
            System.exit(0);
        }
        MemoryRegion<Transactional> mr = h.memoryRegionFromAddress(Transactional.class, rootAddr);
        byte[] bytes = new byte[mr.getInt(0)];
        for (int i = 0; i < mr.getInt(0); i++) {
            bytes[i] = mr.getByte(Integer.BYTES + i);
        }

        System.out.println("Found string \"" + new String(bytes) + "\".");
    }
}
