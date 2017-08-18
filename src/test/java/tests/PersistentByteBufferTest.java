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

package tests;

import lib.util.persistent.*;
import java.util.Arrays;
import java.nio.BufferUnderflowException;
import java.nio.BufferOverflowException;
import java.nio.InvalidMarkException;
import java.nio.charset.Charset;
import java.nio.ByteBuffer;

public class PersistentByteBufferTest {

    static boolean verbose = false;
    public static void main(String[] args) {
        verbose = true;
        run();
    }

    public static boolean run() {
        System.out.println("****************PersistentByteBuffer Tests*************");
        return testBufferPosition() &&
               testBufferLimit() &&
               testBufferClear() &&
               testBufferDuplicate() &&
               testBufferSlice() &&
               testBufferMark() &&
               testBufferBasicPutsGets() &&
               testBufferChar() &&
               testBufferDouble() &&
               testBufferFloat() &&
               testBufferInt() &&
               testBufferShort() &&
               testBufferLong() &&
               testBufferShort() &&
               testBufferLong() &&
               testBufferFlip() &&
               testBufferComparison() &&
               testEmptyBuffer() &&
               testBufferWrap() &&
               testBufferPutBuffer();
    }

    public static boolean testBufferPosition() {
        if (verbose) System.out.println("****************Testing buffer position****************");

        PersistentByteBuffer buf = PersistentByteBuffer.allocate(10);
        buf.put("helloworld".getBytes());
        buf.rewind();

        buf.position(5);
        assert(buf.position() == 5);

        PersistentByteBuffer buf2 = (PersistentByteBuffer)ObjectCache.get(buf.getPointer().addr());
        assert(buf2.position() == 5);
        return true;
    }

    public static boolean testBufferLimit() {
        if (verbose) System.out.println("****************Testing buffer limit****************");

        PersistentByteBuffer buf = PersistentByteBuffer.allocate(10);
        buf.put("helloworld".getBytes());
        buf.rewind();

        buf.limit(5);
        assert(buf.limit() == 5);

        PersistentByteBuffer buf2 = (PersistentByteBuffer)ObjectCache.get(buf.getPointer().addr());
        assert(buf2.limit() == 5);
        return true;
    }

    public static boolean testBufferClear() {
        if (verbose) System.out.println("****************Testing buffer clear****************");

        PersistentByteBuffer buf = PersistentByteBuffer.allocate(15);
        buf.put("hellojavaworld!".getBytes());
        buf.rewind();

        buf.limit(9).position(5);
        assert(buf.limit() == 9);
        assert(buf.position() == 5);

        byte[] bytes = new byte[4];
        buf.get(bytes);
        assert(Arrays.equals(bytes, "java".getBytes()));
        assert(buf.position() == 9);

        assert(buf.hasRemaining() == false);
        assert(buf.remaining() == 0);

        boolean caught = false;
        try {
            buf.get();
        } catch (BufferUnderflowException e) {
            caught = true;
        }
        assert(caught);

        buf.position(5);

        PersistentByteBuffer buf2 = (PersistentByteBuffer)ObjectCache.get(buf.getPointer().addr());

        assert(buf2.limit() == 9);
        assert(buf2.position() == 5);
        assert(buf2.hasRemaining());

        buf2.clear();
        assert(buf.limit() == 15);
        assert(buf2.limit() == 15);
        assert(buf.position() == 0);
        assert(buf2.position() == 0);

        PersistentByteBuffer buf3 = (PersistentByteBuffer)ObjectCache.get(buf2.getPointer().addr());

        assert(buf3.limit() == 15);
        assert(buf3.position() == 0);

        assert(shouldEqual(buf3, "hellojavaworld!"));
        return true;
    }

