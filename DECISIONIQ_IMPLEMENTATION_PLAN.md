# DecisionIQ Architecture and Implementation Plan

## 1. Objective

DecisionIQ is a read-only, multi-tenant fraud decision-intelligence assistant. It accepts natural-language questions about existing transaction decisions, safely retrieves governed evidence from PostgreSQL and knowledge from Milvus, and returns grounded answers.

The implementation is Java-first. Java owns identity, tenant isolation, conversation policy, catalog authority, planning, database execution, evidence, final validation, and auditability. Models may interpret language and explain evidence, but they never authorize data access or generate executable SQL.

Every phase has an exit gate. Work does not move to the next phase until focused tests and ./gradlew clean test pass.

## 2. Current Baseline

Phase 0 and the deterministic Phase 1 request boundary are complete.

- Rejected graph, intent, planning, NLP, semantic-frame, catalog prototype, and data-slice code has been removed.
- The project contains the HTTP boundary, request identity filter, development context adapter, deterministic guardrail, reusable infrastructure adapters, logging, and this plan.
- /agent/ask and /api/assistant/ask execute only the Phase 1 guardrail. Allowed read-only requests return HTTP 501 until interpretation is implemented.
- The clean test build passes.

### Engineering Rules

- Controllers are transport adapters only. They validate transport shape, map HTTP payloads to application commands, invoke one use case, and map the result to HTTP.
- Business policy, orchestration, data access, and model decisions never live in controllers or response DTOs.
- Every class has one primary reason to change. Normalization, validation, segmentation, classification, request policy, audit, and HTTP presentation remain separate responsibilities.
- Dependencies point inward: API depends on application contracts; application depends on domain contracts and ports; infrastructure implements ports. Domain code does not depend on Spring, HTTP, databases, or model vendors.
- Interfaces are introduced for architectural boundaries, replaceable collaborators, or multiple implementations. Interfaces are not created mechanically for every class.
- Configuration is strongly typed and validated at startup. Versioned policy data can change without modifying business algorithms.
- Public contracts use immutable records and explicit outcomes. Exceptions are translated only at the HTTP boundary.
- Refactoring must preserve behavior through focused tests and `./gradlew clean test`. Premature optimization and speculative abstractions are avoided.

## 3. Non-Negotiable Decisions

1. DecisionIQ supports reads and explanations only. There is no write-plan or write-SQL capability.
2. Tenant and user identity come from trusted authentication claims in production. Request fields are development placeholders only.
3. Java validates every model proposal before any data access.
4. Models return typed logical requests. They never return table names, column names, joins, or SQL.
5. PostgreSQL is the authoritative source for transaction facts and catalog metadata.
6. Milvus is a derived semantic discovery index. A Milvus result is never authorization.
7. Redis conversation context is keyed by tenant, user, and conversation. conversationId alone is not a safe key.
8. Context is resolved independently for each request unit. It is never blindly attached to the complete message.
9. Direct facts, lists, and aggregates use deterministic answer rendering.
10. An explanation model is called only when evidence must be summarized or reasoned over.
11. Unsupported, ambiguous, or insufficiently grounded requests fail closed or ask for clarification.
12. New concepts are introduced through versioned catalog data, not hardcoded Java intent enums.
13. HTTP controllers contain no business rules or workflow orchestration.
14. Single-responsibility and dependency-direction violations are corrected before adding the next phase.

## 4. Target Runtime Flow

    Assistant HTTP request
      -> Request and trusted-identity validation
      -> Deterministic read-only safety gate
      -> Structured request interpretation (LLM call 1)
      -> Request-unit validation
      -> Per-unit Redis context resolution
      -> Milvus catalog candidate discovery
      -> PostgreSQL catalog authorization
      -> Governed logical query planning
      -> Read-only SQL compilation
      -> Fact and optional knowledge retrieval
      -> Evidence packet construction
      -> Deterministic rendering or optional explanation (LLM call 2)
      -> Grounding and policy validation
      -> Audit, metrics, and Redis context update
      -> Assistant response

Unsupported, ambiguous, no-data, and failed-validation paths return controlled responses without continuing to later stages.

## 5. Responsibility Boundaries

