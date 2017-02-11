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

/*
 * rbtree.c -- red-black tree implementation /w sentinel nodes
 */

#include <assert.h>
#include <errno.h>
#include <stdio.h>
#include "rbtree_map.h"
#include "hashmap_tx.h"
#include "persistent_byte_buffer.h"
#include "persistent_byte_array.h"
#include "persistent_long.h"
#include "persistent_heap.h"
#include "util.h"

#define NODE_P(_n)\
D_RW(_n)->parent

#define NODE_GRANDP(_n)\
NODE_P(NODE_P(_n))

#define NODE_PARENT_AT(_n, _rbc)\
D_RW(NODE_P(_n))->slots[_rbc]

#define NODE_PARENT_RIGHT(_n)\
NODE_PARENT_AT(_n, RB_RIGHT)

#define NODE_IS(_n, _rbc)\
TOID_EQUALS(_n, NODE_PARENT_AT(_n, _rbc))

#define NODE_IS_RIGHT(_n)\
TOID_EQUALS(_n, NODE_PARENT_RIGHT(_n))

#define NODE_LOCATION(_n)\
NODE_IS_RIGHT(_n)

#define RB_FIRST(_m)\
D_RW(D_RW(_m)->root)->slots[RB_LEFT]

#define NODE_IS_NULL(_n)\
TOID_EQUALS(_n, s)

/*
 * rbtree_key_compare -- compares two keys, which are really just byte buffers
 * returns 0 if equal
 * returns -1 if first < second
 * returns 1 if first > second
 */
static int rbtree_key_compare(PMEMoid first, PMEMoid second) {
    TOID(struct header) *hdr1 = (TOID(struct header)*)(pmemobj_direct(first));
    TOID(struct header) *hdr2 = (TOID(struct header)*)(pmemobj_direct(second));

    if (D_RO(*hdr1)->type != D_RO(*hdr2)->type) {
        printf("Comparing two keys of different types: %d vs. %d!\n", D_RO(*hdr1)->type, D_RO(*hdr2)->type);
        exit(-1);
    }

    int ret;
    switch(D_RO(*hdr1)->type) {
        case PERSISTENT_BYTE_BUFFER_TYPE_OFFSET:
            ret = compare_persistent_bytebuffers(first.off, second.off);
            break;
        case PERSISTENT_BYTE_ARRAY_TYPE_OFFSET:
            ret = compare_persistent_bytearrays(first.off, second.off);
            break;
        case PERSISTENT_LONG_TYPE_OFFSET:
            ret = compare_persistent_longs(first.off, second.off);
            break;
        case AGGREGATE_TYPE_OFFSET:
            ret = call_comparator(first, second);
            break;
        default:
            printf("Does not know how to compare keys of type %d!\n", D_RO(*hdr1)->type);
            exit(-1);
    }

    return ret;
}

/*
 * rbtree_map_new -- allocates a new red-black tree instance
 */
int rbtree_map_new(PMEMobjpool *pop, TOID(struct rbtree_map) *map, char* class_name, void *arg)
{
    int ret = 0;
    TX_BEGIN(pop) {
        *map = TX_ZNEW(struct rbtree_map);

        TOID(struct tree_map_node) s = TX_ZNEW(struct tree_map_node);
        D_RW(s)->color = COLOR_BLACK;
        D_RW(s)->parent = s;
        D_RW(s)->slots[RB_LEFT] = s;
        D_RW(s)->slots[RB_RIGHT] = s;

        TOID(struct tree_map_node) r = TX_ZNEW(struct tree_map_node);
        D_RW(r)->color = COLOR_BLACK;
        D_RW(r)->parent = s;
        D_RW(r)->slots[RB_LEFT] = s;
        D_RW(r)->slots[RB_RIGHT] = s;

        TOID(struct header) h = TX_ZNEW(struct header);
        D_RW(*map)->header = h;
        D_RW(*map)->sentinel = s;
        D_RW(*map)->root = r;
        D_RW(*map)->size = 0;

        D_RW(D_RW(*map)->header)->refCount = 1;
        D_RW(D_RW(*map)->header)->type = TOID_TYPE_NUM(struct rbtree_map);
        D_RW(D_RW(*map)->header)->fieldCount = RBTREE_MAP_FIELD_COUNT;
        D_RW(D_RW(*map)->header)->color = BLACK;

        if (class_name == NULL) {
            D_RW(D_RW(*map)->header)->class_name = TOID_NULL(char);
        } else {
            TOID(char) ptm_class_name = TX_ZALLOC(char, strlen(class_name));
            TX_MEMCPY(pmemobj_direct(ptm_class_name.oid), class_name, strlen(class_name));
            D_RW(D_RW(*map)->header)->class_name = ptm_class_name;
        }
    } TX_ONABORT {
        printf("rbtree_map_new aborted\n");
        fflush(stdout);
        ret = 1;
    } TX_END

    return ret;
}