    public static boolean testBufferDuplicate() {
        if (verbose) System.out.println("****************Testing buffer duplicate****************");

        PersistentByteBuffer buf = PersistentByteBuffer.allocate(15);
        buf.put("hellojavaworld!".getBytes());

        PersistentByteBuffer buf2 = buf.duplicate();

        assert(buf2.position() == 15);
        assert(buf2.limit() == 15);
        assert(buf2.capacity() == 15);
        assert(buf2.position() == buf.position());
        assert(buf2.limit() == buf.limit());
        assert(buf2.capacity() == buf.capacity());
        assert(buf2.equals(buf));

        buf.position(5).limit(9);
        assert(buf2.position() == 15);
        assert(buf2.limit() == 15);

        buf2.position(5).limit(9);
        buf.clear();
        assert(buf2.position() == 5);
        assert(buf2.limit() == 9);
        assert(buf.position() == 0);
        assert(buf.limit() == 15);
        return true;
    }

    public static boolean testBufferSlice() {
        if (verbose) System.out.println("****************Testing buffer slice****************");

        PersistentByteBuffer buf = PersistentByteBuffer.allocate(15);
        buf.put("hellojavaworld!".getBytes());

        buf.position(5).limit(9);

        PersistentByteBuffer buf2 = buf.slice();
        assert(buf2.position() == 0);
        assert(buf2.limit() == 4);
        assert(buf2.capacity() == 4);

        byte[] bytes = new byte[4];
        buf2.get(bytes);
        assert(Arrays.equals(bytes, "java".getBytes()));
        return true;
    }

    public static boolean testBufferMark() {
        if (verbose) System.out.println("****************Testing buffer mark****************");

        PersistentByteBuffer buf = PersistentByteBuffer.allocate(15);
        buf.put("hellojavaworld!".getBytes());

        buf.position(5).limit(9);
        buf.mark();

        byte[] res = new byte[4];
        buf.get(res);
        assert(buf.position() == 9);

        buf.reset();
        assert(buf.position() == 5);

        buf.position(6);
        assert(buf.position() == 6);

        buf.reset();
        assert(buf.position() == 5);

        PersistentByteBuffer buf2 = (PersistentByteBuffer)ObjectCache.get(buf.getPointer().addr());

        buf2.position(8);
        assert(buf2.position() == 8);

        buf2.reset();
        assert(buf2.position() == 5);
        return true;
    }

    public static boolean testBufferBasicPutsGets() {
        if (verbose) System.out.println("****************Testing buffer basic puts/gets****************");

        PersistentByteBuffer buf = PersistentByteBuffer.allocate(15);
        PersistentByteBuffer temp = PersistentByteBuffer.allocate(4);

        buf.put("hellojavaworld!".getBytes());
        buf.rewind();
        assert(buf.get() == "h".getBytes()[0]);
        assert(buf.get() == "e".getBytes()[0]);
        assert(buf.get() == "l".getBytes()[0]);
        assert(buf.get() == "l".getBytes()[0]);
        assert(buf.get() == "o".getBytes()[0]);
        assert(buf.position() == 5);
        assert(buf.remaining() == 10);
        assert(shouldEqual(buf, "hellojavaworld!"));

        buf.rewind();
        byte[] bytes = new byte[15];
        buf.get(bytes);
        assert(Arrays.equals("hellojavaworld!".getBytes(), bytes));
        assert(buf.position() == 15);
        assert(buf.remaining() == 0);

        boolean caught = false;
        try {
            buf.get();
        } catch (BufferUnderflowException e) {
            caught = true;
        }
        assert(caught);

        buf.rewind();
        buf.put(5, ("h".getBytes())[0]);
        assert(buf.position() == 0);
        assert(buf.get(5) == "h".getBytes()[0]);
        assert(shouldEqual(buf, "hellohavaworld!"));

        buf.position(5);
        temp.put("ruby".getBytes());
        temp.rewind();
        buf.put(temp);
        assert(shouldEqual(buf, "hellorubyworld!"));

        assert(buf.position() == 9);
        buf.mark();
        buf.put("t".getBytes()[0]);
        assert(buf.position() == 10);
        buf.reset();
        assert(buf.get() == "t".getBytes()[0]);

        buf.mark();
        buf.put("abcdefg".getBytes(), 2, 5);
        buf.reset();
        buf.get(bytes, 9, 5);
        assert(Arrays.equals("hellojavacdefg!".getBytes(), bytes));
        return true;
    }

