// This binding was generated automatically to ensure consistency across languages
// Generated using ChatGPT (GPT-5) from the canonical Ruby SDK
// API is stable and production-ready

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.TreeMap;
import java.time.Instant;

import java.util.ArrayList;
import java.util.List;


import java.io.IOException;

import java.util.Objects;

public class TestCRUDJT {
    public static final int REQUESTS = 40_000;
    private static final int READY_FOR_CACHE = 2;

    public static Map<String, Object> sortMapRecursively(Map<String, Object> map) {
        TreeMap<String, Object> sortedMap = new TreeMap<>();

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();

            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                sortedMap.put(entry.getKey(), sortMapRecursively(nestedMap));
            } else {
                sortedMap.put(entry.getKey(), value);
            }
        }

        return sortedMap;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        CRUDJT.Config.startMaster(
            Map.of(
                "encrypted_key", "Cm7B68NWsMNNYjzMDREacmpe5sI1o0g40ZC9w1yQW3WOes7Gm59UsittLOHR2dciYiwmaYq98l3tG8h9yXVCxg=="
            )
        );

        System.out.println("OS: " + System.getProperty("os.name"));
        System.out.println("CPU: " + System.getProperty("os.arch"));

        // without metadata
        System.out.println("Checking without metadata...");
        Map<String, Object> data = new HashMap<>();
        data.put("user_id", 42);
        data.put("role", 11);
        Map<String, Object> expectedData = new HashMap<>();
        expectedData.put("data", data);

        Map<String, Object> edData = new HashMap<>();
        edData.put("user_id", 42);
        edData.put("role", 8);
        Map<String, Object> expectedEdData = new HashMap<>();
        expectedEdData.put("data", edData);

        String token = CRUDJT.create(data, -1, -1);

        System.out.println(CRUDJT.read(token).toString().equals(expectedData.toString()));
        System.out.println(CRUDJT.update(token, edData, -1, -1));
        System.out.println(CRUDJT.read(token).toString().equals(expectedEdData.toString()));
        System.out.println(CRUDJT.delete(token));
        System.out.println(CRUDJT.read(token) == null);

        // with ttl
        System.out.println("Checking ttl...");
        long ttl = 5;
        String tokenWithttl = CRUDJT.create(data, ttl, -1);

        long expectedttl = ttl;
        for (int i = 0; i < ttl; i++) {
            System.out.println(new TreeMap<>(CRUDJT.read(tokenWithttl)).toString().equals(
                    new TreeMap<>(Map.of("metadata", Map.of("ttl", expectedttl), "data", data)).toString()));
            expectedttl--;
            TimeUnit.SECONDS.sleep(1);
        }
        System.out.println(CRUDJT.read(tokenWithttl) == null);

        // when expired ttl
        System.out.println("when expired ttl");
        ttl = 1;
        token = CRUDJT.create(data, ttl, -1);
        TimeUnit.SECONDS.sleep(ttl);
        System.out.println(CRUDJT.read(token) == null);
        System.out.println(!CRUDJT.update(token, data, -1, -1));
        System.out.println(!CRUDJT.delete(token));
        System.out.println(!CRUDJT.update(token, data, -1, -1));
        System.out.println(CRUDJT.read(token) == null);

        // with silence_read
        System.out.println("Checking silence_read...");
        long silence_read = 6;
        String tokenWithsilence_read = CRUDJT.create(data, -1, silence_read);

        long expectedsilence_read = silence_read - 1;
        for (int i = 0; i < silence_read; i++) {
            System.out.println(new TreeMap<>(CRUDJT.read(tokenWithsilence_read)).toString().equals(
                    new TreeMap<>(Map.of("metadata", Map.of("silence_read", expectedsilence_read), "data", data)).toString()));

            expectedsilence_read--;
        }
        System.out.println(CRUDJT.read(tokenWithsilence_read) == null);

        // with ttl and silence_read
        ttl = 5;
        silence_read = 5;
        System.out.println("Checking ttl and silence_read...");
        String tokenWithttlAndsilence_read = CRUDJT.create(data, ttl, silence_read);

        expectedttl = ttl;
        expectedsilence_read = silence_read - 1;
        for (int i = 0; i < silence_read; i++) {
            Map<String,Object> originalMap = CRUDJT.read(tokenWithttlAndsilence_read);
            Map<String, Object> sortedMap = sortMapRecursively(originalMap);
            Map<String, Object> expectedMap = new HashMap<>(Map.of("metadata", new HashMap<>(Map.of("ttl", expectedttl, "silence_read", expectedsilence_read)), "data", data));

            System.out.println(sortedMap.equals(expectedMap));
            expectedttl--;
            expectedsilence_read--;
            TimeUnit.SECONDS.sleep(1);
        }
        System.out.println(CRUDJT.read(tokenWithttlAndsilence_read) == null);

        // with scale load
        data = Map.of(
            "user_id", 414243,
            "role", 11,
            "devices", Map.of(
                "ios_expired_at", Instant.now().toString(),
                "android_expired_at", Instant.now().toString(),
                "mobile_app_expired_at", Instant.now().toString()
            ),
            "a", 42
        );

        System.out.println("Checking scale load...");
        for (int i = 0; i < 10; i++) {
            long start, end;

            // when creates
            System.out.println("when creates 40k tokens with Turbo Queue");
            start = System.nanoTime();
            String[] tokens = new String[REQUESTS];
            for (int j = 0; j < REQUESTS; j++) {
                tokens[j] = CRUDJT.create(data, -1, -1);
            }
            end = System.nanoTime();
            System.out.printf("Elapsed time: %.3f seconds\n", (end - start) / 1e9);

            // when reads
            System.out.println("when reads 40k tokens");
            int index = new Random().nextInt(REQUESTS);
            start = System.nanoTime();
            for (int j = 0; j < REQUESTS; j++) {
                CRUDJT.read(tokens[index]);
            }
            end = System.nanoTime();
            System.out.printf("Elapsed time: %.3f seconds\n", (end - start) / 1e9);

            // when updates
            System.out.println("when updates 40k tokens");
            start = System.nanoTime();
            for (int j = 0; j < REQUESTS; j++) {
                CRUDJT.update(tokens[j], edData, -1, -1);
            }
            end = System.nanoTime();
            System.out.printf("Elapsed time: %.3f seconds\n", (end - start) / 1e9);

            // when deletes
            System.out.println("when deletes 40k tokens");
            start = System.nanoTime();
            for (int j = 0; j < REQUESTS; j++) {
                CRUDJT.delete(tokens[j]);
            }
            end = System.nanoTime();
            System.out.printf("Elapsed time: %.3f seconds\n", (end - start) / 1e9);
        }

        // when cached on read
        List<String> previusValues = new ArrayList<>();

        for (int i = 0; i < REQUESTS; i++) {
            previusValues.add(CRUDJT.create(data, -1, -1));
        }

        for (int i = 0; i < REQUESTS; i++) {
            CRUDJT.create(data, -1, -1);
        }

        for (int i = 0; i < READY_FOR_CACHE; i++) {
            long startTime = System.nanoTime();

            for (int j = 0; j < REQUESTS; j++) {
                CRUDJT.read(previusValues.get(j));
            }

            long endTime = System.nanoTime();
            double elapsedSeconds = (endTime - startTime) / 1_000_000_000.0;
            System.out.printf("Iteration %d: %.6f seconds%n", i + 1, elapsedSeconds);
        }
    }
}
