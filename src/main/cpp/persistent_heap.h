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

#include "persistent_structs.h"
#include <errno.h>
#include <unistd.h>
#include <fcntl.h>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <jni.h>

extern PMEMobjpool* pool;
extern TOID(struct root_struct) root;
extern JavaVM *jvm;

POBJ_LAYOUT_BEGIN(persistent_heap);
POBJ_LAYOUT_ROOT(persistent_heap, struct root_struct);
POBJ_LAYOUT_END(persistent_heap);

PMEMobjpool* get_or_create_pool(const char* path, size_t size);
void close_pool();
TOID(struct root_struct) get_root();
void create_root(uint64_t root_size);
int check_root_exists();
uint64_t get_uuid_lo();
