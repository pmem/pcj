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

#include "rbtree_map.h"
#include "hashmap_tx.h"
#include <errno.h>
#include <unistd.h>
#include <fcntl.h>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <jni.h>

#define OFFSET_TO_TOID(_toid, _o)\
    PMEMoid bb_oid = {get_uuid_lo(), (uint64_t)_o};\
    TOID_ASSIGN(_toid, bb_oid);\

struct root_struct {
    TOID(struct rbtree_map) root_map;
    TOID(struct hashmap_tx) vm_offsets;
    uint64_t newest_obj_off;
    PMEMmutex obj_list_mutex;
};

extern PMEMobjpool* pool;
extern TOID(struct root_struct) root;
extern TOID(struct hashmap_tx) vm_offsets;

POBJ_LAYOUT_BEGIN(persistent_heap);
POBJ_LAYOUT_ROOT(persistent_heap, struct root_struct);
POBJ_LAYOUT_END(persistent_heap);

PMEMobjpool* get_or_create_pool();
TOID(struct root_struct) get_root();
uint64_t get_uuid_lo();
int search_through_pool(long offset);
void create_root(TOID(struct root_struct) rt);
int add_to_vm_offsets(uint64_t offset);
int remove_from_vm_offsets(uint64_t offset);
int decrement_from_vm_offsets(uint64_t offset);
int decrement_from_obj_ref_counts(uint64_t key, uint64_t value, void* arg);
int walk_through_vm_offsets();
int inc_ref(PMEMoid oid, int amount);
int dec_ref(PMEMoid oid, int amount);
int delete_struct(PMEMoid oid);
void add_candidate(PMEMoid oid);
int add_to_obj_list(PMEMoid oid);
int delete_from_obj_list(PMEMoid oid);
void walk_through_all_objects();
void collect();
void mark_candidates();
void scan_candidates();
void collect_candidates();
void mark_gray(PMEMoid oid);
void scan(PMEMoid oid);
void scan_black(PMEMoid oid);
void collect_white(PMEMoid oid);
