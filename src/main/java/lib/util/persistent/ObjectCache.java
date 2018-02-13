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
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
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
    private static Set<Long> uncommittedConstructions;
    private static final PersistentHeap heap;
    private static Thread collector;
    private static long counter;                    // only used for ObjectCache stats
    private static final long counterMod = 10000;   // only used for ObjectCache stats
    private static final AnyPersistent cacheLock;
    
    static {
        cache = new ConcurrentHashMap<>();
        queue = new ReferenceQueue<>();
        cacheLock = AnyPersistent.asLock();
        uncommittedConstructions = new ConcurrentSkipListSet<>();
        heap = PersistentMemoryProvider.getDefaultProvider().getHeap();
        collector = new Thread(() -> {
            try {
                while (true) {
                    Ref<?> qref = (Ref)queue.remove();
                    // trace(true, qref.getAddress(), "object enqueued");
                    long address = qref.getAddress();
                    if (Config.ENABLE_MEMORY_STATS) Stats.current.memory.enqueued++;
                    if (!qref.isForAdmin()) {
                        if (uncommittedConstructions.contains(address)) {
                            remove(address);
                            continue;
                        }
                        Transaction.run(() -> {
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
            super(obj, queue);
            this.address = obj.getPointer().addr();
            this.forAdmin = forAdmin;
            if (Config.ENABLE_ALLOC_STATS) Stats.current.allocStats.update(Ref.class.getName(), 0, 25, 1);   // uncomment for allocation stats
        }

        public long getAddress() {return address;}
        public boolean isForAdmin() {return forAdmin;}
        public void setForAdmin(boolean forAdmin) {this.forAdmin = forAdmin;}
        public String toString() {return String.format("Ref(%d, %s)\n", address, isForAdmin());}
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
            if (Config.ENABLE_OBJECT_CACHE_STATS) {if (ref == null) Stats.current.objectCache.simpleMisses++; else Stats.current.objectCache.referentMisses++;}  // uncomment for ObjectCache stats
            obj = objectForAddress(address, forAdmin);
            ref = new Ref(obj, forAdmin);
            cache.put(cacheAddress, ref);
            if (Config.ENABLE_OBJECT_CACHE_STATS) updateCacheSizeStats();                       // uncomment for ObjectCache stats
        }
        else if (ref.isForAdmin() && !forAdmin) {
                // trace(address, "HIT: forAdmin -> !forAdmin");
                ref.setForAdmin(false);
                obj = (T)ref.get();
                obj.initForGC();
                if (Config.ENABLE_OBJECT_CACHE_STATS) Stats.current.objectCache.promotedHits++; // uncomment for ObjectCache stats
        }
        else if (Config.ENABLE_OBJECT_CACHE_STATS) Stats.current.objectCache.simpleHits++;      // uncomment for ObjectCache stats
        assert(obj != null);
        return ref;
    }

    // TODO: not sound, only usable for testing
    public static void clear() {
        cache.clear();
    }

    private static void updateCacheSizeStats() {
        long size;
        if (counter++ % counterMod != 0 || (size = cache.size()) < Stats.current.objectCache.maxSize) return;
        Stats.current.objectCache.maxSize = size;
    }

    @SuppressWarnings("unchecked")
    static <T extends AnyPersistent> T objectForAddress(long address, boolean forAdmin) {
        T obj = Transaction.run(() -> {
            T ans = null;
            try {
                MemoryRegion region = new UncheckedPersistentMemoryRegion(address);
                long classInfoAddress = region.getLong(0);
                ClassInfo ci = ClassInfo.getClassInfo(classInfoAddress);
                ObjectType<T> type = Types.typeForName(ci.className());
                Constructor ctor = ci.getReconstructor();
                ans = (T)ctor.newInstance(new ObjectPointer<T>(type, region));
                if (!forAdmin) ans.initForGC();
            }
            catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                if (e instanceof InvocationTargetException && e.getCause() instanceof TransactionRetryException){
                    throw new TransactionRetryException();
                }
                else throw new RuntimeException("failed to call reflected constructor: " + e.getCause());
            }
            if (!forAdmin) Transaction.addNewObject(ans);
            return ans;
        }, cacheLock);
        return obj;
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
        if (Config.ENABLE_OBJECT_CACHE_STATS) updateCacheSizeStats();                     // uncomment for ObjectCache stats
        Transaction.addNewObject(obj);
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

    public static void uncommittedConstruction(AnyPersistent obj) {
        uncommittedConstructions.add(obj.getPointer().addr());
        // trace(true, obj.getPointer().addr(), "added uncommitedConstruction");
    }

    public static void committedConstruction(AnyPersistent obj) {
        // trace(obj.getPointer().addr(), "committedConstruction called");
        uncommittedConstructions.remove(obj.getPointer().addr());
    }

    static class Address {
        private final long addr;

        Address(long addr) {
            if (Config.ENABLE_ALLOC_STATS) Stats.current.allocStats.update(Address.class.getName(), 0,  16 + 8, 1);  // uncomment for allocation stats
            this.addr = addr;
        }

        long addr() {
            return addr; 
        }

        @Override
        public int hashCode() {
            return Long.hashCode(addr >>> 6);
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
