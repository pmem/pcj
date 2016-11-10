/* Copyright (C) 2016  Intel Corporation
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

#define HEADER_TYPE_OFFSET 1015
#define RBTREE_MAP_TYPE_OFFSET 1017
#define TREE_MAP_NODE_TYPE_OFFSET 1018
#define PERSISTENT_BYTE_BUFFER_TYPE_OFFSET 1019
#define PERSISTENT_BYTE_ARRAY_TYPE_OFFSET 1020
#define HASHMAP_TX_TYPE_OFFSET 1021
#define BUCKETS_TYPE_OFFSET 1022
#define ENTRY_TYPE_OFFSET 1023
#define CHAR_TYPE_OFFSET 1024
#define HASHSET_TYPE_OFFSET 1025

struct rbtree_map;
struct hashmap_tx;
struct hashset;

TOID_DECLARE(struct header, HEADER_TYPE_OFFSET);
TOID_DECLARE(struct rbtree_map, RBTREE_MAP_TYPE_OFFSET);
TOID_DECLARE(struct tree_map_node, TREE_MAP_NODE_TYPE_OFFSET);
TOID_DECLARE(struct persistent_byte_buffer, PERSISTENT_BYTE_BUFFER_TYPE_OFFSET);
TOID_DECLARE(struct persistent_byte_array, PERSISTENT_BYTE_ARRAY_TYPE_OFFSET);
TOID_DECLARE(struct hashmap_tx, HASHMAP_TX_TYPE_OFFSET);
TOID_DECLARE(struct buckets, BUCKETS_TYPE_OFFSET);
TOID_DECLARE(struct entry, ENTRY_TYPE_OFFSET);
TOID_DECLARE(char, CHAR_TYPE_OFFSET);
TOID_DECLARE(struct hashset, HASHSET_TYPE_OFFSET);

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

enum ref_color {
    PURPLE,
    GRAY,
    BLACK,
    WHITE
};

struct header {
    int version;
    int refCount;
    int type;
    int fieldCount;
    ref_color color;
    int is_candidate;
    PMEMmutex obj_mutex;
    uint64_t prev_obj_offset;
    uint64_t next_obj_offset;
};

struct persistent_byte_array {
    TOID(struct header) header;
    PMEMoid bytes;
};

struct persistent_byte_buffer {
    TOID(struct header) header;
    PMEMoid arr;
    int position;
    int limit;
    int capacity;
    int mark;
    int start;
};

enum rb_color {
	COLOR_BLACK,
	COLOR_RED,

	MAX_COLOR
};

enum rb_children {
	RB_LEFT,
	RB_RIGHT,

	MAX_RB
};

struct rbtree_map {
    TOID(struct header) header;
    TOID(struct tree_map_node) sentinel;
    TOID(struct tree_map_node) root;
    int size;
};

struct tree_map_node {
	TOID(struct tree_map_node) parent;
	TOID(struct tree_map_node) slots[MAX_RB];
	PMEMoid key;
	PMEMoid value;
	enum rb_color color;
};

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
};

#define PERSISTENT_BYTE_ARRAY_FIELD_COUNT 0
#define PERSISTENT_BYTE_BUFFER_FIELD_COUNT 1
#define RBTREE_MAP_FIELD_COUNT 0

#endif /* PERSISTENT_STRUCTS_H */
