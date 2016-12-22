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

/* integer hash set implementation which uses only transaction APIs */

#include <stdlib.h>
#include <stdio.h>
#include <errno.h>

#include <libpmemobj.h>
#include "hashmap_tx.h"
#include "hashmap_internal.h"

/*
 * create_hashmap -- hashmap initializer
 */
static void create_hashmap(PMEMobjpool *pop, TOID(struct hashmap_tx) hashmap,
                           int init_size, int resize_allowed, uint32_t seed)
{
	size_t len = init_size;
	size_t sz = sizeof(struct buckets) +
			len * sizeof(TOID(struct entry));

	TX_BEGIN(pop) {
		TX_ADD(hashmap);

		D_RW(hashmap)->seed = seed;
		D_RW(hashmap)->hash_fun_a =
				(uint32_t)(1000.0 * rand() / RAND_MAX) + 1;
		D_RW(hashmap)->hash_fun_b =
				(uint32_t)(100000.0 * rand() / RAND_MAX);
		D_RW(hashmap)->hash_fun_p = HASH_FUNC_COEFF_P;

		D_RW(hashmap)->buckets = TX_ZALLOC(struct buckets, sz);
		D_RW(D_RW(hashmap)->buckets)->nbuckets = len;

        D_RW(hashmap)->init_size = init_size;
        D_RW(hashmap)->resize_allowed = resize_allowed;
	} TX_ONABORT {
		fprintf(stderr, "%s: transaction aborted: %s\n", __func__,
			pmemobj_errormsg());
		abort();
	} TX_END
}

/*
 * hash -- the simplest hashing function,
 * see https://en.wikipedia.org/wiki/Universal_hashing#Hashing_integers
 */
static uint64_t hash
  (const TOID(struct hashmap_tx) *hashmap, const TOID(struct buckets) *buckets, uint64_t value)
{
	uint32_t a = D_RO(*hashmap)->hash_fun_a;
	uint32_t b = D_RO(*hashmap)->hash_fun_b;
	uint64_t p = D_RO(*hashmap)->hash_fun_p;
	size_t len = D_RO(*buckets)->nbuckets;

	return ((a * value + b) % p) % len;

    /*const uint64_t w_bit = 4294967295;
    const int A = 2654435769;
    uint64_t r0 = (value * A) ^ (value+1);;
    uint64_t ret = ((r0 & w_bit)) % len;
    //printf("Returning hash value %lu for input value %lu\n", ret, value);
    fflush(stdout);
    return ret;*/
}

/*
 * hm_tx_rebuild -- rebuilds the hashmap with a new number of buckets
 */
static void hm_tx_rebuild(PMEMobjpool *pop, TOID(struct hashmap_tx) hashmap, size_t new_len)
{
	TOID(struct buckets) buckets_old = D_RO(hashmap)->buckets;

	if (new_len == 0)
		new_len = D_RO(buckets_old)->nbuckets;

	size_t sz_old = sizeof(struct buckets) +
			D_RO(buckets_old)->nbuckets *
			sizeof(TOID(struct entry));
	size_t sz_new = sizeof(struct buckets) +
			new_len * sizeof(TOID(struct entry));

	TX_BEGIN(pop) {
		TX_ADD_FIELD(hashmap, buckets);
		TOID(struct buckets) buckets_new =
				TX_ZALLOC(struct buckets, sz_new);
		D_RW(buckets_new)->nbuckets = new_len;
		pmemobj_tx_add_range(buckets_old.oid, 0, sz_old);

		for (size_t i = 0; i < D_RO(buckets_old)->nbuckets; ++i) {
			while (!TOID_IS_NULL(D_RO(buckets_old)->bucket[i])) {
				TOID(struct entry) en =
					D_RO(buckets_old)->bucket[i];
				uint64_t h = hash(&hashmap, &buckets_new,
						D_RO(en)->key);

				D_RW(buckets_old)->bucket[i] = D_RO(en)->next;

				TX_ADD_FIELD(en, next);
				D_RW(en)->next = D_RO(buckets_new)->bucket[h];
				D_RW(buckets_new)->bucket[h] = en;
			}
		}

		D_RW(hashmap)->buckets = buckets_new;
        TX_MEMSET(pmemobj_direct(buckets_old.oid), 0, sizeof(struct buckets));
		TX_FREE(buckets_old);
	} TX_ONABORT {
		fprintf(stderr, "%s: transaction aborted: %s\n", __func__,
			pmemobj_errormsg());
		/*
		 * We don't need to do anything here, because everything is
		 * consistent. The only thing affected is performance.
		 */
        exit(-1);
	} TX_END

}

