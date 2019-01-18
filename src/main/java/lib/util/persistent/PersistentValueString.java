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

import lib.util.persistent.types.IntField;
import lib.util.persistent.types.ValueField;
import lib.util.persistent.types.ObjectType;
import lib.util.persistent.types.Types;
import static lib.util.persistent.Trace.*;

public final class PersistentValueString extends PersistentImmutableObject implements Comparable<PersistentValueString>, ComparableWith<String>, EquatesWith<String> {
	private static final ValueField<PersistentByteVector> BYTES = new ValueField<>(PersistentByteVector.class);
	private static final ObjectType<PersistentValueString> TYPE = ObjectType.withValueFields(PersistentValueString.class, BYTES);
	private String string;

	public PersistentValueString(String s) {
		super(TYPE, (PersistentValueString self) -> {
			int length = s.length();
			byte[] stringBytes = s.getBytes();
			PersistentByteVector bytes = new PersistentByteVector(length);
			bytes.putBytesAt(stringBytes, 0);
			self.initObjectField(BYTES, bytes);
		});
		this.string = s;
	}

	private PersistentValueString(ObjectPointer<PersistentValueString> p) {super(p);}

	public int length() {
		return getString().length();
	}

	public String toString() {
		return getString();
	}

    public int hashCode() {
        return getString().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PersistentValueString && ((PersistentValueString)o).toString().equals(getString());
    }

    public byte[] getBytes() {
        return getString().getBytes();
    }

    public int compareTo(PersistentValueString anotherString) {
        return getString().compareTo(anotherString.toString());
    }

    public int compareWith(String anotherString) {
        return getString().compareTo(anotherString);
    }

    @Override
    public int equivalentHash() {
        return getString().hashCode();
    }

    @Override
    public boolean equatesWith(String that) {
        return getString().equals(that);
    }

	private String getString() {
		if (string != null) return string;
		PersistentByteVector v = getObjectField(BYTES);
		byte[] bytes = v.getBytes();
		string = new String(bytes);
		return string;
	}
}

