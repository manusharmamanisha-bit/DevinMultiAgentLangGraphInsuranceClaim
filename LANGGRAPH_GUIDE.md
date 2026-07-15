# LangGraph4j in the Multi-Agent Insurance Claim System

This guide explains how **LangGraph** (via its Java port **LangGraph4j**) is used to orchestrate the multi-agent insurance claim workflow.

---

## 1. What is LangGraph?

**LangGraph** is a framework for building stateful, multi-agent AI workflows as graphs. It comes from the LangChain ecosystem. Because this project is written in **Spring Boot / Java**, we use **LangGraph4j** (`org.bsc.langgraph4j`), which provides the same concepts:

- **StateGraph** — defines the structure of the workflow.
- **Nodes** — individual agents or processing steps.
- **Edges** — fixed transitions between nodes.
- **Conditional Edges** — dynamic routing based on the current state.
- **Shared State** — a single state object passed from node to node.
- **CompiledGraph** — a runnable, immutable version of the graph.

LangGraph is ideal when a workflow is **not a straight line**: claims can be rejected, flagged for fraud, sent to manual review, or approved, all at runtime.

---

## 2. Why LangGraph is used here

An insurance claim must pass through several specialized decisions:

1. Intake the claim.
2. Validate the data.
3. Check for fraud.
4. Assess the claim value.
5. Approve or reject.
6. Process payment.
7. Notify the customer.

Some paths are conditional:

- Invalid data → stop.
- High fraud risk → human manual review.
- After manual review approval → continue assessment.
- Rejected approval → stop.

Instead of writing all this branching logic inside one big service class, each responsibility is implemented as a **separate agent**, and **LangGraph4j decides the order and branches**.

---

## 3. The agents

Each agent lives in `src/main/java/com/insurance/claim/agent/` and implements the `Agent` interface.

| Agent | Role |
|---|---|
| <ref_file file="C:\Users\Administrator\AssignmentMultiAgentInsuranceClaim\src\main\java\com\insurance\claim\agent\IntakeAgent.java" /> | Structures the incoming claim and extracts fields. |
| <ref_file file="C:\Users\Administrator\AssignmentMultiAgentInsuranceClaim\src\main\java\com\insurance\claim\agent\ValidationAgent.java" /> | Validates policy, amount, and description. |
| <ref_file file="C:\Users\Administrator\AssignmentMultiAgentInsuranceClaim\src\main\java\com\insurance\claim\agent\FraudDetectionAgent.java" /> | Calls the LLM to classify fraud risk. |
| <ref_file file="C:\Users\Administrator\AssignmentMultiAgentInsuranceClaim\src\main\java\com\insurance\claim\agent\AssessmentAgent.java" /> | Calls the LLM to recommend an amount. |
| <ref_file file="C:\Users\Administrator\AssignmentMultiAgentInsuranceClaim\src\main\java\com\insurance\claim\agent\ApprovalAgent.java" /> | Calls the LLM to approve or reject. |
| <ref_file file="C:\Users\Administrator\AssignmentMultiAgentInsuranceClaim\src\main\java\com\insurance\claim\agent\PaymentAgent.java" /> | Generates a payment reference. |
| <ref_file file="C:\Users\Administrator\AssignmentMultiAgentInsuranceClaim\src\main\java\com\insurance\claim\agent\NotificationAgent.java" /> | Finalizes the claim and builds a notification. |
| <ref_file file="C:\Users\Administrator\AssignmentMultiAgentInsuranceClaim\src\main\java\com\insurance\claim\agent\ManualReviewAgent.java" /> | Pauses the workflow for a human reviewer. |

---

## 4. Code flow

### 4.1 REST request

The flow starts with a REST call handled by <ref_file file="C:\Users\Administrator\AssignmentMultiAgentInsuranceClaim\src\main\java\com\insurance\claim\controller\ClaimController.java" />.

```
POST /api/claims
{
  "policyNumber": "POL-12345",
  "description": "Rear-end collision repair",
  "amount": 2500
}
```

### 4.2 Service layer

`ClaimController` calls `ClaimService.submitClaim(...)`, which creates a `Claim` and hands it to the LangGraph4j workflow runner.

<ref_file file="C:\Users\Administrator\AssignmentMultiAgentInsuranceClaim\src\main\java\com\insurance\claim\service\ClaimService.java" />

### 4.3 LangGraph4j workflow

`ClaimWorkflow` builds and runs the graph.

<ref_file file="C:\Users\Administrator\AssignmentMultiAgentInsuranceClaim\src\main\java\com\insurance\claim\graph\ClaimWorkflow.java" />

It contains two graphs:

1. **Full workflow** — from intake to completion.
2. **Post-review workflow** — resumes a fraud-flagged claim from assessment after a human approves it.

#### Full workflow graph

```
START
  │
  ▼
IntakeAgent
  │
  ▼
ValidationAgent
  │
  ├─ invalid ──▶ END (status = INVALID)
  │
  ├─ amount > 1 lakh ──▶ ManualReviewAgent ──▶ END (status = MANUAL_REVIEW_REQUIRED)
  │
  ▼
FraudDetectionAgent
  │
  ├─ high risk ──▶ ManualReviewAgent ──▶ END (status = MANUAL_REVIEW_REQUIRED)
  │
  ▼
AssessmentAgent
  │
  ▼
ApprovalAgent
  │
  ├─ rejected ──▶ END (status = REJECTED)
  │
  ▼
PaymentAgent
  │
  ▼
NotificationAgent
  │
  ▼
END (status = COMPLETED)
```

