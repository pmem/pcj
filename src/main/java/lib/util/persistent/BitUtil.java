/* Copyright (C) 2018  Intel Corporation
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

public class BitUtil{

    public static void putByte(byte[] ba, int index, byte b) {
        ba[index] = b; 
    }
       
    public static void putShort(byte[] ba, int index, short s) {
        int i = Short.toUnsignedInt(s);
        for (int x=0; x<Short.BYTES; x++) {
            ba[x+index] = ((Integer)i).byteValue();
            i=i>>8;
        }
    }
       
    public static void putInt(byte[] ba, int index, int i) {
        for (int x=0; x<Integer.BYTES; x++) {
            ba[x+index] = ((Integer)i).byteValue();
            i=i>>8;
        }
    }
       
    public static void putLong(byte[] ba, int index, long l) {
        for (int x=0; x<Long.BYTES; x++) {
            ba[x+index] = ((Long)l).byteValue();
            l=l>>8;
        }
    }

    public static byte getByte(byte[] ba, int index) {
        return ba[index];
    }

    public static short getShort(byte[] ba, int index) {
        short ans=0;
        for (int x=0; x<Short.BYTES; x++) {
            ans += (Byte.toUnsignedInt(ba[index+x]) << (8*x));
        }
        return ans;
    }

    public static int getInt(byte[] ba, int index) {
        int ans=0;
        for (int x=0; x<Integer.BYTES; x++) {
            ans += (Byte.toUnsignedInt(ba[index+x]) << (8*x));
        }
        return ans;
    }

    public static long getLong(byte[] ba, int index) {
        long ans=0;
        for (int x=0; x<Long.BYTES; x++) {
            ans += (Byte.toUnsignedLong(ba[index+x]) << (8*x));
        }
        return ans;
    }
}
