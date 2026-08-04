package com.tourbooking.booking.backend.ai.vector;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class InMemoryVectorService implements VectorSearchService {

    @Data
    @AllArgsConstructor
    private static class VectorRecord {
        private String id;
        private float[] vector;
        private Map<String, Object> payload;
    }

    private final Map<String, Map<String, VectorRecord>> collections = new HashMap<>();

    @Override
    public synchronized void upsert(String collection, String id, float[] vector, Map<String, Object> payload) {
        collections.computeIfAbsent(collection, k -> new HashMap<>())
                .put(id, new VectorRecord(id, vector, payload));
    }

    @Override
    public synchronized List<VectorSearchResult> search(String collection, float[] queryVector, int topK) {
        Map<String, VectorRecord> collectionMap = collections.get(collection);
        if (collectionMap == null || collectionMap.isEmpty()) {
            return Collections.emptyList();
        }

        List<VectorSearchResult> results = new ArrayList<>();
        for (VectorRecord record : collectionMap.values()) {
            float similarity = cosineSimilarity(queryVector, record.getVector());
            results.add(VectorSearchResult.builder()
                    .id(record.getId())
                    .score(similarity)
                    .payload(record.getPayload())
                    .build());
        }

        results.sort((a, b) -> Float.compare(b.getScore(), a.getScore()));
        return results.subList(0, Math.min(topK, results.size()));
    }

    @Override
    public synchronized void delete(String collection, String id) {
        Map<String, VectorRecord> collectionMap = collections.get(collection);
        if (collectionMap != null) {
            collectionMap.remove(id);
        }
    }

    private float cosineSimilarity(float[] vectorA, float[] vectorB) {
        if (vectorA == null || vectorB == null || vectorA.length != vectorB.length) return 0f;
        float dotProduct = 0.0f;
        float normA = 0.0f;
        float normB = 0.0f;
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += vectorA[i] * vectorA[i];
            normB += vectorB[i] * vectorB[i];
        }
        if (normA == 0 || normB == 0) return 0f;
        return (float) (dotProduct / (Math.sqrt(normA) * Math.sqrt(normB)));
    }
}
