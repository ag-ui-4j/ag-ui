# Upstream provenance and baseline gaps

> **Upstream extraction (Task 17).** This module has a deterministic, standalone upstream export
> that rewrites `com.koralplay.ai.agui.adk` → `io.github.agui4j.adk`, strips Spring/KoralPlay,
> and builds/tests with no KoralPlay dependency. See the proposal
> `docs/upstream/ag-ui-4j-adk-proposal.md` and the scripts `scripts/export-ag-ui-4j-adk.sh` /
> `scripts/test-export-ag-ui-4j-adk.sh`. **Nothing upstream has been published** — the export and
> proposal are held pending maintainer review and explicit authorization.

## Verified source

- Original AG-UI PR: https://github.com/ag-ui-protocol/ag-ui/pull/923
- Original verified head: `2b44d6f45fdf9430531dcf43241685b0757297de`
- Original head subject: `chore(adk-java): bump artifact + google-adk baseline to 1.5.0`
- Reposted Java PR: https://github.com/Work-m8/ag-ui-4j/pull/65
- Reposted verified head: `9b925e6d86b5efe96ba7c50b958da219a1f6cb60`
- Reposted head subject: `feat(adk-java): add Google ADK integration with CopilotKit example`
- Official Java protocol dependency: https://github.com/ag-ui-4j/ag-ui, version `0.2.0`

The complete `src` tree at original PR #923 head
`sdks/community/java/integrations/adk-java/src` is byte-for-byte identical to the complete
`integrations/adk-java/src` tree at reposted PR #65 head. A recursive `diff -qr` produced no
output. Commit `2b44d6f...` changes only the original integration POM, guide, and example POM on
top of that source tree; the adapter sources and tests are the same reposted baseline.

Local modifications are tracked by this repository's git history.

## Imported source

The following PR #65 baseline source was ported from `com.agui.adk` to
`com.koralplay.ai.agui.adk`:

- `AguiAdkRunnerAdapter.java`, renamed and adapted as `GoogleAdkAgent.java`
- `RunContext.java`
- `SessionManager.java`
- `processor/MessageChunk.java`
- `processor/MessageProcessor.java`
- `processor/ToolResult.java`
- `translator/EventTranslator.java`
- `translator/EventTranslatorFactory.java`
- `translator/PredictStateMapping.java`
- `translator/TranslationContext.java`
- `translator/context/PredictiveState.java`
- `translator/context/StreamingState.java`
- `translator/context/ToolState.java`
- `translator/step/EventTranslationStep.java`
- `translator/step/StateDeltaTranslationStep.java`
- `translator/step/TextMessageStreamStep.java`
- `translator/step/ToolCallRequestStep.java`
- `translator/step/ToolCallResponseStep.java`

All 16 upstream adapter test classes were ported and renamed with the `BaselineTest` suffix.
`GoogleAdkAgentBaselineTest` replaces direct Google `Runner` mocking with the adapter-local
`AdkRunnerClient` fake and includes the missing-frontend-tools characterization.

## Adapter-local additions

- `AdkRunnerClient` is the deterministic seam around the Google ADK runner.
- `GoogleAdkRunnerClient` delegates to official `Runner.appName()` and the five-argument
  `Runner.runAsync(...)` overload.
- The fifth `Map<String, Object>` argument of Google ADK 1.3.0 `Runner.runAsync(...)` is the
  invocation `stateDelta`. The seam preserves that exact meaning, and the baseline passes an empty
  state delta. It exposes no frontend-tool injection channel.
- `GoogleAdkAgent` manually adapts RxJava Reactive Streams subscriptions to JDK `Flow`, forwarding
  demand and cancellation without copying or modifying official AG-UI protocol classes.

## Deliberately omitted upstream files

- `module-info.java`
- Integration `pom.xml`, `README.md`, `ARCHITECTURE.md`, and `INTEGRATION_GUIDE.md`
- All Java and frontend examples, application configuration, routes, handlers, and resources
- Maven wrapper and repository-level POM changes
- Any upstream AG-UI core, JSON, server, contract, or application implementation

The module uses official `io.github.agui4j` 0.2.0 artifacts instead of copied protocol code.

