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
#define HASHMAP_TX_TYPE_OFFSET 1018
#define BUCKETS_TYPE_OFFSET 1019
#define ENTRY_TYPE_OFFSET 1020

struct hashmap_tx;

TOID_DECLARE(char, CHAR_TYPE_OFFSET);
TOID_DECLARE(struct hashmap_tx, HASHMAP_TX_TYPE_OFFSET);
TOID_DECLARE(struct buckets, BUCKETS_TYPE_OFFSET);
TOID_DECLARE(struct entry, ENTRY_TYPE_OFFSET);

// Special PMEMoid that supports addition of null & error code as metadata
typedef struct pmemoid_ne {
    uint64_t value;
    int null;
    int error;
} NEPMEMoid;

static const NEPMEMoid NEOID_NULL = { 0, 1, 0 };
static const NEPMEMoid NEOID_ERR = { 0, 0, 1 };

#define NEOID_IS_ERR(e) ((e).error != 0)
#define NEOID_IS_NULL(e) ((e).null != 0)

// Special PMEMoid that supports addition of error code as metadata
typedef struct pmemoid_e {
    PMEMoid oid;
    int error;
} EPMEMoid;

static const EPMEMoid EOID_NULL = { { 0, 0 }, 0 };
static const EPMEMoid EOID_ERR = { { 0, 0 }, 1 };

#define EOID_IS_ERR(e) ((e).error != 0)

struct entry {
	uint64_t key;
	uint64_t value;

	/* next entry list pointer */
	TOID(struct entry) next;
};

struct buckets {
	/* number of buckets */
	size_t nbuckets;
	/* array of lists */
	TOID(struct entry) bucket[];
};

struct hashmap_tx {
	/* random number generator seed */
	uint32_t seed;

	/* hash function coefficients */
	uint32_t hash_fun_a;
	uint32_t hash_fun_b;
	uint64_t hash_fun_p;

	/* number of values inserted */
	uint64_t count;

	/* buckets */
	TOID(struct buckets) buckets;

    int init_size;
    int resize_allowed;
};

#endif /* PERSISTENT_STRUCTS_H */
