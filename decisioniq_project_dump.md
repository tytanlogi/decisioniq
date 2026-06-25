# DecisionIQ Project Dump

Generated from `/Volumes/yp/yp-red/AI BIT/decisioniq` on 2026-06-25. Heavy/generated folders such as `.git`, `.gradle`, `.idea`, `build`, `out`, and `target` were excluded from source inspection except for reading the test failure report after running Gradle.

## A. Project overview

### Project name
`decisioniq`

### Purpose of the system
DecisionIQ is a Spring Boot assistant service for fraud/decision intelligence questions. The current code is focused on parsing a user question into fraud-decision intents, planning which SQL table fields and RAG chunk types are needed, collecting evidence, and generating an answer through a LangGraph4j workflow. The implemented behavior is still early-stage: intent parsing and query planning are present, while evidence fetching and answer generation are mostly placeholders.

### Tech stack
- Java 21 toolchain
- Spring Boot 3.5.14
- Spring Web
- Spring JDBC dependency
- LangGraph4j 1.8.17 BOM, core, LangChain4j integration, Postgres saver dependency
- LangChain4j 1.15.0 and LangChain4j OpenAI provider
- OpenAI Responses chat model via LangChain4j
- Lombok
- Gradle 8.14.5 wrapper
- YAML-backed assistant data-slice configuration

### Architecture style
A graph-orchestrated, single assistant workflow. It is not currently a true multi-agent system. The graph has multiple nodes, but most nodes are deterministic services or placeholders. The intended architecture is: REST API -> LangGraph4j state graph -> intent parsing -> query planning -> evidence collection -> answer generation.

### Main modules / packages
- `com.app.decisioniq.api.assistant`: REST API, request/response models, validation.
- `com.app.decisioniq.assistant.graph`: LangGraph4j graph configuration, graph nodes, state constants, state wrapper.
- `com.app.decisioniq.assistant.intent`: intent model, intent enum/status types, prompt template.
- `com.app.decisioniq.service.intent`: LLM-backed intent parsing service.
- `com.app.decisioniq.assistant.dataslice`: business data-slice enum, YAML property binding, data-slice catalog/resolver.
- `com.app.decisioniq.assistant.planning`: query plan model and RAG chunk catalog.
- `com.app.decisioniq.service.planning`: QueryPlan construction services.
- `com.app.decisioniq.service.evidence`: SQL/RAG evidence collection placeholders.
- `com.app.decisioniq.service.answer`: answer generation placeholder.
- `com.app.decisioniq.llm.provider.openai`: OpenAI ChatModel configuration.
- `com.app.decisioniq.config`: shared bean configuration.

### Request flow summary
1. Client calls `POST /agent/ask` or `POST /api/assistant/ask` with `{ tenantId, question }`.
2. `AgentApi` validates non-blank question and calls `DecisionAssistantOrchestrator`.
3. The orchestrator invokes `CompiledGraph<DecisionIQAgentState>` with `question` and `tenantId`.
4. `IntentUnderstandingNode` calls OpenAI through `IntentUnderstandingService` and writes `ParsedQuestion` to state.
5. Graph condition checks `clarificationRequired`. If true, `IntentClarificationNode` writes a clarification answer and graph ends.
6. Otherwise `QueryPlanningNode` builds `QueryPlan` using parsed asks, static intent catalogs, and YAML data-slice mappings.
7. `EvidenceCollectionNode` invokes `EvidenceService`, which currently calls only `SQLDataFetchService`; SQL fetch construction is not implemented.
8. `AnswerGenerationNode` currently writes static answer `Hello Answered`.

## B. Build and runtime

### Gradle or Maven details
Gradle project with root name `decisioniq`. The wrapper uses Gradle `8.14.5`. Build file is `build.gradle`.

### Java version
`build.gradle` configures Java toolchain language version `21`.

### Major dependencies
- `org.springframework.boot:spring-boot-starter-web`
- `org.bsc.langgraph4j:langgraph4j-core`
- `org.bsc.langgraph4j:langgraph4j-langchain4j`
- `org.bsc.langgraph4j:langgraph4j-postgres-saver`
- `dev.langchain4j:langchain4j:1.15.0`
- `dev.langchain4j:langchain4j-open-ai:1.15.0`
- `org.springframework:spring-jdbc:7.0.8`
- Lombok compile/annotation processor
- `spring-boot-starter-test`

### App entry point
`/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/DecisioniqApplication.java` starts Spring Boot and enables configuration properties scanning.

### Config files and profiles
- `src/main/resources/application.yaml`: sets Spring application name, imports `decisioniq-data-slices.yaml`, and configures OpenAI properties from environment variables.
- `src/main/resources/decisioniq-data-slices.yaml`: defines business slices and maps them to real table names and fields.
No explicit Spring profiles are present in the inspected source.

### Database configuration
No datasource URL, username, password, driver, or profile-specific datasource config is present in inspected config files. The app injects `JdbcTemplate` in `SQLDataFetchService`, but current tests fail because no `JdbcTemplate` bean is available. There is also no PostgreSQL JDBC driver dependency visible in `build.gradle`.

### External integrations
- OpenAI via LangChain4j `OpenAiResponsesChatModel`, configured by `openai.base-url`, `openai.api-key`, and `openai.model`.
- RAG/Milvus is conceptually represented by `ragMap`, `ragCollectionName`, and `RAGDataFetchService`, but no Milvus client or RAG implementation exists in current code.
- LangGraph4j Postgres saver dependency is included but no checkpointer/saver bean is configured in inspected source.

### Build/test status observed
`./gradlew test` compiles Java successfully but fails `DecisioniqApplicationTests.contextLoads()` due missing `JdbcTemplate` bean. The failure chain reaches `SQLDataFetchService` constructor: `No qualifying bean of type org.springframework.jdbc.core.JdbcTemplate available`.

## C. Package and folder map

### Important folder tree
```text
decisioniq/
├── build.gradle
├── settings.gradle
├── gradle/wrapper/gradle-wrapper.properties
├── src/main/resources/
│   ├── application.yaml
│   ├── decisioniq-data-slices.yaml
│   ├── static/                # empty in inspected tree
│   └── templates/             # empty in inspected tree
├── src/main/java/com/app/decisioniq/
│   ├── DecisioniqApplication.java
│   ├── api/assistant/
│   │   ├── AgentApi.java
│   │   ├── model/
│   │   │   ├── AssistantAskRequest.java
│   │   │   └── AssistantAnswerResponse.java
│   │   └── validation/QuestionValidation.java
│   ├── assistant/
│   │   ├── orchestration/DecisionAssistantOrchestrator.java
│   │   ├── graph/
│   │   │   ├── config/DecisionGraphConfiguration.java
│   │   │   ├── node/*.java
│   │   │   ├── state/DecisionIQAgentState.java
│   │   │   └── constant/*.java
│   │   ├── intent/
│   │   │   ├── model/*.java
│   │   │   ├── prompt/IntentPromptTemplate.java
│   │   │   └── type/*.java
│   │   ├── dataslice/
│   │   │   ├── catalog/IntentToDataSliceMap.java
│   │   │   ├── config/*.java
│   │   │   └── type/DecisionDataSlice.java
│   │   └── planning/
│   │       ├── catalog/IntentToRagChunkTypeMap.java
│   │       └── model/*.java
│   ├── service/
│   │   ├── intent/IntentUnderstandingService.java
│   │   ├── planning/*.java
│   │   ├── evidence/*.java
│   │   └── answer/AnswerGenerationService.java
│   ├── llm/provider/openai/*.java
│   └── config/BeanConfiguration.java
└── src/test/java/com/app/decisioniq/DecisioniqApplicationTests.java
```

### Package descriptions
- `api.assistant`: HTTP boundary. Receives assistant questions and returns answer response.
- `assistant.orchestration`: Graph invocation wrapper.
- `assistant.graph.config`: LangGraph4j graph construction.
- `assistant.graph.node`: NodeAction implementations.
- `assistant.graph.state`: typed wrapper around graph state.
- `assistant.graph.constant`: graph node/state string constants.
- `assistant.intent`: intent JSON model, enum vocabulary, and prompt template.
- `service.intent`: OpenAI-backed intent parser.
- `assistant.dataslice`: business-level slice definitions and YAML binding.
- `assistant.planning`: QueryPlan model and intent-to-RAG mapping.
- `service.planning`: expands parsed intents into SQL/RAG plans.
- `service.evidence`: intended SQL/RAG retrieval layer. Incomplete.
- `service.answer`: intended answer generation layer. Stubbed.
- `llm.provider.openai`: OpenAI ChatModel bean.
- `config`: shared ObjectMapper bean.

## D. File by file summary

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/DecisioniqApplication.java`

- Class / interface / enum name: `DecisioniqApplication`

- Responsibility: Spring Boot entry point. Enables component scanning and configuration-properties scanning.

- Key methods: main(String[])

- Important fields: none

- Dependencies used: SpringApplication, @SpringBootApplication, @ConfigurationPropertiesScan

- Runtime fit: Starts the application and discovers all beans/configuration.

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/api/assistant/AgentApi.java`

- Class / interface / enum name: `AgentApi`

- Responsibility: REST API controller for assistant ask requests.

- Key methods: askLegacyAgent(AssistantAskRequest)

- Important fields: DecisionAssistantOrchestrator

- Dependencies used: Spring Web, AssistantAskRequest, AssistantAnswerResponse, QuestionValidation

- Runtime fit: Receives POST /agent/ask or /api/assistant/ask and delegates to graph orchestrator.

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/api/assistant/model/AssistantAskRequest.java`

- Class / interface / enum name: `AssistantAskRequest`

- Responsibility: Request DTO containing tenantId and question.

- Key methods: record accessors tenantId(), question()

- Important fields: tenantId, question

- Dependencies used: none

- Runtime fit: Input model for AgentApi.

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/api/assistant/model/AssistantAnswerResponse.java`

- Class / interface / enum name: `AssistantAnswerResponse`

- Responsibility: Response DTO containing answer text.

- Key methods: record accessor answer()

- Important fields: answer

- Dependencies used: none

- Runtime fit: Output model for AgentApi.

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/api/assistant/validation/QuestionValidation.java`

- Class / interface / enum name: `QuestionValidation`

- Responsibility: Validates and normalizes incoming assistant request.

- Key methods: normalizeQuestion(AssistantAskRequest)

- Important fields: none

- Dependencies used: ResponseStatusException, HttpStatus

- Runtime fit: Rejects blank question before orchestration.

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/assistant/orchestration/DecisionAssistantOrchestrator.java`

- Class / interface / enum name: `DecisionAssistantOrchestrator`

