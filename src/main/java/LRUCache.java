import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public class LRUCache {
    private static final int CACHE_CAPACITY = 40_000;
    private final Map<String, Map<String, Object>> cache;
    private final Function<String, String> readFunc;

    public LRUCache(Function<String, String> readFunc) {
        this.cache = new LinkedHashMap<String, Map<String, Object>>(CACHE_CAPACITY, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Map<String, Object>> eldest) {
                return size() > CACHE_CAPACITY; // Автоматичне видалення найстаріших записів при перевищенні ємності
            }
        };
        this.readFunc = readFunc;
    }

    public Map<String, Object> get(String token) {
        Map<String, Object> cachedtoken = cache.get(token);
        // System.out.println(cachedtoken);

        if (cachedtoken != null) {
            if (cachedtoken.containsKey("data")) {
                cachedtoken = deepStringifyKeys(cachedtoken);
            }

            cache.put(token, cachedtoken); // Оновлення позиції для LRU
            Map<String, Object> output = new LinkedHashMap<>();

            Map<String, Object> metadata = (Map<String, Object>) cachedtoken.get("metadata");
            // if (metadata != null) {
            //     System.out.println(metadata.containsKey("silence_read"));
            // }

            if (metadata != null && metadata.containsKey("ttl")) {
                long ttl = ((Instant) metadata.get("ttl")).getEpochSecond() - Instant.now().getEpochSecond();
                if (ttl <= 0) {
                    cache.remove(token);
                    return null;
                }

                output.put("metadata", new LinkedHashMap<>());
                ((Map<String, Object>) output.get("metadata")).put("ttl", ttl);
            }

            if (metadata != null && metadata.containsKey("silence_read")) {
                long silence_read = (long) metadata.get("silence_read") - 1;
                output.putIfAbsent("metadata", new LinkedHashMap<>());
                ((Map<String, Object>) output.get("metadata")).put("silence_read", silence_read);

                // System.out.println("silence_read: " + silence_read);
                // System.out.println(output);

                if (silence_read <= 0) {
                    cache.remove(token);
                } else {
                  ((Map<String, Object>) cachedtoken.get("metadata")).put("silence_read", silence_read);
                }
                // System.out.println(readFunc.apply(token));
                // System.out.println(token);
                // String result = this.readFunc.apply(token);
                // System.out.println(result);
                readFunc.apply(token);
            }

            output.put("data", cachedtoken.get("data"));
            // System.out.println(output);
            return output;
        }
        return null;
    }

    public void insert(String key, Object token, long ttl, long silence_read) {
        Map<String, Object> hash = new LinkedHashMap<>();
        hash.put("data", token);

        if (ttl > 0) {
            hash.putIfAbsent("metadata", new LinkedHashMap<>());
            ((Map<String, Object>) hash.get("metadata")).put("ttl", Instant.now().plusSeconds(ttl));
        }

        if (silence_read > 0) {
            hash.putIfAbsent("metadata", new LinkedHashMap<>());
            ((Map<String, Object>) hash.get("metadata")).put("silence_read", silence_read);
        }

        cache.put(key, hash);
    }

    public void forceInsert(String key, Map<String, Object> hash) {
        cache.put(key, hash);
    }

    public void delete(String key) {
        cache.remove(key);
    }

    private Map<String, Object> deepStringifyKeys(Map<String, Object> hash) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : hash.entrySet()) {
            String newKey = entry.getKey().toString();
            Object newtoken = (entry.getValue() instanceof Map) ?
                    deepStringifyKeys((Map<String, Object>) entry.getValue()) : entry.getValue();
            result.put(newKey, newtoken);
        }
        return result;
    }
}
