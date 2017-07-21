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
import lib.util.persistent.PersistentObject;
import lib.util.persistent.Header;

public class ObjectType<T extends PersistentObject> implements Named, Container {
    public static long FIELDS_OFFSET = Header.TYPE.getAllocationSize(); // room for header fields;
    private final List<PersistentType> types;
    private List<PersistentType> staticTypes;
    private final long[] offsets;
    private final long size;
    private final Class<T> cls;
    private int baseIndex;
    private PersistentObject statics;

    private ObjectType(Class<T> cls, List<PersistentType> declaredTypes) {
        this.cls = cls;
        this.types = new ArrayList<PersistentType>();
        this.types.addAll(declaredTypes);
        this.offsets = new long[types.size()];
        long size = 0;
        if (types.size() > 0) {
            offsets[0] = size;
            size += types.get(0).getSize();
            for (int i = 1; i < types.size(); i++) {
                offsets[i] = size;
                size += types.get(i).getSize();
            }
        }
        this.size = size;
        this.baseIndex = 0;
    }

    public ObjectType(Class<T> cls, PersistentType... ts) {
        this(cls, Arrays.asList(ts));
    }

    public ObjectType(Class<T> cls) {
        this(cls, Header.TYPES);
    }

    public Class<T> cls() {
        return cls;
    }

    // orig name
    public static <U extends PersistentObject> ObjectType<U> fromFields(Class<U> cls, PersistentField... fs) {
        return withFields(cls, fs);
    }

    public static <U extends PersistentObject> ObjectType<U> withFields(Class<U> cls, PersistentField... fs) {
        return Header.TYPE.extendWith(cls, fs);
    }

    public <U extends PersistentObject> ObjectType<U> extendWith(Class<U> cls, PersistentType... ts) {
        List<PersistentType> newTs = new ArrayList<>(types);
        newTs.addAll(Arrays.asList(ts));
        ObjectType<U> ans = new ObjectType<>(cls, newTs);
        ans.baseIndex += fieldCount();
        return ans;
    }

    public <U extends PersistentObject> ObjectType<U> extendWith(Class<U> cls, PersistentField... fs) {
        List<PersistentType> ts = new ArrayList<>(types);
        for (int i = 0; i < fs.length; i++) {
            fs[i].setIndex(fieldCount() + i);
            ts.add(fs[i].getType());
        }
        ObjectType<U> ans = new ObjectType<>(cls, ts);
        ans.baseIndex += fieldCount();
        for (int i = 0; i < fs.length; i++) {        
            if (fs[i] instanceof ObjectField) {
               ObjectField<?> objField = (ObjectField<?>)fs[i];
               if (objField.cls() != null) {
                  ObjectType<?> type;
                  if (objField.cls() == cls) type = ans; // recursive type def
                  else type = Types.objectTypeForClass(objField.cls());
                  objField.setType(type);
                  ans.getTypes().set(objField.getIndex(), type);
               }
            }
        }
        return ans;
    }

    public long getAllocationSize() {
        return size;
    }

    @Override public String getName() {
        return cls.getName();
    }

    @Override public long getSize() {
        return Types.LONG.getSize();
    }

    @Override public List<PersistentType> getTypes() {
        return types;
    }

    public List<PersistentType> staticTypes() {
        return staticTypes;
    }


    @Override public long getOffset(int index) {
        return offsets[index];
    }

    public int fieldCount() {
        return types.size();
    }

    public int baseIndex() {
        return baseIndex;
    }

    public PersistentObject statics() {
        return statics;
    }

    public String toString() {
        return "ObjectType(" + getName() + ")";
    }
}

