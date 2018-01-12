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

public class CopyMemoryTest {
    static Heap h = Heap.getHeap("/mnt/mem/persistent_pool", 2147483648L);

    public static void main(String[] args) {
        regionToRegionTest();
        arrayToRegionTest();
        regionToArrayTest();
        System.out.println("=================================All CopyMemory tests passed=================================");
    }

    static void regionToRegionTest() {
        MemoryRegion<Raw> rmr = h.allocateMemoryRegion(Raw.class, 120);
        MemoryRegion<Flushable> fmr = h.allocateMemoryRegion(Flushable.class, 120);
        MemoryRegion<Transactional> tmr = h.allocateMemoryRegion(Transactional.class, 120);

        rmr.putLong(0L, 0x44);
        fmr.putLong(40L, 0x88);
        tmr.putLong(80L, 0xcc);

        fmr.flush();

        // From Raw
        assert(fmr.isFlushed() == true);
        h.copyMemory(rmr, 0, rmr, 10, 10);
        h.copyMemory(rmr, 0, fmr, 50, 10);
        h.copyMemory(rmr, 0, tmr, 90, 10);
        assert(rmr.getLong(10L) == 0x44);
        assert(fmr.getLong(50L) == 0x44);
        assert(tmr.getLong(90L) == 0x44);
        assert(fmr.isFlushed() == false);
        fmr.flush();

        // From Flushable
        assert(fmr.isFlushed() == true);
        h.copyMemory(fmr, 40, rmr, 20, 10);
        h.copyMemory(fmr, 40, fmr, 60, 10);
        h.copyMemory(fmr, 40, tmr, 100, 10);
        assert(rmr.getLong(20L) == 0x88);
        assert(fmr.getLong(60L) == 0x88);
        assert(tmr.getLong(100L) == 0x88);
        assert(fmr.isFlushed() == false);
        fmr.flush();

        // From Transactional
        assert(fmr.isFlushed() == true);
        h.copyMemory(tmr, 80, rmr, 30, 10);
        h.copyMemory(tmr, 80, fmr, 70, 10);
        h.copyMemory(tmr, 80, tmr, 110, 10);
        assert(rmr.getLong(30L) == 0xcc);
        assert(fmr.getLong(70L) == 0xcc);
        assert(tmr.getLong(110L) == 0xcc);
        assert(fmr.isFlushed() == false);
        fmr.flush();

        h.freeMemoryRegion(rmr);
        h.freeMemoryRegion(fmr);
        h.freeMemoryRegion(tmr);
    }

    static void arrayToRegionTest() {
        MemoryRegion<Raw> rmr = h.allocateMemoryRegion(Raw.class, 120);
        MemoryRegion<Flushable> fmr = h.allocateMemoryRegion(Flushable.class, 120);
        MemoryRegion<Transactional> tmr = h.allocateMemoryRegion(Transactional.class, 120);
        byte[] srcArray = new byte[50];

        for (int i = 0; i < srcArray.length; i++) {
            srcArray[i] = (byte)i;
        }

        assert(fmr.isFlushed() == true);
        h.copyFromArray(srcArray, 0, rmr, 0, srcArray.length);
        h.copyFromArray(srcArray, 0, fmr, 0, srcArray.length);
        h.copyFromArray(srcArray, 0, tmr, 0, srcArray.length);

        assert(fmr.isFlushed() == false);
        for (int i = 0; i < srcArray.length; i++) {
            assert(rmr.getByte(i) == (byte)i);
            assert(fmr.getByte(i) == (byte)i);
            assert(tmr.getByte(i) == (byte)i);
        }
        fmr.flush();

        int srcOffset = 25;
        assert(fmr.isFlushed() == true);
        h.copyFromArray(srcArray, srcOffset, rmr, 50, srcArray.length - srcOffset);
        h.copyFromArray(srcArray, srcOffset, fmr, 50, srcArray.length - srcOffset);
        h.copyFromArray(srcArray, srcOffset, tmr, 50, srcArray.length - srcOffset);

        assert(fmr.isFlushed() == false);
        for (int i = srcOffset; i < srcArray.length; i++) {
            assert(rmr.getByte(50 + i - srcOffset) == (byte)i);
            assert(fmr.getByte(50 + i - srcOffset) == (byte)i);
            assert(tmr.getByte(50 + i - srcOffset) == (byte)i);
        }
        fmr.flush();

        h.freeMemoryRegion(rmr);
        h.freeMemoryRegion(fmr);
        h.freeMemoryRegion(tmr);
    }

    static void regionToArrayTest() {
        MemoryRegion<Raw> rmr = h.allocateMemoryRegion(Raw.class, 120);
        MemoryRegion<Flushable> fmr = h.allocateMemoryRegion(Flushable.class, 120);
        MemoryRegion<Transactional> tmr = h.allocateMemoryRegion(Transactional.class, 120);
        byte[] dstArray = new byte[50];

        rmr.putLong(10L, 0x44);
        fmr.putLong(50L, 0x88);
        tmr.putLong(90L, 0xcc);

        h.copyToArray(rmr, 10L, dstArray, 0, 10);
        h.copyToArray(fmr, 50L, dstArray, 10, 10);
        h.copyToArray(tmr, 90L, dstArray, 20, 10);

        assert(dstArray[0] == (byte)0x44);
        assert(dstArray[10] == (byte)0x88);
        assert(dstArray[20] == (byte)0xcc);

        h.freeMemoryRegion(rmr);
        h.freeMemoryRegion(fmr);
        h.freeMemoryRegion(tmr);
    }
}