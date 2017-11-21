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
import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import lib.util.persistent.spi.PersistentMemoryProvider;
import lib.util.persistent.types.Types;
import lib.util.persistent.types.ObjectType;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.ref.ReferenceQueue;
import lib.xpersistent.XRoot;
import lib.xpersistent.XTransaction;
import lib.xpersistent.UncheckedPersistentMemoryRegion;
import static lib.util.persistent.Trace.*;


public class ObjectCache {
    private static final Map<Address, Reference<? extends AnyPersistent>> cache;
    private static ReferenceQueue<AnyPersistent> queue;
    private static Map<Long, PRef<?>> prefs;
    private static final PersistentHeap heap;
    private static Thread collector;
    private static long counter;                    // only used for ObjectCache stats
    private static final long counterMod = 10000;   // only used for ObjectCache stats

    static {
        cache = new ConcurrentHashMap<>();
        queue = new ReferenceQueue<>();
        prefs = new ConcurrentHashMap<>();
        heap = PersistentMemoryProvider.getDefaultProvider().getHeap();
        collector = new Thread(() -> {
            try {
                while (true) {
                    PRef<?> qref = (PRef)queue.remove();
                    trace(qref.getAddress(), "object enqueued");
                    Stats.current.memory.enqueued++;
                    prefs.remove(qref.getAddress());
                    if (!qref.isForAdmin()) {
                        Transaction.run(() -> {
                            long address = qref.getAddress();
                            deregisterObject(address);
                            AnyPersistent obj = get(address, true);
                            Transaction.run(() -> {
                                obj.deleteReference();
                            }, obj);
                            if (Config.REMOVE_FROM_OBJECT_CACHE_ON_ENQUEUE) remove(address);
                        });
                    }
                }
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        });
        collector.setDaemon(true);
        collector.start();
    }

    public static class Ref<T extends AnyPersistent> extends SoftReference<T> {
        private long address;
        private boolean forAdmin;

        public Ref(T obj) {
            this(obj, false);
        }

        public Ref(T obj, boolean forAdmin) {
            super(obj);
            this.address = obj.getPointer().addr();
            this.forAdmin = forAdmin;
        }

        public long getAddress() {return address;}
        public boolean isForAdmin() {return forAdmin;}
        public void setForAdmin(boolean forAdmin) {this.forAdmin = forAdmin;}
        public String toString() {return String.format("Ref(%d, %s)\n", address, isForAdmin());}
    }

    public static class PRef<T extends AnyPersistent> extends PhantomReference<T> {
        private long address;
        private boolean forAdmin;

        public PRef(T obj) {
            this(obj, false);
        }

        public PRef(T obj, boolean forAdmin) {
            super(obj, queue);
            trace("created PRef object for address " + obj.getPointer().addr());
            this.address = obj.getPointer().addr();
            this.forAdmin = forAdmin;
            prefs.put(this.address, this);
        }

        public long getAddress() {return address;}
        public boolean isForAdmin() {return forAdmin;}
        public void setForAdmin(boolean forAdmin) {this.forAdmin = forAdmin;}
        public String toString() {return String.format("PRef(%d, %s)\n", address, isForAdmin());}
    }

    public static <T extends AnyPersistent> T get(long address) {
        return get(address, false);
    }

    @SuppressWarnings("unchecked")
    public static <T extends AnyPersistent> T get(long address, boolean forAdmin) {
        // trace(address, "ObjectCache.get()");
        Ref<?> ref = getReference(address, forAdmin);
        return ref == null ? null :(T)ref.get();
    }

    @SuppressWarnings("unchecked")
    private static <T extends AnyPersistent> Ref<T> getReference(long address, boolean forAdmin) {
        T obj = null;
        Ref ref = null;
        if (address == 0) return null;
        Address cacheAddress = new Address(address);
        ref = (Ref<T>)cache.get(cacheAddress);
        if (ref == null || (obj = (T)ref.get()) == null) {
            // trace(address, "MISS: " + (ref == null ? "simple" : "null referent"));
            // if (ref == null) Stats.current.objectCache.simpleMisses++; else Stats.current.objectCache.referentMisses++;  // uncomment for ObjectCache stats
            obj = objectForAddress(address, forAdmin);
            ref = new Ref(obj, forAdmin);
            cache.put(cacheAddress, ref);
            // updateCacheSizeStats();                       // uncomment for ObjectCache stats
        }
        else if (ref.isForAdmin() && !forAdmin) {
                // trace(address, "HIT: forAdmin -> !forAdmin");
                ref.setForAdmin(false);
                obj = (T)ref.get();
                obj.initForGC();
                // Stats.current.objectCache.promotedHits++; // uncomment for ObjectCache stats
        }
        // else Stats.current.objectCache.simpleHits++;      // uncomment for ObjectCache stats
        assert(obj != null);
        return ref;
    }

    private static void updateCacheSizeStats() {
        long size;
        if (counter++ % counterMod != 0 || (size = cache.size()) < Stats.current.objectCache.maxSize) return;
        Stats.current.objectCache.maxSize = size;
    }

    @SuppressWarnings("unchecked")
    static <T extends AnyPersistent> T objectForAddress(long address, boolean forAdmin) {
        Box<T> box = new Box<>(null);
        Transaction.run(() -> {
            try {
                MemoryRegion region = new UncheckedPersistentMemoryRegion(address);
                long classInfoAddress = region.getLong(0);
                ClassInfo ci = ClassInfo.getClassInfo(classInfoAddress);
                ObjectType<T> type = Types.typeForName(ci.className());
                Constructor ctor = ci.getReconstructor();
                box.set((T)ctor.newInstance(new ObjectPointer<T>(type, region)));
                if (!forAdmin) box.get().initForGC();
            }
            catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                if (e instanceof InvocationTargetException && e.getCause() instanceof TransactionRetryException){
                    throw new TransactionRetryException();
                }
                else throw new RuntimeException("failed to call reflected constructor"+e);
            }
            if (!forAdmin) XTransaction.addNewObject(box.get());
        });
        return box.get();
    }

    public static void remove(long address) {
       // trace(address, "ObjectCache.remove");
        cache.remove(new Address(address));
    }

    static <T extends AnyPersistent> void add(T obj) {
        // trace(obj.getPointer().addr(), "ObjectCache.add");
        long address = obj.getPointer().addr();
        Ref<T> ref = new Ref<>(obj);
        cache.put(new Address(address), ref);
        // updateCacheSizeStats();                     // uncomment for ObjectCache stats
        XTransaction.addNewObject(obj);
    }

    static <T extends AnyPersistent> void registerObject(T obj) {
        // trace(obj.getPointer().addr(), "register object");
        assert(!obj.getPointer().type().isValueBased());
        ((XRoot)(heap.getRoot())).registerObject(obj.getPointer().region().addr());
    }

    static void deregisterObject(long addr) {
        // trace(addr, "deregister object");
        ((XRoot)(heap.getRoot())).deregisterObject(addr);
    }

    public static void committedConstruction(AnyPersistent obj) {
        // trace(obj.getPointer().addr(), "committedConstruction called");
        new PRef<AnyPersistent>(obj);
    }

    static class Address {
        private final long addr;
        Address(long addr) { this.addr = addr; }
        @Override
        public int hashCode() {
            return Long.hashCode(addr >>> 8);
        }
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Address) {
                return addr == ((Address)obj).addr;
            }
            return false;
        }
    }
}
