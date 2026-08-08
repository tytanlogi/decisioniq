# Pre-Milvus HTTP Benchmark

## What this proves

The benchmark invokes `POST /api/admin/pre-catalog-analysis` 1,000 times through Spring MVC. Each request follows the live deterministic sequence:

`HTTP JSON validation -> request boundary -> guardrail -> CoreNLP -> clause decomposition -> operation policy -> context/lookup shaping`

No request in this benchmark calls the remote catalog service, Milvus, OpenAI, evidence retrieval, or SQL. Therefore it measures the **input contract sent before catalog retrieval**, not retrieval quality or answer accuracy.

## Latest result

`PreCatalogAnalysisHttpBenchmarkTest` passed all **1,000 / 1,000** expected outcomes.

| Category | Requests | Expected result | Result |
| --- | ---: | --- | --- |
| Approved context + one lookup | 150 | One search request with `TXN-*` local context | 150 passed |
| Approved context + two lookups | 150 | Two search requests, each carrying the same context | 150 passed |
| Approved context + explanation lookup | 100 | One search request with local context | 100 passed |
| Direct two-lookups | 100 | Two search requests, no preceding local context | 100 passed |
| Malformed local ID (`TXN-ABC...`) | 100 | Ready, but two executable requests; no trusted local context | 100 passed |
| Requested external action (`email`) | 100 | Whole request blocked by operation policy | 100 passed |
| Requested transfer (`export`) | 100 | Whole request blocked by operation policy | 100 passed |
| Raw SQL | 100 | Whole request blocked by first guardrail | 100 passed |
| Script / instruction bypass | 100 | Whole request blocked by first guardrail | 100 passed |

## Failure interpretation

There were **zero contract failures** in this run. A future failed case is classified as follows:

| Failure reason | Meaning | Correct response |
| --- | --- | --- |
| Wrong disposition | A safe request was blocked, or unsafe request was allowed. | Fix the owning guardrail/operation-policy rule and add a regression case. |
| Wrong search count | A context statement became its own search, or a lookup was lost. | Fix clause role/context grouping; do not change catalog retrieval to hide it. |
| Missing/wrong context ID | A lookup did not preserve the immediately preceding valid context. | Fix local-context resolution; do not infer a similar identifier. |
| Retrieval-quality failure | The pre-catalog payload was correct, but the remote catalog did not return the right capability. | Investigate catalog cards, embeddings, lexical retrieval, score/margin calibration, and benchmark labels. |

## Important non-ideal result

`TXN-ABC123` is intentionally **not** accepted as a local transaction context identifier. The current trusted local-ID grammar requires a digit immediately after the prefix. It therefore remains executable text and does not get silently corrected or attached to a later lookup. This is deliberate: fuzzy correction of identifiers risks looking up the wrong customer data.

## Running it

```bash
./gradlew test --tests com.app.decisioniq.api.admin.PreCatalogAnalysisHttpBenchmarkTest
```

For manual API testing, import [DecisionIQ-Pre-Catalog-Analysis.postman_collection.json](postman/DecisionIQ-Pre-Catalog-Analysis.postman_collection.json) into Postman and see [POSTMAN_PRE_CATALOG_ANALYSIS.md](POSTMAN_PRE_CATALOG_ANALYSIS.md).
