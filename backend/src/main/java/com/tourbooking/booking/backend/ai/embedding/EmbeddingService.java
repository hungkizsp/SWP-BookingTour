package com.tourbooking.booking.backend.ai.embedding;

import java.util.List;

public interface EmbeddingService {

    /** Embed single text into float array representation */
    float[] embed(String text);

    /** Embed multiple text strings in batch */
    List<float[]> embedBatch(List<String> texts);
}
