# DecisionIQ: Guardrail-to-NLP Walkthrough

## 1. Purpose

This document explains the **current, implemented DecisionIQ request flow** from the HTTP request boundary through the post-CoreNLP operation-policy decision.

It is written for a reader who is new to Natural Language Processing (NLP), but every implementation claim is tied to the current Java source.

This document covers:

1. HTTP request validation and development request context.
2. The first deterministic guardrail.
3. Stanford CoreNLP analysis.
4. DecisionIQ clause decomposition.
5. The custom YAML-backed operation policy.
6. The final `SAFE_CANDIDATE` or whole-request block decision.

It stops at the deterministic operation-policy decision and does not explain downstream processing stages.

> **Next boundary:** A request that survives this walkthrough as `SAFE_CANDIDATE` is handed to the later capability-interpretation boundary, which is outside this document.

---

## 2. The Current Pipeline

```text
POST /agent/ask or /api/assistant/ask
        |
        v
Transport validation (@Valid)
        |
        v
Request-boundary size validation
        |
        v
Development request-context resolution
        |
        v
Phase 1 deterministic guardrail
  - normalize harmless formatting
  - reject malformed input
  - detect raw SQL
  - detect script attacks
  - detect instruction bypasses
        |
        | only ALLOW_TO_INTERPRET continues
        v
Stanford CoreNLP
  - tokenize
  - sentence split (ssplit)
  - part-of-speech tagging (POS)
  - lemmatization
  - dependency parsing (depparse)
        |
        v
DecisionIQ clause decomposition
        |
        v
YAML-backed operation analysis
  - actions
  - grammatical request form
  - targets
        |
        +-----------------------------+
        |                             |
        v                             v
SAFE_CANDIDATE                 BLOCKING EFFECT
                              - PERSIST
                              - MODIFY
                              - TRANSFER
                              - EXTERNAL_ACTION
                                      |
                                      v
                              Reject whole message
```

The key design point is that these are **three different responsibilities**:

| Layer | What it does | What it does not do |
|---|---|---|
| First deterministic guardrail | Checks request shape and bounded, security-oriented patterns | It does not understand transaction meaning or prove domain relevance |
| Stanford CoreNLP | Produces linguistic structure: words, sentences, POS tags, lemmas, and grammatical dependencies | It does not know DecisionIQ policy or decide whether an operation is allowed |
| DecisionIQ operation policy | Combines CoreNLP grammar with controlled action and target vocabularies | It is not a complete semantic safety model or a general language reasoner |

---

## 3. Entry Through the HTTP API

The request enters either:

- `POST /agent/ask`
- `POST /api/assistant/ask`

Both routes are declared by `AgentApi` at lines 18-20, and the `ask` method is at lines 34-50.

The incoming body is represented by `AssistantAskRequest`:

```json
{
  "tenantId": "tenant-citi-bank",
  "userId": "analyst-1",
  "conversationId": "conversation-123",
  "question": "Give me the details of transaction TX-123 and tell me the model score"
}
```

`@Valid` performs transport-level checks before the application use case is called. For example, tenant, user, conversation, and question are required, and each field has a transport maximum.

### Important trust statement

The current `tenantId` and `userId` values are **development inputs**. They are not authenticated production identity claims.

This is stated in:

- `AssistantAskRequest.java`, lines 6-10.
- `DevelopmentTrustedRequestContextResolver.java`, lines 9-16.

The development resolver copies the supplied values into `TrustedRequestContext` at lines 18-31. A production implementation must replace this with identity established from verified authentication claims.

---

## 4. Phase 1: The First Deterministic Guardrail

`GuardedAssistantRequestService.handle` applies the request boundary first. It invokes the interpretation stage only when the outcome is `ALLOW_TO_INTERPRET`.

The control decision is visible at:

- `GuardedAssistantRequestService.java`, lines 28-36.

### 4.1 Boundary validation

`GuardedRequestBoundaryService.evaluate` performs four ordered activities:

1. Validate externally supplied field sizes.
2. Resolve the request context.
3. Log the source of that context without logging identity values.
4. Evaluate the question guardrail.

These steps are at `GuardedRequestBoundaryService.java`, lines 38-49.

### 4.2 Normalization and input validation