    public static boolean testBufferChar() {
        if (verbose) System.out.println("****************Testing buffer putChar/getChar****************");

        PersistentByteBuffer buf = PersistentByteBuffer.allocate(15);

        buf.putChar('5');
        buf.rewind();
        assert(buf.getChar() == '5');

        buf.putChar(5, '6');
        assert(buf.position() == 2);
        assert(buf.getChar(5) == '6');
        assert(buf.position() == 2);
        return true;
    }

    public static boolean testBufferDouble() {
        if (verbose) System.out.println("****************Testing buffer putDouble/getDouble****************");

        PersistentByteBuffer buf = PersistentByteBuffer.allocate(15);

        buf.putDouble(3.1415926);
        buf.rewind();
        assert(new Double(3.1415926).equals(buf.getDouble()));

        buf.putDouble(4, 1.23456789);
        assert(buf.position() == 8);
        assert(new Double(1.23456789).equals(buf.getDouble(4)));
        assert(buf.position() == 8);
        return true;
    }

    public static boolean testBufferFloat() {
        if (verbose) System.out.println("****************Testing buffer putFloat/getFloat****************");

        PersistentByteBuffer buf = PersistentByteBuffer.allocate(15);

        buf.putFloat((float)3.1415);
        buf.rewind();
        assert(new Float(3.1415).equals(buf.getFloat()));

        buf.putFloat(4, (float)1.2345);
        assert(buf.position() == 4);
        assert(new Float(1.2345).equals(buf.getFloat(4)));
        assert(buf.position() == 4);
        return true;
    }

    public static boolean testBufferInt() {
        if (verbose) System.out.println("****************Testing buffer putInt/getInt****************");

        PersistentByteBuffer buf = PersistentByteBuffer.allocate(15);

        buf.putInt(2016);
        buf.rewind();
        assert(buf.getInt() == 2016);

        buf.putInt(8, 4032);
        assert(buf.position() == 4);
        assert(buf.getInt(8) == 4032);
        assert(buf.position() == 4);
        return true;
    }

    public static boolean testBufferShort() {
        if (verbose) System.out.println("****************Testing buffer putShort/getShort****************");

        PersistentByteBuffer buf = PersistentByteBuffer.allocate(15);

        buf.putShort((short)2016);
        buf.rewind();
        assert(buf.getShort() == 2016);

        buf.putShort(8, (short)4032);
        assert(buf.position() == 2);
        assert(buf.getShort(8) == 4032);
        assert(buf.position() == 2);
        return true;
    }

    public static boolean testBufferLong() {
        if (verbose) System.out.println("****************Testing buffer putLong/getLong****************");

        PersistentByteBuffer buf = PersistentByteBuffer.allocate(15);

        buf.putLong(2016);
        buf.rewind();
        assert(buf.getLong() == 2016);

        buf.putLong(4, 4032);
        assert(buf.position() == 8);
        assert(buf.getLong(4) == 4032);
        assert(buf.position() == 8);
        return true;
    }

    public static boolean testBufferFlip() {
        if (verbose) System.out.println("****************Testing buffer flip****************");

        PersistentByteBuffer buf = PersistentByteBuffer.allocate(15);

        buf.put("hellojavaworld!".getBytes());

        buf.position(5);
        buf.mark();
        buf.flip();

        boolean caught = false;
        try {
            buf.reset();
        } catch (InvalidMarkException e) {
            caught = true;
        }
        assert(caught);

        assert(buf.remaining() == 5);
        assert(buf.limit() == 5);
        assert(buf.position() == 0);
        byte[] bytes = new byte[5];
        buf.get(bytes);
        assert(Arrays.equals(bytes, "hello".getBytes()));
        return true;
    }

