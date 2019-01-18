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

public /*abstract */class ObjectType<T extends AnyPersistent> implements Named, Container {
    public static final ReferenceObjectType<AnyPersistent> GENERIC_REFERENCE = new ReferenceObjectType<AnyPersistent>(AnyPersistent.class, Kind.Reference);
    public static final ObjectType<AnyPersistent> GENERIC_OBJECT = new ObjectType<AnyPersistent>(AnyPersistent.class, Kind.Generic);
    public enum Kind {
        Reference,
        DirectValue,
        IndirectValue,
        Generic
    }

    private final Class<T> cls;
    private final Kind kind;
    protected List<PersistentType> fieldTypes;
    protected long[] offsets;
    protected long size;
    protected Constructor reconstructor;

    protected ObjectType(Class<T> cls, Kind kind) {
        this.cls = cls;
        this.kind = kind;
    }

    public long offset(int index) {return offsets[index];}

    public Kind kind() {return kind;}
                                                                                               // TODO: temp, references leak
    public boolean valueBased() {return kind == Kind.DirectValue || kind == Kind.IndirectValue || kind == Kind.Generic;}

    public /*abstract */long allocationSize() {return 0;}
    public /*abstract */long size() {return 0;}

    public Class<T> cls() {return cls;}

    public Constructor getReconstructor() { 
        if (reconstructor != null) return reconstructor;
        try {
            reconstructor = cls.getDeclaredConstructor(ObjectPointer.class);
            reconstructor.setAccessible(true);
        }
        catch (NoSuchMethodException nsm) {throw new RuntimeException("Unable to get reconstructor: " + nsm.getMessage());}
        return reconstructor;
    }

    public static <U extends AnyPersistent> ObjectType<U> withFields(Class<U> cls, PersistentField... fs) {
        PersistentField[] ordered = fs.length > 1 ? layoutFields(fs) : fs;
        // System.out.println("ordered here = " + Arrays.toString(ordered));
        return Header.TYPE.extendWith(cls, ordered);
    }

    public static <U extends AnyPersistent> ObjectType<U> withValueFields(Class<U> cls, ValueBasedField... fs) {
        return DirectValueObjectType.withFields(cls, fs);
    }

    public static <U extends AnyPersistent> ObjectType<U> indirectWithValueFields(Class<U> cls, ValueBasedField... fs) {
        return IndirectValueObjectType.withFields(cls, fs);
    }

    public <U extends AnyPersistent> ObjectType<U> extendWith(Class<U> cls, PersistentType... ts) {
        List<PersistentType> newTs = new ArrayList<>(fieldTypes());
        newTs.addAll(Arrays.asList(ts));
        ReferenceObjectType<U> ans = new ReferenceObjectType<U>(cls, newTs);
        ans.baseIndex += fieldCount();
        return ans;
    }

    public <U extends AnyPersistent> ObjectType<U> extendWith(Class<U> cls, PersistentField... fs) {
        // System.out.println("ObjectType.extendWith(" + cls + ", " + Arrays.toString(fs));
        List<PersistentType> ts = new ArrayList<>(fieldTypes());
        for (int i = 0; i < fs.length; i++) {
            fs[i].setIndex(fieldCount() + i);
            // System.out.println("adding type " + fs[i].getType() + " for field index " + (fieldCount() + i));
            ts.add(fs[i].getType());
        }
        ReferenceObjectType<U> ans = new ReferenceObjectType<>(cls, ts);
        ans.baseIndex += fieldCount();
        for (int i = 0; i < fs.length; i++) {        
            if (fs[i] instanceof ObjectField) {
               ObjectField<?> objField = (ObjectField<?>)fs[i];
               if (objField.cls() != null) {
                  ObjectType<?> type;
                  if (objField.cls() == cls) type = ans; // recursive type def
                  else type = Types.objectTypeForClass(objField.cls());
                  objField.setType(type);
                  ans.fieldTypes().set(objField.getIndex(), type);
               }
            }
        }
        return ans;
    }

    public static <A extends B, B extends AnyPersistent> ObjectType<A> extendClassWith(Class<A> thisClass, Class<B> superClass, PersistentField... fs) {
        ObjectType<B> baseType = Types.objectTypeForClass(superClass);
        return baseType.extendWith(thisClass, fs);
    }

    // sort fields in descending order by size in bytes; value fields go at end regardless of size
    private static PersistentField[] layoutFields(PersistentField[] ts) {
        // System.out.println("layoutFields: " + java.util.Arrays.toString(ts));
        java.util.Comparator<PersistentField> comp = (PersistentField f, PersistentField g) -> {
            boolean fIsValue = f instanceof ValueField;
            boolean gIsValue = g instanceof ValueField;
            long fSize = f.getType().size();
            long gSize = g.getType().size();
            int ans = 0;
            if (fIsValue) {
                if (gIsValue) ans = 0;
                else ans = 1;  // values go at the end
            }
            else if (gIsValue) {
                if (fIsValue) ans = 0;
                else ans = -1;  // values go at the end
            }
            else ans = fSize > gSize ? -1 : fSize < gSize ? 1 : 0;
            return ans;
        };
        PersistentField[] ordered = Arrays.copyOf(ts, ts.length);
        Arrays.sort(ordered, comp);
        return ordered;
    }

    @Override public String name() {
        return cls.getName();
    }

    @Override public List<PersistentType> fieldTypes() {
        return fieldTypes;
    }

    public int fieldCount() {
        return fieldTypes.size();
    }

    public String toString() {
        return "ObjectType(" + name() + ")";
    }
}

