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

package lib.llpl;

public interface MemoryRegion {
    int MODE_R  = (1 << 0);
    int MODE_W  = (1 << 1);
    int MODE_RW = MODE_R | MODE_W;

    void checkAccess(int mode) throws IllegalAccessException;
    void checkAlive();
    void checkBounds(long offset) throws IndexOutOfBoundsException;
    long addr() throws UnsupportedOperationException;
    byte getByte(long offset);
    void putByte(long offset, byte value);
    short getShort(long offset);
    void putShort(long offset, short value);
    int getInt(long offset);
    void putInt(long offset, int value);
    long getLong(long offset);
    void putLong(long offset, long value);
    long getAddress(long offset);
    void putAddress(long offset, long value);
}
