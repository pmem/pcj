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

public interface MemoryRegion {
    long addr() throws UnsupportedOperationException;

    byte getByte(long offset);
    short getShort(long offset);
    int getInt(long offset);
    long getLong(long offset);

    void putByte(long offset, byte value);
    void putShort(long offset, short value);
    void putInt(long offset, int value);
    void putLong(long offset, long value);

    void putDurableByte(long offset, byte value);
    void putDurableShort(long offset, short value);
    void putDurableInt(long offset, int value);
    void putDurableLong(long offset, long value);

    void putRawByte(long offset, byte value);
    void putRawShort(long offset, short value);
    void putRawInt(long offset, int value);
    void putRawLong(long offset, long value);
    void putRawBytes(long offset, byte[] value);

    void flush(long size);
    void flush(long offset, long size);
}
