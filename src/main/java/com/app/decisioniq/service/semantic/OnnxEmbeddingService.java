package com.app.decisioniq.service.semantic;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(prefix = "decisioniq.embedding.onnx", name = "enabled", havingValue = "true")
public class OnnxEmbeddingService implements LocalEmbeddingService {

    private static final String INPUT_IDS = "input_ids";
    private static final String ATTENTION_MASK = "attention_mask";
    private static final String TOKEN_TYPE_IDS = "token_type_ids";

    private final OrtEnvironment environment;
    private final OrtSession session;
    private final HuggingFaceTokenizer tokenizer;
    private final int dimension;

    public OnnxEmbeddingService(
            @Value("${decisioniq.embedding.onnx.model-path:}") String modelPath,
            @Value("${decisioniq.embedding.onnx.tokenizer-path:}") String tokenizerPath,
            @Value("${decisioniq.embedding.onnx.dimension:384}") int dimension
    ) {
        String cleanModelPath = cleanPath(modelPath);
        String cleanTokenizerPath = cleanPath(tokenizerPath);

        if (!StringUtils.hasText(cleanModelPath)) {
            throw new IllegalArgumentException("decisioniq.embedding.onnx.model-path is required when ONNX embeddings are enabled");
        }
        if (!StringUtils.hasText(cleanTokenizerPath)) {
            throw new IllegalArgumentException("decisioniq.embedding.onnx.tokenizer-path is required when ONNX embeddings are enabled");
        }

        try {
            Path onnxModelPath = Path.of(cleanModelPath);
            Path huggingFaceTokenizerPath = Path.of(cleanTokenizerPath);

            if (!Files.isRegularFile(onnxModelPath)) {
                throw new IllegalArgumentException("ONNX model file not found: " + onnxModelPath);
            }
            if (!Files.isRegularFile(huggingFaceTokenizerPath)) {
                throw new IllegalArgumentException("ONNX tokenizer file not found: " + huggingFaceTokenizerPath);
            }

            this.environment = OrtEnvironment.getEnvironment();
            this.session = environment.createSession(onnxModelPath.toString(), new OrtSession.SessionOptions());
            this.tokenizer = HuggingFaceTokenizer.newInstance(huggingFaceTokenizerPath);
            this.dimension = dimension;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to initialize ONNX embedding service", ex);
        }
    }

    @Override
    public List<Float> embed(String text) {
        if (!StringUtils.hasText(text)) {
            throw new IllegalArgumentException("Text is required for embedding");
        }

        try {
            Encoding encoding = tokenizer.encode(text);
            long[] attentionMask = encoding.getAttentionMask();
            Map<String, OnnxTensor> inputs = new HashMap<>();

            try (
                    OnnxTensor inputIdsTensor = tensor(encoding.getIds());
                    OnnxTensor attentionMaskTensor = tensor(attentionMask);
                    OnnxTensor tokenTypeIdsTensor = tensor(encoding.getTypeIds())
            ) {
                inputs.put(INPUT_IDS, inputIdsTensor);
                inputs.put(ATTENTION_MASK, attentionMaskTensor);
                if (session.getInputNames().contains(TOKEN_TYPE_IDS)) {
                    inputs.put(TOKEN_TYPE_IDS, tokenTypeIdsTensor);
                }

                try (OrtSession.Result result = session.run(inputs)) {
                    List<Float> vector = vectorFromModelOutput(result.get(0).getValue(), attentionMask);
                    validateDimension(vector);
                    return vector;
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate ONNX embedding", ex);
        }
    }

    @PreDestroy
    public void close() throws OrtException {
        session.close();
    }

    private OnnxTensor tensor(long[] values) throws OrtException {
        return OnnxTensor.createTensor(environment, new long[][]{values});
    }

    private List<Float> vectorFromModelOutput(Object output, long[] attentionMask) {
        if (output instanceof float[][][] tokenEmbeddings) {
            return meanPoolAndNormalize(tokenEmbeddings[0], attentionMask);
        }
        if (output instanceof float[][] sentenceEmbeddings && sentenceEmbeddings.length > 0) {
            return normalize(toList(sentenceEmbeddings[0]));
        }
        throw new IllegalStateException("Unsupported ONNX embedding output shape: " + output.getClass().getName());
    }

    private List<Float> meanPoolAndNormalize(float[][] tokenEmbeddings, long[] attentionMask) {
        int dimensions = tokenEmbeddings[0].length;
        float[] pooled = new float[dimensions];
        int tokenCount = 0;

        for (int token = 0; token < tokenEmbeddings.length; token++) {
            if (token < attentionMask.length && attentionMask[token] == 0) {
                continue;
            }
            tokenCount++;
            for (int currentDimension = 0; currentDimension < dimensions; currentDimension++) {
                pooled[currentDimension] += tokenEmbeddings[token][currentDimension];
            }
        }

        int divisor = Math.max(tokenCount, 1);
        for (int currentDimension = 0; currentDimension < dimensions; currentDimension++) {
            pooled[currentDimension] = pooled[currentDimension] / divisor;
        }

        return normalize(toList(pooled));
    }

    private List<Float> normalize(List<Float> vector) {
        double sum = 0.0;
        for (Float value : vector) {
            sum += value * value;
        }

        double norm = Math.sqrt(sum);
        if (norm == 0.0) {
            return vector;
        }

        List<Float> normalized = new ArrayList<>(vector.size());
        for (Float value : vector) {
            normalized.add((float) (value / norm));
        }
        return normalized;
    }

    private List<Float> toList(float[] values) {
        List<Float> vector = new ArrayList<>(values.length);
        for (float value : values) {
            vector.add(value);
        }
        return vector;
    }

    private void validateDimension(List<Float> vector) {
        if (vector.size() != dimension) {
            throw new IllegalStateException("ONNX embedding dimension " + vector.size()
                    + " does not match configured dimension " + dimension);
        }
    }

    private String cleanPath(String path) {
        if (path == null) {
            return "";
        }

        String cleanPath = path.trim();
        while (cleanPath.length() > 1 && (cleanPath.startsWith("\"") || cleanPath.startsWith("'"))) {
            cleanPath = cleanPath.substring(1).trim();
        }
        while (cleanPath.length() > 1 && (cleanPath.endsWith("\"") || cleanPath.endsWith("'"))) {
            cleanPath = cleanPath.substring(0, cleanPath.length() - 1).trim();
        }
        return cleanPath;
    }
}