/*
 * hm_tx_insert -- inserts specified value into the hashmap,
 * returns old value with null/error as appropriate.
 */
NEPMEMoid hm_tx_insert
  (PMEMobjpool *pop, TOID(struct hashmap_tx) hashmap, uint64_t key, uint64_t value)
{
	TOID(struct buckets) buckets = D_RO(hashmap)->buckets;
	TOID(struct entry) var;

    NEPMEMoid ret = NEOID_NULL;

	uint64_t h = hash(&hashmap, &buckets, key);
	int num = 0;

	for (var = D_RO(buckets)->bucket[h];
			!TOID_IS_NULL(var);
			var = D_RO(var)->next) {
		if (D_RO(var)->key == key) {
            ret.null = 0;
            ret.value = D_RO(var)->value;
            D_RW(var)->value = value;
			return ret;
        }
		num++;
	}

	TX_BEGIN(pop) {
		TX_ADD_FIELD(D_RO(hashmap)->buckets, bucket[h]);
		TX_ADD_FIELD(hashmap, count);

		TOID(struct entry) e = TX_ZNEW(struct entry);
		D_RW(e)->key = key;
		D_RW(e)->value = value;
		D_RW(e)->next = D_RO(buckets)->bucket[h];
		D_RW(buckets)->bucket[h] = e;

		D_RW(hashmap)->count++;
		num++;
	} TX_ONABORT {
		fprintf(stderr, "transaction aborted in insert: %s\n",
			pmemobj_errormsg());
		ret = NEOID_ERR;
	} TX_END

	if (NEOID_IS_ERR(ret)) {
		return ret;
    }

	if (D_RO(hashmap)->resize_allowed == 1 &&
	    num > MAX_HASHSET_THRESHOLD ||
			(num > MIN_HASHSET_THRESHOLD &&
			D_RO(hashmap)->count > 2 * D_RO(buckets)->nbuckets)) {
		hm_tx_rebuild(pop, hashmap, D_RO(buckets)->nbuckets * 2);
    }

	return ret;
}

/*
 * hm_tx_remove -- removes specified value from the hashmap,
 * returns old value with null/error as appropriate.
 */
NEPMEMoid hm_tx_remove(PMEMobjpool *pop, TOID(struct hashmap_tx) hashmap, uint64_t key)
{
	TOID(struct buckets) buckets = D_RO(hashmap)->buckets;
	TOID(struct entry) var, prev = TOID_NULL(struct entry);

	uint64_t h = hash(&hashmap, &buckets, key);
	for (var = D_RO(buckets)->bucket[h];
			!TOID_IS_NULL(var);
			prev = var, var = D_RO(var)->next) {
		if (D_RO(var)->key == key)
			break;
	}

	if (TOID_IS_NULL(var))
		return NEOID_NULL;

	TX_BEGIN(pop) {
		if (TOID_IS_NULL(prev))
			TX_ADD_FIELD(D_RO(hashmap)->buckets, bucket[h]);
		else
			TX_ADD_FIELD(prev, next);
		TX_ADD_FIELD(hashmap, count);

		if (TOID_IS_NULL(prev))
			D_RW(buckets)->bucket[h] = D_RO(var)->next;
		else
			D_RW(prev)->next = D_RO(var)->next;
		D_RW(hashmap)->count--;
        TX_MEMSET(pmemobj_direct(var.oid), 0, sizeof(struct entry));
		TX_FREE(var);
	} TX_ONABORT {
		fprintf(stderr, "transaction aborted in remove: %s\n",
			pmemobj_errormsg());
		return NEOID_ERR;
	} TX_END

	if (D_RO(hashmap)->resize_allowed == 1 &&
        D_RO(hashmap)->count < D_RO(buckets)->nbuckets &&
        D_RO(buckets)->nbuckets > 2 * MAX_HASHSET_THRESHOLD) {
		hm_tx_rebuild(pop, hashmap, D_RO(buckets)->nbuckets / 2);
    }

    NEPMEMoid ret = {D_RO(var)->value, 0, 0};
	return ret;
}

