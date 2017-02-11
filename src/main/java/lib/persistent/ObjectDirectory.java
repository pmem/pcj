/* Copyright (C) 2016-17  Intel Corporation
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

import java.lang.reflect.*;
import java.util.Map;

@SuppressWarnings("unchecked")
public class ObjectDirectory {

    static PersistentTreeMap<PersistentByteBuffer, Persistent> map;
    static PhantomQueue<Persistent> pq;

    static {
        pq = new PhantomQueue<>((long info, String name) -> {
            //System.out.println("enqueued: Ref(\"" + name + "\", " + info + ")");
            try {
                Util.decRef(info);
                Util.deregisterOffset(info);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }
        });
        map = PersistentTreeMap.fromOffset(Util.getRoot());
    }

    public synchronized static <T extends Persistent> T put(String key, T value) {
        PersistentByteBuffer k = makeKey(key, value.getClass());
        return (T)(map.put(k, value));
    }

    public synchronized static <T extends Persistent> T get(String key, Class<T> cls) {
        PersistentByteBuffer k = makeKey(key, cls);
        return (T)(map.get(k));
    }

    public synchronized static <T extends Persistent> T remove(String key, Class<T> cls) {
        PersistentByteBuffer k = makeKey(key, cls);
        return (T)(map.remove(k));
    }

    private synchronized static PersistentByteBuffer makeKey(String s, Class<?> cls) {
        String clsName = cls.getName();
        int len = s.length() + clsName.length();
        PersistentByteBuffer ans = PersistentByteBuffer.allocate(len);
        ans.put(s.getBytes());
        ans.put(clsName.getBytes());
        ans.rewind();
        return ans;
    }

    static void registerObject(Persistent obj) { pq.registerObject(obj, obj.getOffset()); }
    static void clearRefs() { pq.clearRefs(); }
    static String statusString() { return pq.toString(); }
}
