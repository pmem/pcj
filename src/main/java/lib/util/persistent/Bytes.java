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

import lib.util.persistent.types.Types;
import lib.util.persistent.types.ObjectType;
import lib.util.persistent.types.BytesType;
import lib.util.persistent.types.LongField;
import lib.util.persistent.types.StringField;
import lib.util.persistent.types.ObjectField;
import lib.util.persistent.types.BytesField;
import lib.util.persistent.types.ValueField;

public final class Bytes extends PersistentObject {
    public Bytes(long size) {
        super(BytesType.forSize(size));
    }

    private Bytes(ObjectPointer<Bytes> p) {super(p);}

    // TODO: implement desired synchronization
    // TODO: decide on mutability
    // TODO: decide on name
    public byte getByte(long index) {return region().getByte(check(index, 1));}
    public short getShort(long index) {return region().getShort(check(index, 2));}
    public int getInt(long index) {return region().getInt(check(index, 4));}
    public long getLong(long index) {return region().getLong(check(index, 8));}

    public void putByte(long index, byte value) {region().putByte(check(index, 1), value);}
    public void putShort(long index, short value) {region().putShort(check(index, 2), value);}
    public void putInt(long index, int value) {region().putInt(check(index, 4), value);}
    public void putLong(long index, long value) {region().putLong(check(index, 8), value);}

    public byte[] getBytes(int index, int size) {
        byte[] ans = new byte[size];
        getBytes(ans, index, size);
        return ans;
    }

    public int getBytes(byte[] bytes, int index, int size) {
        int clamp = Math.min(bytes.length, size);
        check(index, clamp);
        System.arraycopy(((VolatileMemoryRegion)region()).getBytes(), index, bytes, 0, clamp);
        return clamp;
    }

    public void putBytes(int index, byte[] bytes) {
        check(index, bytes.length);
        System.arraycopy(bytes, 0, ((VolatileMemoryRegion)region()).getBytes(), index, bytes.length);
    }

    public long size() {
        return getType().size();
    }

    public String toString() {return String.format("Bytes(size = %d)", size());}

    private long check(long index, long count) {
        if (index >= 0 && index + count <= size()) return index;
        throw new IndexOutOfBoundsException("can't access " + count + " bytes starting at index " + index);
    }
}