`RequestGuardrailService.evaluate`:

1. Normalizes harmless formatting variance.
2. Calculates only the input length for safe audit context.
3. Validates the input.
4. Runs configured security detectors.
5. Records the final outcome without logging the raw question.

See `RequestGuardrailService.java`, lines 35-47.

If validation fails, it returns `REQUEST_INVALID`. If no detector matches, it returns:

```text
outcome    = ALLOW_TO_INTERPRET
reasonCode = READY_FOR_INTERPRETATION
```

The decision construction is at `RequestGuardrailService.java`, lines 53-82.

### 4.3 Security detectors

`GuardrailDetectionService` executes enabled detectors in configured order. A whole-request security block returns immediately.

See `GuardrailDetectionService.java`, lines 29-53.

The current `guardrail.yaml` enables:

- `RAW_SQL`
- `SCRIPT_ATTACK`
- `INSTRUCTION_BYPASS`

The enabled list is at `guardrail.yaml`, lines 87-91. The bounded detector patterns are at lines 14-86.

### What passing Phase 1 means

`ALLOW_TO_INTERPRET` means only:

> No malformed input or configured obvious security pattern was proven at this boundary.

It does **not** mean:

- The question is a valid DecisionIQ question.
- The request is factually meaningful.
- The user is authorized in production.
- The requested data exists.
- The requested operation is read-only.

The last item is why the later CoreNLP operation-policy layer exists.

---

## 5. Stanford CoreNLP Configuration

DecisionIQ creates one lazy `StanfordCoreNLP` bean in `NlpConfiguration`.

The configured annotators are exactly:

```java
tokenize,ssplit,pos,lemma,depparse
```

See `NlpConfiguration.java`, lines 13-19.

| Annotator | Full meaning | Purpose |
|---|---|---|
| `tokenize` | Tokenization | Break text into word-like and punctuation units |
| `ssplit` | Sentence splitting | Identify sentence boundaries |
| `pos` | Part-of-speech tagging | Label grammatical roles such as verb, noun, pronoun, or number |
| `lemma` | Lemmatization | Reduce an inflected word to a normalized dictionary form |
| `depparse` | Dependency parsing | Describe grammatical relationships between words |

### No NER in the current pipeline

Named Entity Recognition (`ner`) is **not configured**.

Therefore this layer does not currently identify `TX-123` as a typed `TRANSACTION_ID`, nor does it directly produce typed money, date, location, customer, or merchant entities.

This is a deliberate truth boundary: older experiments may have had NER-related structures, but the current pipeline described here does not.

---

## 6. Detailed Example

We will trace this question:

```text
Give me the details of transaction TX-123 and tell me the model score
```

The outputs below were verified against the current configured Stanford CoreNLP pipeline and the current `NlpAnalyzer` and `NlpOperationAnalyzer` implementations.

### Step 1: The question reaches NLP only after Phase 1 allows it

`AssistantInterpretationWorkflow.interpret` calls `analyzeOperations` at line 50.

Inside `analyzeOperations`:

```java
NlpAnalysis analysis = nlpAnalyzer.analyze(normalizedQuestion);
List<NlpOperationFrame> frames = operationAnalyzer.analyze(analysis);
```

These calls are at `AssistantInterpretationWorkflow.java`, lines 78-86.

### Step 2: Tokenization

Tokenization does not mean “understand the complete question.” It means “divide the text into addressable pieces.”

For the example, the current pipeline produces:

| Index | Token | Begin | End | Observation |
|---:|---|---:|---:|---|
| 1 | `Give` | 0 | 4 | Verb-like command word |
| 2 | `me` | 5 | 7 | Pronoun |
| 3 | `the` | 8 | 11 | Determiner |
| 4 | `details` | 12 | 19 | Plural noun |
| 5 | `of` | 20 | 22 | Preposition |
| 6 | `transaction` | 23 | 34 | Singular noun |
| 7 | `TX` | 35 | 37 | Noun token |
| 8 | `-` | 37 | 38 | Hyphen token |
| 9 | `123` | 38 | 41 | Number token |
| 10 | `and` | 42 | 45 | Coordinating conjunction |
| 11 | `tell` | 46 | 50 | Verb |
| 12 | `me` | 51 | 53 | Pronoun |
| 13 | `the` | 54 | 57 | Determiner |
| 14 | `model` | 58 | 63 | Noun used as a compound modifier |
| 15 | `score` | 64 | 69 | Noun |