- Responsibility: Thin wrapper over compiled LangGraph4j graph.

- Key methods: answerQuestion(AssistantAskRequest)

- Important fields: CompiledGraph<DecisionIQAgentState>

- Dependencies used: LangGraph4j CompiledGraph

- Runtime fit: Seeds graph state with question and tenantId, invokes graph, returns final answer.

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/assistant/graph/config/DecisionGraphConfiguration.java`

- Class / interface / enum name: `DecisionGraphConfiguration`

- Responsibility: Defines LangGraph4j nodes, edges, and conditional route after intent parsing.

- Key methods: decisionAssistantGraph(...), routeAfterIntentClarification(DecisionIQAgentState)

- Important fields: none

- Dependencies used: LangGraph4j StateGraph, AsyncNodeAction, AsyncEdgeAction, EdgeMappings

- Runtime fit: Builds graph: start -> intent -> clarify or query planning -> evidence collection -> answer.

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/assistant/graph/node/IntentUnderstandingNode.java`

- Class / interface / enum name: `IntentUnderstandingNode`

- Responsibility: Graph node that calls LLM-backed intent parser.

- Key methods: apply(DecisionIQAgentState)

- Important fields: IntentUnderstandingService

- Dependencies used: LangGraph4j NodeAction

- Runtime fit: Reads question, writes ParsedQuestion into intent state key.

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/assistant/graph/node/IntentClarificationNode.java`

- Class / interface / enum name: `IntentClarificationNode`

- Responsibility: Graph node that returns clarification/error answer.

- Key methods: apply(DecisionIQAgentState)

- Important fields: none

- Dependencies used: LangGraph4j NodeAction

- Runtime fit: Writes answer when parsed intent requires clarification.

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/assistant/graph/node/QueryPlanningNode.java`

- Class / interface / enum name: `QueryPlanningNode`

- Responsibility: Graph node that converts parsed intent into QueryPlan.

- Key methods: apply(DecisionIQAgentState), buildQueryPlan(...), requireParsedQuestion(...)

- Important fields: QueryPlanningService

- Dependencies used: LangGraph4j NodeAction

- Runtime fit: Reads parsed question and tenantId, writes QueryPlan into graph state.

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/assistant/graph/node/EvidenceCollectionNode.java`

- Class / interface / enum name: `EvidenceCollectionNode`

- Responsibility: Graph node intended to retrieve evidence from SQL/RAG.

- Key methods: apply(DecisionIQAgentState)

- Important fields: EvidenceService

- Dependencies used: LangGraph4j NodeAction

- Runtime fit: Reads QueryPlan, invokes EvidenceService. Currently writes no evidence back to state.

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/assistant/graph/node/AnswerGenerationNode.java`

- Class / interface / enum name: `AnswerGenerationNode`

- Responsibility: Graph node intended to create final answer.

- Key methods: apply(DecisionIQAgentState)

- Important fields: AnswerGenerationService

- Dependencies used: LangGraph4j NodeAction

- Runtime fit: Currently ignores evidence/service and writes static answer "Hello Answered".

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/assistant/graph/constant/DecisionGraphNode.java`

- Class / interface / enum name: `DecisionGraphNode`

- Responsibility: String constants for graph node names.

- Key methods: none

- Important fields: INTENT_UNDERSTANDING, INTENT_CLARIFICATION, QUERY_PLANNING, EVIDENCE_COLLECTION, ANSWER_GENERATION

- Dependencies used: none

- Runtime fit: Used by graph config to register/connect nodes.

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/assistant/graph/constant/DecisionGraphStateKey.java`

- Class / interface / enum name: `DecisionGraphStateKey`

- Responsibility: String constants for graph state keys.

- Key methods: none

- Important fields: QUESTION_KEY, TENANT_ID_KEY, INTENT_UNDERSTANDING_KEY, QUERY_PLANNING_KEY, EVIDENCE_COLLECTION_KEY, ANSWER_KEY

- Dependencies used: none

- Runtime fit: Used by nodes/state wrapper to read/write graph state.

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/assistant/graph/state/DecisionIQAgentState.java`

- Class / interface / enum name: `DecisionIQAgentState`

- Responsibility: Typed accessor wrapper around LangGraph AgentState.

- Key methods: tenantId(), question(), intent(), clarifyIntent(), collectEvidence(), getQuery(), answer()

- Important fields: none

- Dependencies used: LangGraph4j AgentState

- Runtime fit: Allows nodes to safely retrieve typed graph values. collectEvidence currently expects String and is marked TODO.

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/assistant/intent/model/ParsedQuestion.java`

- Class / interface / enum name: `ParsedQuestion`

- Responsibility: LLM intent parse result object.

- Key methods: Lombok-generated getters/setters

- Important fields: status, transactionId, asks, decisionAssumption, clarificationRequired, cud, clarificationQuestion

- Dependencies used: Lombok @Data

- Runtime fit: Carries parsed asks from intent understanding to clarification/planning.

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/assistant/intent/model/ParsedAsk.java`

- Class / interface / enum name: `ParsedAsk`

- Responsibility: One parsed ask inside a user question.

- Key methods: record accessors

- Important fields: intent, transactionId

- Dependencies used: DecisionIqIntent

- Runtime fit: Used by QueryPlanningService to map each ask to SQL/RAG evidence needs.

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/assistant/intent/prompt/IntentPromptTemplate.java`

- Class / interface / enum name: `IntentPromptTemplate`

- Responsibility: Prompt instructions for strict JSON intent parser.

- Key methods: none

- Important fields: INSTRUCTIONS

- Dependencies used: none

- Runtime fit: Defines LLM behavior for intent parsing, CRUD detection, clarification, and allowed JSON shape.

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/assistant/intent/type/DecisionIqIntent.java`

- Class / interface / enum name: `DecisionIqIntent`

- Responsibility: Intent enum for fraud decision assistant.

- Key methods: enum values

- Important fields: EXPLAIN_TRANSACTION_DECISION, EXPLAIN_APPROVAL, SHOW_MODEL_SCORE, etc.

- Dependencies used: none

- Runtime fit: LLM output and planner input intent vocabulary.

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/assistant/intent/type/DecisionAssumption.java`

- Class / interface / enum name: `DecisionAssumption`

- Responsibility: Decision assumption enum.

- Key methods: enum values

- Important fields: APPROVED, DECLINED, REVIEW

- Dependencies used: none

- Runtime fit: Part of ParsedQuestion.

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/assistant/intent/type/ParsedQuestionStatus.java`

- Class / interface / enum name: `ParsedQuestionStatus`

- Responsibility: Parsed question status enum.

- Key methods: enum values

- Important fields: VALID, CLARIFICATION_REQUIRED, UNKNOWN

- Dependencies used: none

- Runtime fit: Part of ParsedQuestion.

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/service/intent/IntentUnderstandingService.java`

- Class / interface / enum name: `IntentUnderstandingService`

- Responsibility: Builds intent prompt, calls OpenAI chat model, parses JSON into ParsedQuestion.

- Key methods: parseIntent(String), buildPrompt(String), countWords(String), parseAndFlagIntentResponse(String), parseJsonIntentResponse(String), validateIntentResponse(ParsedQuestion)

- Important fields: ChatModel, ObjectMapper

- Dependencies used: LangChain4j ChatModel, Jackson ObjectMapper

- Runtime fit: Used by IntentUnderstandingNode.

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/assistant/dataslice/type/DecisionDataSlice.java`

- Class / interface / enum name: `DecisionDataSlice`

- Responsibility: Business-level data slice enum.

- Key methods: enum values

- Important fields: TRANSACTION_DECISION_SLICE, DECISION_EXPLANATION_SLICE

- Dependencies used: none

- Runtime fit: Intent-to-slice mapping target.

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/assistant/dataslice/catalog/IntentToDataSliceMap.java`

- Class / interface / enum name: `IntentToDataSliceMap`

- Responsibility: Static catalog mapping intents to data slices.

- Key methods: slicesFor(DecisionIqIntent)

- Important fields: INTENT_TO_DATA_SLICES

- Dependencies used: DecisionIqIntent, DecisionDataSlice

- Runtime fit: Currently maps only EXPLAIN_TRANSACTION_DECISION.

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/assistant/dataslice/config/AssistantDataSliceProperties.java`

- Class / interface / enum name: `AssistantDataSliceProperties`

- Responsibility: Configuration properties root for YAML data slices.

- Key methods: getDataSlices() via Lombok

- Important fields: dataSlices

- Dependencies used: @ConfigurationProperties

- Runtime fit: Binds decisioniq.assistant.data-slices from YAML.

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/assistant/dataslice/config/DataSliceProperties.java`

- Class / interface / enum name: `DataSliceProperties`

- Responsibility: Record for one YAML data slice.

- Key methods: record accessors

- Important fields: description, tables

- Dependencies used: TableFieldsProperties

- Runtime fit: Represents business slice metadata and table mappings.

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/assistant/dataslice/config/TableFieldsProperties.java`

- Class / interface / enum name: `TableFieldsProperties`

- Responsibility: Record for one table field list in YAML.

- Key methods: record accessor fields()

- Important fields: fields

- Dependencies used: none

- Runtime fit: Represents fields to fetch from a table.

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/assistant/dataslice/config/DataSliceResolver.java`

- Class / interface / enum name: `DataSliceResolver`

- Responsibility: Bean to resolve a configured data slice.

- Key methods: resolve(DecisionDataSlice)

- Important fields: AssistantDataSliceProperties

- Dependencies used: Spring Component

- Runtime fit: Currently not used by SQLQueryPlanGenerator, which reads properties directly.

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/assistant/planning/catalog/IntentToRagChunkTypeMap.java`

- Class / interface / enum name: `IntentToRagChunkTypeMap`

- Responsibility: Static catalog mapping intents to RAG chunk types.

- Key methods: chunkTypesFor(DecisionIqIntent)

- Important fields: INTENT_TO_CHUNK_TYPES

- Dependencies used: DecisionIqIntent

- Runtime fit: Currently maps only EXPLAIN_TRANSACTION_DECISION to three chunk types.

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/assistant/planning/model/QueryPlan.java`

- Class / interface / enum name: `QueryPlan`

- Responsibility: Planning output carrying tenant, transaction, intents, SQL table/field map, and RAG chunk map.

- Key methods: Lombok-generated getters/setters

- Important fields: tenantId, transactionId, intents, ragCollectionName, sqlTableFieldsMap, ragMap

- Dependencies used: MultiValueMap, DecisionIqIntent