| Component | Authoritative responsibility |
| --- | --- |
| HTTP/API | Request shape, authentication context, status codes |
| Safety gate | Size limits, malformed input, read-only enforcement |
| Request interpreter | Language interpretation and multi-question decomposition |
| Redis context | Active references and compact conversation state |
| Milvus | Top-k candidate discovery by semantic similarity |
| PostgreSQL catalog | Logical fields, physical mappings, joins, policies, versions |
| Java catalog binder | Candidate authorization and ambiguity detection |
| Java planner/compiler | Typed plan, safe joins, tenant filters, parameterized SQL |
| PostgreSQL facts | Existing transaction and decision evidence |
| Knowledge RAG | Policy, rule, model, and explainability documentation |
| Answer router | Deterministic versus model-generated answer selection |
| Grounding validator | Claim-to-evidence validation and fail-closed behavior |
| Observability | Trace, latency, tokens, prompt/model/catalog versions |

## 6. Package Architecture

The new code uses capability and ownership boundaries:

    com.app.decisioniq
      api
        assistant
        error
      application
        workflow
        routing
      domain
        interpretation
        context
        catalog
        query
        evidence
        answer
      infrastructure
        openai
        redis
        milvus
        postgres
        observability
      config

Domain packages contain records, enums, and policies without Spring or vendor classes. Infrastructure packages implement domain ports. Application services orchestrate domain behavior.

## 7. Canonical Interpretation Contracts

The first model call must return strict structured output matching Java contracts.

    InterpretationResult
      requestType: DOMAIN_QUERY | SMALL_TALK | OUT_OF_SCOPE | UNSUPPORTED_MUTATION
      units: List<RequestUnit>
      ambiguities: List<Ambiguity>
      confidence: decimal
      modelVersion: string

    RequestUnit
      unitId: string
      order: integer
      action: LOOKUP | LIST | COUNT | AGGREGATE | EXPLAIN | COMPARE | SUMMARIZE
      scope: EXPLICIT | FOLLOW_UP | NEW_QUERY | DEPENDS_ON_UNIT
      subjectText: string
      requestedFacts: List<RequestedFact>
      constraints: List<RequestConstraint>
      entityReferences: List<EntityReference>
      dependsOnUnitIds: List<string>
      resultShape: ResultShape
      requiresClarification: boolean

    RequestConstraint
      fieldPhrase: string
      operator: EQUALS | NOT_EQUALS | LESS_THAN | GREATER_THAN | BETWEEN | IN
      rawValue: string
      normalizedValue: typed value when safely available
      valueType: TEXT | NUMBER | MONEY | BOOLEAN | TEMPORAL | IDENTIFIER | LOCATION

The contracts contain logical language only. They cannot represent physical database identifiers or SQL.

## 8. Model Call Budget

### Call 1: Request Interpretation

Purpose:

- classify supported, small-talk, out-of-scope, and mutation requests;
- split multiple questions into ordered units;
- identify actions, requested facts, constraints, references, dependencies, and ambiguity;
- return strict structured output.

The prompt contains the raw question, supported action schema, and a small catalog-neutral vocabulary. It does not contain the database schema or complete conversation history.

### Call 2: Optional Explanation

Purpose:

- explain why a decision happened;
- compare evidence;
- summarize complex evidence.

The prompt contains only the question, interpreted units, compact evidence, and response policy. Fact lookup, list, count, and aggregate responses do not use this call.

### Future Cost Optimization

A local semantic router may bypass Call 1 for calibrated small-talk, obvious unsupported input, or reusable plans. It is added only after evaluation proves that it does not create unacceptable false negatives.

## 9. Data Ownership

### Redis Conversation Context

Key:

    decisioniq:context:v1:{tenantId}:{userId}:{conversationId}

Store only active transaction/case/correlation references, recent successful unit summaries, reusable evidence references, required versions, expiration, and last-updated time.

Do not store unrestricted prompts, complete database rows, or unlimited conversation history.

### Authoritative Catalog in PostgreSQL

The versioned catalog contains:

- logical field ID and searchable description;
- value type and permitted operators;
- physical table and column mapping;
- tenant policy and tenant column;
- approved joins and shared keys;
- reference-data namespace;
- availability, lifecycle status, and version.

Flyway owns the schema. Catalog administration is separate from the assistant query API.

### Milvus Catalog Index

Milvus stores a derived projection:

    logicalId, searchableText, embedding, catalogVersion, status

Search returns top-k logical IDs. Java reloads those IDs from PostgreSQL and rejects unknown, inactive, stale, ambiguous, or unauthorized candidates.

### Transaction Facts

PostgreSQL is the only source for transaction facts. Queries use a read-only identity, mandatory tenant predicates, statement timeouts, row limits, and parameterized values.