Offsets use the Java substring convention: `begin` is inclusive and `end` is exclusive.

Notice that `TX-123` becomes three tokens: `TX`, `-`, and `123`. Tokenization preserves the source offsets needed for a later layer to reconstruct the original span, but this current layer does not itself label the span as a transaction ID.

`NlpAnalyzer.toToken` maps the CoreNLP output into DecisionIQ's token contract at `NlpAnalyzer.java`, lines 83-91.

### Step 3: Sentence splitting

CoreNLP keeps the example as **one sentence**:

```text
Sentence 0:
Give me the details of transaction TX-123 and tell me the model score
```

This distinction matters:

- A **sentence** is a punctuation/linguistic sentence boundary.
- A **clause** is a smaller request or predicate inside a sentence.

One sentence can contain multiple requests. DecisionIQ therefore does not stop at sentence splitting.

`NlpAnalyzer.analyze` creates a `CoreDocument`, runs the shared pipeline, and maps every CoreNLP sentence into `NlpAnalysis.Sentence` at lines 32-50.

The shared pipeline is synchronized at lines 39-42 because one pipeline instance is reused.

### Step 4: Part-of-speech tagging

Part of Speech (`POS`) describes how a token behaves grammatically in this sentence.

| Token | POS | Full meaning | Why it matters here |
|---|---|---|---|
| `Give` | `VB` | Verb, base form | A base-form leading verb can represent an imperative request |
| `me` | `PRP` | Personal pronoun | Recipient of “give” or “tell” |
| `the` | `DT` | Determiner | Introduces a noun phrase |
| `details` | `NNS` | Plural noun | Object requested by “give” |
| `of` | `IN` | Preposition/subordinating conjunction | Links details to transaction context |
| `transaction` | `NN` | Singular common noun | Compound context around the identifier token |
| `TX` | `NN` | Singular common noun | CoreNLP does not know it is a DecisionIQ identifier |
| `-` | `HYPH` | Hyphen | Punctuation inside the identifier-like span |
| `123` | `CD` | Cardinal number | Numeric part of the identifier-like span |
| `and` | `CC` | Coordinating conjunction | Connects two predicates/requests |
| `tell` | `VB` | Verb, base form | Begins the second imperative request |
| `model` | `NN` | Singular common noun | Compound modifier of score |
| `score` | `NN` | Singular common noun | Object requested by “tell” |

POS tags are grammatical evidence, not business authorization. For example, `VB` helps Java decide whether `delete` is being requested, but POS alone does not decide policy.

### Step 5: Lemmatization

A lemma is the normalized dictionary form of a token. It lets policy compare a stable word form rather than every grammatical variation.

| Token | Lemma |
|---|---|
| `Give` | `give` |
| `me` | `I` |
| `the` | `the` |
| `details` | `detail` |
| `of` | `of` |
| `transaction` | `transaction` |
| `TX` | `tx` |
| `-` | `-` |
| `123` | `123` |
| `and` | `and` |
| `tell` | `tell` |
| `model` | `model` |
| `score` | `score` |

Examples outside this sentence:

- `transactions` can become `transaction`.
- `approved` can become `approve`.
- `deleting` can become `delete`.

The operation analyzer compares normalized lemmas to YAML vocabulary. It does not compare only the original surface spelling.

### Step 6: Dependency parsing

A dependency parse describes which word grammatically depends on which other word.

The current parse for the example is:

```text
root(0, Give)
iobj(Give, me)
det(details, the)
obj(Give, details)
case(TX, of)
compound(TX, transaction)
nmod:of(details, TX)
punct(TX, -)
nummod(TX, 123)
cc(tell, and)
conj:and(Give, tell)
iobj(tell, me)
det(score, the)
compound(score, model)
obj(tell, score)
```

Common relations in this example:

| Relation | Meaning |
|---|---|
| `root` | Main predicate of the sentence |
| `iobj` | Indirect object |
| `obj` | Direct object |
| `det` | Determiner relationship |
| `compound` | Tokens forming a compound noun phrase |
| `nmod:of` | Noun modifier introduced by “of” |
| `nummod` | Numeric modifier |
| `cc` | Coordinating conjunction |
| `conj:and` | Predicates or phrases coordinated by “and” |

