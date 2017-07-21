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

import java.util.Random;
import java.util.function.Supplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;


public class Util {

    private static Random random = new Random(System.nanoTime());

    public static int randomInt(int max) {return random.nextInt(max);}

    public static void sleep(long ms) {
        try {Thread.sleep(ms);} catch (Exception e) {e.printStackTrace();}
    }

    public static void join(Thread[] ts) {
        try {
            for (int i = 0; i < ts.length; i++) ts[i].join();
        } 
        catch (InterruptedException ie) {ie.printStackTrace();}
    }

    @FunctionalInterface
    public interface ByteSupplier {byte getAsByte();}

    @FunctionalInterface
    public interface ShortSupplier {short getAsShort();}

    public static <T> T synchronizedBlock(PersistentObject obj, Supplier<T> func) { 
        // System.out.println("Supplier");
        T ans;
        TransactionInfo info = lib.xpersistent.XTransaction.tlInfo.get();
        boolean inTransaction = info.state == Transaction.State.Active;
        if (!inTransaction) {
            obj.monitorEnter();
            try {ans = func.get();}
            finally {obj.monitorExit();}
        }
        else {
            boolean success = obj.monitorEnterTimeout();
            if (success) {
                info.transaction.addLockedObject(obj);
                ans = func.get();
            }
            else {
                if (inTransaction) throw new TransactionRetryException();
                else throw new RuntimeException("failed to acquire lock (timeout)");
            }
        }
        return ans;
    }

    public static byte synchronizedBlock(PersistentObject obj, ByteSupplier func) { 
        // System.out.println("ByteSupplier");
        byte ans;
        TransactionInfo info = lib.xpersistent.XTransaction.tlInfo.get();
        boolean inTransaction = info.state == Transaction.State.Active;
        if (!inTransaction) {
            obj.monitorEnter();
            try {ans = func.getAsByte();}
            finally {obj.monitorExit();}
        }
        else {
            boolean success = obj.monitorEnterTimeout();
            if (success) {
                info.transaction.addLockedObject(obj);
                ans = func.getAsByte();
            }
            else {
                if (inTransaction) throw new TransactionRetryException();
                else throw new RuntimeException("failed to acquire lock (timeout)");
            }
        }
        return ans;
    }

    public static short synchronizedBlock(PersistentObject obj, ShortSupplier func) { 
        // System.out.println("ShortSupplier");
        short ans;
        TransactionInfo info = lib.xpersistent.XTransaction.tlInfo.get();
        boolean inTransaction = info.state == Transaction.State.Active;
        if (!inTransaction) {
            obj.monitorEnter();
            try {ans = func.getAsShort();}
            finally {obj.monitorExit();}
        }
        else {
            boolean success = obj.monitorEnterTimeout();
            if (success) {
                info.transaction.addLockedObject(obj);
                ans = func.getAsShort();
            }
            else {
                if (inTransaction) throw new TransactionRetryException();
                else throw new RuntimeException("failed to acquire lock (timeout)");
            }
        }
        return ans;
    }

    public static int synchronizedBlock(PersistentObject obj, IntSupplier func) { 
        // System.out.println("IntSupplier");
        int ans;
        TransactionInfo info = lib.xpersistent.XTransaction.tlInfo.get();
        boolean inTransaction = info.state == Transaction.State.Active;
        if (!inTransaction) {
            obj.monitorEnter();
            try {ans = func.getAsInt();}
            finally {obj.monitorExit();}
        }
        else {
            boolean success = obj.monitorEnterTimeout();
            if (success) {
                info.transaction.addLockedObject(obj);
                ans = func.getAsInt();
            }
            else {
                if (inTransaction) throw new TransactionRetryException();
                else throw new RuntimeException("failed to acquire lock (timeout)");
            }
        }
        return ans;
    }

    public static long synchronizedBlock(PersistentObject obj, LongSupplier func) { 
        // System.out.println("LongSupplier");
        long ans;
        TransactionInfo info = lib.xpersistent.XTransaction.tlInfo.get();
        boolean inTransaction = info.state == Transaction.State.Active;
        if (!inTransaction) {
            obj.monitorEnter();
            try {ans = func.getAsLong();}
            finally {obj.monitorExit();}
        }
        else {
            boolean success = obj.monitorEnterTimeout();
            if (success) {
                info.transaction.addLockedObject(obj);
                ans = func.getAsLong();
            }
            else {
                if (inTransaction) throw new TransactionRetryException();
                else throw new RuntimeException("failed to acquire lock (timeout)");
            }
        }
        return ans;
    }
}