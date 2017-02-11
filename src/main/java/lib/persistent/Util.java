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

public class Util {

    static {
        System.loadLibrary("Persistent");
        nativeOpenPool();
    }

    public synchronized static void debugPool() {
        debugPool(0);
    }

    public synchronized static void debugPool(int verbosity) {
        nativeDebugPool(verbosity);
    }

    synchronized static long getRoot() {
        return nativeGetRoot();
    }

    synchronized static void registerOffset(long offset) {
        nativeRegisterOffset(offset);
    }

    synchronized static void deregisterOffset(long offset) {
        nativeDeregisterOffset(offset);
    }

    static void incRef(long offset) {
        nativeIncRef(offset);
    }

    static void decRef(long offset) {
        nativeDecRef(offset);
    }

    public static void openPool() {
        nativeOpenPool();
    }

    public static void closePool() {
        nativeClosePool();
    }

    private synchronized static native void nativeOpenPool();
    private synchronized static native void nativeClosePool();
    private synchronized static native long nativeGetRoot();
    private synchronized static native void nativeDebugPool(int verbosity);
    private synchronized static native void nativeRegisterOffset(long offset);
    private synchronized static native void nativeDeregisterOffset(long offset);
    private synchronized static native void nativeDecRef(long offset);
    private synchronized static native void nativeIncRef(long offset);
}