DecisionIQ maps roots and sorted dependency edges into `NlpAnalysis.Dependency` at `NlpAnalyzer.java`, lines 94-104.

### Step 7: Clause decomposition

The dependency `conj:and(Give, tell)` indicates that `tell` starts another coordinated predicate.

DecisionIQ's clause logic checks conjunctions and whether the dependent word begins an independent clause. That logic is at:

- `NlpAnalyzer.java`, lines 107-167.
- `NlpAnalyzer.java`, lines 169-200.

The example becomes:

```text
Clause 0
  text: Give me the details of transaction TX-123
  begin: 0
  end: 41
  firstTokenIndex: 1
  lastTokenIndex: 9

Clause 1
  text: tell me the model score
  begin: 46
  end: 69
  firstTokenIndex: 11
  lastTokenIndex: 15
```

The conjunction itself is used as a separator and is not included in either clause text.

The current implementation can also cut clauses around:

- Semicolons.
- Selected grammatical markers.
- Supplemental relations such as apposition, vocative, discourse, parataxis, and generic dependency relations when comma-delimited.

See `NlpAnalyzer.java`, lines 117-166 and 220-250.

This decomposition is deterministic Java logic built on CoreNLP output. CoreNLP provides the sentence and dependency graph; DecisionIQ decides how to turn that graph into request units.

### Step 8: Operation-frame extraction

`NlpOperationAnalyzer` produces one `NlpOperationFrame` for each clause.

For the example, the verified frames are:

```text
Frame 0
  sentenceIndex: 0
  clauseIndex: 0
  text: Give me the details of transaction TX-123
  actions: [give]
  objects: [i, detail]
  targets: []
  effect: SAFE_CANDIDATE

Frame 1
  sentenceIndex: 0
  clauseIndex: 1
  text: tell me the model score
  actions: [tell]
  objects: [i, score]
  targets: []
  effect: SAFE_CANDIDATE
```

Two details should not be hidden:

1. The object list contains normalized grammatical objects, so pronoun `me` appears as lemma `i`.
2. `targets` is empty because this field contains only nouns matching the operation policy's configured **presentation or persistent targets**. It is not a list of business facts such as model score or transaction ID.

Frame construction is at `NlpOperationAnalyzer.java`, lines 34-103.

---

## 7. The YAML Operation Policy

The YAML file is:

```text
src/main/resources/operation-policy.yaml
```

It is imported from `application.yaml`, lines 4-9, and bound to `OperationPolicyProperties` through the prefix `decisioniq.operation-policy` at `OperationPolicyProperties.java`, lines 14-20.

The application uses `@ConfigurationPropertiesScan` in `DecisioniqApplication.java`, lines 5 and 7-8.

### 7.1 Action categories

| Category | Representative configured values | Result when grammatically requested |
|---|---|---|
| `construct` | build, create, generate, make, produce | Blocks only when combined with a persistent target |
| `persist` | archive, commit, materialize, persist, replicate, save, store, write | `PERSIST` |
| `modify` | approve, block, cancel, change, decline, delete, refund, reverse, update | `MODIFY` |
| `transfer` | copy, download, export, move, upload | `TRANSFER` |
| `external` | email, message, notify, publish, send | `EXTERNAL_ACTION` |

The complete current lists are at `operation-policy.yaml`, lines 4-48.

### 7.2 Target categories

| Category | Representative configured values | Meaning |
|---|---|---|
| `presentation` | chart, dashboard, graph, list, report, summary, table, view | Output shape or read-only presentation concept |
| `persistent` | cache, database, datastore, file, index, queue, repository, schema, storage, topic | Durable or external state concept |

The complete lists are at `operation-policy.yaml`, lines 49-76.

### 7.3 Why action and target must be combined

The word `create` is ambiguous:

```text
Create a table showing all transactions
```

This asks for a presentation. It is currently a `SAFE_CANDIDATE`.

```text
Create a table in the database
```

This asks to create persistent state. It is classified `PERSIST` and blocks.