- Runtime fit: Main bridge from QueryPlanningNode to evidence retrieval.

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/assistant/planning/model/SqlData.java`

- Class / interface / enum name: `SqlData`

- Responsibility: Unused SQL evidence requirement model.

- Key methods: Lombok-generated getters/setters

- Important fields: tables

- Dependencies used: SqlTableSelection

- Runtime fit: Not currently wired into QueryPlan flow.

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/assistant/planning/model/SqlTableSelection.java`

- Class / interface / enum name: `SqlTableSelection`

- Responsibility: Unused model for table name and fields.

- Key methods: Lombok-generated getters/setters

- Important fields: name, fields

- Dependencies used: none

- Runtime fit: Not currently wired into QueryPlan flow.

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/service/planning/QueryPlanningService.java`

- Class / interface / enum name: `QueryPlanningService`

- Responsibility: Builds QueryPlan from ParsedQuestion.

- Key methods: planQuery(...), buildQueryPlanMetaData(...), getDetails(...), getDataSliceForIntent(...)

- Important fields: SQLQueryPlanGenerator

- Dependencies used: IntentToDataSliceMap, IntentToRagChunkTypeMap

- Runtime fit: Used by QueryPlanningNode.

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/service/planning/SQLQueryPlanGenerator.java`

- Class / interface / enum name: `SQLQueryPlanGenerator`

- Responsibility: Expands decision data slices into intent-keyed SQL table/field plan.

- Key methods: getDataForEachSQLSlices(...), buildSqlData(...), validateAndAddValue(...)

- Important fields: AssistantDataSliceProperties

- Dependencies used: Spring MultiValueMap

- Runtime fit: Called by QueryPlanningService for every parsed ask.

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/service/planning/RAGQueryGenerator.java`

- Class / interface / enum name: `RAGQueryGenerator`

- Responsibility: Adds RAG collection and chunk types to QueryPlan.

- Key methods: buildRAGQueryPlan(...)

- Important fields: none

- Dependencies used: QueryPlan

- Runtime fit: Called by QueryPlanningService for every parsed ask.

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/service/evidence/EvidenceService.java`

- Class / interface / enum name: `EvidenceService`

- Responsibility: Coordinator for evidence collection.

- Key methods: initiateEvidenceCollectionMechanism(QueryPlan)

- Important fields: SQLDataFetchService, RAGDataFetchService

- Dependencies used: Spring Service

- Runtime fit: Currently calls only SQLDataFetchService, not RAG.

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/service/evidence/SQLDataFetchService.java`

- Class / interface / enum name: `SQLDataFetchService`

- Responsibility: Intended JDBC data fetch service.

- Key methods: initiateQuery(QueryPlan), prepareQuery(MultiValueMap<...>)

- Important fields: JdbcTemplate

- Dependencies used: Spring JDBC

- Runtime fit: Currently loops plan but prepareQuery is empty and no data is returned.

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/service/evidence/RAGDataFetchService.java`

- Class / interface / enum name: `RAGDataFetchService`

- Responsibility: Placeholder RAG evidence service.

- Key methods: none

- Important fields: none

- Dependencies used: Spring Service

- Runtime fit: Currently empty.

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/service/answer/AnswerGenerationService.java`

- Class / interface / enum name: `AnswerGenerationService`

- Responsibility: Placeholder answer service.

- Key methods: generateAnswer(String)

- Important fields: none

- Dependencies used: Spring Service

- Runtime fit: Currently returns static "answer completed" and is not used by node.

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/llm/provider/openai/OpenAiModelConfiguration.java`

- Class / interface / enum name: `OpenAiModelConfiguration`

- Responsibility: Creates LangChain4j OpenAI ChatModel bean.

- Key methods: openAiChatModel(OpenAiProperties)

- Important fields: none

- Dependencies used: OpenAiResponsesChatModel, OpenAiProperties

- Runtime fit: Provides @Qualifier("openAiChatModel") dependency for IntentUnderstandingService.

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/llm/provider/openai/OpenAiProperties.java`

- Class / interface / enum name: `OpenAiProperties`

- Responsibility: Configuration properties for OpenAI provider.

- Key methods: record accessors

- Important fields: baseUrl, apiKey, model

- Dependencies used: @ConfigurationProperties

- Runtime fit: Bound from openai.* properties in application.yaml.

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/config/BeanConfiguration.java`

- Class / interface / enum name: `BeanConfiguration`

- Responsibility: General bean configuration.

- Key methods: initializeObjectMapper()

- Important fields: none

- Dependencies used: ObjectMapper

- Runtime fit: Provides Jackson ObjectMapper for intent parsing.

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/test/java/com/app/decisioniq/DecisioniqApplicationTests.java`

- Class / interface / enum name: `DecisioniqApplicationTests`

- Responsibility: Default Spring Boot context load test.

- Key methods: contextLoads()

- Important fields: none

- Dependencies used: SpringBootTest

- Runtime fit: Currently fails due missing JdbcTemplate bean.

## E. End to end execution flow

### One full request lifecycle

1. Controller: `AgentApi.askLegacyAgent` receives `AssistantAskRequest` over `POST /agent/ask` or `POST /api/assistant/ask`. The controller supports JSON input and JSON output, with CORS allowing `http://localhost:5173`.

2. Validation: `QuestionValidation.normalizeQuestion` checks `question` for null/blank and throws `400 BAD_REQUEST` if missing. It does not validate `tenantId`.

3. Orchestration: `DecisionAssistantOrchestrator.answerQuestion` invokes `CompiledGraph<DecisionIQAgentState>` with state keys `question` and `tenantId`.

4. Graph start: `DecisionGraphConfiguration` sends `START` to `intent_understanding`.

5. Intent processing: `IntentUnderstandingNode` calls `IntentUnderstandingService.parseIntent`. The service builds a prompt from `IntentPromptTemplate.INSTRUCTIONS`, the user question, and all `DecisionIqIntent` enum values. It calls `ChatModel.chat(...)` and parses the raw response into `ParsedQuestion` using Jackson.

6. State transition: `IntentUnderstandingNode` writes `ParsedQuestion` under `DecisionGraphStateKey.INTENT_UNDERSTANDING_KEY`, which is currently the string `intentKey`.

7. Conditional branch: `routeAfterIntentClarification` checks `state.clarifyIntent()`. If present and `clarificationRequired == true`, graph routes to `intent_clarification`; otherwise it routes to `query_planning`.

8. Clarification path: `IntentClarificationNode` writes `clarificationQuestion` to `answer` and graph ends. If no parsed question exists, it writes `Cannot answer the question, unable to find the intent`.

9. Query planning path: `QueryPlanningNode` gets `ParsedQuestion`, calls `QueryPlanningService.planQuery(parsedQuestion, tenantId)`, and writes `QueryPlan` under `queryPlanning`.

10. Query planning details: `QueryPlanningService` sets tenantId and transactionId, iterates parsed asks, maps each intent to `DecisionDataSlice` values through `IntentToDataSliceMap`, expands slice YAML into SQL table/fields through `SQLQueryPlanGenerator`, maps intent to RAG chunk types through `IntentToRagChunkTypeMap`, and stores intents.

11. Data fetching: `EvidenceCollectionNode` reads `state.getQuery()`. If present, it calls `EvidenceService.initiateEvidenceCollectionMechanism(query)`. `EvidenceService` currently calls only `SQLDataFetchService.initiateQuery(query)`. `SQLDataFetchService` loops intent maps but `prepareQuery(...)` is empty. No SQL is executed and no evidence result is returned.

12. State after evidence: `EvidenceCollectionNode` returns `Map.of()`, so `DecisionGraphStateKey.EVIDENCE_COLLECTION_KEY` is not written.

13. Answer generation: `AnswerGenerationNode` logs the query plan and writes static `answer = "Hello Answered"`. It does not call `AnswerGenerationService.generateAnswer`, does not read SQL evidence, and does not read RAG evidence.

14. Final response: orchestrator reads `finalState.answer()` and `AgentApi` wraps it in `AssistantAnswerResponse`. Current successful runtime answer would be static if the app starts and reaches answer generation.

## F. Graph and assistant logic

### Graph nodes
- `intent_understanding`: implemented by `IntentUnderstandingNode`. Reads `question`; writes `intentKey` with `ParsedQuestion`.
- `intent_clarification`: implemented by `IntentClarificationNode`. Reads `intentKey`; writes `answer`. Ends graph.
- `query_planning`: implemented by `QueryPlanningNode`. Reads `intentKey` and `tenantId`; writes `queryPlanning` with `QueryPlan`.
- `evidence_collection`: implemented by `EvidenceCollectionNode`. Reads `queryPlanning`; currently calls evidence service and writes nothing.
- `answer_generation`: implemented by `AnswerGenerationNode`. Reads `queryPlanning` for logging; writes static `answer`.

### Node order
```text
START
  -> intent_understanding
      -> if clarificationRequired: intent_clarification -> END
      -> else: query_planning -> evidence_collection -> answer_generation -> END
```

### Conditions / branching
Only one branch exists: after `intent_understanding`, `routeAfterIntentClarification` checks whether parsed question exists and has `clarificationRequired == true`. It does not check `status`, `UNKNOWN`, or `cud` directly.

### State keys used
- `question`: original user question.
- `tenantId`: tenant identifier from request.
- `intentKey`: parsed intent payload.
- `queryPlanning`: built QueryPlan.
- `evidenceCollectionKey`: intended evidence key, currently not written by any node.
- `answer`: final answer string.

### What each node reads and writes
| Node | Reads | Writes | Current status |
|---|---|---|---|
| IntentUnderstandingNode | `question` | `intentKey` | Implemented with OpenAI call |
| IntentClarificationNode | `intentKey` | `answer` | Implemented |
| QueryPlanningNode | `intentKey`, `tenantId` | `queryPlanning` | Implemented |
| EvidenceCollectionNode | `queryPlanning` | nothing | Incomplete |
| AnswerGenerationNode | `queryPlanning` | `answer` | Stubbed static answer |

## G. Data and query model

### `QueryPlan`
`QueryPlan` is the planning object passed from query planning to evidence collection. It contains:
- `tenantId`: tenant from request.
- `transactionId`: transaction parsed from the question.
- `intents`: parsed intents from `ParsedAsk`.
- `ragCollectionName`: set to `decisioniq_evidence_chunks` by `RAGQueryGenerator` when RAG chunks exist.
- `sqlTableFieldsMap`: `Map<String, MultiValueMap<String, List<String>>>`. The outer key is intent name. The inner key is real table name. The inner value is a Spring `MultiValueMap` value, effectively `List<List<String>>`, because each table may receive multiple field groups from multiple slices.
- `ragMap`: `Map<String, List<String>>` keyed by intent name, with chunk types as values.

