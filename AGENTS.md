# Multi-Agent Insurance Claim System - Project Notes

## Build & Test Commands

```powershell
# Compile only
mvn compile

# Run tests (uses deterministic stub LLM)
mvn test

# Run the application with the default stub LLM
mvn spring-boot:run
```

## Architecture

- **Java 17 + Spring Boot 3.2.5 + Maven**
- **Orchestration framework**: [LangGraph4j](https://github.com/langgraph4j/langgraph4j) - the Java port of LangGraph for stateful, multi-agent workflows.
  - `ClaimGraphState` defines the graph schema.
  - `ClaimWorkflow` wires nodes and conditional edges into two `CompiledGraph` instances:
    - Full workflow: intake → validation → fraud → (manual review) → assessment → approval → payment → notification.
    - Post-review workflow: assessment → approval → payment → notification (used after a manual review approves a flagged claim).
- **Separate agents** (each is a Spring component under `com.insurance.claim.agent`):
  - `IntakeAgent`
  - `ValidationAgent`
  - `FraudDetectionAgent`
  - `AssessmentAgent`
  - `ApprovalAgent`
  - `PaymentAgent`
  - `NotificationAgent`
  - `ManualReviewAgent`
- **LLM abstraction**: `LlmService` interface with pluggable providers selected by `llm.provider`:
  - `anthropic` → `AnthropicLlmService` (Anthropic Messages API)
  - `openai` → `OpenAiLlmService` (OpenAI-compatible `/chat/completions`)
  - `stub` (default) → `StubLlmService` (deterministic local responses)
- **REST API** in `ClaimController`:
  - `POST /api/claims`
  - `GET /api/claims/{id}`
  - `GET /api/claims`
  - `POST /api/claims/{id}/review`

## Run with Anthropic

Activate the `anthropic` Spring profile and pass your API key on the command line:

```powershell
Set-Location "C:\Users\Administrator\AssignmentMultiAgentInsuranceClaim"
mvn spring-boot:run -Dspring.profiles.active=anthropic -Danthropic.api-key="sk-nB3EcsOfESPVwMuIjcdggA"
```

Then submit a claim:

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/claims" `
  -Method POST -ContentType "application/json" `
  -Body '{"policyNumber":"POL-12345","description":"Rear-end collision repair","amount":2500}'
```

The LangGraph4j workflow will route the claim through Intake → Validation → Fraud → Assessment → Approval → Payment → Notification, calling Anthropic at each LLM step.

## Web UI

A simple web UI is available at:

```
http://localhost:8080/
```

<ref_file file="C:\Users\Administrator\AssignmentMultiAgentInsuranceClaim\src\main\resources\static\index.html" />

Use it to submit claims, view the claim list, see the **Manual Review Queue** (claims with status `MANUAL_REVIEW_REQUIRED`), and approve/reject pending reviews directly from the queue.

## Configuration

### Anthropic

```properties
llm.provider=anthropic
anthropic.base-url=https://api.anthropic.com
anthropic.model=global.anthropic.claude-haiku-4-5-20251001-v1:0
anthropic.api-key=${ANTHROPIC_API_KEY}
anthropic.max-tokens=1024
```

> The model string above is configured as requested. Note that it looks like an AWS Bedrock inference-profile ID; if Anthropic's native API rejects it, switch to a standard Anthropic model name such as `claude-3-haiku-20240307`.

### OpenAI-compatible

```properties
llm.provider=openai
openai.base-url=https://api.openai.com/v1
openai.model=gpt-3.5-turbo
openai.api-key=${OPENAI_API_KEY}
```

## Notes

- `Claim` implements `Serializable` because LangGraph4j clones state via serialization during graph execution.
- The graph returns a serialized copy of the claim; `ClaimService` stores the processed copy returned by `ClaimWorkflow`.
- Do not commit API keys. Pass them via environment variables or command-line `-D` properties.