/*
 * rbtree_map_clear_node -- (internal) clears this node and its children
 */
static void rbtree_map_clear_node(TOID(struct rbtree_map) map, TOID(struct tree_map_node) p,
                                  TOID(struct hashmap_tx) locks)
{
    TOID(struct tree_map_node) s = D_RO(map)->sentinel;

    if (!NODE_IS_NULL(D_RO(p)->slots[RB_LEFT]))
        rbtree_map_clear_node(map, D_RO(p)->slots[RB_LEFT], locks);

    if (!NODE_IS_NULL(D_RO(p)->slots[RB_RIGHT]))
        rbtree_map_clear_node(map, D_RO(p)->slots[RB_RIGHT], locks);

    if (!(OID_IS_NULL(D_RO(p)->key))) {
        lock_objs(locks, D_RO(p)->key, -1);
        dec_ref(D_RW(p)->key, 1, 1);
    }
    if (!(OID_IS_NULL(D_RO(p)->value))) {
        lock_objs(locks, D_RO(p)->value, -1);
        dec_ref(D_RW(p)->value, 1, 1);
    }

    TX_FREE(p);
    TX_MEMSET(pmemobj_direct(p.oid), 0, sizeof(struct tree_map_node));
}

/*
 * rbtree_map_clear -- removes all elements from the map
 */
int rbtree_map_clear(PMEMobjpool *pop, TOID(struct rbtree_map) map)
{
    int ret = 0;
    if (D_RO(map)->size != 0) {
        TX_BEGIN(pop) {
            PMEMoid locks_entry_oid = POBJ_LIST_INSERT_NEW_HEAD(pop, &D_RW(root)->locks, list, sizeof(struct locks_entry), create_locks_entry, (void*)(&D_RO(map)->size));
            TOID(struct locks_entry) locks;
            TOID_ASSIGN(locks, locks_entry_oid);
            rbtree_map_clear_node(map, D_RW(map)->root, D_RO(locks)->locks);
            TX_ADD_FIELD(map, root);
            TX_ADD_FIELD(map, sentinel);

            TX_FREE(D_RW(map)->sentinel);
            TX_MEMSET(pmemobj_direct((D_RW(map)->sentinel).oid), 0, sizeof(struct tree_map_node));

            TOID(struct tree_map_node) s = TX_ZNEW(struct tree_map_node);
            D_RW(s)->color = COLOR_BLACK;
            D_RW(s)->parent = s;
            D_RW(s)->slots[RB_LEFT] = s;
            D_RW(s)->slots[RB_RIGHT] = s;

            TOID(struct tree_map_node) r = TX_ZNEW(struct tree_map_node);
            D_RW(r)->color = COLOR_BLACK;
            D_RW(r)->parent = s;
            D_RW(r)->slots[RB_LEFT] = s;
            D_RW(r)->slots[RB_RIGHT] = s;

            D_RW(map)->sentinel = s;
            D_RW(map)->root = r;
            D_RW(map)->size = 0;

            hm_tx_foreach(pop, D_RO(locks)->locks, unlock_from_locks, NULL);
            hm_tx_delete(pop, &D_RW(locks)->locks);
            POBJ_LIST_REMOVE_FREE(pop, &D_RW(root)->locks, locks, list);
        } TX_ONABORT {
            printf("rbtree_map_clear aborted\n");
            fflush(stdout);
            ret = 1;
        } TX_END
    }

    return ret;
}


/*
 * rbtree_map_delete -- cleanups and frees red-black tree instance
 */
