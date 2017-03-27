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

import lib.util.persistent.types.PersistentField;
import java.util.List;
import java.util.Arrays;

public interface Persistent<T extends Persistent<T>> //extends Reference<T>
{
    // public default ptr() {return getPointer();}
    // public default T get() {return ptr().deref();}
    // public defalut void set(T t) {setPointer(t.ptr());}

    public Pointer<? extends T> getPointer();

    public static PersistentByte persistent(byte x) {return new PersistentByte(x);}
    public static PersistentShort persistent(short x) {return new PersistentShort(x);}
    public static PersistentInteger persistent(int x) {return new PersistentInteger(x);}
    public static PersistentLong persistent(long x) {return new PersistentLong(x);}
    public static PersistentFloat persistent(float x) {return new PersistentFloat(x);}
    public static PersistentDouble persistent(double x) {return new PersistentDouble(x);}
    public static PersistentCharacter persistent(char x) {return new PersistentCharacter(x);}
    public static PersistentBoolean persistent(boolean x) {return new PersistentBoolean(x);}
    public static PersistentString persistent(String x) {return new PersistentString(x);}
}
