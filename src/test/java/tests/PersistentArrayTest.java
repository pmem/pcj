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

package tests;

import lib.util.persistent.*;

public class PersistentArrayTest {

    static boolean verbose = false;
    public static void main(String[] args) {
        verbose = true;
        run();
    }

    public static boolean run() {
        System.out.println("****************PersistentArray Tests******************");
        return intArrayTest() &&
               byteArrayTest() &&
               shortArrayTest() &&
               longArrayTest() &&
               booleanArrayTest() &&
               charArrayTest() &&
               floatArrayTest() &&
               doubleArrayTest() &&
               stringArrayTest() &&
               toArrayTest();
    }

    public static boolean intArrayTest() {
        if (verbose) System.out.println("****************IntArray Tests*************************");
        PersistentIntArray pa = new PersistentIntArray(10);
        assert(pa.length() == 10);
        for (int i = 0; i < 10; i++) {
            pa.set(i, i);
        }
        for (int i = 0; i < 10; i++) {
            assert(pa.get(i) == i);
        }
        boolean caught = false;
        try {
            pa.set(10, 10);
        } catch (IndexOutOfBoundsException exception) {
            caught = true;
        }
        assert(caught);
        return true;
    }

    public static boolean byteArrayTest() {
        if (verbose) System.out.println("****************ByteArray Tests************************");
        PersistentByteArray pa = new PersistentByteArray(10);
        assert(pa.length() == 10);
        for (byte i = 0; i < 10; i++) {
            pa.set(i, i);
        }
        for (byte i = 0; i < 10; i++) {
            assert(pa.get(i) == i);
        }
        boolean caught = false;
        try {
            pa.set(10, (byte)10);
        } catch (IndexOutOfBoundsException exception) {
            caught = true;
        }
        assert(caught);
        return true;
    }

    public static boolean shortArrayTest() {
        if (verbose) System.out.println("****************ShortArray Tests***********************");
        PersistentShortArray pa = new PersistentShortArray(10);
        assert(pa.length() == 10);
        for (short i = 0; i < 10; i++) {
            pa.set(i, i);
        }
        for (short i = 0; i < 10; i++) {
            assert(pa.get(i) == i);
        }
        boolean caught = false;
        try {
            pa.set(10, (short)10);
        } catch (IndexOutOfBoundsException exception) {
            caught = true;
        }
        assert(caught);
        return true;
    }

    public static boolean longArrayTest() {
        if (verbose) System.out.println("****************LongArray Tests************************");
        PersistentLongArray pa = new PersistentLongArray(10);
        assert(pa.length() == 10);
        for (long i = 0; i < 10L; i++) {
            pa.set((int)i, i);
        }
        for (long i = 0; i < 10L; i++) {
            assert(pa.get((int)i) == i);
        }
        boolean caught = false;
        try {
            pa.set(10, 10L);
        } catch (IndexOutOfBoundsException exception) {
            caught = true;
        }
        assert(caught);
        return true;
    }

    public static boolean booleanArrayTest() {
        if (verbose) System.out.println("****************BooleanArray Tests*********************");
        PersistentBooleanArray pa = new PersistentBooleanArray(10);
        assert(pa.length() == 10);
        for (int i = 0; i < 10; i++) {
            pa.set(i, ((i % 2) == 0));
        }
        for (int i = 0; i < 10; i++) {
            assert(pa.get(i) == ((i % 2) == 0));
        }
        boolean caught = false;
        try {
            pa.set(10, true);
        } catch (IndexOutOfBoundsException exception) {
            caught = true;
        }
        assert(caught);
        return true;
    }

    public static boolean charArrayTest() {
        if (verbose) System.out.println("****************CharArray Tests************************");
        PersistentCharArray pa = new PersistentCharArray(10);
        assert(pa.length() == 10);
        for (char i = 0; i < 10; i++) {
            pa.set(i, i);
        }
        for (char i = 0; i < 10; i++) {
            assert(pa.get(i) == i);
        }
        boolean caught = false;
        try {
            pa.set(10, (char)10);
        } catch (IndexOutOfBoundsException exception) {
            caught = true;
        }
        assert(caught);
        return true;
    }

    public static boolean floatArrayTest() {
        if (verbose) System.out.println("****************FloatArray Tests***********************");
        PersistentFloatArray pa = new PersistentFloatArray(10);
        assert(pa.length() == 10);
        for (int i = 0; i < 10; i++) {
            pa.set(i, (float)i + 0.5f);
        }
        for (int i = 0; i < 10; i++) {
            assert(((Float)(pa.get(i))).equals((float)i + 0.5f));
        }
        boolean caught = false;
        try {
            pa.set(10, 10f);
        } catch (IndexOutOfBoundsException exception) {
            caught = true;
        }
        assert(caught);
        return true;
    }

    public static boolean doubleArrayTest() {
        if (verbose) System.out.println("****************DoubleArray Tests**********************");
        PersistentDoubleArray pa = new PersistentDoubleArray(10);
        assert(pa.length() == 10);
        for (int i = 0; i < 10; i++) {
            pa.set(i, (double)i + 0.5);
        }
        for (int i = 0; i < 10; i++) {
            assert(((Double)(pa.get(i))).equals((double)i + 0.5));
        }
        boolean caught = false;
        try {
            pa.set(10, (short)10);
        } catch (IndexOutOfBoundsException exception) {
            caught = true;
        }
        assert(caught);
        return true;
    }

    public static boolean stringArrayTest() {
        if (verbose) System.out.println("****************StringArray Tests**********************");
        PersistentArray<PersistentString> pa = new PersistentArray<>(10);
        PersistentString[] pss = new PersistentString[10];
        assert(pa.length() == 10);
        for (int i = 0; i < 10; i++) {
            pss[i] = new PersistentString("hello" + i);
            pa.set(i, pss[i]);
        }
        for (int i = 0; i < 10; i++) {
            assert(pa.get(i) == pss[i]);
            assert(pa.get(i).equals(new PersistentString("hello"+i)));
        }
        return true;
    }

    public static boolean toArrayTest() {
        if (verbose) System.out.println("****************ToArray Tests**************************");
        PersistentArray<PersistentString> pa = new PersistentArray<>(10);
        PersistentString[] pss = new PersistentString[5];
        for (int i = 0; i < 10; i++) {
            pa.set(i, new PersistentString("hello"+i));
        }
        pss = pa.toArray(pss);
        assert(pss.length == 10);
        for (int i = 0; i < 10; i++) {
            assert(pss[i] == pa.get(i));
            assert(pss[i].equals(new PersistentString("hello"+i)));
        }
        return true;
    }
}