Example current plan for `EXPLAIN_TRANSACTION_DECISION` and transaction `0012`:
```json
{
  "tenantId": "tenant-citi-bank",
  "transactionId": "0012",
  "intents": ["EXPLAIN_TRANSACTION_DECISION"],
  "ragCollectionName": "decisioniq_evidence_chunks",
  "sqlTableFieldsMap": {
    "EXPLAIN_TRANSACTION_DECISION": {
      "decision_cases": [
        ["case_id", "correlation_id", "customer_ref", "account_ref", "event_time", "decision_name", "scenario_type"],
        ["case_id", "correlation_id", "customer_ref"]
      ],
      "transaction_context": [
        ["amount", "currency", "transaction_type", "channel", "merchant_name", "merchant_category", "transaction_country", "transaction_city"]
      ],
      "decision_outcomes": [
        ["decision_action"]
      ],
      "model_outputs": [
        ["model_id", "score_name", "score", "risk_band"]
      ],
      "rule_evaluations": [
        ["rule_name", "rule_meaning", "status", "decision_candidate"]
      ],
      "decision_reason_codes": [
        ["reason_order", "reason_text"]
      ],
      "risk_signals": [
        ["known_device", "known_beneficiary", "known_merchant", "international_transaction", "odd_hour", "authentication_method", "failed_login_count_last_hour", "amount_above_balance"]
      ]
    }
  },
  "ragMap": {
    "EXPLAIN_TRANSACTION_DECISION": [
      "DECISION_CASE_SUMMARY",
      "MODEL_SCORE_RISK",
      "TRANSACTION_RISK_SIGNALS"
    ]
  }
}
```

### `SQLDataFetchService`
`SQLDataFetchService` is intended to consume `QueryPlan.sqlTableFieldsMap`, construct SQL queries using `JdbcTemplate`, execute them, and return evidence. Current implementation only loops through `query.getSqlTableFieldsMap().entrySet()` and calls empty `prepareQuery(...)`. It returns `void`. There is no current query execution, no table/field validation, no result model, and no state update.

### DB query construction logic
Current code does not implement DB query construction. The table and field plan is built earlier by `SQLQueryPlanGenerator`. To execute it safely, the evidence layer would need to flatten `List<List<String>>` into unique field lists per table, validate table/field names against configured/allowed metadata, construct SQL using validated identifiers, and bind values such as `tenantId` and `transactionId` through `JdbcTemplate` parameters. This is not present in current code.

### Request / response / intent / model classes
- `AssistantAskRequest`: public API request with `tenantId` and `question`.
- `AssistantAnswerResponse`: public API response with `answer`.
- `ParsedQuestion`: LLM parse result with status, transactionId, asks, decision assumption, clarification flag, CRUD flag, and clarification question.
- `ParsedAsk`: one ask with `DecisionIqIntent` and transactionId.
- `DecisionIqIntent`: vocabulary of supported assistant intents. Many are defined but only `EXPLAIN_TRANSACTION_DECISION` is mapped to SQL/RAG.
- `DecisionAssumption`: approved/declined/review enum.
- `ParsedQuestionStatus`: valid/clarification/unknown enum.
- `DecisionDataSlice`: business slice enum; currently two values.
- `DataSliceProperties` and `TableFieldsProperties`: YAML binding records for data slices and table fields.
- `SqlData` and `SqlTableSelection`: older/planned SQL model classes, currently unused.

### Incomplete or stubbed logic
- `SQLDataFetchService.prepareQuery` is empty.
- `RAGDataFetchService` is empty.
- `EvidenceService` does not call RAG service.
- `EvidenceCollectionNode` does not write evidence to graph state.
- `AnswerGenerationNode` returns static `Hello Answered`.
- `AnswerGenerationService` returns static `answer completed` and is unused.
- No evidence bundle/model exists.
- No datasource config or `JdbcTemplate` bean exists in current app context.
- No memory/session context exists for follow-up questions.

## H. Important code snippets

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/DecisioniqApplication.java`

```java
package com.app.decisioniq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DecisioniqApplication {

	public static void main(String[] args) {
		SpringApplication.run(DecisioniqApplication.class, args);
	}

}

```

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/api/assistant/AgentApi.java`

```java
package com.app.decisioniq.api.assistant;

import com.app.decisioniq.api.assistant.model.AssistantAskRequest;
import com.app.decisioniq.api.assistant.model.AssistantAnswerResponse;
import com.app.decisioniq.api.assistant.validation.QuestionValidation;
import com.app.decisioniq.assistant.orchestration.DecisionAssistantOrchestrator;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping({"/agent", "/api/assistant"})
public class AgentApi {

    private final DecisionAssistantOrchestrator decisionAssistantOrchestrator;

    public AgentApi(DecisionAssistantOrchestrator decisionAssistantOrchestrator) {
        this.decisionAssistantOrchestrator = decisionAssistantOrchestrator;
    }

    @PostMapping(
            value = "/ask",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public AssistantAnswerResponse askLegacyAgent(@RequestBody AssistantAskRequest assistantAskRequest) {
        String answer = decisionAssistantOrchestrator.answerQuestion(QuestionValidation.normalizeQuestion(assistantAskRequest));
        return new AssistantAnswerResponse(answer);
    }
}

```

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/api/assistant/model/AssistantAskRequest.java`

```java
package com.app.decisioniq.api.assistant.model;

/**
 * Request payload accepted by the assistant UI.
 * tenantId and userRole are part of the public API contract so the graph can enforce
 * tenant-scoped retrieval and role-aware answers as those stages are connected.
 */
public record AssistantAskRequest(
        String tenantId,
        String question
) { }

```

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/api/assistant/model/AssistantAnswerResponse.java`

```java
package com.app.decisioniq.api.assistant.model;

/**
 * Response payload consumed by the React assistant UI.
 */
public record AssistantAnswerResponse(String answer) {
}

```

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/api/assistant/validation/QuestionValidation.java`

```java
package com.app.decisioniq.api.assistant.validation;

import com.app.decisioniq.api.assistant.model.AssistantAskRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class QuestionValidation {

    public static AssistantAskRequest normalizeQuestion(AssistantAskRequest assistantAskRequest) {
        if (assistantAskRequest.question() == null || assistantAskRequest.question().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question is required");
        }
        return assistantAskRequest;
    }

}

```

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/assistant/graph/config/DecisionGraphConfiguration.java`

```java
package com.app.decisioniq.assistant.graph.config;

import com.app.decisioniq.assistant.graph.constant.DecisionGraphNode;
import com.app.decisioniq.assistant.graph.node.*;
import com.app.decisioniq.assistant.graph.state.DecisionIQAgentState;
import com.app.decisioniq.assistant.intent.model.ParsedQuestion;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphDefinition;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncEdgeAction;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.utils.EdgeMappings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration
public class DecisionGraphConfiguration {

    @Bean
    public CompiledGraph<DecisionIQAgentState> decisionAssistantGraph(IntentUnderstandingNode intentUnderstandingNode,
            IntentClarificationNode intentClarificationNode,
            QueryPlanningNode queryPlanningNode,
            EvidenceCollectionNode evidenceCollectionNode,
            AnswerGenerationNode answerGenerationNode
    ) throws GraphStateException {
        StateGraph<DecisionIQAgentState> graph = new StateGraph<>(DecisionIQAgentState::new);

        graph.addNode(DecisionGraphNode.INTENT_UNDERSTANDING, AsyncNodeAction.node_async(intentUnderstandingNode));
        graph.addNode(DecisionGraphNode.INTENT_CLARIFICATION, AsyncNodeAction.node_async(intentClarificationNode));
        graph.addNode(DecisionGraphNode.QUERY_PLANNING, AsyncNodeAction.node_async(queryPlanningNode));
        graph.addNode(DecisionGraphNode.EVIDENCE_COLLECTION,AsyncNodeAction.node_async(evidenceCollectionNode));
        graph.addNode(DecisionGraphNode.ANSWER_GENERATION, AsyncNodeAction.node_async(answerGenerationNode));

        graph.addEdge(GraphDefinition.START, DecisionGraphNode.INTENT_UNDERSTANDING);
        graph.addConditionalEdges(
                DecisionGraphNode.INTENT_UNDERSTANDING,
                AsyncEdgeAction.edge_async(this::routeAfterIntentClarification),
                EdgeMappings.builder()
                        .to(DecisionGraphNode.INTENT_CLARIFICATION,"clarification")
                        .to(DecisionGraphNode.QUERY_PLANNING,"continue")
                        .build()
        );
        graph.addEdge(DecisionGraphNode.INTENT_CLARIFICATION, GraphDefinition.END);
        graph.addEdge(DecisionGraphNode.QUERY_PLANNING, DecisionGraphNode.EVIDENCE_COLLECTION);
        graph.addEdge(DecisionGraphNode.EVIDENCE_COLLECTION,DecisionGraphNode.ANSWER_GENERATION);
        graph.addEdge(DecisionGraphNode.ANSWER_GENERATION, GraphDefinition.END);

        return graph.compile();
    }

    private String routeAfterIntentClarification(DecisionIQAgentState state){
        Optional<ParsedQuestion> parsedQuestion = state.clarifyIntent();
        if (parsedQuestion.isPresent() && parsedQuestion.get().isClarificationRequired()){
            return "clarification";
        }
        return "continue";
    }
}

```

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/assistant/graph/node/IntentUnderstandingNode.java`

```java
package com.app.decisioniq.assistant.graph.node;

import com.app.decisioniq.assistant.graph.constant.DecisionGraphStateKey;
import com.app.decisioniq.assistant.graph.state.DecisionIQAgentState;
import com.app.decisioniq.assistant.intent.model.ParsedQuestion;
import com.app.decisioniq.service.intent.IntentUnderstandingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class IntentUnderstandingNode implements NodeAction<DecisionIQAgentState> {

    private final IntentUnderstandingService intentUnderstandingService;

    public IntentUnderstandingNode(IntentUnderstandingService intentUnderstandingService) {
        this.intentUnderstandingService = intentUnderstandingService;
    }

    @Override
    public Map<String, Object> apply(DecisionIQAgentState state) throws JsonProcessingException {
        ParsedQuestion parsedQuestionJson = intentUnderstandingService.parseIntent(state.question());
        return Map.of(DecisionGraphStateKey.INTENT_UNDERSTANDING_KEY, parsedQuestionJson);
    }

}

```

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/assistant/graph/node/IntentClarificationNode.java`

