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

public class PersistentStringTest {


    public static void main(String[] args) {
        System.out.println("****************PersistentString Tests****************");
        testBasic();
    }

    public static void testBasic() {
//Test creation and length

        PersistentString s1 = new PersistentString("hello");
        PersistentString s2 = new PersistentString("world");

        PersistentString s3 = new PersistentString("good");
        PersistentString s4 = new PersistentString("bye");

    ObjectDirectory.put("pstring", s1);

        assert(s1.length() == 5);
        assert(s2.length() == 5);

//Test toString
    //System.out.println(s1+" "+s2);

    String v1 = "good";
    String v2 = "bye";

//Test getBytes
    assert(v1.equals(new String(s3.getBytes())));
    assert(v2.equals(new String(s4.getBytes())));

//Test equals
    assert(!s1.equals(s4));
    assert(s4.equals(s4));

//Test CompareTo
    assert(s4.compareTo(s3)<1);
    assert(s3.compareTo(s1)<1);
    assert(s1.compareTo(s2)<1);
    assert(s4.compareTo(s2)<1);

//Testing rematerialized string
    PersistentString s1_copy = ObjectDirectory.get("pstring", PersistentString.class);
    assert(s1_copy.length() == s1.length());
    assert(s1_copy.equals(s1));
    assert(s1_copy.compareTo(s2)<1);
    //System.out.println("string is "+s1_copy);

//Testing empty string
    PersistentString empty = new PersistentString("");
    assert (empty.length() == 0);
    String e = "";
    assert (empty.toString().equals(e.toString()));
    assert (e.equals(new String(empty.getBytes())));
    assert (empty.compareTo(s3)<1);
    assert (e.compareTo("good")<1);
    }

}