int rbtree_map_delete(PMEMobjpool *pop, TOID(struct rbtree_map) *map)
{
    int ret = 0;
    TX_BEGIN(pop) {
        TX_ADD(*map);
        rbtree_map_clear(pop, *map);
        free_header((*map).oid);
        TX_MEMSET(pmemobj_direct((*map).oid), 0, sizeof(struct rbtree_map));
        TX_FREE(*map);
        *map = TOID_NULL(struct rbtree_map);
    } TX_ONABORT {
        ret = 1;
    } TX_END

    return ret;
}

/*
 * rbtree_map_rotate -- (internal) performs a left/right rotation around a node
 */
static void rbtree_map_rotate
  (TOID(struct rbtree_map) map, TOID(struct tree_map_node) node, enum rb_children c)
{
    TOID(struct tree_map_node) child = D_RO(node)->slots[!c];
    TOID(struct tree_map_node) s = D_RO(map)->sentinel;

    TX_ADD(node);
    TX_ADD(child);

    D_RW(node)->slots[!c] = D_RO(child)->slots[c];

    if (!TOID_EQUALS(D_RO(child)->slots[c], s))
        TX_SET(D_RW(child)->slots[c], parent, node);

    NODE_P(child) = NODE_P(node);

    TX_SET(NODE_P(node), slots[NODE_LOCATION(node)], child);

    D_RW(child)->slots[c] = node;
    D_RW(node)->parent = child;
}

/*
 * rbtree_map_insert_bst -- (internal) inserts a node in regular BST fashion
 */
static EPMEMoid rbtree_map_insert_bst(TOID(struct rbtree_map) map, TOID(struct tree_map_node) n)
{
    TOID(struct tree_map_node) parent = D_RO(map)->root;
    TOID(struct tree_map_node) *dst = &RB_FIRST(map);
    TOID(struct tree_map_node) s = D_RO(map)->sentinel;

    EPMEMoid ret = EOID_NULL;

    D_RW(n)->slots[RB_LEFT] = s;
    D_RW(n)->slots[RB_RIGHT] = s;

    int key_comp;

    while (!NODE_IS_NULL(*dst)) {
        key_comp = rbtree_key_compare(D_RO(n)->key, D_RO(*dst)->key);
        if (key_comp == 0) {  // current node key matches
            break;
        } else {
            parent = *dst;
            // key_comp > 0: n > dst, go to the right of dst
            // key_comp < 0: n < dst, go to the left of dst
            dst = &D_RW(*dst)->slots[(key_comp > 0) ? RB_RIGHT : RB_LEFT];
        }
    }

    // By this point dst is where the insertion should be, and it could be null
    // Return dst's current value (null if dst is null)

    if (!NODE_IS_NULL(*dst)) {  // non-null dst has the key
        ret.oid = D_RO(*dst)->value;
        pmemobj_tx_add_range_direct(dst, sizeof (*dst));
        D_RW(*dst)->value = D_RO(n)->value;
        TX_FREE(n);
        TX_MEMSET(pmemobj_direct(n.oid), 0, sizeof(struct tree_map_node));
    } else {       // dst is null, replace wholesale
        TX_ADD_FIELD(map, size);
        D_RW(map)->size++;
        TX_SET(n, parent, parent);
        pmemobj_tx_add_range_direct(dst, sizeof (*dst));
        *dst = n;
    }

    return ret;
}

/*
 * rbtree_map_recolor -- (internal) restores red-black tree properties
 */
static TOID(struct tree_map_node) rbtree_map_recolor
  (TOID(struct rbtree_map) map, TOID(struct tree_map_node) n, enum rb_children c)
{
    TOID(struct tree_map_node) uncle = D_RO(NODE_GRANDP(n))->slots[!c];

    if (D_RO(uncle)->color == COLOR_RED) {
        TX_SET(uncle, color, COLOR_BLACK);
        TX_SET(NODE_P(n), color, COLOR_BLACK);
        TX_SET(NODE_GRANDP(n), color, COLOR_RED);
        return NODE_GRANDP(n);
    } else {
        if (NODE_IS(n, !c)) {
            n = NODE_P(n);
            rbtree_map_rotate(map, n, c);
        }

        TX_SET(NODE_P(n), color, COLOR_BLACK);
        TX_SET(NODE_GRANDP(n), color, COLOR_RED);
        rbtree_map_rotate(map, NODE_GRANDP(n), c == RB_LEFT ? RB_RIGHT : RB_LEFT);
    }

    return n;
}

