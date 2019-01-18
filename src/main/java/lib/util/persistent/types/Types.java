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

import lib.util.persistent.AnyPersistent;
import lib.util.persistent.ClassInfo;
import java.lang.reflect.Field;

public class Types {

    public static final PrimitiveType BYTE = new PrimitiveType(1);
    public static final PrimitiveType SHORT = new PrimitiveType(2);
    public static final PrimitiveType INT = new PrimitiveType(4);
    public static final PrimitiveType LONG = new PrimitiveType(8);
    public static final CarriedType FLOAT = new CarriedType(INT);
    public static final CarriedType DOUBLE = new CarriedType(LONG);
    public static final CarriedType CHAR = new CarriedType(INT);
    public static final CarriedType BOOLEAN = new CarriedType(BYTE);
    public static final CarriedType VALUE = new CarriedType(LONG);
    public static final CarriedType REFERENCE = new CarriedType(LONG);
    public static final CarriedType POINTER = new CarriedType(LONG);
    public static final ObjectType<AnyPersistent> GENERIC_REFERENCE = ObjectType.GENERIC_REFERENCE;
    public static final ObjectType<AnyPersistent> GENERIC_OBJECT = ObjectType.GENERIC_OBJECT;

    public static final String TYPE_FIELD_NAME = "TYPE";

    @SuppressWarnings("unchecked")
    public static synchronized <T extends AnyPersistent> PersistentType typeForClass(Class<T> cls) {
        return ClassInfo.getType(cls);
    }

    @SuppressWarnings("unchecked")
    public static <T extends AnyPersistent> ObjectType<T> objectTypeForClass(Class<T> cls) {
        return (ObjectType<T>)typeForClass(cls);
    }
 
    @SuppressWarnings("unchecked")
    public static synchronized <T extends AnyPersistent> ObjectType<T> typeForName(String name) {
        try {
            Class<T> cls = (Class<T>)ClassInfo.getClassInfo(name).cls();
            if (cls == null) {
                cls = (Class<T>)Class.forName(name);
            }
            return objectTypeForClass(cls);
        } 
        catch (ClassNotFoundException e) {throw new RuntimeException("class not found for name " + name);}
    }
}
