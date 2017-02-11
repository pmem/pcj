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

import java.lang.Long;
import java.lang.IllegalArgumentException;
import java.lang.IndexOutOfBoundsException;
import java.lang.UnsupportedOperationException;

public class PersistentLong implements Comparable, Persistent {

    private long offset;

    static {
        System.loadLibrary("Persistent");
        Util.openPool();
    }

    public synchronized long getOffset() {
        return this.offset;
    }

    public PersistentLong(long val) {
        synchronized (PersistentLong.class) {
            this.offset = nativeSetLong(val);
            ObjectDirectory.registerObject(this);
        }
    }

    public synchronized String toString() {
	    return Long.toString(this.toLong());
    }

    public synchronized long toLong() {
        return nativeGetLong(offset);
    }


    @Override
    public int compareTo(Object o) {
        if (!(o instanceof PersistentLong))
            throw new ClassCastException(o.getClass().getName() + " cannot be cast to PersistentLong");

        PersistentLong l = (PersistentLong)o;

        long a = this.toLong();
        long other = l.toLong();
        return a < other ? -1 : a == other ? 0 : 1;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(this.toLong());
    }

    private synchronized native long nativeSetLong(long val);
    private synchronized native long nativeGetLong(long val);

}
