package com.tourbooking.booking.backend.ai.vector;

import java.util.List;
import java.util.Map;

public interface VectorSearchService {

    /** Upsert vector item into a collection */
    void upsert(String collection, String id, float[] vector, Map<String, Object> payload);

    /** Search for top K similar items by query vector */
    List<VectorSearchResult> search(String collection, float[] queryVector, int topK);

    /** Delete vector item by ID */
    void delete(String collection, String id);
}
