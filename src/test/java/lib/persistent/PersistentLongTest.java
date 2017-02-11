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

import java.util.*;

public class PersistentLongTest {


    public static void main(String[] args) {
        System.out.println("****************PersistentLong Tests****************");
        testBasic();
    }

    public static void testBasic() {
//Test creation

        PersistentLong s1 = new PersistentLong(123456789L);
        PersistentLong s2 = new PersistentLong(987654321L);


    ObjectDirectory.put("plong", s1);

//Test toString
    //System.out.println(s1+" "+s2);

//Test equals
    assert(!s1.equals(s2));
    assert(s1.equals(s1));

//Test CompareTo
    assert(s2.compareTo(s1) == 1);

    }

}