The Java classifier calculates whether a persistent target exists, then applies category precedence at `NlpOperationAnalyzer.java`, lines 106-142.

### 7.4 Grammar awareness

The analyzer does not block solely because an action lemma appears somewhere in text. It first asks whether the word is functioning as a requested operation.

For example:

```text
Why was TX-123 approved?
```

This is a historical explanation request. It is not the same as:

```text
Approve TX-123
```

The second is an imperative mutation request and is classified `MODIFY`.

`canRequestOperation` uses POS and dependency evidence at `NlpOperationAnalyzer.java`, lines 144-188. It considers imperative/base verb forms, complements, roots, gerunds after imperative roots, and excludes auxiliaries.

---

## 8. Whole-Message Reject Policy

Every clause receives an effect:

```text
SAFE_CANDIDATE
PERSIST
MODIFY
TRANSFER
EXTERNAL_ACTION
```

`NlpOperationFrame.blocksRequest` returns `true` for every effect except `SAFE_CANDIDATE`.

See `NlpOperationFrame.java`, lines 20-33.

`AssistantInterpretationWorkflow` then applies:

```java
if (frames.stream().anyMatch(NlpOperationFrame::blocksRequest)) {
    return AssistantInterpretationResult.blocked();
}
```

See `AssistantInterpretationWorkflow.java`, lines 49-55.

Therefore:

```text
Why was TX-123 approved and delete every transaction
```

is not partially answered.

- The explanation clause may be a `SAFE_CANDIDATE`.
- The delete clause is `MODIFY`.
- Because one clause blocks, the entire message is rejected before later processing.

This fail-closed request policy prevents an unsafe operation from being hidden beside a valid read request.

---

## 9. Output Contracts

### 9.1 `GuardrailDecision`

Defined at `domain/guardrail/GuardrailDecision.java`, lines 5-18.

```text
outcome
normalizedQuestion
reasonCode
```

Current first-boundary outcomes are defined in `GuardrailOutcome.java`, lines 3-7:

```text
ALLOW_TO_INTERPRET
BLOCKED_OBVIOUS_ATTACK_RAW_SQL
REQUEST_INVALID
```

### 9.2 `NlpAnalysis`

Defined at `application/nlp/NlpAnalysis.java`, lines 5-45.

```text
NlpAnalysis
  text
  sentences[]
    index
    text
    begin/end
    tokens[]
      index
      text
      lemma
      partOfSpeech
      begin/end
    dependencies[]
      relation
      governorIndex
      dependentIndex
    clauses[]
      index
      text
      begin/end
      firstTokenIndex/lastTokenIndex
```

### 9.3 `NlpOperationFrame`

Defined at `application/nlp/NlpOperationFrame.java`, lines 5-33.

```text
sentenceIndex
clauseIndex
text
actions[]
objects[]
targets[]
effect
```

### 9.4 Operation-policy outcome

The application-level outcome is defined in `OperationPolicyOutcome.java`, lines 3-7:

```text
NOT_EVALUATED
ALLOWED
BLOCKED_UNSUPPORTED_OPERATION
```

`AssistantInterpretationResult.blocked` maps an NLP policy block to `BLOCKED_UNSUPPORTED_OPERATION` at `AssistantInterpretationResult.java`, lines 23-31.

---

## 10. Code Navigation

Line numbers reflect the current source at the time this document was written.

