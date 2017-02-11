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
import java.util.Set;
import java.util.SortedMap;
import java.util.AbstractSet;
import java.util.Iterator;

public class TestUtil {

    public static void main(String[] args) {}

    static PersistentByteBuffer createByteBuffer(int size, String value) {
        PersistentByteBuffer buf = PersistentByteBuffer.allocate(size);
        buf.put(value.getBytes());
        buf.rewind();
        return buf;
    }

    // Some helper functions
    static PersistentByteBuffer createOrFetchByteBuffer(int size, String value) {
        String bufName = value + size;
        if (getBuf(bufName) == null) {
            PersistentByteBuffer buf = PersistentByteBuffer.allocate(size);
            buf.put(value.getBytes());
            buf.rewind();
            ObjectDirectory.put(bufName, buf);
            return buf;
        } else {
            PersistentByteBuffer buf = ObjectDirectory.get(bufName, PersistentByteBuffer.class);
            return buf;
        }
    }

    static String decode(PersistentByteBuffer buf) {
        if (buf == null) return "NULL";
        byte[] value = new byte[buf.remaining()];
        buf.mark();
        buf.get(value);
        buf.reset();
        return new String(value);
    }

    static void printKVPair(PersistentByteBuffer key, PersistentByteBuffer val) {
        System.out.println("(" + decode(key) + ", " + decode(val) + ")");
    }

    static void retrieveKVPair(PersistentByteBuffer key, PersistentTreeMap<PersistentByteBuffer, PersistentByteBuffer> map) {
        System.out.println("In map: (" + decode(key) + ", " + decode(map.get(key)) + ").");
    }

    static void iterateMap(SortedMap<PersistentByteBuffer, PersistentByteBuffer> map) {
        for (Map.Entry<PersistentByteBuffer, PersistentByteBuffer> e : map.entrySet()) {
            printKVPair(e.getKey(), e.getValue());
        }
    }

    static PersistentTreeMap<PersistentByteBuffer, PersistentByteBuffer> getOrCreateMap(String name) {
        PersistentTreeMap<PersistentByteBuffer, PersistentByteBuffer> map;
        if ((map = ObjectDirectory.get(name, PersistentTreeMap.class)) == null) {
            map = new PersistentTreeMap<PersistentByteBuffer, PersistentByteBuffer>();
            ObjectDirectory.put(name, map);
        }
        return map;
    }

    static PersistentByteBuffer getBuf(String name) {
        PersistentByteBuffer buf;
        if ((buf = ObjectDirectory.get(name, PersistentByteBuffer.class)) == null) {
            return null;
        } else {
            return buf;
        }
    }

    static boolean shouldEqual(PersistentByteBuffer buf, String expected) {
        byte[] value = new byte[buf.capacity()];
        int position = buf.position();
        int limit = buf.limit();
        buf.position(0).limit(buf.capacity());
        buf.get(value);
        buf.position(position).limit(limit);
        return new String(value).equals(expected);
    }
}