/*
 * rbtree_map_insert -- inserts a new key-value pair into the map
 */
EPMEMoid rbtree_map_insert
  (PMEMobjpool *pop, TOID(struct rbtree_map) map, PMEMoid key, PMEMoid value)
{
    EPMEMoid ret = EOID_NULL;

    TX_BEGIN(pop) {
        TOID(struct tree_map_node) n = TX_ZNEW(struct tree_map_node);
        D_RW(n)->key = key;
        D_RW(n)->value = value;

        ret = rbtree_map_insert_bst(map, n);
        if (OID_IS_NULL(ret.oid)) {    // returned null, so this was a new insertion

            D_RW(n)->color = COLOR_RED;
            while (D_RO(NODE_P(n))->color == COLOR_RED) {
                n = rbtree_map_recolor(map, n,
                        NODE_LOCATION(NODE_P(n)) == 1 ? RB_RIGHT : RB_LEFT);
            }

            TX_SET(RB_FIRST(map), color, COLOR_BLACK);
        }
    } TX_ONABORT {
        printf("rbtree_map_insert aborted\n");
        fflush(stdout);
        ret = EOID_ERR;    // special EPMEMoid with error code
    } TX_END

    return ret;
}

/*
 * rbtree_map_successor -- returns the successor of a node
 */
TOID(struct tree_map_node) rbtree_map_successor
  (TOID(struct rbtree_map) map, TOID(struct tree_map_node) n)
{
    TOID(struct tree_map_node) dst = D_RO(n)->slots[RB_RIGHT];
    TOID(struct tree_map_node) s = D_RO(map)->sentinel;

    if (!TOID_EQUALS(s, dst)) {
        while (!TOID_EQUALS(s, D_RO(dst)->slots[RB_LEFT]))
            dst = D_RO(dst)->slots[RB_LEFT];
    } else {
        dst = D_RO(n)->parent;
        while (TOID_EQUALS(n, D_RO(dst)->slots[RB_RIGHT])) {
            n = dst;
            dst = NODE_P(dst);
        }
        if (TOID_EQUALS(dst, D_RO(map)->root))
            return TOID_NULL(struct tree_map_node);
    }

    if (!(TOID_EQUALS(s, dst))) {
        return dst;
    } else {
        return TOID_NULL(struct tree_map_node);
    }
}

/*
 * rbtree_map_predecessor -- returns the predecessor of a node
 */
TOID(struct tree_map_node) rbtree_map_predecessor
  (TOID(struct rbtree_map) map, TOID(struct tree_map_node) n)
{
    TOID(struct tree_map_node) dst = D_RO(n)->slots[RB_LEFT];
    TOID(struct tree_map_node) s = D_RO(map)->sentinel;

    if (!TOID_EQUALS(s, dst)) {
        while (!TOID_EQUALS(s, D_RO(dst)->slots[RB_RIGHT]))
            dst = D_RO(dst)->slots[RB_RIGHT];
    } else {
        dst = D_RO(n)->parent;
        while (TOID_EQUALS(n, D_RO(dst)->slots[RB_LEFT])) {
            n = dst;
            dst = NODE_P(dst);
        }
        if (TOID_EQUALS(dst, D_RO(map)->root))
            return TOID_NULL(struct tree_map_node);
    }

    if (!(TOID_EQUALS(s, dst))) {
        return dst;
    } else {
        return TOID_NULL(struct tree_map_node);
    }
}

/*
 * rbtree_map_first_element -- returns the first (smallest) element of a node
 */
TOID(struct tree_map_node) rbtree_map_first_element(TOID(struct rbtree_map) map)
{
    if (D_RO(map)->size == 0) return TOID_NULL(struct tree_map_node);

    TOID(struct tree_map_node) cur = RB_FIRST(map);

    // while predecessor is not null, get the predecessor
    while (!(TOID_IS_NULL(rbtree_map_predecessor(map, cur)))) {
        cur = rbtree_map_predecessor(map, cur);
    }

    return cur;
}

/*
 * rbtree_map_last_element -- returns the last (largest) element of a node
 */