### Knowledge RAG

A separate Milvus collection stores policy, rule, model, feature, and explainability documentation. It never stores authoritative transaction facts.

## 10. Implementation Roadmap

### Phase 0: Clean Foundation - COMPLETE

Build:

- remove rejected prototype code and dependencies;
- retain reusable infrastructure and HTTP contracts;
- return HTTP 501 from the incomplete request path.

Exit gate:

- no obsolete package references;
- ./gradlew clean test passes.

### Phase 1: Request Boundary and Error Contract

Build:

- replace static validation with a Spring validation boundary;
- validate tenant, user, conversation, question length, and content type;
- introduce trusted RequestContext separate from client-controlled fields;
- add request/correlation IDs;
- add a consistent API error body and global exception handler;
- apply a deterministic, pre-LLM guardrail without restricting future read-only questions to a small intent list;
- block the complete message when any unit contains a mutation, raw SQL, an obvious attack, or instruction bypass;
- return clarification for genuinely unresolved context and never guess;
- keep per-unit outcomes for diagnostics while request-level policy remains fail closed;
- externalize bounded vocabulary, limits, segmentation, response templates, and explicit policy in versioned configuration;
- keep detector algorithms in Java while loading versioned, bounded signatures from a dedicated classpath guardrail policy file that is compiled and validated at startup;
- retain HTTP 501 after successful guardrail evaluation until interpretation is implemented.

Tests:

- valid request;
- missing request and each required field;
- oversized and malformed question;
- correlation ID propagation;
- harmless noise combined with a valid read-only question;
- read-only plus mutation blocks the complete message;
- raw SQL, obvious attack, and instruction bypass block the complete message;
- unclear context produces clarification;
- client tenant remains explicitly development-sourced rather than production trusted.

Exit gate:

- controller tests prove the request boundary and error contract;
- clean test build passes.

### Phase 2: Interpretation Domain Contracts

Build:

- implement canonical interpretation records and enums;
- enforce unique unit IDs, ordering, dependencies, typed values, and ambiguity;
- define InterpretationPort with no model-provider dependency;
- define a versioned JSON schema for structured model output.

Tests:

- serialization round trips;
- invalid actions, operators, values, dependencies, and duplicate IDs;
- single and multi-unit examples.

Exit gate:

- contracts compile independently of Spring and OpenAI;
- invalid model output cannot produce a valid domain object.

### Phase 3: Structured LLM Request Interpreter

Build:

- implement the OpenAI adapter behind InterpretationPort;
- require strict structured output;
- add prompt versioning, timeout, retry, and response-size policy;
- validate model output again in Java;
- classify small-talk, out-of-scope, and unsupported mutation requests;
- preserve ambiguity instead of guessing;
- add deterministic malformed-input rejection and defense-in-depth mutation rejection.

Golden corpus:

- lookup, explanation, list, count, aggregate, compare, and summarize;
- independent and dependent multiple questions;
- explicit and contextual references;
- temporal, amount, location, model-score, rule, and risk-signal filters;
- spelling variations;
- small-talk, abuse, noise, symbols, unsupported domains, and mutations.

Exit gate:

- at least 200 versioned golden cases;
- agreed interpretation accuracy is met;
- no mutation reaches planning;
- timeout and invalid-output behavior is tested.

### Phase 4: Per-Unit Conversation Context

Build:

- define ConversationContext and ConversationContextPort;
- implement tenant/user/conversation Redis keys and TTL;
- resolve explicit references first;
- resolve same-message dependencies second;
- use active Redis references only for FOLLOW_UP units;
- keep list, aggregate, and NEW_QUERY units independent unless linked;
- return clarification when required context is absent.

Tests:

- explicit ID overrides Redis;
- a model-score follow-up resolves the active transaction;
- plural list query ignores active transaction;
- mixed multi-unit messages resolve independently;
- cross-tenant and cross-user access is impossible;
- expired context produces clarification.

Exit gate:

- context precedence and isolation tests pass;
- context resolution requires no database or model call.

### Phase 5: Authoritative Semantic Catalog

Build:

- create Flyway migrations for fields, joins, references, and versions;
- use extensible logical IDs rather than concept enums;
- create catalog repository and cache ports;
- seed only mappings verified against the DecisionIQ schema;
- represent unsupported mappings and missing reference data explicitly;
- add lifecycle and versioning.

Tests:

