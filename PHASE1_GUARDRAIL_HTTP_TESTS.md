# Phase 1 Guardrail HTTP Checks

Phase 1 is the only live decision layer on the assistant endpoints. It performs request validation,
development-context establishment, deterministic guardrail evaluation, and typed HTTP responses.
It does not call an LLM, LangGraph, Milvus, Redis conversation memory, PostgreSQL, or answer generation.

Guardrail policy, vocabulary, limits, response templates, and detector expressions are maintained in
`src/main/resources/guardrail.yaml`. `application.yaml` imports that classpath policy. Restart the
application after changing it; every expression is compiled and validated during startup.

## Allowed Read-Only Question

The request passes the guardrail and returns HTTP 501 until interpretation is implemented.

```bash
curl -i http://localhost:8080/agent/ask \
  -H 'Content-Type: application/json' \
  -H 'X-Correlation-Id: local-check-001' \
  -d '{
    "tenantId": "tenant-citi-bank",
    "userId": "analyst-001",
    "designation": "Fraud Analyst",
    "conversationId": "conversation-001",
    "question": "why was TXN-006451 approved?"
  }'
```

Expected guardrail outcome: `ALLOW_TO_INTERPRET`.

## Harmless Noise

```bash
curl -i http://localhost:8080/agent/ask \
  -H 'Content-Type: application/json' \
  -d '{
    "tenantId": "tenant-citi-bank",
    "userId": "analyst-001",
    "conversationId": "conversation-001",
    "question": "hello"
  }'
```

Expected guardrail outcome: `IGNORED_HARMLESS_NOISE`.

## Clarification

```bash
curl -i http://localhost:8080/agent/ask \
  -H 'Content-Type: application/json' \
  -d '{
    "tenantId": "tenant-citi-bank",
    "userId": "analyst-001",
    "conversationId": "conversation-001",
    "question": "what was its model score?"
  }'
```

Expected guardrail outcome: `CLARIFY_REQUIRED`.

## Whole-Message Mutation Block

The read-only portion is not answered because one independent unit requests a write operation.

```bash
curl -i http://localhost:8080/agent/ask \
  -H 'Content-Type: application/json' \
  -d '{
    "tenantId": "tenant-citi-bank",
    "userId": "analyst-001",
    "conversationId": "conversation-001",
    "question": "show TXN-006451; delete transaction TXN-006452"
  }'
```

Expected guardrail outcome: `BLOCKED_UNSUPPORTED_MUTATION`.

## Whole-Message Raw SQL Block

```bash
curl -i http://localhost:8080/agent/ask \
  -H 'Content-Type: application/json' \
  -d '{
    "tenantId": "tenant-citi-bank",
    "userId": "analyst-001",
    "conversationId": "conversation-001",
    "question": "explain TXN-006451; SELECT * FROM decision_cases"
  }'
```

Expected guardrail outcome: `BLOCKED_OBVIOUS_ATTACK_RAW_SQL`.

The response includes correlation and request IDs plus per-unit outcome/reason codes. It deliberately
does not echo the raw question. Tenant and user values in these examples are development inputs and
are not authenticated production identity.