```java
package com.app.decisioniq.assistant.graph.node;

import com.app.decisioniq.assistant.graph.constant.DecisionGraphStateKey;
import com.app.decisioniq.assistant.graph.state.DecisionIQAgentState;
import com.app.decisioniq.assistant.intent.model.ParsedQuestion;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class IntentClarificationNode implements NodeAction<DecisionIQAgentState> {

    @Override
    public Map<String, Object> apply(DecisionIQAgentState state) {
        String message="Cannot answer the question, unable to find the intent";
        Optional<ParsedQuestion> intentClarification = state.clarifyIntent();
        if (intentClarification.isPresent()){
            message=intentClarification.get().getClarificationQuestion();
        }
        return Map.of(DecisionGraphStateKey.ANSWER_KEY,message);
    }
}

```

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/assistant/graph/node/QueryPlanningNode.java`

```java
package com.app.decisioniq.assistant.graph.node;

import com.app.decisioniq.assistant.graph.constant.DecisionGraphStateKey;
import com.app.decisioniq.assistant.graph.state.DecisionIQAgentState;
import com.app.decisioniq.assistant.intent.model.ParsedQuestion;
import com.app.decisioniq.assistant.planning.model.QueryPlan;
import com.app.decisioniq.service.planning.QueryPlanningService;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class QueryPlanningNode implements NodeAction<DecisionIQAgentState> {

    private final QueryPlanningService queryPlanningService;

    public QueryPlanningNode(QueryPlanningService queryPlanningService) {
        this.queryPlanningService = queryPlanningService;
    }

    @Override
    public Map<String, Object> apply(DecisionIQAgentState state) {
        QueryPlan queryPlan = buildQueryPlan(state);
        return Map.of(DecisionGraphStateKey.QUERY_PLANNING_KEY,queryPlan);
    }
    private QueryPlan buildQueryPlan(DecisionIQAgentState state) {
        ParsedQuestion parsedQuestion = requireParsedQuestion(state);
        return queryPlanningService.planQuery(parsedQuestion, state.tenantId());
    }

    private ParsedQuestion requireParsedQuestion(DecisionIQAgentState state) {
        return state.clarifyIntent()
                .orElseThrow(() -> new IllegalStateException(
                        "Parsed question missing before query planning. Check graph routing from IntentUnderstandingNode."
                ));
    }}

```

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/assistant/graph/node/EvidenceCollectionNode.java`

```java
package com.app.decisioniq.assistant.graph.node;

import com.app.decisioniq.assistant.graph.state.DecisionIQAgentState;
import com.app.decisioniq.assistant.planning.model.QueryPlan;
import com.app.decisioniq.service.evidence.EvidenceService;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class EvidenceCollectionNode implements NodeAction<DecisionIQAgentState> {

    private final EvidenceService evidenceService;

    @Autowired
    public EvidenceCollectionNode(EvidenceService evidenceService) {
        this.evidenceService = evidenceService;
    }

    @Override
    public Map<String, Object> apply(DecisionIQAgentState state) {
        Optional<QueryPlan> query = state.getQuery();
        query.ifPresent(evidenceService::initiateEvidenceCollectionMechanism);
        return Map.of();
    }
}

```

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/assistant/graph/node/AnswerGenerationNode.java`

```java
package com.app.decisioniq.assistant.graph.node;

import com.app.decisioniq.assistant.graph.constant.DecisionGraphStateKey;
import com.app.decisioniq.assistant.graph.state.DecisionIQAgentState;
import com.app.decisioniq.service.answer.AnswerGenerationService;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.bsc.langgraph4j.state.AgentState;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.app.decisioniq.assistant.graph.constant.DecisionGraphStateKey.ANSWER_KEY;

@Component
@Slf4j
public class AnswerGenerationNode implements NodeAction<DecisionIQAgentState> {

    private final AnswerGenerationService answerGenerationService;

    public AnswerGenerationNode(AnswerGenerationService answerGenerationService) {
        this.answerGenerationService = answerGenerationService;
    }

    @Override
    public Map<String, Object> apply(DecisionIQAgentState state) {
        log.info("Generating answer for the query {}",state.getQuery());
        return Map.of(ANSWER_KEY,"Hello Answered");
    }
}

```

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/assistant/graph/state/DecisionIQAgentState.java`

```java
package com.app.decisioniq.assistant.graph.state;

import com.app.decisioniq.assistant.graph.constant.DecisionGraphStateKey;
import com.app.decisioniq.assistant.intent.model.ParsedQuestion;
import com.app.decisioniq.assistant.planning.model.QueryPlan;
import org.bsc.langgraph4j.state.AgentState;

import java.util.Map;
import java.util.Optional;

public class DecisionIQAgentState extends AgentState {

    public DecisionIQAgentState(Map<String, Object> initData) {
        super(initData);
    }

    public String tenantId(){
        return this.<String>value(DecisionGraphStateKey.TENANT_ID_KEY)
                .orElseThrow(()->new IllegalStateException("TenantId is not found"));
    }

    public String question() {
        return this.<String>value(DecisionGraphStateKey.QUESTION_KEY)
                .orElseThrow(() -> new IllegalStateException("Question missing from graph state"));
    }

    public Optional<ParsedQuestion> intent() {
        return this.<ParsedQuestion>value(DecisionGraphStateKey.INTENT_UNDERSTANDING_KEY);
    }

    public Optional<ParsedQuestion> clarifyIntent(){
        return this.value(DecisionGraphStateKey.INTENT_UNDERSTANDING_KEY);
    }

    //Need to change the return type
    public String collectEvidence(){
        return this.<String>value(DecisionGraphStateKey.EVIDENCE_COLLECTION_KEY)
                .orElseThrow(() -> new IllegalStateException("Question missing from graph state"));
    }

    public Optional<QueryPlan> getQuery() {
        return this.value(DecisionGraphStateKey.QUERY_PLANNING_KEY);
    }

    public String answer() {
        return this.<String>value(DecisionGraphStateKey.ANSWER_KEY)
                .orElseThrow(() -> new IllegalStateException("Answer missing from graph state"));
    }

}

```

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/assistant/graph/constant/DecisionGraphNode.java`

```java
package com.app.decisioniq.assistant.graph.constant;

public final class DecisionGraphNode {

    private DecisionGraphNode() {
    }

    public static final String INTENT_UNDERSTANDING = "intent_understanding";
    public static final String INTENT_CLARIFICATION = "intent_clarification";
    public static final String QUERY_PLANNING = "query_planning";
    public static final String EVIDENCE_COLLECTION="evidence_collection";
    public static final String ANSWER_GENERATION = "answer_generation";
}

```

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/assistant/graph/constant/DecisionGraphStateKey.java`

```java
package com.app.decisioniq.assistant.graph.constant;

public final class DecisionGraphStateKey {

    private DecisionGraphStateKey() {
    }

    public static final String QUESTION_KEY = "question";
    public static final String TENANT_ID_KEY = "tenantId";
    public static final String INTENT_UNDERSTANDING_KEY = "intentKey";
    public static final String QUERY_PLANNING_KEY = "queryPlanning";
    public static final String EVIDENCE_COLLECTION_KEY="evidenceCollectionKey";
    public static final String ANSWER_KEY = "answer";
}

```

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/assistant/orchestration/DecisionAssistantOrchestrator.java`

```java
package com.app.decisioniq.assistant.orchestration;

import com.app.decisioniq.api.assistant.model.AssistantAskRequest;
import com.app.decisioniq.assistant.graph.constant.DecisionGraphStateKey;
import com.app.decisioniq.assistant.graph.state.DecisionIQAgentState;
import org.bsc.langgraph4j.CompiledGraph;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DecisionAssistantOrchestrator {

    private final CompiledGraph<DecisionIQAgentState> decisionAssistantGraph;

    public DecisionAssistantOrchestrator(CompiledGraph<DecisionIQAgentState> decisionAssistantGraph) {
        this.decisionAssistantGraph = decisionAssistantGraph;
    }

    public String answerQuestion(AssistantAskRequest assistantAskRequest) {
        DecisionIQAgentState finalState = decisionAssistantGraph.invoke(Map.of(
                DecisionGraphStateKey.QUESTION_KEY, assistantAskRequest.question(),
                DecisionGraphStateKey.TENANT_ID_KEY,assistantAskRequest.tenantId()
        )).orElseThrow(() -> new IllegalStateException("Graph did not return a final state"));

        return finalState.answer();
    }
}

```

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/assistant/planning/model/QueryPlan.java`

```java
package com.app.decisioniq.assistant.planning.model;

import com.app.decisioniq.assistant.intent.type.DecisionIqIntent;
import lombok.Data;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Internal data-getQuery plan produced by getQuery planning.
 *
 * <p>The plan tells the downstream evidence fetch nodes what SQL evidence and RAG evidence are
 * required for the parsed user intent. It does not execute SQL, call Milvus, or call the LLM.</p>
 */
@Data
public class QueryPlan implements Serializable {

    private String tenantId;
    private String transactionId;
    private List<DecisionIqIntent> intents;
    private String ragCollectionName;
    private Map<String,MultiValueMap<String,List<String >>> sqlTableFieldsMap =new HashMap<>();
    private Map<String,List<String>> ragMap=new HashMap<>();
}

```

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/service/evidence/SQLDataFetchService.java`

```java
package com.app.decisioniq.service.evidence;

import com.app.decisioniq.assistant.planning.model.QueryPlan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Map;

@Service
public class SQLDataFetchService {


    private JdbcTemplate jdbcTemplate;

    @Autowired
    public SQLDataFetchService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void initiateQuery(QueryPlan query){

        for (Map.Entry<String,MultiValueMap<String,List<String>>> sqlTableFieldValue:query.getSqlTableFieldsMap().entrySet()){
            prepareQuery(sqlTableFieldValue.getValue());
        }
    }

    private void prepareQuery(MultiValueMap<String, List<String>> sqlTableFieldsMap){

    }


}

```

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/service/evidence/EvidenceService.java`

```java
package com.app.decisioniq.service.evidence;

import com.app.decisioniq.assistant.planning.model.QueryPlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EvidenceService {

    private final SQLDataFetchService sqlDataFetchService;
    private final RAGDataFetchService ragDataFetchService;

    @Autowired
    public EvidenceService(SQLDataFetchService sqlDataFetchService, RAGDataFetchService ragDataFetchService) {
        this.sqlDataFetchService = sqlDataFetchService;
        this.ragDataFetchService = ragDataFetchService;
    }

    public void initiateEvidenceCollectionMechanism(QueryPlan query) {
        log.info("Evidence collection Started");
        sqlDataFetchService.initiateQuery(query);
    }
}

```

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/service/evidence/RAGDataFetchService.java`

```java
package com.app.decisioniq.service.evidence;

import org.springframework.stereotype.Service;

@Service
public class RAGDataFetchService {
}

```

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/assistant/intent/model/ParsedQuestion.java`

