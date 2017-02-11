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

public class PersistentTuple8<T1 extends Persistent, T2 extends Persistent, T3 extends Persistent, T4 extends Persistent, T5 extends Persistent, T6 extends Persistent, T7 extends Persistent, T8 extends Persistent> implements Persistent {

    private long offset;

    static {
        System.loadLibrary("Persistent");
        Util.openPool();
    }

    public PersistentTuple8() {
        this(null, null, null, null, null, null, null, null);
    }

    public PersistentTuple8(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8) {
        synchronized (PersistentTuple8.class) {
            Transaction.run(() -> {
                this.offset = Aggregation.nativeAggregate(this.getClass().getName(), 8);
                _1(t1);
                _2(t2);
                _3(t3);
                _4(t4);
                _5(t5);
                _6(t6);
                _7(t7);
                _8(t8);
                ObjectDirectory.registerObject(this);
            });
        }
    }

    @SuppressWarnings("unchecked") public synchronized T1 _1() {
        return (T1)Aggregation.nativeGetField(this.getOffset(), 0);
    }

    @SuppressWarnings("unchecked") public synchronized T2 _2() {
        return (T2)Aggregation.nativeGetField(this.getOffset(), 1);
    }

    @SuppressWarnings("unchecked") public synchronized T3 _3() {
        return (T3)Aggregation.nativeGetField(this.getOffset(), 2);
    }

    @SuppressWarnings("unchecked") public synchronized T4 _4() {
        return (T4)Aggregation.nativeGetField(this.getOffset(), 3);
    }

    @SuppressWarnings("unchecked") public synchronized T5 _5() {
        return (T5)Aggregation.nativeGetField(this.getOffset(), 4);
    }

    @SuppressWarnings("unchecked") public synchronized T6 _6() {
        return (T6)Aggregation.nativeGetField(this.getOffset(), 5);
    }

    @SuppressWarnings("unchecked") public synchronized T7 _7() {
        return (T7)Aggregation.nativeGetField(this.getOffset(), 6);
    }

    @SuppressWarnings("unchecked") public synchronized T8 _8() {
        return (T8)Aggregation.nativeGetField(this.getOffset(), 7);
    }

    public synchronized void _1(T1 t1) {
        Transaction.run(() -> {
            long off = t1 == null ? 0 : t1.getOffset();
            Aggregation.nativeSetField(this.getOffset(), 0, off);
        });
    }

    public synchronized void _2(T2 t2) {
        Transaction.run(() -> {
            long off = t2 == null ? 0 : t2.getOffset();
            Aggregation.nativeSetField(this.getOffset(), 1, off);
        });
    }

    public synchronized void _3(T3 t3) {
        Transaction.run(() -> {
            long off = t3 == null ? 0 : t3.getOffset();
            Aggregation.nativeSetField(this.getOffset(), 2, off);
        });
    }

    public synchronized void _4(T4 t4) {
        Transaction.run(() -> {
            long off = t4 == null ? 0 : t4.getOffset();
            Aggregation.nativeSetField(this.getOffset(), 3, off);
        });
    }

    public synchronized void _5(T5 t5) {
        Transaction.run(() -> {
            long off = t5 == null ? 0 : t5.getOffset();
            Aggregation.nativeSetField(this.getOffset(), 4, off);
        });
    }

    public synchronized void _6(T6 t6) {
        Transaction.run(() -> {
            long off = t6 == null ? 0 : t6.getOffset();
            Aggregation.nativeSetField(this.getOffset(), 5, off);
        });
    }

    public synchronized void _7(T7 t7) {
        Transaction.run(() -> {
            long off = t7 == null ? 0 : t7.getOffset();
            Aggregation.nativeSetField(this.getOffset(), 6, off);
        });
    }

    public synchronized void _8(T8 t8) {
        Transaction.run(() -> {
            long off = t8 == null ? 0 : t8.getOffset();
            Aggregation.nativeSetField(this.getOffset(), 7, off);
        });
    }

    public synchronized long getOffset() {
        return offset;
    }

    public synchronized int hashCode() {
        return _1().hashCode() + _2().hashCode() + _3().hashCode() + _4().hashCode() + _5().hashCode() + _6().hashCode() + _7().hashCode() + _8().hashCode();
    }

    public synchronized boolean equals(Object obj) {
        if (!(obj instanceof PersistentTuple8)) return false;

        PersistentTuple8 that = (PersistentTuple8)obj;
        if (!(this._1().equals(that._1())) ||
            !(this._2().equals(that._2())) ||
            !(this._3().equals(that._3())) ||
            !(this._4().equals(that._4())) ||
            !(this._5().equals(that._5())) ||
            !(this._6().equals(that._6())) ||
            !(this._7().equals(that._7())) ||
            !(this._8().equals(that._8())))
            return false;

        return true;
    }

    public synchronized String toString() {
        return "{" + _1().toString() + ", " + _2().toString() + ", " + _3().toString() + ", " + _4().toString() + ", " + _5().toString() + ", " + _6().toString() + ", " + _7().toString() + ", " + _8().toString() + "}";
    }
}