- duplicate IDs and invalid mappings;
- operator and value-type compatibility;
- required tenant column;
- join connectivity;
- unavailable region/reference data;
- version activation and rollback.

Exit gate:

- catalog authorizes the first vertical slice without Milvus;
- every physical identifier comes from catalog data.

### Phase 6: Milvus Catalog Discovery

Build:

- define CatalogDiscoveryPort;
- create the derived Milvus collection and index;
- use existing ONNX infrastructure for local embeddings;
- index catalog entries idempotently by version;
- retrieve top-k logical IDs for each request unit;
- validate IDs and versions against PostgreSQL;
- never use a similarity threshold as authorization.

Tests:

- indexing and reindexing;
- stale and unknown IDs rejected;
- top-k recall for facts and filters;
- multi-concept recall;
- spelling and synonym variation;
- latency and embedding dimensions.

Exit gate:

- catalog recall@k meets the target;
- Java remains correct when Milvus returns noisy candidates.

### Phase 7: Governed Catalog Binding

Build:

- bind facts and constraints to approved logical IDs;
- validate operators, value types, mappings, tenant policy, and reference data;
- detect ambiguous city/country, score/amount, and similar fields;
- return approved, clarification, and unsupported results;
- never infer missing physical mappings.

Tests:

- model score versus transaction amount;
- Dubai city binding;
- last-week temporal binding;
- APAC fails without governed membership;
- aggregate risk signal requires specific fields;
- invalid operators and unavailable mappings fail closed.

Exit gate:

- every approved binding has logical ID and provenance;
- no physical data access occurs in this phase.

### Phase 8: Logical Query Planning

Build:

- define immutable QueryPlan, unit plans, projections, predicates, aggregation, sorting, paging, and dependencies;
- create plans only from approved bindings;
- require tenant scope and row limits;
- resolve relative times with request timezone and clock;
- validate joins and result shape;
- calculate a canonical plan fingerprint.

Tests:

- transaction explanation;
- amount greater than 100, POS, Dubai, last 24 hours;
- count and aggregate;
- model score plus transaction fields;
- multiple independent units;
- unsupported joins, missing tenant, and excessive limits.

Exit gate:

- planning is deterministic;
- plans contain logical IDs only and cannot contain SQL.

### Phase 9: Safe SQL Compilation

Build:

- compile SQL from the catalog and join registry;
- use NamedParameterJdbcTemplate or a structured query library;
- interpolate catalog-approved identifiers only;
- bind every user/model value as a parameter;
- force tenant filters, read-only transactions, timeout, and maximum rows;
- support required projection, filter, join, count, aggregate, sort, and paging;
- reject mutations by construction.

Tests:

- SQL snapshots;
- parameter separation;
- tenant filter in every plan;
- join generation;
- injection attempts remain values;
- no mutation API exists;
- PostgreSQL Testcontainers integration.

Exit gate:

- first vertical-slice plan returns correct tenant facts;
- no arbitrary identifier or SQL injection path exists.

### Phase 10: Evidence Retrieval and Packaging

Build:

- execute approved SQL plans;
- retrieve optional knowledge through a separate port;
- use a bounded executor for independent SQL/RAG work;
- define EvidencePacket with sources, rows, documents, provenance, timestamps, and missing evidence;
- separate no-data, partial-data, and failure;
- redact fields not allowed in prompts or responses.

Tests:

- complete, partial, empty, and failed retrieval;
- provenance for every value;
- bounded concurrency and timeouts;
- no cross-tenant evidence;
- evidence-size limits.

Exit gate:

- first vertical slice has sufficient evidence;
- retrieval contains no answer-generation logic.

### Phase 11: Answer Routing and Generation

Build:

- define FACT, LIST, COUNT, AGGREGATE, EXPLANATION, COMPARISON, SUMMARY, CLARIFICATION, and UNSUPPORTED modes;
- create deterministic renderers for non-reasoning modes;
- add the optional grounded explanation adapter;
- use structured claims and evidence references;
- support multiple unit answers in original order.

Tests:

- model-score fact response is concise;
- count/list/aggregate do not call the explanation model;
- approval explanation uses supplied evidence only;
- multi-question answers remain separate and ordered;
- missing evidence is stated.

Exit gate:

- direct routes use zero explanation calls;
- explanation prompts contain only authorized evidence.

### Phase 12: Grounding and Final Policy Validation

Build:

- validate every claim against evidence references;
- verify identifiers, amounts, scores, locations, dates, counts, and outcomes;
- enforce answer scope and sensitive-field policy;
- reject unsupported citations and invented facts;
- permit at most one controlled regeneration;
- return deterministic failure when validation cannot pass.

