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

import lib.util.persistent.types.ObjectType;
import lib.util.persistent.types.Types;
import lib.util.persistent.types.PersistentType;

public class Header {
    static final int TYPE_NAME = 0;  // must be first field
    static final int VERSION = 1;
    static final int REF_COUNT = 2;
    static final int REF_COLOR = 3;

    public static final PersistentType[] TYPES = new PersistentType[] {
        Types.LONG,        // TYPE_NAME  // TODO: should reference shared name or type
        Types.LONG,        // VERSION
        Types.INT,         // REF_COUNT
        Types.BYTE,        // REF_COLOR
    };

    public static final ObjectType<PersistentObject> TYPE = new ObjectType<>(PersistentObject.class, TYPES);
}