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

package examples.database;

import java.nio.ByteBuffer;
import lib.util.persistent.*;
import lib.util.persistent.types.*;


public class Key extends PersistentObject implements Comparable<Key> {
    private static final ObjectField<PersistentString> KEY = new ObjectField<>(PersistentString.class);
    public static final ObjectType<Key> TYPE = ObjectType.withFields(Key.class, KEY);

    public Key(String key) {
        super(TYPE);
        Transaction.run(() -> {
            setObjectField(KEY, new PersistentString(key));
        });
    }

    public Key(ObjectType<? extends Key> type) { super(type); }

    public Key(ObjectPointer<? extends Key> p) { super(p); }

    public PersistentString key() {
        return getObjectField(KEY);
    }

    @Override
    public int compareTo(Key that) {
        return this.key().compareTo(that.key());
    }

    @Override
    public String toString() {
        return key().toString();
    }
    
    public ByteBuffer toByteBuf(){
        String s = toString();
        ByteBuffer b = ByteBuffer.allocate(s.length());
        return b.put(s.getBytes());
    }
}