| Responsibility | File | Key lines |
|---|---|---:|
| HTTP routes and use-case call | `src/main/java/com/app/decisioniq/api/assistant/AgentApi.java` | 18-20, 34-50 |
| Request transport contract and dev trust warning | `src/main/java/com/app/decisioniq/api/assistant/model/AssistantAskRequest.java` | 6-29 |
| Boundary-before-NLP orchestration | `src/main/java/com/app/decisioniq/application/assistant/GuardedAssistantRequestService.java` | 24-46 |
| Boundary validation/context/guardrail order | `src/main/java/com/app/decisioniq/application/assistant/GuardedRequestBoundaryService.java` | 34-49 |
| Development-only context resolver | `src/main/java/com/app/decisioniq/infrastructure/context/DevelopmentTrustedRequestContextResolver.java` | 9-31 |
| Guardrail coordination | `src/main/java/com/app/decisioniq/application/guardrail/RequestGuardrailService.java` | 31-82 |
| Ordered detector execution | `src/main/java/com/app/decisioniq/application/guardrail/GuardrailDetectionService.java` | 29-53 |
| Guardrail policy | `src/main/resources/guardrail.yaml` | 1-107 |
| CoreNLP annotator configuration | `src/main/java/com/app/decisioniq/config/nlp/NlpConfiguration.java` | 13-19 |
| CoreNLP execution and sentence mapping | `src/main/java/com/app/decisioniq/application/nlp/NlpAnalyzer.java` | 32-80 |
| Token mapping | `src/main/java/com/app/decisioniq/application/nlp/NlpAnalyzer.java` | 83-91 |
| Dependency mapping | `src/main/java/com/app/decisioniq/application/nlp/NlpAnalyzer.java` | 94-104 |
| Clause decomposition | `src/main/java/com/app/decisioniq/application/nlp/NlpAnalyzer.java` | 107-200, 220-250 |
| NLP typed contract | `src/main/java/com/app/decisioniq/application/nlp/NlpAnalysis.java` | 5-45 |
| Operation-frame extraction | `src/main/java/com/app/decisioniq/application/nlp/NlpOperationAnalyzer.java` | 24-104 |
| Effect classification | `src/main/java/com/app/decisioniq/application/nlp/NlpOperationAnalyzer.java` | 106-142 |
| Requested-operation grammar | `src/main/java/com/app/decisioniq/application/nlp/NlpOperationAnalyzer.java` | 144-188 |
| Operation frame and blocking effects | `src/main/java/com/app/decisioniq/application/nlp/NlpOperationFrame.java` | 5-33 |
| Operation YAML binding | `src/main/java/com/app/decisioniq/config/nlp/OperationPolicyProperties.java` | 14-50 |
| Operation policy vocabulary | `src/main/resources/operation-policy.yaml` | 1-76 |
| NLP call and whole-message block | `src/main/java/com/app/decisioniq/application/assistant/AssistantInterpretationWorkflow.java` | 45-62, 78-97 |

---

## 11. Current Limitations and Truth Labels

### Implemented now

- Request transport validation.
- Configured boundary size validation.
- Development request-context resolution.
- Deterministic raw-SQL, script-attack, and instruction-bypass detection.
- CoreNLP tokenization, sentence splitting, POS tagging, lemmatization, and dependency parsing.
- DecisionIQ clause decomposition.
- YAML-backed action and target vocabulary.
- Grammar-aware operation classification.
- Whole-message blocking when any clause requests a blocking operation.
- Typed, immutable NLP and operation-frame outputs.
- Structural logging of unit count and effects without the raw question.

### Not implemented in this layer

- Named Entity Recognition.
- Typed transaction-ID extraction.
- Money, date, duration, location, customer, account, or merchant extraction.
- Spell correction.
- Business synonym normalization beyond the configured operation vocabulary.
- Domain relevance validation.
- Authorization to transaction data.
- Cross-turn conversation memory.
- Semantic confidence scores.
- A statistical safety or relevance probability.
- Any downstream interpretation, retrieval, execution, or answer stage.

### Policy caveats

1. The operation policy is bounded by configured vocabulary. Unlisted synonyms can be missed.
2. Parser errors on malformed or highly conversational text can affect classification.
3. `SAFE_CANDIDATE` means “no blocking operation was proven,” not “this is safe and valid in every later layer.”
4. Presentation targets are recognized, but the classifier currently checks only whether a **persistent** target exists when interpreting a `construct` action.
5. An unknown target combined with `create` can currently default to `SAFE_CANDIDATE` because persistence was not proven.
6. `transfer` and `external` actions are blocked without role-specific exceptions.
7. The YAML version is validated metadata; the analyzer does not branch behavior by version.
8. YAML validation checks shape and nonempty lists, but it does not detect semantically contradictory or overlapping vocabulary.

---

## 12. Debugger Breakpoints

The following breakpoints show the flow in order.

