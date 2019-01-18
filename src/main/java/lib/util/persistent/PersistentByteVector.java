    /* Copyright (C) 2018 Intel Corporation
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

    import lib.util.persistent.types.LongField;
    import lib.util.persistent.types.ValueField;
    import lib.util.persistent.types.ObjectType;
    import lib.util.persistent.types.Types;
    import lib.xpersistent.UncheckedPersistentMemoryRegion;

    public final class PersistentByteVector extends PersistentObject {
    private static final LongField POINTER = new LongField();
    private static final ObjectType<PersistentByteVector> TYPE = ObjectType.withValueFields(PersistentByteVector.class, POINTER);
    private static final long BYTES_OFFSET = 8;
    private MemoryRegion data;

    public PersistentByteVector(long capacity) {
        super(TYPE);
        long regionSize = regionSize(capacity);
        this.data = new VolatileMemoryRegion(regionSize);
        data.putLong(0, capacity);
    }

    public PersistentByteVector(byte[] bytes) {
        this(bytes.length);
        putBytesAt(bytes, 0);
    }

    public PersistentByteVector(ObjectPointer<PersistentByteVector> p) {
        super(p);
    }

    @Override
    void onSet() {
        long capacity = capacity();
        long regionSize = regionSize(capacity);
        MemoryRegion region = heap.allocateRegion(regionSize);
        if (Config.ENABLE_ALLOC_STATS) Stats.current.allocStats.update("lib.util.persistent.PersistentByteVector (payload)", regionSize, 0, 1);
        Util.memCopyVP(data, 0, region, 0, regionSize); // this is transactional
        // will leak if we crash here
        setLongField(POINTER, region.addr());
    }

    @Override
    void onGet() {
        long offset = getLongField(POINTER);
        assert(offset != 0);
        MemoryRegion region = new UncheckedPersistentMemoryRegion(offset);
        long capacity = region.getLong(0);
        long regionSize = regionSize(capacity);
        data = new VolatileMemoryRegion(regionSize);
        Util.memCopyPV(region, 0, data, 0, regionSize);
    }

    @Override
    void onFree(long offset) {
        MemoryRegion region = new UncheckedPersistentMemoryRegion(offset);
        long bytesRegionOffset = region.getLong(0);
        heap.freeRegion(new UncheckedPersistentMemoryRegion(bytesRegionOffset));
    }

    public byte getByteAt(long index) {return data.getByte(check(index, 1));}
    public short getShortAt(long index) {return data.getShort(check(index, 2));}
    public int getIntAt(long index) {return data.getInt(check(index, 4));}
    public long getLongAt(long index) {return data.getLong(check(index, 8));}

    public void putByteAt(long index, byte value) {data.putByte(check(index, 1), value);}
    public void putShortAt(long index, short value) {data.putShort(check(index, 2), value);}
    public void putIntAt(long index, int value) {data.putInt(check(index, 4), value);}
    public void putLongAt(long index, long value) {data.putLong(check(index, 8), value);}

    public byte[] getBytesAt(int index, int size) {
        byte[] ans = new byte[size];
        getBytesAt(ans, index, size);
        return ans;
    }

    public int getBytesAt(byte[] bytes, int index, int size) {
        int clamp = Math.min(bytes.length, size);
        check(index, clamp);
        System.arraycopy(((VolatileMemoryRegion)data).getBytes(), intOffset(index), bytes, 0, clamp);
        return clamp;
    }

    public void putBytesAt(byte[] bytes, int index) {
        check(index, bytes.length);
        System.arraycopy(bytes, 0, ((VolatileMemoryRegion)data).getBytes(), intOffset(index), bytes.length);
    }

    public byte[] getBytes() {
        long capacity = capacity();
    	if (capacity > Integer.MAX_VALUE) throw new RuntimeException("toBytes: PersistentByteVector is too large");
    	byte[] ans = new byte[(int)capacity];
        System.arraycopy(((VolatileMemoryRegion)data).getBytes(), intOffset(0), ans, 0, (int)capacity);
        return ans;
    }
    
    public long capacity() {return data.getLong(0);}

    private long offset(long index) {return BYTES_OFFSET + index;}

    private int intOffset(int index) {return (int)BYTES_OFFSET + index;}

    private long regionSize(long capacity) {return BYTES_OFFSET + capacity;}

    private long check(long index, long count) {
        if (index >= 0 && index + count <= capacity()) return offset(index);
        throw new IndexOutOfBoundsException("can't access " + count + " bytes starting at index " + index);
    } 
}
