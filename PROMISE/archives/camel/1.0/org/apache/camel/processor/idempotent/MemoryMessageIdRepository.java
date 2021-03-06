package org.apache.camel.processor.idempotent;

import org.apache.camel.util.LRUCache;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * A memory based implementation of {@link MessageIdRepository}.
 * Care should be taken to use a suitable underlying {@link Map} to avoid this class being a memory leak
 *
 * @version $Revision: 1.1 $
 */
public class MemoryMessageIdRepository implements MessageIdRepository {
    private Map cache;

    /**
     * Creates a new MemoryMessageIdRepository with a memory based respository. <b>Warning</b> this
     * method should only really be used for testing as it will involve keeping all message IDs in RAM.
     */
    public static MessageIdRepository memoryMessageIdRepository() {
        return memoryMessageIdRepository(new HashMap());
    }

    /**
     * Creates a new MemoryMessageIdRepository with a memory based respository. <b>Warning</b> this
     * method should only really be used for testing as it will involve keeping all message IDs in RAM.
     */
    public static MessageIdRepository memoryMessageIdRepository(int cacheSize) {
        return memoryMessageIdRepository(new LRUCache(cacheSize));
    }

    /**
     * Creates a new MemoryMessageIdRepository using the given {@link Map} to use to store the
     * processed Message ID objects. Warning be cafeful of the implementation of Map you use as
     * if you are not careful it could be a memory leak.
     */
    public static MessageIdRepository memoryMessageIdRepository(Map cache) {
        return new MemoryMessageIdRepository(cache);
    }

    public MemoryMessageIdRepository(Map set) {
        this.cache = set;
    }

    public boolean contains(String messageId) {
        synchronized (cache) {
            if (cache.containsKey(messageId)) {
                return true;
            }
            else {
                cache.put(messageId, messageId);
                return false;
            }
        }
    }
}