```java
package com.app.decisioniq.assistant.intent.model;

import com.app.decisioniq.assistant.intent.type.DecisionAssumption;
import com.app.decisioniq.assistant.intent.type.ParsedQuestionStatus;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ParsedQuestion implements Serializable {
    ParsedQuestionStatus status;
    String transactionId;
    List<ParsedAsk> asks;
    DecisionAssumption decisionAssumption;
    boolean clarificationRequired;
    boolean cud;
    String clarificationQuestion;
}

```

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/assistant/intent/model/ParsedAsk.java`

```java
package com.app.decisioniq.assistant.intent.model;

import com.app.decisioniq.assistant.intent.type.DecisionIqIntent;

import java.io.Serializable;

public record ParsedAsk(
        DecisionIqIntent intent,
        String transactionId
) implements Serializable {
    private static final long serialVersionUID = 1L;
}

```

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/assistant/intent/prompt/IntentPromptTemplate.java`

```java
package com.app.decisioniq.assistant.intent.prompt;

public final class IntentPromptTemplate {

    private IntentPromptTemplate() {
    }

    public static final String INSTRUCTIONS = """
            You are a strict intent parser for a fraud decision assistant.
            Return only valid JSON.
            Use only the allowed intent names provided in the prompt.
            Do not invent transaction IDs, decisions, model scores, or facts.

            Your job is to detect every user ask, not just the first ask.
            A single question can produce multiple asks.

            Intent selection rules:
            - Always choose the most specific intent.
            - Use EXPLAIN_TRANSACTION_DECISION only when the user asks a general decision explanation without approval, decline, review, model score, rule, reason code, risk, trace, feature, policy, or similar-case language.
            - Approval-like words such as approved, allowed, let through, got through, passed, not blocked, not stopped, greenlit, or permitted must map to EXPLAIN_APPROVAL.
            - Decline-like words such as declined, rejected, blocked, stopped, denied, failed, or not allowed must map to EXPLAIN_DECLINE.
            - Review-like words such as review, manual review, pending review, held, or queued must map to EXPLAIN_REVIEW.
            - Risk-language such as risk, risky, high risk, critical risk, suspicious, unusual, red flags, unexpected, concerning, fraud signal, or risk signal must add EXPLAIN_RISK_SIGNALS.
            - Model score language such as model score, score, fraud score, risk score, scoring, or scored must add SHOW_MODEL_SCORE.
            - Rule language such as rule, rule fired, policy rule, or trigger must add SHOW_RULE_FIRED unless the user asks for the rule definition, then use EXPLAIN_RULE.
            - Reason language such as reason, reason code, reason codes, factors, or drivers must add SHOW_REASON_CODES.
            - Threshold language such as threshold, band range, low medium high critical range, or score range must add SHOW_MODEL_SCORE_THRESHOLDS.
            - If the question is fraud-decision related but missing required information, use CLARIFICATION_REQUIRED.
            - If the question is not related to fraud decision investigation, use UNKNOWN.

            Examples:
            User: Why was trx 1234 approved even though risk is high and this is not expected?
            Output asks: EXPLAIN_APPROVAL and EXPLAIN_RISK_SIGNALS

            User: Why was TXN-006450 let through even though it looks risky?
            Output asks: EXPLAIN_APPROVAL and EXPLAIN_RISK_SIGNALS

            User: Give me the model score for TXN-006450.
            Output asks: SHOW_MODEL_SCORE
            
            IMPORTANT: If the user intent is about write[create/update/delte] operation to database then it needs be flagged and we need to set cud to true since that is invalid request and shouldn't be done and also we have to set clarificationQuestion in appropriate way to let know the customer.Else set it as false.

            Required JSON shape:
            {
              "status": "VALID | CLARIFICATION_REQUIRED | UNKNOWN",
              "transactionId": "string or null",
              "asks": [
                {
                  "intent": "one allowed intent name",
                  "transactionId": "string or null"
                }
              ],
              "decisionAssumption": "APPROVED | DECLINED | REVIEW | null",
              "clarificationRequired": true or false,
              "cud":true
              "clarificationQuestion": "string or null"
            }
            """;
}

```

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/assistant/intent/type/DecisionIqIntent.java`

```java
package com.app.decisioniq.assistant.intent.type;

public enum DecisionIqIntent {

    EXPLAIN_TRANSACTION_DECISION,
    EXPLAIN_APPROVAL,
    EXPLAIN_DECLINE,
    EXPLAIN_REVIEW,

    SHOW_TRANSACTION_SUMMARY,
    SHOW_MODEL_SCORE,
    SHOW_MODEL_SCORE_THRESHOLDS,
    SHOW_RULE_FIRED,
    SHOW_REASON_CODES,
    SHOW_RISK_BAND,

    SHOW_CUSTOMER_HISTORY,
    COMPARE_WITH_CUSTOMER_HISTORY,

    SHOW_DMP_TRACE,
    SHOW_GENERATED_FEATURES,

    FIND_SIMILAR_CASES,
    FIND_SUSPICIOUS_APPROVALS,

    EXPLAIN_RISK_SIGNALS,

    EXPLAIN_RULE,
    EXPLAIN_POLICY,

    SUBMIT_FEEDBACK,

    CLARIFICATION_REQUIRED,
    UNKNOWN
}

```

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/assistant/intent/type/DecisionAssumption.java`

```java
package com.app.decisioniq.assistant.intent.type;

public enum DecisionAssumption {
    APPROVED,
    DECLINED,
    REVIEW
}

```

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/assistant/intent/type/ParsedQuestionStatus.java`

```java
package com.app.decisioniq.assistant.intent.type;

public enum ParsedQuestionStatus {
    VALID,
    CLARIFICATION_REQUIRED,
    UNKNOWN
}

```

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/service/intent/IntentUnderstandingService.java`

```java
package com.app.decisioniq.service.intent;

import com.app.decisioniq.assistant.intent.model.ParsedQuestion;
import com.app.decisioniq.assistant.intent.prompt.IntentPromptTemplate;
import com.app.decisioniq.assistant.intent.type.DecisionIqIntent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@Slf4j
public class IntentUnderstandingService {

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    public IntentUnderstandingService(@Qualifier("openAiChatModel") ChatModel chatModel, ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
    }

    public ParsedQuestion parseIntent(String question) throws JsonProcessingException {
        ParsedQuestion parsedQuestion;
        if (countWords(question) <= 3) {
            log.warn("Please provide a clearer fraud decision question.");
        }
        String response = chatModel.chat(buildPrompt(question));
        return parseAndFlagIntentResponse(response);
    }

    private String buildPrompt(String question) {
        String finalPrompt = """
                %s

                User question:
                %s
 
                Allowed intent names:
                %s
                """.formatted(
                IntentPromptTemplate.INSTRUCTIONS,
                question,
                Arrays.toString(DecisionIqIntent.values())
        );
        log.info("Final intent prompt {}", finalPrompt);
        return finalPrompt;
    }

    private int countWords(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }

        return text.trim().split("\\s+").length;
    }

    private ParsedQuestion parseAndFlagIntentResponse(String response) throws JsonProcessingException {
        ParsedQuestion parsedResponse = parseJsonIntentResponse(response);
        validateIntentResponse(parsedResponse);
        return parsedResponse;
    }

    private ParsedQuestion parseJsonIntentResponse(String response) throws JsonProcessingException{
        return objectMapper.readValue(response, ParsedQuestion.class);
    }

    private void validateIntentResponse(ParsedQuestion parsedResponse){
        if (parsedResponse.isCud()){
            log.warn("Intent is having a CRUD operation");
        }
    }
}

```

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/assistant/dataslice/type/DecisionDataSlice.java`

```java
package com.app.decisioniq.assistant.dataslice.type;

/**
 * Business-level data slices used by getQuery planning.
 */
public enum DecisionDataSlice {

    TRANSACTION_DECISION_SLICE,
    DECISION_EXPLANATION_SLICE
}

```

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/assistant/dataslice/catalog/IntentToDataSliceMap.java`

```java
package com.app.decisioniq.assistant.dataslice.catalog;

import com.app.decisioniq.assistant.dataslice.type.DecisionDataSlice;
import com.app.decisioniq.assistant.intent.type.DecisionIqIntent;

import java.util.List;
import java.util.Map;

/**
 * Maps an assistant intent to the business data slices required to answer it.
 */
public final class IntentToDataSliceMap {

    private IntentToDataSliceMap() {
    }

    private static final Map<DecisionIqIntent, List<DecisionDataSlice>> INTENT_TO_DATA_SLICES = Map.of(
            DecisionIqIntent.EXPLAIN_TRANSACTION_DECISION,
            List.of(
                    DecisionDataSlice.TRANSACTION_DECISION_SLICE,
                    DecisionDataSlice.DECISION_EXPLANATION_SLICE
            )
    );

    public static List<DecisionDataSlice> slicesFor(DecisionIqIntent intent) {
        return INTENT_TO_DATA_SLICES.getOrDefault(intent, List.of());
    }
}

```

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/assistant/dataslice/config/AssistantDataSliceProperties.java`

```java
package com.app.decisioniq.assistant.dataslice.config;

import com.app.decisioniq.assistant.dataslice.type.DecisionDataSlice;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * YAML-backed configuration for reusable data slices.
 */
@Getter
@ConfigurationProperties(prefix = "decisioniq.assistant")
public class AssistantDataSliceProperties {

    private final Map<DecisionDataSlice, DataSliceProperties> dataSlices;

    public AssistantDataSliceProperties(Map<DecisionDataSlice, DataSliceProperties> dataSlices) {
        this.dataSlices = dataSlices == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(dataSlices));
    }

}

```

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/assistant/dataslice/config/DataSliceProperties.java`

```java
package com.app.decisioniq.assistant.dataslice.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * YAML configuration for one reusable data slice.
 */
public record DataSliceProperties(String description, Map<String, TableFieldsProperties> tables) {

    public DataSliceProperties(String description, Map<String, TableFieldsProperties> tables) {
        this.description = description;
        this.tables = tables == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(tables));
    }

}

```

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/assistant/dataslice/config/TableFieldsProperties.java`

```java
package com.app.decisioniq.assistant.dataslice.config;

import java.util.Collections;
import java.util.List;

/**
 * YAML configuration for the fields required from one table.
 */
public record TableFieldsProperties(List<String> fields) {

    public TableFieldsProperties(List<String> fields) {
        this.fields = fields == null ? List.of() : Collections.unmodifiableList(fields);
    }

}

```

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/assistant/dataslice/config/DataSliceResolver.java`

