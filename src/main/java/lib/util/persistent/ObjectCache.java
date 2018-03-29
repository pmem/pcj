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
    static final ThreadLocal<Boolean> adminMode;    // TODO: temp workaround for reconstructor side-effects during heap cleanup
    
    static {
        cache = new ConcurrentHashMap<>();
        queue = new ReferenceQueue<>();
        cacheLock = AnyPersistent.asLock();
        uncommittedConstructions = new ConcurrentSkipListSet<>();
        adminMode = ThreadLocal.withInitial(() -> false);
        heap = PersistentMemoryProvider.getDefaultProvider().getHeap();
        collector = new Thread(() -> {
            try {
                while (true) {
                    Ref<?> ref = (Ref)queue.remove();
                    long address = ref.getAddress();
                    if (address == 0) {
                        // trace(true, address, "ignoring address");
                        continue;
                    }
                    if (uncommittedConstructions.contains(address)) {
                        // trace(true, address, "ignoring address");
                        uncommittedConstructions.remove(address);
                        continue;
                    }
                    // trace(true, ref.getAddress(), "object enqueued and processing");
                    if (Config.ENABLE_MEMORY_STATS) Stats.current.memory.enqueued++;
                        AnyPersistent obj = get(address, true);
                        obj.deleteReference(true);
                }
            } 
            catch (InterruptedException ie) {
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
            this.address = obj.addr();
            this.forAdmin = forAdmin;
            //trace(true, this.address, "new ref created!");
            if (Config.ENABLE_ALLOC_STATS) Stats.current.allocStats.update(Ref.class.getName(), 0, 25, 1);   // uncomment for allocation stats
        }

        public void clear() {address = 0;}
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
        // trace(true, address, "ObjectCache.get(), forAdmin = %s", forAdmin);
        Ref<?> ref = getReference(address, forAdmin);
        return ref == null ? null :(T)ref.get();
    }

    @SuppressWarnings("unchecked")
    static <T extends AnyPersistent> Ref<T> getReference(long address, boolean forAdmin) {
        T obj = null;
        Ref ref = null;
        if (address == 0) return null;
        Address cacheAddress = new Address(address);
        ref = (Ref<T>)cache.get(cacheAddress);
        if (ref == null || (obj = (T)ref.get()) == null) {
            ref = Transaction.getReconstructedObject(address);
            if (ref == null || (obj = (T)ref.get()) == null) {
                // trace(true, address, "MISS: " + (ref == null ? "simple" : "null referent"));
                if (Config.ENABLE_OBJECT_CACHE_STATS) {if (ref == null) Stats.current.objectCache.simpleMisses++; else Stats.current.objectCache.referentMisses++;}  // uncomment for ObjectCache stats
                boolean admin = ObjectCache.adminMode.get() || forAdmin;
                ref = objectForAddress(address, admin);
                obj = (T)ref.get();
                if (Config.ENABLE_OBJECT_CACHE_STATS) updateCacheSizeStats();                       // uncomment for ObjectCache stats
            }
        }
        else if (ref.isForAdmin() && !forAdmin && !adminMode.get()) {
                // trace(true, address, "HIT: forAdmin -> !forAdmin");
                ref.setForAdmin(false);
                obj = (T)ref.get();
                if (Config.ENABLE_OBJECT_CACHE_STATS) Stats.current.objectCache.promotedHits++; // uncomment for ObjectCache stats
        }
        else if (Config.ENABLE_OBJECT_CACHE_STATS) Stats.current.objectCache.simpleHits++;      // uncomment for ObjectCache stats
                // trace(true,address, "simple HIT in OC");
        // assert(obj != null):address;
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
    static <T extends AnyPersistent> Ref<T> objectForAddress(long address, boolean forAdmin) {
        final Address addr =  new Address(address);
        return Util.synchronizedBlock(cacheLock, ()->{ 
        Ref<T> ans = null;
        if ((ans = (Ref<T>)cache.get(addr)) != null && ans.get() != null) {
                 // trace(true, address, "OFA HIT: in OC");
            // assert(ans.get() != null);
            return ans; 
        }
        else {
            T obj = null;
            try {
                MemoryRegion region = new UncheckedPersistentMemoryRegion(address);
                long classInfoAddress = region.getLong(0);
                ClassInfo ci = ClassInfo.getClassInfo(classInfoAddress);
                ObjectType<T> type = Types.typeForName(ci.className());
                Constructor ctor = ci.getReconstructor();
                obj = (T)ctor.newInstance(new ObjectPointer<T>(type, region));
            }
            catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                if (e instanceof InvocationTargetException && e.getCause() instanceof TransactionRetryException){
                    throw new TransactionRetryException();
                }
                else throw new RuntimeException("failed to call reflected constructor: " + e.getCause());
            }
            ans = new Ref(obj, forAdmin);
            if (!Transaction.addReconstructedObject(address, ans)) cache.put(new Address(address), ans);
        }
        return ans; 
        });
    }
    public static void remove(long address) {
        // trace(true, address, "ObjectCache.remove");
        cache.remove(new Address(address));
    }

    @SuppressWarnings("unchecked")
    static <T extends AnyPersistent> void add(long address, Ref ref) {
        // trace(obj.getPointer().addr(), "ObjectCache.add");
        cache.put(new Address(address), ref);
    }
    
    /*static <T extends AnyPersistent> void add(T obj) {
        // trace(obj.getPointer().addr(), "ObjectCache.add");
        long address = obj.getPointer().addr();
        //Transaction.addNewObject(obj);
        Ref<T> ref = new Ref<>(obj);
        cache.put(new Address(address), ref);
        if (Config.ENABLE_OBJECT_CACHE_STATS) updateCacheSizeStats();                     // uncomment for ObjectCache stats
    }*/

    public static void uncommittedConstruction(long addr) {
        uncommittedConstructions.add(addr);
        // trace(true, obj.addr(), "added uncommitedConstruction");
    }

    public static void uncommittedConstruction(AnyPersistent obj) {
        uncommittedConstructions.add(obj.addr());
        // trace(true, obj.addr(), "added uncommitedConstruction");
    }

    public static void committedConstruction(AnyPersistent obj) {
        // trace(obj.addr(), "committedConstruction called");
        uncommittedConstructions.remove(obj.addr());
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
