/* Copyright (C) 2016  Intel Corporation
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

package lib.persistent;

import java.lang.IllegalArgumentException;
import java.lang.IndexOutOfBoundsException;
import java.lang.UnsupportedOperationException;

public class PersistentString implements Comparable, Persistent {

    private long offset;
    private int length;

    static {
        System.loadLibrary("Persistent");
        Util.openPool();
    }

    synchronized static boolean isValidOffset(long offset) {
        return nativeCheckByteArrayExists(offset);
    }

    public synchronized long getOffset() {
        return this.offset;
    }

    public synchronized long length() {
        if (length == 0)
            this.length = nativeGetLength(offset);
        return this.length;
    }

    public PersistentString(String str) {
        synchronized (PersistentString.class) {
	    this.length = str.length();
            this.offset = nativeReserveByteArrayMemory(this.length);
            ObjectDirectory.registerObject(this);
            nativePut(offset, str.getBytes(), 0, this.length);
        }
    }

    public synchronized String toString() {
        if ( length == 0)
            this.length = nativeGetLength(offset);
	byte[] bytes = new byte[length];
        nativeGet(offset, bytes, 0, length);
	return new String(bytes);
    }

    @Override
    public int hashCode() {
           return this.toString().hashCode();
    }

    public byte[] getBytes() {
        if ( length == 0)
            this.length = nativeGetLength(offset);
	byte[] bytes = new byte[length];
        nativeGet(offset, bytes, 0, length);
	return bytes;
    }

    @Override
    public boolean equals(Object o) {
	return o instanceof PersistentString && ((PersistentString)o).toString().equals(this.toString());
    }

    @Override
    public int compareTo(Object o) {
        if (!(o instanceof PersistentString))
            throw new ClassCastException(o.getClass().getName() + " cannot be cast to PersistentString");
        PersistentString that = (PersistentString)o;

	return this.toString().compareTo(that.toString());

    }

    private synchronized static native boolean nativeCheckByteArrayExists(long offset);
    private synchronized native long nativeReserveByteArrayMemory(int length);
    private synchronized native int nativeGetLength(long offset);
    private synchronized native int nativePut(long offset, byte[] bytes, int arrayoffset, int length);
    private synchronized native int nativeGet(long offset, byte[] bytes, int arrayoffset, int length);

}