```java
package com.app.decisioniq.assistant.dataslice.config;

import com.app.decisioniq.assistant.dataslice.type.DecisionDataSlice;
import org.springframework.stereotype.Component;

/**
 * Resolves one reusable data-slice definition from YAML configuration.
 */
@Component
public class DataSliceResolver {

    private final AssistantDataSliceProperties properties;

    public DataSliceResolver(AssistantDataSliceProperties properties) {
        this.properties = properties;
    }

    public DataSliceProperties resolve(DecisionDataSlice dataSlice) {
        DataSliceProperties configuredSlice = properties.getDataSlices().get(dataSlice);
        if (configuredSlice == null) {
            throw new IllegalArgumentException("No YAML data-slice configuration found for " + dataSlice);
        }
        return configuredSlice;
    }
}

```

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/assistant/planning/catalog/IntentToRagChunkTypeMap.java`

```java
package com.app.decisioniq.assistant.planning.catalog;

import com.app.decisioniq.assistant.intent.type.DecisionIqIntent;

import java.util.List;
import java.util.Map;

/**
 * Maps an assistant intent to the RAG chunk types useful for semantic evidence retrieval.
 */
public final class IntentToRagChunkTypeMap {

    private IntentToRagChunkTypeMap() {
    }

    private static final Map<DecisionIqIntent, List<String>> INTENT_TO_CHUNK_TYPES = Map.of(
            DecisionIqIntent.EXPLAIN_TRANSACTION_DECISION,
            List.of(
                    "DECISION_CASE_SUMMARY",
                    "MODEL_SCORE_RISK",
                    "TRANSACTION_RISK_SIGNALS"
            )
    );

    public static List<String> chunkTypesFor(DecisionIqIntent intent) {
        return INTENT_TO_CHUNK_TYPES.getOrDefault(intent, List.of());
    }
}

```

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/service/planning/QueryPlanningService.java`

```java
package com.app.decisioniq.service.planning;

import com.app.decisioniq.assistant.dataslice.catalog.IntentToDataSliceMap;
import com.app.decisioniq.assistant.dataslice.config.AssistantDataSliceProperties;
import com.app.decisioniq.assistant.dataslice.type.DecisionDataSlice;
import com.app.decisioniq.assistant.intent.model.ParsedAsk;
import com.app.decisioniq.assistant.intent.model.ParsedQuestion;
import com.app.decisioniq.assistant.intent.type.DecisionIqIntent;
import com.app.decisioniq.assistant.planning.catalog.IntentToRagChunkTypeMap;
import com.app.decisioniq.assistant.planning.model.QueryPlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Service
@Slf4j
public class QueryPlanningService {

    private final SQLQueryPlanGenerator sqlQueryPlanGenerator;

    @Autowired
    public QueryPlanningService( SQLQueryPlanGenerator sqlQueryPlanGenerator) {
        this.sqlQueryPlanGenerator = sqlQueryPlanGenerator;
    }

    public QueryPlan planQuery(ParsedQuestion parsedQuestionJson,String tenantId) {
        log.info("Planning getQuery for parsed question {}", parsedQuestionJson);
        QueryPlan queryPlan=buildQueryPlanMetaData(parsedQuestionJson,tenantId);
        getDetails(parsedQuestionJson,queryPlan);
        return queryPlan;
    }

    private QueryPlan buildQueryPlanMetaData(ParsedQuestion parsedQuestionJson,String tenantId){
        QueryPlan queryPlan =new QueryPlan();
        queryPlan.setTenantId(tenantId);
        queryPlan.setTransactionId(parsedQuestionJson.getTransactionId());
        return queryPlan;
    }

    private void getDetails(ParsedQuestion parsedQuestion,QueryPlan queryPlan){
        getDataSliceForIntent(parsedQuestion.getAsks(),queryPlan);
    }

    private void getDataSliceForIntent(List<ParsedAsk> parsedAsks,QueryPlan queryPlan){
        List<DecisionIqIntent> decisionIqIntentList=new ArrayList<>();
        for (ParsedAsk parsedAsk:parsedAsks){
            List<DecisionDataSlice> decisionDataSlices = IntentToDataSliceMap.slicesFor(parsedAsk.intent());
            sqlQueryPlanGenerator.getDataForEachSQLSlices(parsedAsk.intent(),decisionDataSlices,queryPlan);
            List<String> ragChunkList = IntentToRagChunkTypeMap.chunkTypesFor(parsedAsk.intent());
            RAGQueryGenerator.buildRAGQueryPlan(ragChunkList,queryPlan,parsedAsk.intent());
            decisionIqIntentList.add(parsedAsk.intent());
        }
        queryPlan.setIntents(decisionIqIntentList);
    }

}

```

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/service/planning/SQLQueryPlanGenerator.java`

```java
package com.app.decisioniq.service.planning;

import com.app.decisioniq.assistant.dataslice.config.AssistantDataSliceProperties;
import com.app.decisioniq.assistant.dataslice.config.DataSliceProperties;
import com.app.decisioniq.assistant.dataslice.config.TableFieldsProperties;
import com.app.decisioniq.assistant.dataslice.type.DecisionDataSlice;
import com.app.decisioniq.assistant.intent.type.DecisionIqIntent;
import com.app.decisioniq.assistant.planning.model.QueryPlan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class SQLQueryPlanGenerator {

     private final AssistantDataSliceProperties assistantDataSliceProperties;

     @Autowired
    public SQLQueryPlanGenerator(AssistantDataSliceProperties assistantDataSliceProperties) {
        this.assistantDataSliceProperties = assistantDataSliceProperties;
    }

    public  void getDataForEachSQLSlices(DecisionIqIntent intent, List<DecisionDataSlice> decisionDataSlicesList, QueryPlan queryPlan){
        for (DecisionDataSlice decisionDataSlice:decisionDataSlicesList){
            DataSliceProperties dataSliceProperties = assistantDataSliceProperties.getDataSlices().get(decisionDataSlice);
            buildSqlData(dataSliceProperties,queryPlan,intent);
        }
    }

    private static void buildSqlData(DataSliceProperties dataSliceProperties, QueryPlan queryPlan, DecisionIqIntent intent) {
        Map<String, TableFieldsProperties> tables = dataSliceProperties.tables();

        for (Map.Entry<String, TableFieldsProperties> tableFieldsPropertiesEntry:tables.entrySet()){
            if (!queryPlan.getSqlTableFieldsMap().isEmpty() && queryPlan.getSqlTableFieldsMap().get(intent.name()).containsKey(tableFieldsPropertiesEntry.getKey())){
                validateAndAddValue(queryPlan, tableFieldsPropertiesEntry,intent);
            }else {
                MultiValueMap<String,List<String>> tableFieldsMap=new LinkedMultiValueMap<>();
                List<List<String>> fields=new ArrayList<>();
                fields.add(new ArrayList<>(tableFieldsPropertiesEntry.getValue().fields()));
                tableFieldsMap.put(tableFieldsPropertiesEntry.getKey(),fields);
                if (queryPlan.getSqlTableFieldsMap().get(intent.name())==null){
                    queryPlan.getSqlTableFieldsMap().put(intent.name(),tableFieldsMap);
                }else if (!queryPlan.getSqlTableFieldsMap().get(intent.name()).isEmpty()){
                    queryPlan.getSqlTableFieldsMap().get(intent.name()).addAll(tableFieldsMap);
                }
            }
        }
    }

    private static void validateAndAddValue(QueryPlan queryPlan, Map.Entry<String, TableFieldsProperties> tableFieldsPropertiesEntry, DecisionIqIntent intent) {
        MultiValueMap<String, List<String>> multiValueTableFieldsMap = queryPlan.getSqlTableFieldsMap().get(intent.name());
        List<List<String>> existingTableFieldlist=multiValueTableFieldsMap.get(tableFieldsPropertiesEntry.getKey());

        List<List<String>> tempTableFieldsHolder=new ArrayList<>();
        for (List<String> tableFields:existingTableFieldlist){
            if (!tableFields.equals(tableFieldsPropertiesEntry.getValue().fields())){
                tempTableFieldsHolder.add(tableFieldsPropertiesEntry.getValue().fields());
            }
        }
        if (!tempTableFieldsHolder.isEmpty()){
            existingTableFieldlist.addAll(tempTableFieldsHolder);
        }
    }

}

```

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/service/planning/RAGQueryGenerator.java`

```java
package com.app.decisioniq.service.planning;

import com.app.decisioniq.assistant.intent.type.DecisionIqIntent;
import com.app.decisioniq.assistant.planning.model.QueryPlan;

import java.util.List;

public class RAGQueryGenerator {

    public static void buildRAGQueryPlan(List<String> ragChunkList, QueryPlan queryPlan, DecisionIqIntent intent){
        queryPlan.setRagCollectionName("decisioniq_evidence_chunks");
        queryPlan.getRagMap().put(intent.name(),ragChunkList);
    }

}

```

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/llm/provider/openai/OpenAiModelConfiguration.java`

```java
package com.app.decisioniq.llm.provider.openai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiResponsesChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OpenAiProperties.class)
@Slf4j
public class OpenAiModelConfiguration {

    @Bean
    public ChatModel openAiChatModel(OpenAiProperties properties) {
        return OpenAiResponsesChatModel.builder()
                .apiKey(properties.apiKey())
                .modelName(properties.model())
                .temperature(0.0)
                .baseUrl(properties.baseUrl())
                .build();
    }
}

```

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/llm/provider/openai/OpenAiProperties.java`

```java
package com.app.decisioniq.llm.provider.openai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "openai")
public record OpenAiProperties(
        String baseUrl,
        String apiKey,
        String model
) {
}

```

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/java/com/app/decisioniq/config/BeanConfiguration.java`

```java
package com.app.decisioniq.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

    @Bean
    public ObjectMapper initializeObjectMapper(){
        return new ObjectMapper();
    }
}

```

### `/Volumes/yp/yp-red/AI BIT/decisioniq/build.gradle`

```groovy
plugins {
	id 'java'
	id 'org.springframework.boot' version '3.5.14'
	id 'io.spring.dependency-management' version '1.1.7'
}

