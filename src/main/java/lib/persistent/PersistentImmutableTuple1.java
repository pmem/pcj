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

package lib.persistent;

public class PersistentImmutableTuple1<T1 extends Persistent> implements Persistent {

    private long offset;

    static {
        System.loadLibrary("Persistent");
        Util.openPool();
    }

    public PersistentImmutableTuple1() {
        this(null);
    }

    public PersistentImmutableTuple1(T1 t1) {
        synchronized (PersistentImmutableTuple1.class) {
            Transaction.run(() -> {
                this.offset = Aggregation.nativeAggregate(this.getClass().getName(), 1);
                _1(t1);
                ObjectDirectory.registerObject(this);
            });
        }
    }

    @SuppressWarnings("unchecked") public T1 _1() {
        return (T1)Aggregation.nativeGetField(this.getOffset(), 0);
    }

    private synchronized void _1(T1 t1) {
        Transaction.run(() -> {
            long off = t1 == null ? 0 : t1.getOffset();
            Aggregation.nativeSetField(this.getOffset(), 0, off);
        });
    }

    public synchronized long getOffset() {
        return offset;
    }

    public synchronized int hashCode() {
        return _1().hashCode();
    }

    public synchronized boolean equals(Object obj) {
        if (!(obj instanceof PersistentImmutableTuple1)) return false;

        PersistentImmutableTuple1 that = (PersistentImmutableTuple1)obj;
        if (!(this._1().equals(that._1())))
            return false;

        return true;
    }

    public synchronized String toString() {
        return "{" + _1().toString() + "}";
    }
}
