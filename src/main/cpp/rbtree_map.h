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

/*
 * rbtree_map.h -- TreeMap sorted collection implementation
 */

#ifndef RBTREE_MAP_H
#define RBTREE_MAP_H

#include <libpmemobj.h>
#include "persistent_structs.h"

int rbtree_map_check(PMEMobjpool *pop, TOID(struct rbtree_map) map);
int rbtree_map_new(PMEMobjpool *pop, TOID(struct rbtree_map) *map, void *arg);
int rbtree_map_delete(PMEMobjpool *pop, TOID(struct rbtree_map) *map);
EPMEMoid rbtree_map_insert(PMEMobjpool *pop, TOID(struct rbtree_map) map,
	PMEMoid key, PMEMoid value);
TOID(struct tree_map_node) rbtree_map_successor(TOID(struct rbtree_map) map, TOID(struct tree_map_node) n);
TOID(struct tree_map_node) rbtree_map_predecessor(TOID(struct rbtree_map) map, TOID(struct tree_map_node) n);
TOID(struct tree_map_node) rbtree_map_first_element(TOID(struct rbtree_map) map);
TOID(struct tree_map_node) rbtree_map_last_element(TOID(struct rbtree_map) map);
EPMEMoid rbtree_map_remove(PMEMobjpool *pop, TOID(struct rbtree_map) map,
		PMEMoid key);
int rbtree_map_clear(PMEMobjpool *pop, TOID(struct rbtree_map) map);
TOID(struct tree_map_node) rbtree_map_get(PMEMobjpool *pop, TOID(struct rbtree_map) map,
		PMEMoid key);
int rbtree_map_lookup(PMEMobjpool *pop, TOID(struct rbtree_map) map,
		PMEMoid key);
int rbtree_map_foreach(PMEMobjpool *pop, TOID(struct rbtree_map) map,
	int (*cb)(PMEMoid key, PMEMoid value, void *arg), void *arg);
int rbtree_map_is_empty(PMEMobjpool *pop, TOID(struct rbtree_map) map);
int rbtree_map_size(PMEMobjpool *pop, TOID(struct rbtree_map) map);

#endif /* RBTREE_MAP_H */
