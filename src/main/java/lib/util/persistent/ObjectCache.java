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
import java.lang.ref.ReferenceQueue;
import lib.xpersistent.XRoot;
import lib.xpersistent.UncheckedPersistentMemoryRegion;
import static lib.util.persistent.PersistentObject.trace;


public class ObjectCache {
    private static final Map<Long, Reference<? extends PersistentObject>> cache;
    private static ReferenceQueue<PersistentObject> queue;
    private static final PersistentHeap heap;
    private static Thread collector;

    static {
        cache = new ConcurrentHashMap<>();
        queue = new ReferenceQueue<>();
        heap = PersistentMemoryProvider.getDefaultProvider().getHeap();
        collector = new Thread(() -> {
            try {
                while (true) {
                    Ref<?> qref = (Ref)queue.remove();
                    // trace(qref.getAddress(), String.format("object enqueued, qref.isForAdmin() = %s)", qref.isForAdmin()));
                    if (!qref.isForAdmin()) {
                        synchronized(ObjectCache.class) {
                            Transaction.run(() -> {
                                long address = qref.getAddress();
                                deregisterObject(address);
                                PersistentObject obj = get(address);
                                Transaction.run(() -> {
                                    obj.deleteReference();
                                }, obj);
                            });
                        }
                    }
                }
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        });
        collector.setDaemon(true);
        collector.start();
    }

    public static class Ref<T extends PersistentObject> extends WeakReference<T> {
        private long address;
        private boolean forAdmin;

        public Ref(T obj) {
            this(obj, false);
        }

        public Ref(T obj, boolean forAdmin) {
            super(obj, queue);
            this.address = obj.getPointer().addr();
            this.forAdmin = forAdmin;
        }

        public long getAddress() {return address;}
        public boolean isForAdmin() {return forAdmin;}
        public void setForAdmin(boolean forAdmin) {this.forAdmin = forAdmin;}
        public String toString() {return String.format("Ref(%d, %s)\n", address, isForAdmin());}
    }

    public static <T extends PersistentObject> T get(long address) {
        return get(address, false);
    }

    @SuppressWarnings("unchecked")
    public static <T extends PersistentObject> T get(long address, boolean forAdmin) {
        Ref<?> ref = getReference(address, forAdmin);
        return ref == null ? null :(T)ref.get();
    }

    @SuppressWarnings("unchecked")
    private static <T extends PersistentObject> Ref<T> getReference(long address, boolean forAdmin) {
        T obj = null;
        Ref ref = null;
        if (address == 0) return null;
        ref = (Ref<T>)cache.get(address);
        if (ref == null || (obj = (T)ref.get()) == null) {   
            obj = objectForAddress(address);
            if (!forAdmin) obj.initForGC();       
            ref = new Ref(obj, forAdmin);
            cache.put(address, ref);
        }
        else {
            if (ref.isForAdmin() && !forAdmin) {   
                // trace(address, "HIT: forAdmin -> !forAdmin");
                ref.setForAdmin(false);
                obj = (T)ref.get();
                obj.initForGC();
            }
        }
        assert(obj != null);
        return ref;
    }

    @SuppressWarnings("unchecked")
    static <T extends PersistentObject> T objectForAddress(long address) {
        MemoryRegion valueRegion = new UncheckedPersistentMemoryRegion(address);
        long typeNameAddr = valueRegion.getLong(0);
        MemoryRegion typeNameRegion = new UncheckedPersistentMemoryRegion(typeNameAddr);
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

    static void remove(long address) {
       // trace(address, "ObjectCache.remove");
        cache.remove(address);
    }

    static <T extends PersistentObject> void add(T obj) {
        // trace(obj.getPointer().addr(), "ObjectCache.add");
        long address = obj.getPointer().addr();
        assert(cache.get(address) == null);
        Ref<T> ref = new Ref<>(obj);
        cache.put(address, ref);
    }

    static <T extends PersistentObject> void registerObject(T obj) {
        // trace(obj.getPointer().addr(), "register object");
        ((XRoot)(heap.getRoot())).registerObject(obj.getPointer().region().addr());
    }

    static void deregisterObject(long addr) {
        // trace(addr, "deregister object");
        ((XRoot)(heap.getRoot())).deregisterObject(addr);
    }
}
