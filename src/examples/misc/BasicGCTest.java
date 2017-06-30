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

package examples.misc;

import lib.util.persistent.*;
import lib.util.persistent.spi.*;
import lib.xpersistent.*;
import java.util.*;

class BasicGCTest {
    public static void main(String[] args) {
        ((XHeap)PersistentMemoryProvider.getDefaultProvider().getHeap()).open();
        long curMemRegCount = ((XHeap)PersistentMemoryProvider.getDefaultProvider().getHeap()).debug();
        System.out.println("currently in heap " + curMemRegCount);
        assert(curMemRegCount == 6 || curMemRegCount == 22);
        PersistentHashMap<PersistentString, PersistentHashMap> hm = new PersistentHashMap<>();
        PersistentString s = new PersistentString("hello");
        PersistentHashMap<PersistentString, PersistentHashMap> hm2 = new PersistentHashMap<>();
        PersistentString s2 = new PersistentString("hello");
        hm.put(s, hm);
        hm.put(s2, hm2);
        hm2.put(s, hm);
        hm2.put(s2, hm2);

        PersistentSkipListMap<PersistentString, PersistentString> map = new PersistentSkipListMap<>();
        for (int i = 0; i < 1000; i++) {
            map.put(new PersistentString("hello"+i), new PersistentString("world"+i));
        }

        /*try {
            final ArrayList<Object[]> allocations = new ArrayList<Object[]>();
            int size;
            while( (size = Math.min(Math.abs((int)Runtime.getRuntime().freeMemory()),Integer.MAX_VALUE))>0 )
                allocations.add( new Object[size] );
        } catch( OutOfMemoryError e ) {
            // great!
        }
        System.gc();
        try {
            Thread.sleep(5000);
        } catch (Exception e) {}*/
        ((XHeap)PersistentMemoryProvider.getDefaultProvider().getHeap()).debug();
    }
}
