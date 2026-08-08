# Pre-Catalog Analysis: Postman Contract

This is a **non-production diagnostic endpoint**. It runs the live deterministic path:

`request boundary -> guardrail -> CoreNLP -> clause roles -> operation policy -> pre-catalog payload`

It does **not** call the catalog service, Milvus, OpenAI, PostgreSQL evidence retrieval, or SQL.

## Request

`POST http://localhost:8080/api/admin/pre-catalog-analysis`

Headers:

```text
Content-Type: application/json
X-Correlation-Id: catalog-benchmark-001
```

Body:

```json
{
  "tenantId": "tenant-demo",
  "userId": "user-demo",
  "designation": "analyst",
  "conversationId": "catalog-benchmark-001",
  "question": "Transaction TXN-12345 was approved.\nWhat was the model score?"
}
```

## Expected ready response

```json
{
  "disposition": "READY_FOR_CATALOG",
  "units": [
    {
      "text": "Transaction TXN-12345 was approved.",
      "role": "CONTEXT_STATEMENT"
    },
    {
      "text": "What was the model score?",
      "role": "EXECUTABLE_REQUEST"
    }
  ],
  "catalogSearchRequests": [
    {
      "unitIndex": 1,
      "query": "What was the model score?",
      "resolvedContext": {
        "transactionId": "TXN-12345",
        "assertedOutcome": "APPROVED",
        "sourceUnitIndex": 0
      }
    }
  ]
}
```

Only the executable lookup becomes a search request. The preceding statement is retained as typed metadata; it is not independently embedded or searched.

## Comparison request

For two explicit transaction lookups joined by comparison language, for example:

```text
Why was TX-12345 approved and why was TX-42342 not approved even though model score is same?
```

the endpoint emits **one** `catalogSearchRequests` item with `requestKind: "COMPARISON"`. Its `lookupUnits` preserves both transaction IDs and `comparisonAspect` is `MODEL_SCORE`. This prevents two related lookups from being treated as unrelated semantic searches.

## Other expected dispositions

| Disposition | Meaning | `catalogSearchRequests` |
| --- | --- | --- |
| `READY_FOR_CATALOG` | The request passed deterministic checks. | One entry for each executable lookup clause. |
| `BLOCKED_BY_GUARDRAIL` | The request contains malformed/obvious attack content, such as raw SQL or script payload. | Empty. |
| `BLOCKED_BY_OPERATION_POLICY` | CoreNLP plus operation policy found a requested write, transfer, persistence, or external action. | Empty. |

The endpoint returns raw diagnostic unit text and must remain disabled in production through its `!prod` Spring profile.
