/*
 * Copyright (c) 2017 Intel Corporation.
*/

/*
 * Copyright (c) 2001, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package lib.util.persistent;

import java.nio.ByteBuffer;
import static lib.util.persistent.Trace.*;

public class VolatileMemoryRegion implements MemoryRegion {
    private final byte[] bytes;
    public final long size;

    public VolatileMemoryRegion(long size) {
        this.size = size;
        bytes = new byte[(int)size];
    }

    public long addr() {return -1;}
    public void checkAccess(int mode) throws IllegalAccessException {}
    public void checkAlive() {}
    public void checkBounds(long offset) throws IndexOutOfBoundsException {}

    public byte[] getBytes() {return bytes;}

    public byte getByte(long offset) {
        return bytes[(int)offset];
    }

    public void putByte(long offset, byte value) {
        bytes[(int)offset] = value;
    }

    public short getShort(long offset) {
        return Bits.getShort(bytes, (int)offset);
    }

    public void putShort(long offset, short value) {
        Bits.putShort(bytes, (int)offset, value);
    }

    public int getInt(long offset) {
        return Bits.getInt(bytes, (int)offset);
    }

    public void putInt(long offset, int value) {
        Bits.putInt(bytes, (int)offset, value);
    }

    public long getLong(long offset) {
        return Bits.getLong(bytes, (int)offset);
    }

    public void putLong(long offset, long value) {
        Bits.putLong(bytes, (int)offset, value);
    }

    public long getAddress(long offset) {
        return getLong(offset);
    }

    public void putAddress(long offset, long value) {
        putLong(offset, value);
    }

    public void putDurableByte(long offset, byte value) {putByte(offset, value);}
    public void putDurableShort(long offset, short value) {putShort(offset, value);}
    public void putDurableInt(long offset, int value) {putInt(offset, value);}
    public void putDurableLong(long offset, long value) {putLong(offset, value);}
    public void putDurableAddress(long offset, long value) {putAddress(offset, value);}

    public void putRawByte(long offset, byte value) {putByte(offset, value);}
    public void putRawShort(long offset, short value) {putShort(offset, value);}
    public void putRawInt(long offset, int value) {putInt(offset, value);}
    public void putRawLong(long offset, long value) {putLong(offset, value);}
    public void putRawAddress(long offset, long value) {putAddress(offset, value);}

    public void putRawBytes(long offset, byte[] value) {
        System.arraycopy(value, 0, bytes, (int)offset, value.length);
    }

    public void flush(long size) {}
    public void flush(long offset, long size) {}

    // from OpenJDK Grep Code
    static class Bits {

        /*
         * Methods for unpacking primitive values from byte arrays starting at
         * given offsets.
         */

        static boolean getBoolean(byte[] b, int off) {
            return b[off] != 0;
        }

        static char getChar(byte[] b, int off) {
            return (char) (((b[off + 1] & 0xFF) << 0) +
                           ((b[off + 0]) << 8));
        }

        static short getShort(byte[] b, int off) {
            return (short) (((b[off + 1] & 0xFF) << 0) +
                            ((b[off + 0]) << 8));
        }

        static int getInt(byte[] b, int off) {
            return ((b[off + 3] & 0xFF) << 0) +
                   ((b[off + 2] & 0xFF) << 8) +
                   ((b[off + 1] & 0xFF) << 16) +
                   ((b[off + 0]) << 24);
        }

        static float getFloat(byte[] b, int off) {
            int i = ((b[off + 3] & 0xFF) << 0) +
                    ((b[off + 2] & 0xFF) << 8) +
                    ((b[off + 1] & 0xFF) << 16) +
                    ((b[off + 0]) << 24);
            return Float.intBitsToFloat(i);
        }

        static long getLong(byte[] b, int off) {
            return ((b[off + 7] & 0xFFL) << 0) +
                   ((b[off + 6] & 0xFFL) << 8) +
                   ((b[off + 5] & 0xFFL) << 16) +
                   ((b[off + 4] & 0xFFL) << 24) +
                   ((b[off + 3] & 0xFFL) << 32) +
                   ((b[off + 2] & 0xFFL) << 40) +
                   ((b[off + 1] & 0xFFL) << 48) +
                   (((long) b[off + 0]) << 56);
        }

        static double getDouble(byte[] b, int off) {
            long j = ((b[off + 7] & 0xFFL) << 0) +
                     ((b[off + 6] & 0xFFL) << 8) +
                     ((b[off + 5] & 0xFFL) << 16) +
                     ((b[off + 4] & 0xFFL) << 24) +
                     ((b[off + 3] & 0xFFL) << 32) +
                     ((b[off + 2] & 0xFFL) << 40) +
                     ((b[off + 1] & 0xFFL) << 48) +
                     (((long) b[off + 0]) << 56);
            return Double.longBitsToDouble(j);
        }

        /*
         * Methods for packing primitive values into byte arrays starting at given
         * offsets.
         */

        static void putBoolean(byte[] b, int off, boolean val) {
            b[off] = (byte) (val ? 1 : 0);
        }

        static void putChar(byte[] b, int off, char val) {
            b[off + 1] = (byte) (val >>> 0);
            b[off + 0] = (byte) (val >>> 8);
        }

        static void putShort(byte[] b, int off, short val) {
            b[off + 1] = (byte) (val >>> 0);
            b[off + 0] = (byte) (val >>> 8);
        }

        static void putInt(byte[] b, int off, int val) {
            b[off + 3] = (byte) (val >>> 0);
            b[off + 2] = (byte) (val >>> 8);
            b[off + 1] = (byte) (val >>> 16);
            b[off + 0] = (byte) (val >>> 24);
        }

        static void putFloat(byte[] b, int off, float val) {
            int i = Float.floatToIntBits(val);
            b[off + 3] = (byte) (i >>> 0);
            b[off + 2] = (byte) (i >>> 8);
            b[off + 1] = (byte) (i >>> 16);
            b[off + 0] = (byte) (i >>> 24);
        }

        static void putLong(byte[] b, int off, long val) {
            b[off + 7] = (byte) (val >>> 0);
            b[off + 6] = (byte) (val >>> 8);
            b[off + 5] = (byte) (val >>> 16);
            b[off + 4] = (byte) (val >>> 24);
            b[off + 3] = (byte) (val >>> 32);
            b[off + 2] = (byte) (val >>> 40);
            b[off + 1] = (byte) (val >>> 48);
            b[off + 0] = (byte) (val >>> 56);
        }

        static void putDouble(byte[] b, int off, double val) {
            long j = Double.doubleToLongBits(val);
            b[off + 7] = (byte) (j >>> 0);
            b[off + 6] = (byte) (j >>> 8);
            b[off + 5] = (byte) (j >>> 16);
            b[off + 4] = (byte) (j >>> 24);
            b[off + 3] = (byte) (j >>> 32);
            b[off + 2] = (byte) (j >>> 40);
            b[off + 1] = (byte) (j >>> 48);
            b[off + 0] = (byte) (j >>> 56);
        }
    }
}
