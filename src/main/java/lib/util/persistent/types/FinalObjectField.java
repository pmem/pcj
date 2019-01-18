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
 * Software Foundation, Inc., 51 Franklin Street, Fifth P,
 * Boston, MA  02110-1301, USA.
 */

package lib.util.persistent.types;

import lib.util.persistent.AnyPersistent;

public class FinalObjectField<T extends AnyPersistent> extends PersistentField {
   private Class<T> cls;

	public FinalObjectField(ObjectType<T> type) {
		super(type);
	}

	public FinalObjectField() {
		super(Types.GENERIC_REFERENCE);
	}

	public FinalObjectField(Class<T> cls) {
      	this();
      	this.cls = cls;
	}

    protected Class<T> cls() {
       return cls;
    }

    public String toString() {return "FinalObjectField(" + getType() + ")";}
}
