package com.uditagarwal.cache;

import com.uditagarwal.cache.exceptions.NotFoundException;
import com.uditagarwal.cache.exceptions.StorageFullException;
import com.uditagarwal.cache.policies.EvictionPolicy;
import com.uditagarwal.cache.storage.Storage;

/**
 * The skeleton class that implements elementary behaviour for a cache
 * Accepts an EvictionPolicy & Storage in it's constructor for following purposes
 * - EvictionPolicy: determines the logic by which victim keys are determined for evicting when there's no more space
 *                   left to put more K-V pairs
 * - Storage: Provides an interface to an actual storage layer such as memory or network in which the data (K-V) pairs
 *            of cache is held
 * @param <Key> key datatype
 * @param <Value> value datatype
 */
public class Cache<Key, Value> {

    private final EvictionPolicy<Key> evictionPolicy;
    private final Storage<Key, Value> storage;

    /**
     * Creates an object of Cache<K, V> class
     * @param evictionPolicy The policy enforced for evicting keys when there is no capacity left to add more K-V pairs
     * @param storage The storage layer used for actually persisting K-V pairs
     */
    public Cache(EvictionPolicy<Key> evictionPolicy, Storage<Key, Value> storage) {
        this.evictionPolicy = evictionPolicy;
        this.storage = storage;
    }

    public void put(Key key, Value value) {
        try {
            // add K-V pair to storage
            this.storage.add(key, value);
            // and inform the evictionPolicy about keyAccess (to update it's internal key-acccess time ledger
            // for enforcing LRU eviction policy)
            this.evictionPolicy.keyAccessed(key);
        } catch (StorageFullException exception) {
            System.out.println("Got storage full. Will try to evict.");

            // determine the key to be removed (this method should be called 'getKeyToEvict')
            Key keyToRemove = evictionPolicy.evictKey();

            // if no key to remove, raise exception (logical error in the code / design or some external factor well
            // beyond scope of this simple cache problem)
            if (keyToRemove == null) {
                throw new RuntimeException("Unexpected State. Storage full and no key to evict.");
            }

            // make space by dropping the victim (least recently used) K-V pair
            System.out.println("Creating space by evicting item..." + keyToRemove);
            this.storage.remove(keyToRemove);

            // note that here we are NOT adding the K-V pair in cache directly
            // instead after having created the necessary space for key,
            // we recursively invoke this method again (neat!)
            put(key, value);
        }
    }

    public Value get(Key key) {
        try {
            // retrieve the value against this key
            Value value = this.storage.get(key);
            // and then inform the eviction policy to update it's key-access times ledger
            this.evictionPolicy.keyAccessed(key);

            //  return the value retrieved from cache against this key
            return value;
        } catch (NotFoundException notFoundException) {
            // in case the key doesn't exist, we log the error and return null
            System.out.println("Tried to access non-existing key.");
            return null;
        }
    }

}
