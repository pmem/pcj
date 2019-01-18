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

import lib.util.persistent.types.ObjectType;

public final class ObjectPointer<T extends AnyPersistent> 
{
    private final ObjectType<T> type;
    private final MemoryRegion region;

    public ObjectPointer(ObjectType<T> type, MemoryRegion region)
    {
        assert(type != null);
        assert(region != null);
        this.type = type;
        this.region = region;
    }

    ObjectType<T> type() {
        return type;
    }

    MemoryRegion region() {
        return region;
    }

    public long addr() {
        return region.addr();
    }

    public String toString() {
        return "ObjectPointer(" + type + ", " + region + ")";
    }

    public int hashCode() {
        return Long.hashCode(addr());
    }
}
