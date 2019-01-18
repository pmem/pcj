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
import java.util.concurrent.atomic.*;

public final class PersistentAtomicReference<T extends AnyPersistent> extends PersistentObject {

	private static final ObjectField<AnyPersistent> VALUE = new ObjectField<>();
	private static final ObjectType<PersistentAtomicReference> TYPE = ObjectType.withFields(PersistentAtomicReference.class, VALUE);

	public PersistentAtomicReference() {
		super(TYPE);
	}

	public PersistentAtomicReference(T t) {
		super(TYPE, (PersistentAtomicReference self) -> {
			self.initObjectField(VALUE, t);
		});
	}

	private PersistentAtomicReference(ObjectPointer<PersistentAtomicReference> p) {
		super(p);
	}

	public void set(T t) {
		setObjectField(VALUE, t);
	}

	@SuppressWarnings("unchecked")
	public T get() {
		return (T)getObjectField(VALUE);
	}

	public boolean compareAndSet(T expect, T update) {
        return Util.synchronizedBlock(this, () -> {
		    if(get() != expect) return false; 
		    set(update);
		    return true;
        });
	}   

}
