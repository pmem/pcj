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

package examples.misc;

import lib.util.persistent.PersistentValue;
import lib.util.persistent.ValuePointer;
import lib.util.persistent.types.IntField;
import lib.util.persistent.types.LongField;
import lib.util.persistent.types.ValueType;
import lib.util.persistent.types.ValueField;
import lib.util.persistent.types.ByteField;
import lib.util.persistent.types.ShortField;
import static lib.util.persistent.Persistent.*;

public class TestNestedValues {
    public static void main(String[] args) {
        EmployeeId alice = new EmployeeId(12345, 1000);
        EmployeeId bob = new EmployeeId(12345, 1001);
        EngineerId engAlice = new EngineerId((byte)12, alice, (short)13);
        System.out.println(engAlice);
        assert(engAlice.toString().equals("EngineerId(12, EmployeeId(12345, Tag(56, 57), 1000), 13)"));
    }

   public static class EngineerId extends PersistentValue {
        private static final ByteField LAB = new ByteField();
        private static final ValueField<EmployeeId> EID = ValueField.forClass(EmployeeId.class);
        private static final ShortField CLEARANCE = new ShortField();
        public static final ValueType TYPE = ValueType.fromFields(LAB, EID, CLEARANCE);

        public EngineerId(byte lab, EmployeeId eid, short clearance) {
            super(TYPE, EngineerId.class);
            setByteField(LAB, lab);
            setValueField(EID, eid);
            setShortField(CLEARANCE, clearance);
        }

        public EngineerId(ValuePointer<EngineerId> p) {super(p);}

        public String toString() {
            return String.format("EngineerId(%d, %s, %d)", getByteField(LAB), getValueField(EID), getShortField(CLEARANCE));
        }
    }

    public static class EmployeeId extends PersistentValue {
        private static final IntField SEGMENT = new IntField();
        private static final ValueField<Tag> TAG = ValueField.forClass(Tag.class);
        private static final LongField NUMBER = new LongField();
        public static final ValueType TYPE = ValueType.fromFields(SEGMENT, TAG, NUMBER);

        public EmployeeId(int segment, long number) {
            super(TYPE, EmployeeId.class);
            setIntField(SEGMENT, segment);
            setValueField(TAG, new Tag((byte)56, 57));
            setLongField(NUMBER, number);
        }

        public EmployeeId(ValuePointer<EmployeeId> p) {super(p);}

        public int getSegment() {return getIntField(SEGMENT);}
        public long getNumber() {return getLongField(NUMBER);}

        public String toString() {
            return String.format("EmployeeId(%d, %s, %d)", getSegment(), getValueField(TAG), getNumber());
        }
    }

    public static class Tag extends PersistentValue {
        private static final ByteField B = new ByteField();
        private static final IntField I = new IntField();
        public static final ValueType TYPE = ValueType.fromFields(B, I);

        public Tag(byte b, int i) {
            super(TYPE, Tag.class);
            setByteField(B, b);
            setIntField(I, i);
        }

        public Tag(ValuePointer<Tag> p) {super(p);}

        public String toString() {
            return String.format("Tag(%d, %d)", getByteField(B), getIntField(I));
        }
    }
}
