/* Copyright (C) 2015, 2016  Intel Corporation
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

#ifndef HASHMAP_H
#define HASHMAP_H

/* common API provided by both implementations */

#include <stddef.h>
#include <stdint.h>

struct hashmap_args {
	uint32_t seed;
};

enum hashmap_cmd {
	HASHMAP_CMD_REBUILD,
	HASHMAP_CMD_DEBUG,
};

#endif /* HASHMAP_H */
