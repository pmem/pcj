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
import lib.util.persistent.spi.PersistentMemoryProvider;
import lib.xpersistent.XHeap;
import static lib.util.persistent.Trace.*;
import lib.util.persistent.types.ObjectType;
import java.util.function.Supplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.BooleanSupplier;

public class Util {

    private static final XHeap heap = (XHeap)PersistentMemoryProvider.getDefaultProvider().getHeap();
    private static final Random random = new Random(System.nanoTime());

    public static PersistentByte persistent(byte x) {return new PersistentByte(x);}
    public static PersistentShort persistent(short x) {return new PersistentShort(x);}
    public static PersistentInteger persistent(int x) {return new PersistentInteger(x);}
    public static PersistentLong persistent(long x) {return new PersistentLong(x);}
    public static PersistentFloat persistent(float x) {return new PersistentFloat(x);}
    public static PersistentDouble persistent(double x) {return new PersistentDouble(x);}
    public static PersistentCharacter persistent(char x) {return new PersistentCharacter(x);}
    public static PersistentBoolean persistent(boolean x) {return new PersistentBoolean(x);}
    public static PersistentString persistent(String x) {return new PersistentString(x);}

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

    public static void memCopyPP(MemoryRegion src, long srcOffset, MemoryRegion dst, long dstOffset, long size) {
        // trace(true, "memCopyPP src = %s, srcOffset = %d, dst = %s, dstOffset = %d, size = %d", src, srcOffset, dst, dstOffset, size);
        heap.memcpy(src, srcOffset, dst, dstOffset, size);
    }

    public static void memCopyVV(MemoryRegion src, long srcOffset, MemoryRegion dst, long dstOffset, long size) {
        // System.out.println("src size = " + ((VolatileMemoryRegion)src).size);
        // System.out.println("dst size = " + ((VolatileMemoryRegion)dst).size);
        // trace(true, "memCopyVV src = %d, srcOffset = %d, dst = %d, dstOffset = %d, size = %d", src.addr(), srcOffset, dst.addr(), dstOffset, size);
        for (long i = 0; i < size; i++) dst.putByte(dstOffset + i, src.getByte(srcOffset + i));

    }
    
    public static void memCopyVP(MemoryRegion src, long srcOffset, MemoryRegion dst, long dstOffset, long size) {
        // trace(true, "memCopyVP src = %s, srcOffset = %d, dst = %s, dstOffset = %d, size = %d", src, srcOffset, dst, dstOffset, size);
        Transaction.run(() -> {
            for (long i = 0; i < size; i++) dst.putByte(dstOffset + i, src.getByte(srcOffset + i));
        });
    }
    
    public static void memCopyPV(MemoryRegion src, long srcOffset, MemoryRegion dst, long dstOffset, long size) {
        // trace(true, "memCopyPV src = %s, srcOffset = %d, dst = %s, dstOffset = %d, size = %d", src, srcOffset, dst, dstOffset, size);
        for (long i = 0; i < size; i++) dst.putByte(dstOffset + i, src.getByte(srcOffset + i));
    }

    public static void memCopy(ObjectType fromType, ObjectType toType, MemoryRegion src, long srcOffset, MemoryRegion dst, long dstOffset, long size) {
        // trace(true, "memCopy fromType = %s (IVB = %s), toType = %s (IVB = %s)", fromType, fromType.isValueBased(), toType, toType.isValueBased());
        if (fromType.isValueBased()) {
            if (toType.isValueBased()) memCopyVV(src, srcOffset, dst, dstOffset, size);
            else memCopyVP(src, srcOffset, dst, dstOffset, size);
        }
        else {
            if (toType.isValueBased()) memCopyPV(src, srcOffset, dst, dstOffset, size);
            else memCopyPP(src, srcOffset, dst, dstOffset, size);
        }
    }

    public static float time(Runnable func) {
        long start = System.nanoTime();
        func.run();
        return (float)(System.nanoTime() - start) / 1e9f;
    }

    @FunctionalInterface
    public interface ByteSupplier {byte getAsByte();}

    @FunctionalInterface
    public interface ShortSupplier {short getAsShort();}

    public static <T> T synchronizedBlock(AnyPersistent obj, Supplier<T> func) {
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

    public static byte synchronizedBlock(AnyPersistent obj, ByteSupplier func) {
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

    public static short synchronizedBlock(AnyPersistent obj, ShortSupplier func) {
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

    public static int synchronizedBlock(AnyPersistent obj, IntSupplier func) {
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

    public static long synchronizedBlock(AnyPersistent obj, LongSupplier func) {
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

    public static boolean synchronizedBlock(AnyPersistent obj, BooleanSupplier func) {
        // System.out.println("BooleanSupplier");
        boolean ans;
        TransactionInfo info = lib.xpersistent.XTransaction.tlInfo.get();
        boolean inTransaction = info.state == Transaction.State.Active;
        if (!inTransaction) {
            obj.monitorEnter();
            try {ans = func.getAsBoolean();}
            finally {obj.monitorExit();}
        }
        else {
            boolean success = obj.monitorEnterTimeout();
            if (success) {
                info.transaction.addLockedObject(obj);
                ans = func.getAsBoolean();
            }
            else {
                if (inTransaction) throw new TransactionRetryException();
                else throw new RuntimeException("failed to acquire lock (timeout)");
            }
        }
        return ans;
    }

    public static void synchronizedBlock(AnyPersistent obj, Runnable func) {
        TransactionInfo info = lib.xpersistent.XTransaction.tlInfo.get();
        boolean inTransaction = info.state == Transaction.State.Active;
        if (!inTransaction) {
            obj.monitorEnter();
            try {func.run();}
            finally {obj.monitorExit();}
        }
        else {
            boolean success = obj.monitorEnterTimeout();
            if (success) {
                info.transaction.addLockedObject(obj);
                func.run();
            }
            else {
                if (inTransaction) throw new TransactionRetryException();
                else throw new RuntimeException("failed to acquire lock (timeout)");
            }
        }
    }
}