Tests:

- altered values and unsupported claims rejected;
- missing citations rejected;
- tenant leakage rejected;
- valid deterministic and explanation answers pass;
- regeneration is bounded.

Exit gate:

- every factual answer is evidence-grounded;
- tenant-leakage tests have zero failures.

### Phase 13: Conversation Update and Reuse

Build:

- update context only after a successful grounded response;
- store active references and compact unit summaries;
- separate evidence/result cache from conversation memory;
- include tenant, plan fingerprint, catalog version, and freshness in cache keys;
- apply TTL, size, and eviction;
- never cache authorization failures or ambiguity.

Tests:

- success updates active transaction;
- failure does not corrupt context;
- cache cannot cross tenants;
- catalog change invalidates reuse;
- size and TTL limits work.

Exit gate:

- follow-ups reuse context correctly;
- reuse reduces work without changing answers.

### Phase 14: Workflow Orchestration

Build:

- reintroduce LangGraph4j only after services are independently tested;
- define compact typed graph state;
- add nodes for validation, interpretation, context, discovery, binding, planning, retrieval, answering, validation, and memory update;
- add branches for small-talk, unsupported, clarification, no-data, and failure;
- keep business logic in services, not nodes;
- add checkpointing only for a proven resume/recovery requirement.

Tests:

- every graph branch;
- retry and timeout behavior;
- state isolation under concurrency;
- complete vertical-slice integration.

Exit gate:

- /agent/ask runs the new graph end to end;
- HTTP 501 is removed only now.

### Phase 15: Observability and Evaluation

Build:

- OpenTelemetry traces across API, model, Redis, Milvus, PostgreSQL, and validation;
- Langfuse prompt/model traces and evaluation metadata;
- structured logs with safe request, unit, plan, and version IDs;
- latency, tokens, cost, errors, clarification, cache, and grounding metrics;
- offline benchmark runner and reviewed-feedback storage;
- prompt/model/catalog/embedding/application version correlation.

Quality metrics:

- request type, action, and decomposition accuracy;
- constraint and reference accuracy;
- catalog recall@k and binding accuracy;
- valid-plan rate;
- answer groundedness and exact-value accuracy;
- p50/p95 latency and tokens per request;
- zero cross-tenant leakage.

Exit gate:

- every production request is traceable;
- regressions compare by prompt, model, catalog, and application version.

### Phase 16: Security and Production Deployment

Build:

- real authentication and authorization;
- tenant/user context from signed claims;
- read-only database identity and network isolation;
- external secret management;
- rate limits, bulkheads, circuit breakers, and graceful degradation;
- PII redaction and prompt-injection controls;
- stateless container deployment;
- managed/external Redis, PostgreSQL, and Milvus;
- Kubernetes probes, limits, autoscaling, canary rollout, and rollback;
- backup, restore, disaster recovery, and index-rebuild procedures.

Exit gate:

- threat model and security tests pass;
- load, resilience, recovery, and deployment objectives pass;
- operational runbooks are complete.

## 11. First Vertical Slice

The first capability is:

    Why was transaction TXN-006451 approved?

Required evidence:

- tenant-scoped transaction/case identity;
- transaction amount, channel, merchant, and location;
- final decision outcome;
- model score and risk band;
- evaluated rules;
- reason codes;
- relevant risk signals.

The slice is complete only when the HTTP request produces a grounded explanation with provenance and audit data, with no arbitrary SQL or cross-tenant path.

## 12. Expansion Order

1. Direct transaction facts and contextual follow-ups.
2. Filtered transaction lists with paging and sorting.
3. Counts and aggregates over time windows.
4. Multi-table projections such as model score plus transaction location.
5. Multiple independent questions in one message.
6. Parent-child questions and comparisons.
7. Knowledge RAG for policy, rule, and model documentation.
8. Evidence/result caching and calibrated local semantic routing.
9. Advanced evaluation, feedback review, and controlled optimization.

## 13. Definition of Done for Every Phase

A phase is complete only when:

- scope and contracts are documented;
- implementation contains no placeholder behavior for that phase;
- unit, negative, and boundary tests pass;
- external systems have integration tests;
- tenant and read-only controls are tested when data is involved;
- logs and metrics expose success and failure;
- ./gradlew clean test passes;
- superseded code is removed;
- the exit gate is accepted before the next phase starts.