/*
 * hm_tx_foreach -- prints all values from the hashmap
 */
int hm_tx_foreach
  (PMEMobjpool *pop, TOID(struct hashmap_tx) hashmap,
   int (*cb)(uint64_t key, uint64_t value, void *arg), void *arg)
{
	TOID(struct buckets) buckets = D_RO(hashmap)->buckets;
	TOID(struct entry) var;

	int ret = 0;
	for (size_t i = 0; i < D_RO(buckets)->nbuckets; ++i) {
		if (TOID_IS_NULL(D_RO(buckets)->bucket[i]))
			continue;

		for (var = D_RO(buckets)->bucket[i]; !TOID_IS_NULL(var);
				var = D_RO(var)->next) {
			ret = cb(D_RO(var)->key, D_RO(var)->value, arg);
			if (ret)
				break;
		}
	}

	return ret;
}

/*
 * hm_tx_clear -- clears the map but does not delete the map itself
 */
int hm_tx_clear(PMEMobjpool *pop, TOID(struct hashmap_tx) hashmap)
{
	TOID(struct buckets) buckets = D_RO(hashmap)->buckets;
	TOID(struct entry) cur, next;

    int ret = 0;
    TX_BEGIN(pop) {
        TX_ADD(hashmap);

        for (size_t i = 0; i < D_RO(buckets)->nbuckets; ++i) {
            if (TOID_IS_NULL(D_RO(buckets)->bucket[i]))
                continue;

            cur = D_RO(buckets)->bucket[i];
            do {
                next = D_RO(cur)->next;
                TX_MEMSET(pmemobj_direct(cur.oid), 0, sizeof(struct entry));
                TX_FREE(cur);
                cur = next;
            } while (!TOID_IS_NULL(cur));
        }

        TX_FREE(buckets);
        D_RW(hashmap)->count = 0;

        create_hashmap(pop, hashmap, D_RO(hashmap)->init_size, D_RO(hashmap)->resize_allowed, D_RO(hashmap)->seed);
    } TX_ONABORT {
        ret = 1;
    } TX_END

    return ret;
}

/*
 * hm_tx_delete -- clears the map and deletes the map itself
 */
int hm_tx_delete(PMEMobjpool *pop, TOID(struct hashmap_tx)* hashmap)
{
    int ret = 0;
    TX_BEGIN(pop) {
        hm_tx_clear(pop, *hashmap);
        TX_MEMSET(pmemobj_direct((*hashmap).oid), 0, sizeof(struct hashmap_tx));
        TX_FREE(*hashmap);
        *hashmap = TOID_NULL(struct hashmap_tx);
    } TX_ONABORT {
        ret = 1;
    } TX_END

    return ret;
}

/*
 * hm_tx_debug -- prints complete hashmap state
 */
