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

import lib.util.persistent.spi.PersistentMemoryProvider;
import lib.util.persistent.types.Types;
import static lib.util.persistent.Trace.*;

class RawString {
    private final MemoryRegion region;
    private final String string;

    public RawString(String s) {
        this.string = s;
        final Box<MemoryRegion> box = new Box<>();
        Transaction.run(() -> {
            MemoryRegion r = box.set(PersistentMemoryProvider.getDefaultProvider().getHeap().allocateRegion(Types.INT.size() + s.length()));
            putString(r, 0, s);
        });
        this.region = box.get();
    }

    public RawString(MemoryRegion region) {
        this.region = region;
        this.string = getString(0);
    }

    public MemoryRegion getRegion() {
        return region;
    }

    public String toString() {
        return string;
    }

    private String getString(long offset) {
        int size = region.getInt(offset);
        if (size < 0 || size > 128) {
            System.out.format("Heap appears to be corrupt: RawString at address %d size = %d\n", region.addr() + offset, size);
            new Exception().printStackTrace();
            System.exit(-1);
        }
        byte[] bytes = new byte[size];
        long base = offset + Types.INT.size();
        for (int i = 0; i < bytes.length; i++) bytes[i] = region.getByte(base + i);
        String s = new String(bytes);
        return s;
    }

    private void putString(MemoryRegion region, long offset, String s) {
        byte[] bytes = s.getBytes();
        Transaction.run(() -> {
            region.putInt(offset, bytes.length);
            long base = offset + Types.INT.size();
            for (int i = 0; i < bytes.length; i++) region.putByte(base + i, bytes[i]);
        });
    }
}

