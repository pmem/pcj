/* Copyright (C) 2016  Intel Corporation
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

import java.util.Map;

public class MultithreadTest {

    static final int NUM_THREADS = 10;
    static final int NUM_ITERATIONS = 5000;

    public static void main(String[] args) {
        testBasic();
    }

    public static void testBasic() {
        System.out.println("=======================================Testing " + (NUM_THREADS * NUM_ITERATIONS) + " insertions======================================");

        for (int k = 0; k < 1; k++) {
            Thread[] threads = new Thread[NUM_THREADS];
            PersistentSortedMap m1 = TestUtil.getOrCreateMap("m1");
            m1.clear();
            long start = System.nanoTime();
            for (int i = 0; i < threads.length; i++) {
                threads[i] = new Thread( ()->{
                    for (int j = 0; j < NUM_ITERATIONS; j++) {
                        PersistentByteBuffer key = PersistentByteBuffer.allocate(10);
                        key.put(("hello"+j).getBytes()).rewind();
                        PersistentByteBuffer val = PersistentByteBuffer.allocate(10);
                        val.put(("world"+j).getBytes()).rewind();
                        PersistentByteBuffer out = m1.put(key, val);
                    }
                });
                threads[i].start();
            }
            for (int i = 0; i < threads.length; i++) {
                try {
                    threads[i].join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    System.exit(-1);
                }
            }
            long end = System.nanoTime();
            System.out.println("Total time " + (end - start));
            System.out.println("size is " + m1.size());
            assert(m1.size() == NUM_ITERATIONS);
            m1.clear();
        }
    }
}
