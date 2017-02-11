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

public class ObjectDirectoryTest {

    public static void main(String[] args) {
        System.out.println("****************ObjectDirectory Tests****************");
        ObjectDirectoryTest();
    }

    public static void ObjectDirectoryTest() {
        PersistentTreeMap<PersistentByteBuffer, PersistentByteBuffer> m1 = new PersistentTreeMap<PersistentByteBuffer, PersistentByteBuffer>();
        ObjectDirectory.put("m1", m1);

        PersistentByteBuffer k = TestUtil.createByteBuffer(10, "hello");
        PersistentByteBuffer v = TestUtil.createByteBuffer(10, "world");
        m1.put(k, v);

        assert(m1.size() == 1);
        assert(ObjectDirectory.get("m1", PersistentTreeMap.class).equals(m1));
        assert(ObjectDirectory.get("m1", PersistentTreeMap.class).getOffset() == m1.getOffset());

        ObjectDirectory.remove("m1", PersistentTreeMap.class);

        assert(ObjectDirectory.get("m1", PersistentTreeMap.class) == null);
    }
}
