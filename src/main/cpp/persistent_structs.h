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

#include <libpmemobj.h>

#ifndef PERSISTENT_STRUCTS_H
#define PERSISTENT_STRUCTS_H

#ifndef PERSISTENT_TYPE_OFFSET
#define PERSISTENT_TYPE_OFFSET 1016
#endif

#define CHAR_TYPE_OFFSET 1017
#define OBJECT_TYPE_OFFSET 1018

typedef char object;

TOID_DECLARE(char, CHAR_TYPE_OFFSET);
TOID_DECLARE(object, OBJECT_TYPE_OFFSET);

struct root_struct {
    TOID(char) root_memory_region;
};

#endif /* PERSISTENT_STRUCTS_H */