TOID(struct tree_map_node) rbtree_map_last_element(TOID(struct rbtree_map) map)
{
    if (D_RO(map)->size == 0) return TOID_NULL(struct tree_map_node);

    TOID(struct tree_map_node) cur = RB_FIRST(map);

    // while successor is not null, get the successor
    while (!(TOID_IS_NULL(rbtree_map_successor(map, cur)))) {
        cur = rbtree_map_successor(map, cur);
    }

    return cur;
}

/*
 * rbtree_map_find_node -- (internal) returns the node that contains the key
 */
static TOID(struct tree_map_node) rbtree_map_find_node
  (TOID(struct rbtree_map) map, PMEMoid key)
{
    TOID(struct tree_map_node) dst = RB_FIRST(map);
    TOID(struct tree_map_node) s = D_RO(map)->sentinel;

    while (!NODE_IS_NULL(dst)) {
        if (rbtree_key_compare(D_RO(dst)->key, key) == 0)
            return dst;

        int key_comp = rbtree_key_compare(key, D_RO(dst)->key);
        dst = D_RO(dst)->slots[(key_comp > 0) ? RB_RIGHT : RB_LEFT];
    }

    return TOID_NULL(struct tree_map_node);
}

/*
 * rbtree_map_repair_branch -- (internal) restores red-black tree in one branch
 */
static TOID(struct tree_map_node) rbtree_map_repair_branch
  (TOID(struct rbtree_map) map, TOID(struct tree_map_node) n, enum rb_children c)
{
    TOID(struct tree_map_node) sb = NODE_PARENT_AT(n, !c); /* sibling */
    if (D_RO(sb)->color == COLOR_RED) {
        TX_SET(sb, color, COLOR_BLACK);
        TX_SET(NODE_P(n), color, COLOR_RED);
        rbtree_map_rotate(map, NODE_P(n), c);
        sb = NODE_PARENT_AT(n, !c);
    }

    if (D_RO(D_RO(sb)->slots[RB_RIGHT])->color == COLOR_BLACK &&
        D_RO(D_RO(sb)->slots[RB_LEFT])->color == COLOR_BLACK) {
        TX_SET(sb, color, COLOR_RED);
        return D_RO(n)->parent;
    } else {
        if (D_RO(D_RO(sb)->slots[!c])->color == COLOR_BLACK) {
            TX_SET(D_RW(sb)->slots[c], color, COLOR_BLACK);
            TX_SET(sb, color, COLOR_RED);
            rbtree_map_rotate(map, sb, c == RB_LEFT ? RB_RIGHT : RB_LEFT);
            sb = NODE_PARENT_AT(n, !c);
        }
        TX_SET(sb, color, D_RO(NODE_P(n))->color);
        TX_SET(NODE_P(n), color, COLOR_BLACK);
        TX_SET(D_RW(sb)->slots[!c], color, COLOR_BLACK);
        rbtree_map_rotate(map, NODE_P(n), c);

        return RB_FIRST(map);
    }

    return n;
}

/*
 * rbtree_map_repair -- (internal) restores red-black tree properties
 * after remove
 */
static void rbtree_map_repair(TOID(struct rbtree_map) map, TOID(struct tree_map_node) n)
{
    /* if left, repair right sibling, otherwise repair left sibling. */
    while (!TOID_EQUALS(n, RB_FIRST(map)) && D_RO(n)->color == COLOR_BLACK)
        n = rbtree_map_repair_branch(map, n, NODE_LOCATION(n) == 1 ? RB_RIGHT : RB_LEFT);

    TX_SET(n, color, COLOR_BLACK);
}

/*
 * rbtree_map_remove -- removes key-value pair from the map
 */
