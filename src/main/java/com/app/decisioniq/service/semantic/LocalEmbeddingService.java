package com.app.decisioniq.service.semantic;

import java.util.List;

public interface LocalEmbeddingService {

    List<Float> embed(String text);
}
