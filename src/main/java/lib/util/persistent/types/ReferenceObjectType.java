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

package lib.util.persistent.types;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import lib.util.persistent.AnyPersistent;
import lib.util.persistent.ObjectPointer;
import lib.util.persistent.ObjectDirectory;
import lib.util.persistent.ClassInfo;
import lib.util.persistent.PersistentString;
import lib.util.persistent.Header;
import java.lang.reflect.Field;
import java.lang.reflect.Constructor;
import static lib.util.persistent.Trace.*;

public class ReferenceObjectType<T extends AnyPersistent> extends ObjectType<T> {
    public static long FIELDS_OFFSET = Header.TYPE.allocationSize(); // room for header fields;
    public int baseIndex;
    private long allocationSize;

    public ReferenceObjectType(Class<T> cls, List<PersistentType> declaredTypes) {
        super(cls, ObjectType.Kind.Reference);
        fieldTypes = new ArrayList<PersistentType>();
        fieldTypes.addAll(declaredTypes);
        offsets = new long[fieldTypes.size()];
        long size = 0;
        if (fieldTypes.size() > 0) {
            offsets[0] = size;
            size += fieldTypes.get(0).size();
            // System.out.println("ReferenceObjectType ctor, cls = " + cls);
            for (int i = 1; i < fieldTypes.size(); i++) {
                PersistentType fieldType = fieldTypes.get(i);
                // System.out.println("field type " + i + " = " + fieldType);
                offsets[i] = size;
                // System.out.println("offsets[" + i + "] = " + offsets[i]);
                // long delta = fieldType.size();
                long delta = 0;
                if (fieldType instanceof ObjectType) {
                    ObjectType<?> objectType = (ObjectType)fieldType;
                    switch (objectType.kind()) {
                        case Reference: 
                        case IndirectValue:
                        case Generic: 
                            delta = Types.REFERENCE.size(); 
                            break;
                        case DirectValue: 
                            delta = objectType.allocationSize();
                            break;
                        default: throw new IllegalArgumentException("Unknown Kind");
                    }
                }
                else delta = fieldType.size();
                // System.out.println("delta = " + delta);
                size += delta;
            }
        }
        this.allocationSize = size;
        this.size = Types.LONG.size();
        baseIndex = 0;
    }

    public ReferenceObjectType(Class<T> cls, PersistentType... ts) {
        this(cls, Arrays.asList(ts));
    }

    public ReferenceObjectType(Class<T> cls) {
        this(cls, Header.TYPES);
    }

    ReferenceObjectType(Class<T> cls, ObjectType.Kind kind) {
        super(cls, kind);
    }

    @Override public long size() {return Types.LONG.size();}

    @Override public long allocationSize() {return allocationSize;}

    public int baseIndex() {
        return baseIndex;
    }

    public String toString() {
        return "ReferenceObjectType(" + name() + ")";
    }
}