The actual graph wiring:

<ref_snippet file="C:\Users\Administrator\AssignmentMultiAgentInsuranceClaim\src\main\java\com\insurance\claim\graph\ClaimWorkflow.java" lines="75-97" />

### 4.4 Shared state

The graph state is defined by `ClaimGraphState`.

<ref_file file="C:\Users\Administrator\AssignmentMultiAgentInsuranceClaim\src\main\java\com\insurance\claim\graph\ClaimGraphState.java" />

It carries one `Claim` object through every node. `Claim` implements `Serializable` because LangGraph4j clones state via serialization during execution.

### 4.5 Agent as a node

`AgentNode` wraps each `Agent` into a LangGraph4j `NodeAction`.

<ref_file file="C:\Users\Administrator\AssignmentMultiAgentInsuranceClaim\src\main\java\com\insurance\claim\graph\AgentNode.java" />

Each node receives the current state, runs its agent, mutates the `Claim`, and returns the updated claim. The graph then routes to the next node based on conditional edges.

### 4.6 Conditional routing

Edges are defined with `addConditionalEdges(...)`. The router lambdas read the current `Claim` state and return a command that maps to the next node.

Examples from <ref_file file="C:\Users\Administrator\AssignmentMultiAgentInsuranceClaim\src\main\java\com\insurance\claim\graph\ClaimWorkflow.java" />:

- After validation: go to manual review if amount > 100,000, go to fraud check if valid, otherwise end.
- After fraud detection: go to manual review if score ≥ 0.7, otherwise assess.
- After approval: go to payment if approved, otherwise end.

### 4.7 LLM calls inside agents

Agents that need AI (fraud, assessment, approval) call an `LlmService` abstraction.

<ref_file file="C:\Users\Administrator\AssignmentMultiAgentInsuranceClaim\src\main\java\com\insurance\claim\llm\LlmService.java" />

Three implementations are available, selected by `llm.provider`:

| Provider | Class | When to use |
|---|---|---|
| `stub` (default) | <ref_file file="C:\Users\Administrator\AssignmentMultiAgentInsuranceClaim\src\main\java\com\insurance\claim\llm\StubLlmService.java" /> | Local development and tests; deterministic responses. |
| `openai` | <ref_file file="C:\Users\Administrator\AssignmentMultiAgentInsuranceClaim\src\main\java\com\insurance\claim\llm\OpenAiLlmService.java" /> | Any OpenAI-compatible `/chat/completions` endpoint. |
| `anthropic` | <ref_file file="C:\Users\Administrator\AssignmentMultiAgentInsuranceClaim\src\main\java\com\insurance\claim\llm\AnthropicLlmService.java" /> | Native Anthropic Messages API. |

Provider selection is configured in <ref_file file="C:\Users\Administrator\AssignmentMultiAgentInsuranceClaim\src\main\java\com\insurance\claim\config\LlmConfig.java" />.

---

## 5. Manual review flow

When a claim is flagged for fraud or has an amount greater than 100,000 (1 lakh), the full workflow ends at `ManualReviewAgent` with status `MANUAL_REVIEW_REQUIRED`.

A human reviewer can then call:

```
POST /api/claims/{id}/review
{
  "approved": true,
  "notes": "Manual review cleared"
}
```

`ClaimService` runs the **post-review workflow**, which starts at `AssessmentAgent` and continues through approval, payment, and notification.

<ref_snippet file="C:\Users\Administrator\AssignmentMultiAgentInsuranceClaim\src\main\java\com\insurance\claim\graph\ClaimWorkflow.java" lines="104-117" />

---

## 6. How to run

### With the deterministic stub (default)

```powershell
mvn spring-boot:run
```

### With Anthropic

```powershell
mvn spring-boot:run -Dspring.profiles.active=anthropic -Danthropic.api-key="sk-ant-..."
```

### With an OpenAI-compatible endpoint

```powershell
mvn spring-boot:run -Dllm.provider=openai -Dopenai.api-key="sk-..."
```

### Submit a claim via REST

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/claims" `
  -Method POST -ContentType "application/json" `
  -Body '{"policyNumber":"POL-12345","description":"Rear-end collision repair","amount":2500}'
```

### Use the web UI

A simple web UI is included at:

```
http://localhost:8080/
```

<ref_file file="C:\Users\Administrator\AssignmentMultiAgentInsuranceClaim\src\main\resources\static\index.html" />

It lets you:

- Submit claims through a form.
- View all claims in a table.
- View the **Manual Review Queue**, which lists only claims with status `MANUAL_REVIEW_REQUIRED`.
- Approve or reject pending manual reviews directly from the queue.
- Perform manual reviews by claim ID.

The page calls the same REST endpoints the backend exposes.

---

## 7. Summary

LangGraph4j turns a complex, branching insurance claim process into a clean graph:

- **Nodes** = separate agents with single responsibilities.
- **Edges** = fixed transitions.
- **Conditional edges** = runtime decisions (valid/invalid, fraud clear/flagged, approved/rejected).
- **Shared state** = the `Claim` object that every agent reads and updates.
- **Compilation + invocation** = executes the entire workflow in order, with the framework handling routing.

This makes the system easy to extend: add a new agent, register it as a node, and connect it with an edge — without rewriting imperative control flow.
