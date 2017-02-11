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

package lib.persistent.spi;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import lib.persistent.Transaction;
import lib.xpersistent.XPersistentMemory;
//import lib.persistent.Heap;

public abstract class PersistentMemoryProvider {
    private static final ConcurrentHashMap<String, PersistentMemoryProvider> providers;
    private static PersistentMemoryProvider defaultProvider;

    static {
        providers = new ConcurrentHashMap<>();
        registerDefaultProvider(new XPersistentMemory());
    }

    static void registerDefaultProvider(PersistentMemoryProvider provider) {
        registerProvider(provider);
        defaultProvider = provider;
    }

    public static void registerProvider(PersistentMemoryProvider provider) {
        providers.put(provider.name(), provider);
    }

    public static PersistentMemoryProvider defaultProvider() {
        return defaultProvider;
    }

    public static PersistentMemoryProvider getProvider(String name) {
        return providers.get(name);
    }

    public static Collection<PersistentMemoryProvider> installedProviders() {
        return providers.values();
    }

    public abstract String name();
    public abstract Transaction newTransaction();
    //public abstract Heap getHeap();
}
