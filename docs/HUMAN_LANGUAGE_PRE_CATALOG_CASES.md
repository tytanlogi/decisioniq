# Human-Language Pre-Catalog Cases

Import [DecisionIQ-Human-Ambiguity-Cases.postman_collection.json](postman/DecisionIQ-Human-Ambiguity-Cases.postman_collection.json) into Postman. Its default URL is `http://localhost:8082/api/admin/pre-catalog-analysis`.

| Case | Expected architectural result |
| --- | --- |
| 01 | `LOOKUP`; historical `was approved` must not block as a write. |
| 02 | One `COMPARISON`, two IDs, aspect `MODEL_SCORE`. |
| 03 | One `COMPARISON`, two IDs, general aspect `UNSPECIFIED`. |
| 04 | One `LOOKUP` with `resolvedContext: TX-12345`. |
| 05 | Two lookups, each carrying the same local context. |
| 06 | Desired next behavior: second lookup should inherit the explicit first ID. This is a deliberate regression probe. |
| 07 | Desired next behavior: harmless conversational noise must not become a separate semantic search. This is a regression probe. |
| 08 | The exact ID must survive; spelling tolerance is a later retrieval concern, not identifier correction. |
| 09 | Desired future behavior: clarification, because the second comparison target is missing. |
| 10 | Whole message must block; valid lookup is not partially answered when a prohibited write occurs. |

Cases 06, 07, and 09 are intentionally included because they represent human language boundaries the current deterministic pre-catalog layer has not fully solved. They should become explicit regression tests after we agree the response contract: inherited local reference, ignored noise, or clarification.
