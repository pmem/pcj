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
#include <errno.h>
#include <unistd.h>
#include <fcntl.h>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <jni.h>

extern PMEMobjpool* pool;
extern TOID(struct root_struct) root;

struct root_struct {
    uint64_t root_val;
};

POBJ_LAYOUT_BEGIN(persistent_heap);
POBJ_LAYOUT_ROOT(persistent_heap, struct root_struct);
POBJ_LAYOUT_END(persistent_heap);

#define CHAR_TYPE_OFFSET 1017
TOID_DECLARE(char, CHAR_TYPE_OFFSET);

PMEMobjpool *get_or_create_pool(const char* path, size_t size);
uint64_t get_uuid_lo();