static void hm_tx_debug(PMEMobjpool *pop, TOID(struct hashmap_tx) hashmap)
{
	TOID(struct buckets) buckets = D_RO(hashmap)->buckets;
	TOID(struct entry) var;

	fprintf(stdout, "a: %u b: %u p: %lu\n", D_RO(hashmap)->hash_fun_a,
		D_RO(hashmap)->hash_fun_b, D_RO(hashmap)->hash_fun_p);
	fprintf(stdout, "count: %lu, buckets: %lu\n", D_RO(hashmap)->count,
		D_RO(buckets)->nbuckets);

	for (size_t i = 0; i < D_RO(buckets)->nbuckets; ++i) {
		if (TOID_IS_NULL(D_RO(buckets)->bucket[i]))
			continue;

		int num = 0;
		fprintf(stdout, "%lu: ", i);
		for (var = D_RO(buckets)->bucket[i]; !TOID_IS_NULL(var);
				var = D_RO(var)->next) {
			fprintf(stdout, "%lu->%lu ", D_RO(var)->key, D_RO(var)->value);
			num++;
		}
		fprintf(stdout, "(%d)\n", num);
	}
    fflush(stdout);
}

/*
 * hm_tx_get -- gets specified value, possibly null
 */
NEPMEMoid hm_tx_get(PMEMobjpool *pop, TOID(struct hashmap_tx) hashmap, uint64_t key)
{
	TOID(struct buckets) buckets = D_RO(hashmap)->buckets;
	TOID(struct entry) var;

    NEPMEMoid ret = NEOID_NULL;

	uint64_t h = hash(&hashmap, &buckets, key);

	for (var = D_RO(buckets)->bucket[h];
			!TOID_IS_NULL(var);
			var = D_RO(var)->next)
		if (D_RO(var)->key == key) {
            ret.null = 0;
            ret.value = D_RO(var)->value;
            break;
        }

	return ret;
}

/*
 * hm_tx_lookup -- checks whether specified value exists
 */
int hm_tx_lookup(PMEMobjpool *pop, TOID(struct hashmap_tx) hashmap, uint64_t key)
{
	TOID(struct buckets) buckets = D_RO(hashmap)->buckets;
	TOID(struct entry) var;

	uint64_t h = hash(&hashmap, &buckets, key);

	for (var = D_RO(buckets)->bucket[h];
			!TOID_IS_NULL(var);
			var = D_RO(var)->next)
		if (D_RO(var)->key == key)
			return 1;

	return 0;
}

/*
 * hm_tx_count -- returns number of elements
 */
size_t hm_tx_count(PMEMobjpool *pop, TOID(struct hashmap_tx) hashmap)
{
	return D_RO(hashmap)->count;
}

/*
 * hm_tx_init -- recovers hashmap state, called after pmemobj_open
 */
int hm_tx_init(PMEMobjpool *pop, TOID(struct hashmap_tx) hashmap)
{
	srand(D_RO(hashmap)->seed);
	return 0;
}

/*
 * hm_tx_new -- allocates new hashmap
 */
int hm_tx_new(PMEMobjpool *pop, TOID(struct hashmap_tx) *map,
              int init_size, int resize_allowed, void *arg)
{
	struct hashmap_args *args = (struct hashmap_args*)arg;
	int ret = 0;
	TX_BEGIN(pop) {
		*map = TX_ZNEW(struct hashmap_tx);

		uint32_t seed = args ? args->seed : 0;
		create_hashmap(pop, *map, init_size, resize_allowed, seed);
	} TX_ONABORT {
		ret = -1;
	} TX_END

	return ret;
}

/*
 * hm_tx_check -- checks if specified persistent object is an
 * instance of hashmap
 */
int hm_tx_check(PMEMobjpool *pop, TOID(struct hashmap_tx) hashmap)
{
	return TOID_IS_NULL(hashmap) || !TOID_VALID(hashmap);
}

/*
 * hm_tx_cmd -- execute cmd for hashmap
 */
int hm_tx_cmd
  (PMEMobjpool *pop, TOID(struct hashmap_tx) hashmap, unsigned cmd, uint64_t arg)
{
	switch (cmd) {
		case HASHMAP_CMD_REBUILD:
			hm_tx_rebuild(pop, hashmap, arg);
			return 0;
		case HASHMAP_CMD_DEBUG:
			hm_tx_debug(pop, hashmap);
			return 0;
		default:
			return -EINVAL;
	}
}
