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

package lib.util.persistent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.ref.Reference;
import lib.util.persistent.spi.PersistentMemoryProvider;
import lib.util.persistent.types.Types;
import lib.util.persistent.types.ObjectType;
import java.lang.reflect.Constructor;

class ObjectCache {
    private static final Map<Long, Reference<? extends PersistentObject>> cache = new ConcurrentHashMap<>();
    private static final PersistentHeap heap = PersistentMemoryProvider.getDefaultProvider().getHeap();

    @SuppressWarnings("unchecked")
    static <T extends PersistentObject> T getReference(long address) {
        // System.out.format("thread id: %d, getReferece(%d)\n", Thread.currentThread().getId(), address);
        T obj = null;
        synchronized(ObjectCache.class) {
            if (address == 0) return null;
            Reference<T> ref = (Reference<T>)cache.get(address);
            if (ref == null || (obj = ref.get()) == null) {
                obj = referenceForAddress(address);
                cache.put(address, new SoftReference<T>(obj));
            }
        }
        assert(obj != null);
        return obj;
    }

    @SuppressWarnings("unchecked")
    private static <T extends PersistentObject> T referenceForAddress(long address) {
        // System.out.format("thread id: %d, referenceForAddress(%d)\n", Thread.currentThread().getId(), address);
        MemoryRegion valueRegion = heap.regionFromAddress(address);
        long typeNameAddr = valueRegion.getLong(0);
        MemoryRegion typeNameRegion = heap.regionFromAddress(typeNameAddr);
        String typeName = PersistentObject.typeNameFromRegion(typeNameRegion);
        ObjectType<T> type = Types.typeForName(typeName);
        Class<T> cls = type.cls();
        T obj = null;
        try {
            Constructor ctor = cls.getDeclaredConstructor(ObjectPointer.class);
            ctor.setAccessible(true);
            obj = (T)ctor.newInstance(new ObjectPointer<T>(type, valueRegion));
        }
        catch (Exception e) {e.printStackTrace();}
        return obj;
    }

    static void removeReference(long addr) {
        // System.out.format("thread id: %d, removeReference(%d)\n", Thread.currentThread().getId(), addr);
        cache.remove(addr);
    }

    // only called from PersistentObject allocating constructor
    static <T extends PersistentObject> void addReference(T obj) {
        // System.out.format("thread id: %d, addReference(%d)\n", Thread.currentThread().getId(), obj.getPointer().addr());
        assert(cache.get(obj.getPointer().addr()) == null);
        Reference<T> ref = new SoftReference<>(obj);
        cache.put(obj.getPointer().addr(), ref);
    }
}