    public static boolean testBufferComparison() {
        if (verbose) System.out.println("****************Testing buffer comparison****************");

        PersistentByteBuffer buf1 = PersistentByteBuffer.allocate(15);
        PersistentByteBuffer buf2 = PersistentByteBuffer.allocate(10);
        // ByteBuffer buf3 = ByteBuffer.allocate(15);
        // ByteBuffer buf4 = ByteBuffer.allocate(10);

        buf1.put("helloworld".getBytes());
        buf2.put("helloworld".getBytes());
        // buf3.put("helloworld".getBytes()).rewind();
        // buf4.put("helloworld".getBytes()).rewind();

        assert(buf1.equals(buf2) == false);
        // buf1.rewind();
        // buf2.rewind();
        
        assert(buf1.compareTo(buf2) > 0);  // because buf1 has 5 remaining and buf2 has 0 remaining
        buf1.rewind();
        buf2.rewind();

        // assert(buf1.compareTo(buf4) > 0);  // because buf1 has 15 remaining and buf2 has 10 remaining
        // buf1.rewind();
        // buf4.rewind();

        assert(buf1.equals(buf2) == false);
        assert(buf1.compareTo(buf2) > 0);  // because buf1 has size 15 and buf2 has size 10

        buf1.limit(10);

        assert(buf1.equals(buf2));
        assert(buf1.compareTo(buf2) == 0); // only compare between position (0) and limit (10)
        return true;
    }

    public static boolean testEmptyBuffer() {
        if (verbose) System.out.println("****************Testing buffer size 0****************");

        PersistentByteBuffer buf = PersistentByteBuffer.allocate(0);
        assert(buf.capacity() == 0);
        assert(buf.remaining() == 0);
        assert(buf.position() == 0);
        assert(buf.limit() == 0);

        boolean caught = false;
        try {
            buf.put("hello".getBytes()[0]);
        } catch (BufferOverflowException e) {
            caught = true;
        }
        assert(caught);

        caught = false;
        try {
            buf.get();
        } catch (BufferUnderflowException e) {
            caught = true;
        }
        assert(caught);
        return true;
    }

    static boolean testBufferWrap() {
        if (verbose) System.out.println("****************Testing buffer wrap******************");

        String h = "hello world!";
        byte[] bytes = h.getBytes(Charset.forName("UTF-8"));

        PersistentByteBuffer buf = PersistentByteBuffer.copyWrap(bytes);
        assert(buf.capacity() == h.length());
        assert(buf.remaining() == h.length());
        assert(buf.position() == 0);
        assert(buf.limit() == h.length());
        assert(shouldEqual(buf, h));

        PersistentByteBuffer buf2 = PersistentByteBuffer.copyWrap(bytes, 3, 5);
        assert(buf2.capacity() == h.length());
        assert(buf2.remaining() == 5);
        assert(buf2.position() == 3);
        assert(buf2.limit() == 8);

        byte[] out = new byte[5];
        buf2.get(out);
        assert(new String(out).equals("lo wo"));

        PersistentByteBuffer buf3 = PersistentByteBuffer.wrap(new PersistentByteArray(bytes), 3, 5);
        assert(buf3.capacity() == h.length());
        assert(buf3.remaining() == 5);
        assert(buf3.position() == 3);
        assert(buf3.limit() == 8);

        buf3.get(out);
        assert(new String(out).equals("lo wo"));

        return true;
    }

    static boolean testBufferPutBuffer() {
        if (verbose) System.out.println("****************Testing buffer put wrapper***********");

        ByteBuffer buf = ByteBuffer.allocate(10);
        buf.put("helloworld".getBytes()).rewind();

        PersistentByteBuffer buf2 = PersistentByteBuffer.allocate(10);
        buf2.put(buf);

        buf.rewind();
        buf2.rewind();

        byte[] bytes1 = new byte[10], bytes2 = new byte[10];
        buf.get(bytes1);
        buf2.get(bytes2);
        assert(Arrays.equals(bytes1, bytes2));

        return true;
    }

    static boolean shouldEqual(PersistentByteBuffer buf, String expected) {
        byte[] value = new byte[buf.capacity()];
        int position = buf.position();
        int limit = buf.limit();
        buf.position(0).limit(buf.capacity());
        buf.get(value);
        buf.position(position).limit(limit);
        return new String(value).equals(expected);
    }
}