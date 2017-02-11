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

public class MemoryRegionObjectTest {

    public static void main(String[] args) {
        System.out.println("****************MemoryRegionObject Tests***************");
        testPutGet();
        testOutOfBounds();
    }

    public static void testPutGet() {
        MemoryRegionObject reg = new MemoryRegionObject(16);

        reg.putByte(0, (byte)5);
        assert(reg.getByte(0) == (byte)5);

        reg.putShort(1, (short)5);
        assert(reg.getShort(1) == (short)5);
        assert(reg.getByte(0) == (byte)5);
        assert(reg.getShort(0) == (short)1285);

        reg.putInt(2, 327686);
        assert(reg.getInt(2) == 327686);
        assert(reg.getShort(1) == (short)1541);
        assert(reg.getInt(1) == 83887621);
        assert(reg.getInt(0) == 394501);

        reg.putLong(4, 123456789101112L);
        assert(reg.getLong(4) == 123456789101112L);
        assert(reg.getLong(3) == 31604938009884672L);
        assert(reg.getInt(3) == 255473664);
        assert(reg.getShort(3) == (short)14336);
        assert(reg.getByte(3) == (byte)0);

        ObjectDirectory.put("reg", reg);
        MemoryRegionObject reg2 = ObjectDirectory.get("reg", MemoryRegionObject.class);
        assert(reg.getLong(4) == 123456789101112L);
        assert(reg.getLong(3) == 31604938009884672L);
        assert(reg.getInt(3) == 255473664);
        assert(reg.getShort(3) == (short)14336);
        assert(reg.getByte(3) == (byte)0);
    }

    public static void testOutOfBounds() {
        MemoryRegionObject reg = new MemoryRegionObject(16);

        boolean caught = false;
        try {
            reg.putLong(10, 5L);
        } catch (IndexOutOfBoundsException e) {
            caught = true;
        }
        assert(caught);

        caught = false;
        try {
            reg.putLong(8, 5L);
        } catch (IndexOutOfBoundsException e) {
            caught = true;
        }
        assert(!caught);
    }
}