## Official model/API differences

### Old `com.agui.*` models to official `io.github.agui4j.*`

- `RunAgentParameters` and its builder became the immutable `RunAgentInput` record.
- Official `RunAgentInput` requires non-null thread and run IDs. The imported fallback that generated
  a run ID from `null` is unreachable; its baseline test is retained but disabled with that reason.
- Mutable `BaseMessage` subclasses and JavaBean accessors became immutable `Message` records and
  record accessors.
- `Context` moved under `core.agent`.
- `ToolCall` moved under `core.message` and uses immutable `FunctionCall` records.
- Role values are typed `Role` enum constants rather than lower-case mutable string properties.
- Official event types implement the sealed `Event` interface; tests use concrete event records
  rather than mocking that interface.

### RxJava to JDK Flow

- The imported adapter returned `Flowable<BaseEvent>` and accepted a reactive `Single<String>` user
  ID. Official `Agent.run` returns `Flow.Publisher<Event>` and has only `RunAgentInput`.
- The port resolves the user ID through the required builder `Function<RunAgentInput, String>` seam
  at subscription time, keeps the internal imported RxJava pipeline, and converts it at the boundary.
- The boundary forwards JDK demand and cancellation to the RxJava subscription.

### Serialization and event constructors

- The old mutable event setters and copied `EventFactory` helpers do not exist in official 0.2.0.
  The port uses official immutable event constructors.
- `StateDeltaEvent` requires `JsonPatchOperation` values; the imported add-patch mapping is retained
  using those official records.
- `TextMessageStartEvent` requires typed `Role.ASSISTANT`.
- `CustomEvent` requires a non-null name and value. The minimum port uses `predictState` and retains
  the imported payload as both value and raw event.
- Google ADK normalizes absent function-response and state-delta collections to empty collections;
  the two upstream null-only tests are retained but disabled with explicit reasons.

## Characterized baseline gaps

These are intentionally not fixed in this baseline task:

- **Frontend tools:** request-defined frontend tools are carried in request `RunConfig` metadata and
  resolved by `AgUiToolset` when that stateless toolset is installed on an ADK `LlmAgent`. The
  application-level root-agent wiring remains outside this adapter module until the Spring migration.
- **Request state shape:** `RunAgentInput.state()` is ignored. Only Google ADK action state deltas are
  translated, and they are emitted as root-level `add` patches without richer state reconciliation.
- **Predictive-state shape:** predictive mappings are not populated from the request; the official
  `CustomEvent` name is only the minimum constructor seam for the imported raw payload.
- **Context and extensions:** request context, parent run ID, forwarded properties, and raw tool-schema
  extensions are not consumed by the baseline except indirectly through a caller-supplied user-ID
  extractor.
- **Concurrency:** processed-message writes and pending-tool writes retain the imported per-session
  JVM lock approach. Message marking is fire-and-forget via `subscribe()`, so ordering, error
  propagation, and cross-process coordination remain incomplete.
- **Authentication:** there is no authentication integration. The synchronous user-ID extractor is a
  required builder seam; omitting it fails at build time, while blank, null, or throwing extractors
  produce a run error event.
- **Session cleanup (fixed):** explicit cleanup now uses the official
  `(appName, userId, sessionId)` order and delegates deletion through the official ADK session
  service after authoritative lookup.
- **Lifecycle and cancellation (fixed):** run-owned pending-call persistence no longer uses replay
  caches; JDK cancellation disposes upstream work, and message/session finalization stays joined to
  the run transaction.
- **Error lifecycle (fixed):** failures after `RUN_STARTED` emit `RUN_ERROR` followed by one
  `RUN_FINISHED` carrying a structured error outcome (`status`, `code`, `message`, `rawError`).
- **Tool-result lifecycle (fixed):** tool names are resolved from authoritative ADK session history
  plus the request, blank IDs fail explicitly, `confirm_changes` remains filtered, and duplicate
  IDs are deterministic with the last occurrence winning.
- **Resource lifecycle (fixed):** bridge `close()` idempotently cancels active runs and closes the
  runner plus closeable memory/session services.
