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

package lib.vmem;

import java.nio.*;
import java.nio.charset.*;
import java.util.*;

public class HeapRAMTest {

    static HeapRAM mem = new HeapRAM();

    static boolean shouldEqual(ByteBuffer buf, String expected) {
        byte[] value = new byte[buf.capacity()];
        int position = buf.position();
        int limit = buf.limit();
        buf.position(0).limit(buf.capacity());
        buf.get(value);
        buf.position(position).limit(limit);
        return new String(value).equals(expected);
    }

    public static void main(String[] args) {
        System.out.println("=================================================HeapRAM Tests=================================================");
        testBufferPosition();
        testBufferLimit();
        testBufferClear();
        testBufferDuplicate();
        testBufferSlice();
        testBufferMark();
        testBufferBasicPutsGets();
        testBufferChar();
        testBufferDouble();
        testBufferFloat();
        testBufferInt();
        testBufferShort();
        testBufferLong();
        testBufferShort();
        testBufferLong();
        testBufferFlip();
        testBufferComparison();
        testEmptyBuffer();
    }

    public static void testBufferPosition() {
        System.out.println("=======================================Testing buffer position=======================================");

        ByteBuffer buf = mem.allocateByteBuffer(10);
        buf.put("helloworld".getBytes());
        buf.rewind();

        buf.position(5);
        assert(buf.position() == 5);
    }

    public static void testBufferLimit() {
        System.out.println("=======================================Testing buffer limit=======================================");

        ByteBuffer buf = mem.allocateByteBuffer(10);
        buf.put("helloworld".getBytes());
        buf.rewind();

        buf.limit(5);
        assert(buf.limit() == 5);
    }

    public static void testBufferClear() {
        System.out.println("=======================================Testing buffer clear=======================================");

        ByteBuffer buf = mem.allocateByteBuffer(15);
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
    }

    public static void testBufferDuplicate() {
        System.out.println("=======================================Testing buffer duplicate=======================================");

        ByteBuffer buf = mem.allocateByteBuffer(15);
        buf.put("hellojavaworld!".getBytes());

        ByteBuffer buf2 = buf.duplicate();

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
    }

    public static void testBufferSlice() {
        System.out.println("=======================================Testing buffer slice======================================");

        ByteBuffer buf = mem.allocateByteBuffer(15);
        buf.put("hellojavaworld!".getBytes());

        buf.position(5).limit(9);

        ByteBuffer buf2 = buf.slice();
        assert(buf2.position() == 0);
        assert(buf2.limit() == 4);
        assert(buf2.capacity() == 4);

        byte[] bytes = new byte[4];
        buf2.get(bytes);
        assert(Arrays.equals(bytes, "java".getBytes()));
    }

    public static void testBufferMark() {
        System.out.println("=======================================Testing buffer mark======================================");

        ByteBuffer buf = mem.allocateByteBuffer(15);
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
    }

    public static void testBufferBasicPutsGets() {
        System.out.println("=======================================Testing buffer basic puts/gets======================================");

        ByteBuffer buf = mem.allocateByteBuffer(15);
        ByteBuffer temp = mem.allocateByteBuffer(4);

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
    }

    public static void testBufferChar() {
        System.out.println("=======================================Testing buffer putChar/getChar======================================");

        ByteBuffer buf = mem.allocateByteBuffer(15);

        buf.putChar('5');
        buf.rewind();
        assert(buf.getChar() == '5');

        buf.putChar(5, '6');
        assert(buf.position() == 2);
        assert(buf.getChar(5) == '6');
        assert(buf.position() == 2);
    }

    public static void testBufferDouble() {
        System.out.println("=======================================Testing buffer putDouble/getDouble======================================");

        ByteBuffer buf = mem.allocateByteBuffer(15);

        buf.putDouble(3.1415926);
        buf.rewind();
        assert(new Double(3.1415926).equals(buf.getDouble()));

        buf.putDouble(4, 1.23456789);
        assert(buf.position() == 8);
        assert(new Double(1.23456789).equals(buf.getDouble(4)));
        assert(buf.position() == 8);
    }

    public static void testBufferFloat() {
        System.out.println("=======================================Testing buffer putFloat/getFloat======================================");

        ByteBuffer buf = mem.allocateByteBuffer(15);

        buf.putFloat((float)3.1415);
        buf.rewind();
        assert(new Float(3.1415).equals(buf.getFloat()));

        buf.putFloat(4, (float)1.2345);
        assert(buf.position() == 4);
        assert(new Float(1.2345).equals(buf.getFloat(4)));
        assert(buf.position() == 4);
    }

    public static void testBufferInt() {
        System.out.println("=======================================Testing buffer putInt/getInt======================================");

        ByteBuffer buf = mem.allocateByteBuffer(15);

        buf.putInt(2016);
        buf.rewind();
        assert(buf.getInt() == 2016);

        buf.putInt(8, 4032);
        assert(buf.position() == 4);
        assert(buf.getInt(8) == 4032);
        assert(buf.position() == 4);
    }

    public static void testBufferShort() {
        System.out.println("=======================================Testing buffer putShort/getShort======================================");

        ByteBuffer buf = mem.allocateByteBuffer(15);

        buf.putShort((short)2016);
        buf.rewind();
        assert(buf.getShort() == 2016);

        buf.putShort(8, (short)4032);
        assert(buf.position() == 2);
        assert(buf.getShort(8) == 4032);
        assert(buf.position() == 2);
    }

    public static void testBufferLong() {
        System.out.println("=======================================Testing buffer putLong/getLong======================================");

        ByteBuffer buf = mem.allocateByteBuffer(15);

        buf.putLong(2016);
        buf.rewind();
        assert(buf.getLong() == 2016);

        buf.putLong(4, 4032);
        assert(buf.position() == 8);
        assert(buf.getLong(4) == 4032);
        assert(buf.position() == 8);
    }

    public static void testBufferFlip() {
        System.out.println("=======================================Testing buffer flip======================================");

        ByteBuffer buf = mem.allocateByteBuffer(15);

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
    }

    public static void testBufferComparison() {
        System.out.println("=======================================Testing buffer comparison======================================");

        ByteBuffer buf1 = mem.allocateByteBuffer(15);
        ByteBuffer buf2 = mem.allocateByteBuffer(10);

        buf1.put("helloworld".getBytes());
        buf2.put("helloworld".getBytes());

        assert(buf1.equals(buf2) == false);
        assert(buf1.compareTo(buf2) > 0);  // because buf1 has 5 remaining and buf2 has 0 remaining

        buf1.rewind();
        buf2.rewind();

        assert(buf1.equals(buf2) == false);
        assert(buf1.compareTo(buf2) > 0);  // because buf1 has size 15 and buf2 has size 10

        buf1.limit(10);

        assert(buf1.equals(buf2));
        assert(buf1.compareTo(buf2) == 0); // only compare between position (0) and limit (10)
    }

    public static void testEmptyBuffer() {
        System.out.println("=======================================Testing buffer size 0======================================");

        ByteBuffer buf = mem.allocateByteBuffer(0);
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
    }
}