group = 'com.app'
version = '0.0.1-SNAPSHOT'

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation 'org.springframework.boot:spring-boot-starter-web'
	compileOnly 'org.projectlombok:lombok'
	annotationProcessor 'org.projectlombok:lombok'

	/*
	 * LangGraph4j is used as the assistant workflow orchestrator.
	 */
	implementation platform("org.bsc.langgraph4j:langgraph4j-bom:1.8.17")

	// Core graph runtime: StateGraph, nodes, edges, compiled graph, routing, and graph execution.
	implementation "org.bsc.langgraph4j:langgraph4j-core"

	// Integration helpers for using LangChain4j models/tools inside LangGraph4j graph nodes.
	implementation "org.bsc.langgraph4j:langgraph4j-langchain4j"

	// Optional graph checkpoint persistence for resumable/debuggable workflows backed by Postgres.
	implementation "org.bsc.langgraph4j:langgraph4j-postgres-saver"

	// Source: https://mvnrepository.com/artifact/dev.langchain4j/langchain4j
	implementation("dev.langchain4j:langchain4j:1.15.0")
	// OpenAI model provider for hosted intent parsing and final answer generation.
	// Keep this provider isolated so other providers can be added without changing assistant logic.
	implementation("dev.langchain4j:langchain4j-open-ai:1.15.0")

	// Source: https://mvnrepository.com/artifact/org.springframework/spring-jdbc
	implementation("org.springframework:spring-jdbc:7.0.8")

	testImplementation 'org.springframework.boot:spring-boot-starter-test'
	testCompileOnly 'org.projectlombok:lombok'
	testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
	testAnnotationProcessor 'org.projectlombok:lombok'
}

tasks.named('test') {
	useJUnitPlatform()
}

```

### `/Volumes/yp/yp-red/AI BIT/decisioniq/settings.gradle`

```groovy
rootProject.name = 'decisioniq'

```

### `/Volumes/yp/yp-red/AI BIT/decisioniq/gradle/wrapper/gradle-wrapper.properties`

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.14.5-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists

```

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/resources/application.yaml`

```yaml
spring:
  application:
    name: decisioniq
  config:
    import: classpath:decisioniq-data-slices.yaml
openai:
  base-url: https://api.openai.com/v1
  api-key: ${OPENAI_API_KEY}
  model: ${MODEL_NAME}

```

### `/Volumes/yp/yp-red/AI BIT/decisioniq/src/main/resources/decisioniq-data-slices.yaml`

```yaml
decisioniq:
  assistant:
    data-slices:
      TRANSACTION_DECISION_SLICE:
        description: Core transaction and final decision facts.
        tables:
          decision_cases:
            fields:
              - case_id
              - correlation_id
              - customer_ref
              - account_ref
              - event_time
              - decision_name
              - scenario_type
          transaction_context:
            fields:
              - amount
              - currency
              - transaction_type
              - channel
              - merchant_name
              - merchant_category
              - transaction_country
              - transaction_city
          decision_outcomes:
            fields:
              - decision_action
      DECISION_EXPLANATION_SLICE:
        description: Evidence used to explain why the final decision happened.
        tables:
          decision_outcomes:
            fields:
              - decision_action
          decision_cases:
            fields:
              - case_id
              - correlation_id
              - customer_ref
          model_outputs:
            fields:
              - model_id
              - score_name
              - score
              - risk_band
          rule_evaluations:
            fields:
              - rule_name
              - rule_meaning
              - status
              - decision_candidate
          decision_reason_codes:
            fields:
              - reason_order
              - reason_text
          risk_signals:
            fields:
              - known_device
              - known_beneficiary
              - known_merchant
              - international_transaction
              - odd_hour
              - authentication_method
              - failed_login_count_last_hour
              - amount_above_balance

```

## I. Risks, gaps, and TODOs

- Spring context test currently fails because `SQLDataFetchService` requires `JdbcTemplate`, but no `JdbcTemplate` bean is available. There is no datasource config in `application.yaml`.

- `build.gradle` uses `org.springframework:spring-jdbc:7.0.8` directly instead of `spring-boot-starter-jdbc`. This can bypass normal Boot dependency conventions. No PostgreSQL JDBC driver is declared.

- `SQLDataFetchService.prepareQuery(...)` is empty; no SQL is constructed or executed.

- `SQLDataFetchService` returns `void`; no evidence result model exists.

- `EvidenceCollectionNode` returns `Map.of()` and does not write `EVIDENCE_COLLECTION_KEY`. Downstream answer generation has no evidence to read.

- `DecisionIQAgentState.collectEvidence()` expects a `String` and has a comment `Need to change the return type`; no node writes this state.

- `EvidenceService` injects `RAGDataFetchService` but never calls it.

- `RAGDataFetchService` is empty; Milvus/RAG integration is not implemented.

- `AnswerGenerationNode` ignores `AnswerGenerationService` and always returns `Hello Answered`.

- `AnswerGenerationService.generateAnswer` is stubbed and returns `answer completed`.

- `IntentToDataSliceMap` maps only `EXPLAIN_TRANSACTION_DECISION`; all other intents from the prompt produce no SQL slices.

- `IntentToRagChunkTypeMap` maps only `EXPLAIN_TRANSACTION_DECISION`; all other intents produce no RAG chunk types.

- `SQLQueryPlanGenerator.buildSqlData` can throw `NullPointerException` when `sqlTableFieldsMap` is non-empty but does not contain the current intent key. The code checks `!map.isEmpty()` and immediately calls `get(intent.name()).containsKey(...)`.

- `SQLQueryPlanGenerator.getDataForEachSQLSlices` does not handle missing YAML config for a `DecisionDataSlice`; `dataSliceProperties.tables()` would throw if the slice is absent.

- `SQLQueryPlanGenerator.validateAndAddValue` can add duplicate incoming field groups multiple times if multiple existing field groups are not equal to incoming fields. It should check whether the incoming field list already exists, not add once per non-equal existing list.

- QueryPlan uses nested `Map<String, MultiValueMap<String, List<String>>>`, which is powerful but awkward. Fetching requires flattening `List<List<String>>` into unique fields per table.

- No table/field identifier validation exists before future SQL construction. Dynamic table/column SQL must be validated before concatenation.

- Query planning stores `transactionId` from `ParsedQuestion`, not from each `ParsedAsk`. If asks can carry different transaction IDs, that distinction is lost.

- `QueryPlanningService.getDataSliceForIntent` assumes `parsedQuestion.getAsks()` is non-null.

- Graph routing only checks `clarificationRequired`; it does not explicitly route `UNKNOWN` or `cud == true` unless the LLM sets clarificationRequired.

- `IntentPromptTemplate` has a JSON shape example missing a comma after `"cud":true`, and typo `delte`. This can degrade LLM output quality.

- `IntentUnderstandingService.parseJsonIntentResponse` assumes the LLM returns raw JSON without Markdown fences or prose. There is no cleanup/retry/repair logic.

- `IntentUnderstandingService.validateIntentResponse` only logs when `cud` is true. It does not block or convert to clarification.

- `QuestionValidation` validates only question, not `tenantId`. `DecisionAssistantOrchestrator` puts tenantId in `Map.of`; if tenantId is null, `Map.of` throws `NullPointerException`.

- No authentication, authorization, role check, tenant verification, or row-level access guard exists in code.

- No conversation/session ID, Redis memory, active transaction context, or follow-up resolver exists yet.

- No error-handling layer or controller advice is present.

- CORS is hardcoded to `http://localhost:5173`.

- OpenAI configuration requires `OPENAI_API_KEY` and `MODEL_NAME`; no default model is provided.

- `AssistantAskRequest` comment mentions `userRole`, but the record has only `tenantId` and `question`.

- `AgentApi` has unused imports `HttpStatus` and `ResponseStatusException`.

- `SqlData` and `SqlTableSelection` look like older/planned models and are not used in the current flow.

- LangGraph4j Postgres saver dependency is present but no checkpoint persistence is configured.

- No direct API/integration tests exist for `/agent/ask`; only default context load test exists and currently fails.

## J. Final cheat sheet

- Project root is `/Volumes/yp/yp-red/AI BIT/decisioniq`.

- Root Gradle project name is `decisioniq`.

- Java toolchain is 21.

- Spring Boot plugin version is `3.5.14`.

- The app entry point is `DecisioniqApplication`.

- HTTP API is `AgentApi` under `/agent` and `/api/assistant`.

- Main endpoint is `POST /ask`.

- Request model is `AssistantAskRequest(String tenantId, String question)`.

- Response model is `AssistantAnswerResponse(String answer)`.

- Only question blank validation exists; tenantId is not validated.

- Graph orchestration is done with LangGraph4j `CompiledGraph<DecisionIQAgentState>`.

- Graph nodes are intent understanding, intent clarification, query planning, evidence collection, and answer generation.

- Graph state keys are `question`, `tenantId`, `intentKey`, `queryPlanning`, `evidenceCollectionKey`, and `answer`.

- Intent parsing uses LangChain4j `ChatModel` with OpenAI Responses model.

- OpenAI properties are `openai.base-url`, `openai.api-key`, and `openai.model`.

- Intent prompt is in `IntentPromptTemplate.INSTRUCTIONS`.

- Intent parser returns `ParsedQuestion` with `asks`, `transactionId`, `clarificationRequired`, and `cud`.

- Intent vocabulary is `DecisionIqIntent`.

- Only `EXPLAIN_TRANSACTION_DECISION` is currently mapped to SQL data slices.

- Only `EXPLAIN_TRANSACTION_DECISION` is currently mapped to RAG chunk types.

- Data-slice definitions are YAML-backed in `decisioniq-data-slices.yaml`.

- Current slices are `TRANSACTION_DECISION_SLICE` and `DECISION_EXPLANATION_SLICE`.

- YAML maps slices to real tables like `decision_cases`, `transaction_context`, `decision_outcomes`, `model_outputs`, `rule_evaluations`, `decision_reason_codes`, and `risk_signals`.

- `QueryPlan` carries tenantId, transactionId, intents, ragCollectionName, SQL table/field map, and RAG map.

- `sqlTableFieldsMap` is keyed by intent name, then table name, then field groups.

- `ragMap` is keyed by intent name with chunk type lists.

- `RAGQueryGenerator` sets collection name to `decisioniq_evidence_chunks`.

- `EvidenceCollectionNode` currently invokes evidence service but writes no evidence to state.

- `EvidenceService` currently calls only SQL fetch, not RAG fetch.

- `SQLDataFetchService.prepareQuery` is empty.

- `RAGDataFetchService` is empty.

- `AnswerGenerationNode` returns static `Hello Answered`.

- `AnswerGenerationService` is stubbed and currently unused by node.

- `./gradlew test` compiles Java but fails Spring context load due missing `JdbcTemplate` bean.

- No datasource properties are present in `application.yaml`.

- No PostgreSQL JDBC driver dependency is declared.

- No Redis/session memory or follow-up context resolver exists yet.

- No source attribution/evidence bundle model exists yet.

- The current architecture is a single graph-based assistant workflow, not true multi-agent.

- Next implementation step should be SQL evidence retrieval: flatten query plan fields, validate identifiers, execute `JdbcTemplate`, return evidence bundle, and store it in graph state.