EPMEMoid rbtree_map_remove(PMEMobjpool *pop, TOID(struct rbtree_map) map, PMEMoid key)
{
    EPMEMoid ret = EOID_NULL;

    TOID(struct tree_map_node) n = rbtree_map_find_node(map, key);
    if (TOID_IS_NULL(n))
        return ret;

    ret.oid = D_RO(n)->value;

    TOID(struct tree_map_node) s = D_RO(map)->sentinel;
    TOID(struct tree_map_node) r = D_RO(map)->root;

    TOID(struct tree_map_node) y = (NODE_IS_NULL(D_RO(n)->slots[RB_LEFT]) ||
                    NODE_IS_NULL(D_RO(n)->slots[RB_RIGHT]))
                    ? n : (TOID_IS_NULL(rbtree_map_successor(map, n)) ? s : rbtree_map_successor(map, n));

    TOID(struct tree_map_node) x = NODE_IS_NULL(D_RO(y)->slots[RB_LEFT]) ?
            D_RO(y)->slots[RB_RIGHT] : D_RO(y)->slots[RB_LEFT];

    TX_BEGIN(pop) {
        TX_SET(x, parent, NODE_P(y));
        if (TOID_EQUALS(NODE_P(x), r)) {
            TX_SET(r, slots[RB_LEFT], x);
        } else {
            TX_SET(NODE_P(y), slots[NODE_LOCATION(y)], x);
        }

        if (D_RO(y)->color == COLOR_BLACK)
            rbtree_map_repair(map, x);

        if (!TOID_EQUALS(y, n)) {
            TX_ADD(y);
            D_RW(y)->slots[RB_LEFT] = D_RO(n)->slots[RB_LEFT];
            D_RW(y)->slots[RB_RIGHT] = D_RO(n)->slots[RB_RIGHT];
            D_RW(y)->parent = D_RO(n)->parent;
            D_RW(y)->color = D_RO(n)->color;
            TX_SET(D_RW(n)->slots[RB_LEFT], parent, y);
            TX_SET(D_RW(n)->slots[RB_RIGHT], parent, y);

            TX_SET(NODE_P(n), slots[NODE_LOCATION(n)], y);
        }
        TX_FREE(n);
        TX_MEMSET(pmemobj_direct(n.oid), 0, sizeof(struct tree_map_node));

        TX_ADD_FIELD(map, size);
        D_RW(map)->size--;
    } TX_ONABORT {
        printf("rbtree_map_remove aborted\n");
        fflush(stdout);
        ret = EOID_ERR;    // special EPMEMoid with error code
    } TX_END

    return ret;
}

/*
 * rbtree_map_get -- searches for a value of the key
 */
TOID(struct tree_map_node) rbtree_map_get
  (PMEMobjpool *pop, TOID(struct rbtree_map) map, PMEMoid key)
{
    return rbtree_map_find_node(map, key);
}

/*
 * rbtree_map_lookup -- searches if key exists
 */
int rbtree_map_lookup(PMEMobjpool *pop, TOID(struct rbtree_map) map, PMEMoid key)
{
    TOID(struct tree_map_node) node = rbtree_map_find_node(map, key);
    if (TOID_IS_NULL(node))
        return 0;

    return 1;
}

/*
 * rbtree_map_foreach_node -- (internal) recursively traverses tree
 */
static int rbtree_map_foreach_node
  (TOID(struct rbtree_map) map, TOID(struct tree_map_node) p,
   int (*cb)(PMEMoid key, PMEMoid value, void *arg), void *arg)
{
    int ret = 0;

    if (TOID_EQUALS(p, D_RO(map)->sentinel))
        return 0;

    if ((ret = rbtree_map_foreach_node(map,
        D_RO(p)->slots[RB_LEFT], cb, arg)) == 0) {
        if ((ret = cb(D_RO(p)->key, D_RO(p)->value, arg)) == 0)
            rbtree_map_foreach_node(map,
                D_RO(p)->slots[RB_RIGHT], cb, arg);
    }

    return ret;
}

/*
 * rbtree_map_foreach -- initiates recursive traversal
 */
int rbtree_map_foreach
  (PMEMobjpool *pop, TOID(struct rbtree_map) map,
   int (*cb)(PMEMoid key, PMEMoid value, void *arg), void *arg)
{
    return rbtree_map_foreach_node(map, RB_FIRST(map), cb, arg);
}

/*
 * rbtree_map_is_empty -- checks whether the tree map is empty
 */
int rbtree_map_is_empty(PMEMobjpool *pop, TOID(struct rbtree_map) map)
{
    return TOID_IS_NULL(RB_FIRST(map));
}

/*
 * rbtree_map_check -- check if given persistent object is a tree map
 */
int rbtree_map_check(PMEMobjpool *pop, TOID(struct rbtree_map) map)
{
    return TOID_IS_NULL(map) || !TOID_VALID(map);
}

/*
 * rbtree_map_size -- returns the current size of the tree
 */
int rbtree_map_size(PMEMobjpool *pop, TOID(struct rbtree_map) map)
{
    return D_RO(map)->size;
}
