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

#ifndef HASHMAP_TX_H
#define HASHMAP_TX_H

#include <stddef.h>
#include <stdint.h>
#include <libpmemobj.h>
#include "hashmap.h"
#include "persistent_structs.h"

int hm_tx_check(PMEMobjpool *pop, TOID(struct hashmap_tx) hashmap);
int hm_tx_new(PMEMobjpool *pop, TOID(struct hashmap_tx) *map, void *arg);
int hm_tx_init(PMEMobjpool *pop, TOID(struct hashmap_tx) hashmap);
NEPMEMoid hm_tx_insert(PMEMobjpool *pop, TOID(struct hashmap_tx) hashmap,
		uint64_t key, uint64_t value);
NEPMEMoid hm_tx_remove(PMEMobjpool *pop, TOID(struct hashmap_tx) hashmap,
		uint64_t key);
NEPMEMoid hm_tx_get(PMEMobjpool *pop, TOID(struct hashmap_tx) hashmap,
		uint64_t key);
int hm_tx_lookup(PMEMobjpool *pop, TOID(struct hashmap_tx) hashmap,
		uint64_t key);
int hm_tx_foreach(PMEMobjpool *pop, TOID(struct hashmap_tx) hashmap,
	int (*cb)(uint64_t key, uint64_t value, void *arg), void *arg);
size_t hm_tx_count(PMEMobjpool *pop, TOID(struct hashmap_tx) hashmap);
int hm_tx_cmd(PMEMobjpool *pop, TOID(struct hashmap_tx) hashmap,
		unsigned cmd, uint64_t arg);
int hm_tx_clear(PMEMobjpool *pop, TOID(struct hashmap_tx) hashmap);

#endif /* HASHMAP_TX_H */
