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

import lib.util.persistent.types.ArrayType;
import lib.util.persistent.types.ReferenceArrayType;
import lib.util.persistent.types.Types;
import lib.util.persistent.types.PersistentType;
import lib.util.persistent.types.ObjectType;
import java.lang.reflect.Constructor;
import static lib.util.persistent.Trace.*;

abstract class AbstractPersistentImmutableArray extends AbstractPersistentArray {
    protected AbstractPersistentImmutableArray(ArrayType<? extends AnyPersistent> type, int count, Object data) {
        super(type, count, data);
    }

    protected AbstractPersistentImmutableArray(ArrayType<? extends AnyPersistent> type, int count) {
        super(type, count);
    }

    protected AbstractPersistentImmutableArray(ObjectPointer<? extends AbstractPersistentImmutableArray>  p) {
        super(p);
    }

    @Override
    protected byte getByte(long offset) {
        return getRegionByte(offset);
    }

    @Override
    protected short getShort(long offset) {
        return getRegionShort(offset);
    }

    @Override
    protected int getInt(long offset) {
        return getRegionInt(offset);
    }

    @Override
    protected long getLong(long offset) {
        // trace(true, "APIA.getLong(%d)", offset); 
        return getRegionLong(offset);
    }

    @Override
    @SuppressWarnings("unchecked")
    <T extends AnyPersistent> T getObject(long offset, PersistentType type) {
        // trace(true, "APIA.getObject(%d, %s)", offset, type);
        T ans = null;
        long addr = getLong(offset);
        if (addr != 0) ans = (T)ObjectCache.get(addr);
        return ans;
    }

    @Override
    @SuppressWarnings("unchecked") <T extends AnyPersistent> T getValueObject(long offset, PersistentType type) {
        // trace(true, "APIA.getValueObject(%d, %s)", offset, type);
        MemoryRegion srcRegion = region();
        MemoryRegion dstRegion = new VolatileMemoryRegion(type.size());
        // trace(true, "APIA.getValueObject, src addr = %d, srcOffset = %d, dst  = %s, size = %d", srcRegion.addr(), offset, dstRegion, type.size());
        Util.memCopy(getType(), (ObjectType)type, srcRegion, offset, dstRegion, 0L, type.size());
        ObjectPointer p = new ObjectPointer<T>((ObjectType)type, dstRegion);
        return AnyPersistent.reconstruct(new ObjectPointer<T>((ObjectType)type, dstRegion));
    }
}
