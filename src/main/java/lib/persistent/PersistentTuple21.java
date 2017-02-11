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

public class PersistentTuple21<T1 extends Persistent, T2 extends Persistent, T3 extends Persistent, T4 extends Persistent, T5 extends Persistent, T6 extends Persistent, T7 extends Persistent, T8 extends Persistent, T9 extends Persistent, T10 extends Persistent, T11 extends Persistent, T12 extends Persistent, T13 extends Persistent, T14 extends Persistent, T15 extends Persistent, T16 extends Persistent, T17 extends Persistent, T18 extends Persistent, T19 extends Persistent, T20 extends Persistent, T21 extends Persistent> implements Persistent {

    private long offset;

    static {
        System.loadLibrary("Persistent");
        Util.openPool();
    }

    public PersistentTuple21() {
        this(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    public PersistentTuple21(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8, T9 t9, T10 t10, T11 t11, T12 t12, T13 t13, T14 t14, T15 t15, T16 t16, T17 t17, T18 t18, T19 t19, T20 t20, T21 t21) {
        synchronized (PersistentTuple21.class) {
            Transaction.run(() -> {
                this.offset = Aggregation.nativeAggregate(this.getClass().getName(), 21);
                _1(t1);
                _2(t2);
                _3(t3);
                _4(t4);
                _5(t5);
                _6(t6);
                _7(t7);
                _8(t8);
                _9(t9);
                _10(t10);
                _11(t11);
                _12(t12);
                _13(t13);
                _14(t14);
                _15(t15);
                _16(t16);
                _17(t17);
                _18(t18);
                _19(t19);
                _20(t20);
                _21(t21);
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

    @SuppressWarnings("unchecked") public synchronized T9 _9() {
        return (T9)Aggregation.nativeGetField(this.getOffset(), 8);
    }

    @SuppressWarnings("unchecked") public synchronized T10 _10() {
        return (T10)Aggregation.nativeGetField(this.getOffset(), 9);
    }

    @SuppressWarnings("unchecked") public synchronized T11 _11() {
        return (T11)Aggregation.nativeGetField(this.getOffset(), 10);
    }

    @SuppressWarnings("unchecked") public synchronized T12 _12() {
        return (T12)Aggregation.nativeGetField(this.getOffset(), 11);
    }

    @SuppressWarnings("unchecked") public synchronized T13 _13() {
        return (T13)Aggregation.nativeGetField(this.getOffset(), 12);
    }

    @SuppressWarnings("unchecked") public synchronized T14 _14() {
        return (T14)Aggregation.nativeGetField(this.getOffset(), 13);
    }

    @SuppressWarnings("unchecked") public synchronized T15 _15() {
        return (T15)Aggregation.nativeGetField(this.getOffset(), 14);
    }

    @SuppressWarnings("unchecked") public synchronized T16 _16() {
        return (T16)Aggregation.nativeGetField(this.getOffset(), 15);
    }

    @SuppressWarnings("unchecked") public synchronized T17 _17() {
        return (T17)Aggregation.nativeGetField(this.getOffset(), 16);
    }

    @SuppressWarnings("unchecked") public synchronized T18 _18() {
        return (T18)Aggregation.nativeGetField(this.getOffset(), 17);
    }

    @SuppressWarnings("unchecked") public synchronized T19 _19() {
        return (T19)Aggregation.nativeGetField(this.getOffset(), 18);
    }

    @SuppressWarnings("unchecked") public synchronized T20 _20() {
        return (T20)Aggregation.nativeGetField(this.getOffset(), 19);
    }

    @SuppressWarnings("unchecked") public synchronized T21 _21() {
        return (T21)Aggregation.nativeGetField(this.getOffset(), 20);
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

    public synchronized void _9(T9 t9) {
        Transaction.run(() -> {
            long off = t9 == null ? 0 : t9.getOffset();
            Aggregation.nativeSetField(this.getOffset(), 8, off);
        });
    }

    public synchronized void _10(T10 t10) {
        Transaction.run(() -> {
            long off = t10 == null ? 0 : t10.getOffset();
            Aggregation.nativeSetField(this.getOffset(), 9, off);
        });
    }

    public synchronized void _11(T11 t11) {
        Transaction.run(() -> {
            long off = t11 == null ? 0 : t11.getOffset();
            Aggregation.nativeSetField(this.getOffset(), 10, off);
        });
    }

    public synchronized void _12(T12 t12) {
        Transaction.run(() -> {
            long off = t12 == null ? 0 : t12.getOffset();
            Aggregation.nativeSetField(this.getOffset(), 11, off);
        });
    }

    public synchronized void _13(T13 t13) {
        Transaction.run(() -> {
            long off = t13 == null ? 0 : t13.getOffset();
            Aggregation.nativeSetField(this.getOffset(), 12, off);
        });
    }

    public synchronized void _14(T14 t14) {
        Transaction.run(() -> {
            long off = t14 == null ? 0 : t14.getOffset();
            Aggregation.nativeSetField(this.getOffset(), 13, off);
        });
    }

    public synchronized void _15(T15 t15) {
        Transaction.run(() -> {
            long off = t15 == null ? 0 : t15.getOffset();
            Aggregation.nativeSetField(this.getOffset(), 14, off);
        });
    }

    public synchronized void _16(T16 t16) {
        Transaction.run(() -> {
            long off = t16 == null ? 0 : t16.getOffset();
            Aggregation.nativeSetField(this.getOffset(), 15, off);
        });
    }

    public synchronized void _17(T17 t17) {
        Transaction.run(() -> {
            long off = t17 == null ? 0 : t17.getOffset();
            Aggregation.nativeSetField(this.getOffset(), 16, off);
        });
    }

    public synchronized void _18(T18 t18) {
        Transaction.run(() -> {
            long off = t18 == null ? 0 : t18.getOffset();
            Aggregation.nativeSetField(this.getOffset(), 17, off);
        });
    }

    public synchronized void _19(T19 t19) {
        Transaction.run(() -> {
            long off = t19 == null ? 0 : t19.getOffset();
            Aggregation.nativeSetField(this.getOffset(), 18, off);
        });
    }

    public synchronized void _20(T20 t20) {
        Transaction.run(() -> {
            long off = t20 == null ? 0 : t20.getOffset();
            Aggregation.nativeSetField(this.getOffset(), 19, off);
        });
    }

    public synchronized void _21(T21 t21) {
        Transaction.run(() -> {
            long off = t21 == null ? 0 : t21.getOffset();
            Aggregation.nativeSetField(this.getOffset(), 20, off);
        });
    }

    public synchronized long getOffset() {
        return offset;
    }

    public synchronized int hashCode() {
        return _1().hashCode() + _2().hashCode() + _3().hashCode() + _4().hashCode() + _5().hashCode() + _6().hashCode() + _7().hashCode() + _8().hashCode() + _9().hashCode() + _10().hashCode() + _11().hashCode() + _12().hashCode() + _13().hashCode() + _14().hashCode() + _15().hashCode() + _16().hashCode() + _17().hashCode() + _18().hashCode() + _19().hashCode() + _20().hashCode() + _21().hashCode();
    }

    public synchronized boolean equals(Object obj) {
        if (!(obj instanceof PersistentTuple21)) return false;

        PersistentTuple21 that = (PersistentTuple21)obj;
        if (!(this._1().equals(that._1())) ||
            !(this._2().equals(that._2())) ||
            !(this._3().equals(that._3())) ||
            !(this._4().equals(that._4())) ||
            !(this._5().equals(that._5())) ||
            !(this._6().equals(that._6())) ||
            !(this._7().equals(that._7())) ||
            !(this._8().equals(that._8())) ||
            !(this._9().equals(that._9())) ||
            !(this._10().equals(that._10())) ||
            !(this._11().equals(that._11())) ||
            !(this._12().equals(that._12())) ||
            !(this._13().equals(that._13())) ||
            !(this._14().equals(that._14())) ||
            !(this._15().equals(that._15())) ||
            !(this._16().equals(that._16())) ||
            !(this._17().equals(that._17())) ||
            !(this._18().equals(that._18())) ||
            !(this._19().equals(that._19())) ||
            !(this._20().equals(that._20())) ||
            !(this._21().equals(that._21())))
            return false;

        return true;
    }

    public synchronized String toString() {
        return "{" + _1().toString() + ", " + _2().toString() + ", " + _3().toString() + ", " + _4().toString() + ", " + _5().toString() + ", " + _6().toString() + ", " + _7().toString() + ", " + _8().toString() + ", " + _9().toString() + ", " + _10().toString() + ", " + _11().toString() + ", " + _12().toString() + ", " + _13().toString() + ", " + _14().toString() + ", " + _15().toString() + ", " + _16().toString() + ", " + _17().toString() + ", " + _18().toString() + ", " + _19().toString() + ", " + _20().toString() + ", " + _21().toString() + "}";
    }
}