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

import lib.util.persistent.PersistentObject;
import lib.util.persistent.PersistentValue;
import lib.util.persistent.Persistent;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

public class Types {

    public static final PrimitiveType BYTE = new PrimitiveType(1);
    public static final PrimitiveType SHORT = new PrimitiveType(2);
    public static final PrimitiveType INT = new PrimitiveType(4);
    public static final PrimitiveType LONG = new PrimitiveType(8);
    public static final CarriedType FLOAT = new CarriedType(INT);
    public static final CarriedType DOUBLE = new CarriedType(LONG);
    public static final CarriedType CHAR = new CarriedType(INT);
    public static final CarriedType BOOLEAN = new CarriedType(BYTE);
    public static final CarriedType OBJECT = new CarriedType(LONG);
    public static final CarriedType VALUE = new CarriedType(LONG);

    public static final String TYPE_FIELD_NAME = "TYPE";
    public static final String OLD_TYPE_FIELD_NAME = "type";

    private static final ConcurrentHashMap<Class<?>,PersistentType> typemap=new ConcurrentHashMap<>();
    @SuppressWarnings("unchecked")
    public static synchronized <T extends Persistent<?>> PersistentType typeForClass(Class<T> cls) {
        try {
            PersistentType t=typemap.get(cls);
            if (t!=null) return t;
            Field typeField = null;
            PersistentType type;
            for (int i=0;i<3;++i) {
                try {
                    switch (i) {
                        case 0: // TYPE_FIELD_NAME
                        case 2: // OLD_TYPE_FIELD_NAME
                            typeField = cls.getDeclaredField(i==0 ? TYPE_FIELD_NAME : OLD_TYPE_FIELD_NAME);
                            typeField.setAccessible(true);
                            type = (PersistentType) typeField.get(null);
                            //typeField.setAccessible(false); // PPR: No thread safe if interlace two threads
                            if (type == null) throw new RuntimeException("type field is null in " + cls);
                            typemap.put(cls,type);
                            return type;
                        case 1: // Scala
                            Class<?> scalaObject = cls.getClassLoader().loadClass(cls.getName() + "$");
                            Field moduleField=scalaObject.getDeclaredField("MODULE$");
                            Object module = moduleField.get(null);
                            typeField=scalaObject.getDeclaredField(TYPE_FIELD_NAME);
                            typeField.setAccessible(true);
                            type = (PersistentType) typeField.get(module);
                            //typeField.setAccessible(false);
                            if (type == null) throw new RuntimeException("type field is null in " + cls);
                            typemap.put(cls,type);
                            return type;
                    }
                } catch (NoSuchFieldException e) { // Ignore
                } catch (ClassNotFoundException e) { // Ignore
                }
            }
            throw new RuntimeException("no type field in " + cls);
        }
        catch (IllegalAccessException e) {throw new RuntimeException("illegal access on type field in " + cls);}
    }

    @SuppressWarnings("unchecked")
    public static <T extends PersistentObject> ObjectType<T> objectTypeForClass(Class<T> cls) {
        return (ObjectType<T>)typeForClass(cls);
    }

    @SuppressWarnings("unchecked")
    public static ValueType valueTypeForClass(Class<? extends PersistentValue> cls) {
        return (ValueType)typeForClass(cls);
    }

    @SuppressWarnings("unchecked")
    public static synchronized <T extends PersistentObject> ObjectType<T> typeForName(String name) {
        try {
            Class<T> cls = (Class<T>)Class.forName(name);
            return objectTypeForClass(cls);
        } 
        catch (ClassNotFoundException e) {throw new RuntimeException("class not found for name " + name);}
    }
}
