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

import lib.util.persistent.types.*;
import lib.util.persistent.types.ObjectType;
import lib.util.persistent.types.ArrayType;
import lib.util.persistent.types.ReferenceArrayType;
import lib.util.persistent.front.PersistentClass;

@PersistentClass
public final class PersistentString extends PersistentImmutableObject implements Comparable<PersistentString>, ComparableWith<String>, EquatesWith<String> {
    private static final FinalIntField LENGTH = new FinalIntField();
    private static final FinalObjectField<PersistentImmutableByteArray> BYTES = new FinalObjectField<>(PersistentImmutableByteArray.class);
    private static final ObjectType<PersistentString> TYPE = ObjectType.withFields(PersistentString.class, LENGTH, BYTES);
    private final String s;

    public static PersistentString make(String s) {
        return Transaction.run(() -> {
            return new PersistentString(s);
        });
    }

    // TODO: make constructors private
    public PersistentString(String s) {
        super(TYPE, (PersistentImmutableObject obj) -> {
            obj.initIntField(LENGTH, s.length());
            obj.initObjectField(BYTES, new PersistentImmutableByteArray(s.getBytes()));
        });
        this.s = s;
    }

    public PersistentString(ObjectPointer<PersistentString> p) {
        super(p);
        boolean oldAdminMode = ObjectCache.adminMode.get();
        ObjectCache.adminMode.set(true);
        PersistentImmutableByteArray ba = getObjectField(BYTES);
        ObjectCache.adminMode.set(oldAdminMode);
        if (ba != null) s = new String(ba.toArray());
        else s = null;
    }

    public int length() {
        return s.length();
    }

    public String toString() {
        return s;
    }

    public int hashCode() {
        return s.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PersistentString && ((PersistentString)o).toString().equals(s);
    }

    public byte[] getBytes() {
        return s.getBytes();
    }

    public int compareTo(PersistentString anotherString) {
        /*int len1 = length();
        int len2 = anotherString.length();
        int lim = Math.min(len1, len2);
        byte[] v1 = getBytes();
        byte[] v2 = anotherString.getBytes();

        int k = 0;
        while (k < lim) {
            byte b1 = v1[k];
            byte b2 = v2[k];
            if (b1 != b2) {
                return b1 - b2;
            }
            k++;
        }
        return len1 - len2;*/
        return s.compareTo(anotherString.toString());
    }

    public int compareWith(String anotherString) {
        return s.compareTo(anotherString);
    }

    @Override
    public int equivalentHash() {
        return s.hashCode();
    }

    @Override
    public boolean equatesWith(String that) {
        return s.equals(that);
    }
}
