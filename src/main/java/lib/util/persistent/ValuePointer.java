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

import lib.util.persistent.types.ValueType;
import java.lang.reflect.Constructor;

public class ValuePointer<T extends PersistentValue> extends Pointer<T>
{
    private final ValueType type;
    private final MemoryRegion region;
    private final Class<T> cls;

    public ValuePointer(ValueType type, MemoryRegion region, Class<T> cls)
    {
        assert(type != null);
        assert(region != null);
        assert(cls != null);
        this.type = type;
        this.region = region;
        this.cls = cls;
    }

    ValueType type() {
        return type;
    }

    MemoryRegion region() {
        return region;
    }

    long addr() {
        return region.addr();
    }

    @SuppressWarnings("unchecked")
    T deref()
    {
        try {
            Constructor ctor = cls.getDeclaredConstructor(ValuePointer.class);
            ctor.setAccessible(true);
            T obj = (T)ctor.newInstance(this);
            return obj;
        }
        catch (Exception e) {e.printStackTrace();}
        return null;
    }

    public String toString() {
        return "ValuePointer(" + type + ", " + region + ")";
    }

    public int hashCode() {
        return Long.hashCode(addr());
    }
}