| Order | Breakpoint | What to inspect |
|---:|---|---|
| 1 | `AgentApi.java:50` | Request mapped into `AssistantRequestCommand` and sent to the use case |
| 2 | `GuardedAssistantRequestService.java:30` | Start of request-boundary decision |
| 3 | `GuardedRequestBoundaryService.java:40` | Request field validation |
| 4 | `GuardedRequestBoundaryService.java:43` | Development trusted-context object |
| 5 | `RequestGuardrailService.java:37` | Normalized question |
| 6 | `RequestGuardrailService.java:43` | Final first-guardrail decision construction |
| 7 | `GuardrailDetectionService.java:39` | Result from each enabled detector |
| 8 | `GuardedAssistantRequestService.java:33` | `ALLOW_TO_INTERPRET` gate into NLP |
| 9 | `AssistantInterpretationWorkflow.java:50` | Entry into operation analysis |
| 10 | `NlpAnalyzer.java:41` | CoreNLP annotation call |
| 11 | `NlpAnalyzer.java:58` | Tokens generated for one sentence |
| 12 | `NlpAnalyzer.java:61` | CoreNLP dependency graph |
| 13 | `NlpAnalyzer.java:63` | DecisionIQ clause construction |
| 14 | `NlpOperationAnalyzer.java:38` | Clause-local token set |
| 15 | `NlpOperationAnalyzer.java:69` | Extracted action lemmas |
| 16 | `NlpOperationAnalyzer.java:86` | Policy-recognized targets |
| 17 | `NlpOperationAnalyzer.java:91` | Effect classification call |
| 18 | `NlpOperationAnalyzer.java:122` | Persistent-target decision |
| 19 | `NlpOperationAnalyzer.java:124` | Classification precedence begins |
| 20 | `AssistantInterpretationWorkflow.java:53` | Whole-message block decision |

For the worked example, inspect:

```text
analysis.sentences
analysis.sentences[0].tokens
analysis.sentences[0].dependencies
analysis.sentences[0].clauses
frames[0]
frames[1]
```

---

## 13. Glossary

| Term | Meaning in DecisionIQ |
|---|---|
| Deterministic | The same configured rules and inputs produce the same policy decision; no model sampling is involved |
| Guardrail | An early boundary that rejects malformed or explicitly unsafe request patterns |
| NLP | Natural Language Processing; software analysis of human language |
| Token | A word-like or punctuation unit with a source position |
| Offset | Character position in the original question |
| Sentence split | Separation of the input into linguistic sentences |
| POS | Part of Speech; a grammatical label such as verb, noun, pronoun, or number |
| Lemma | Normalized dictionary form of a word |
| Dependency parse | Directed grammatical relationships between tokens |
| Governor | The head word in a dependency relationship |
| Dependent | The word grammatically attached to the governor |
| Clause | A smaller predicate/request unit inside a sentence |
| Action | A verb lemma considered by the operation policy |
| Target | A configured presentation or persistent noun recognized by the operation policy |
| Effect | The operation-policy classification assigned to one clause |
| `SAFE_CANDIDATE` | No configured blocking operation was proven; later validation is still required |
| `PERSIST` | A request to create or retain durable state |
| `MODIFY` | A request to change existing business/system state |
| `TRANSFER` | A request to copy, download, export, move, or upload data |
| `EXTERNAL_ACTION` | A request to communicate or publish outside the read-only assistant boundary |
| Whole-message block | Reject the complete request when any clause has a blocking effect |
| NER | Named Entity Recognition; not configured in the current pipeline |

---

## 14. The Short Oral Explanation

DecisionIQ first validates the request and applies a small deterministic security boundary for malformed input, raw SQL, script attacks, and instruction bypasses. Only an allowed question reaches Stanford CoreNLP. CoreNLP tokenizes the question, splits sentences, assigns POS tags and lemmas, and produces a dependency graph. DecisionIQ then uses that graph to split a sentence into request clauses. For each clause, Java combines the grammatical role of the action with a versioned YAML vocabulary of actions and targets. A clause becomes either `SAFE_CANDIDATE`, `PERSIST`, `MODIFY`, `TRANSFER`, or `EXTERNAL_ACTION`. If any clause has a blocking effect, DecisionIQ rejects the whole message. `SAFE_CANDIDATE` is intentionally narrow: it means the operation-policy layer did not prove an unsupported operation; it does not mean that domain relevance, authorization, evidence, or answer correctness has already been established.
