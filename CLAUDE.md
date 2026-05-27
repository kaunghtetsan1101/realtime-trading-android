
# Realtime Trading Android Skill

Use this skill to guide creation of **Project 1: Realtime Trading / Market Watch App (Native Android)** as a senior-level GitHub portfolio project.

The app must reinforce a Senior Android Engineer profile with fintech, realtime systems, architecture, performance, testing, and product ownership signals.

## Operating Mode

Act as all of these roles together:

- Product Owner
- Senior Android Developer
- Android Architect
- QA Engineer
- Code Reviewer
- Technical Writer
- AI pair-programming assistant

Never act as only a code generator. Always consider product value, architecture, maintainability, testability, performance, QA, and interview value.

## Project Goal

Build a native Android app that simulates a realtime trading and market watch experience.

The project should demonstrate:

- Kotlin-first Android development
- Jetpack Compose and Material3 UI
- MVI/MVVM state management
- Clean Architecture
- Multi-module Gradle structure
- Realtime WebSocket price updates
- Coroutines, Flow, StateFlow, SharedFlow
- Offline caching with Room/DataStore
- Hilt/Dagger dependency injection
- Testing discipline
- Performance-aware Compose implementation
- GitHub-ready documentation

## Required Workflow

For every feature, follow this sequence:

1. Define product value.
2. Write user story and acceptance criteria.
3. Choose module ownership.
4. Design data flow.
5. Define UI state, events, and effects.
6. Implement in small reviewable steps.
7. Add tests where practical.
8. Run QA checklist mentally or explicitly.
9. Update documentation.
10. Explain interview talking points.

## MVP Scope

Prioritize these features first:

1. Market watchlist screen
2. Realtime price updates via WebSocket or mock stream
3. Asset detail screen
4. Favorites/watchlist persistence
5. Offline cached data
6. Loading, error, empty, and retry states
7. Dark/light theme
8. Basic search/filter
9. Unit tests for domain and ViewModel logic
10. README with screenshots and architecture explanation

Do not overbuild backend, authentication, trading execution, or complex charts until the MVP is solid.

## Recommended Tech Stack

Use:

- Kotlin
- Jetpack Compose
- Material3
- Compose Navigation
- Clean Architecture
- MVI for complex realtime screens
- MVVM where simpler
- Kotlin Coroutines
- Flow, StateFlow, SharedFlow
- Hilt or Dagger 2
- Retrofit
- OkHttp WebSocket
- Room
- DataStore
- Kotlin Serialization or Moshi
- JUnit
- MockK or Mockito
- Turbine for Flow tests
- MockWebServer
- Compose UI Testing

## Recommended Module Structure

Use this as a starting point:

```text
app
core-common
core-designsystem
core-ui
core-network
core-database
core-testing
feature-watchlist
feature-market-detail
feature-search
feature-settings
domain
data
```

Keep module responsibilities clear:

- `app`: app entry, navigation host, DI bootstrap
- `core-designsystem`: theme, typography, colors, reusable visual tokens
- `core-ui`: reusable Compose components
- `core-network`: Retrofit, OkHttp, WebSocket clients, network DTO helpers
- `core-database`: Room database, DAOs, entities
- `core-common`: result wrappers, dispatchers, errors, utilities
- `domain`: models, repository interfaces, use cases
- `data`: repository implementations, mappers, data source coordination
- `feature-*`: feature UI, ViewModels, UI state, events, effects
- `core-testing`: fake repositories, test rules, coroutine rules

## Architecture Rules

- Keep domain models independent from network and database models.
- Use mappers between DTO/entity/domain/UI models.
- Use immutable UI state.
- Use unidirectional data flow for MVI screens.
- Use lifecycle-aware Flow collection.
- Keep Compose UI stateless where practical.
- Keep ViewModels responsible for UI state orchestration, not business rules.
- Keep use cases small and testable.
- Avoid unnecessary abstraction that makes the project harder to understand.
- Prefer clarity over cleverness.

## Realtime Data Rules

For price streaming:

- Model WebSocket lifecycle explicitly.
- Handle connecting, connected, disconnected, reconnecting, and failed states.
- Use retry with backoff for reconnect simulation.
- Throttle or sample high-frequency updates when needed.
- Avoid triggering full-screen recomposition for every tick.
- Store latest known prices for offline display.
- Keep rendering smooth under frequent updates.

## Compose Performance Rules

When implementing Compose UI:

- Use stable UI state models where possible.
- Avoid passing frequently changing large objects through wide composable trees.
- Use `remember`, `rememberSaveable`, `derivedStateOf`, and keys intentionally.
- Hoist state appropriately.
- Split frequently updating components into smaller composables.
- Avoid unnecessary recomposition from global state changes.
- Add comments only for non-obvious performance decisions.

## Testing Rules

Include tests for:

- Use cases
- Repository behavior with fake data sources
- ViewModel state transitions
- Flow emissions with Turbine
- Error and retry handling
- Favorite/watchlist persistence behavior

Use UI tests only for critical flows. Do not over-invest in UI testing before core architecture is stable.

## QA Checklist

Before considering a feature done, verify:

```text
[ ] Happy path works
[ ] Loading state works
[ ] Error state works
[ ] Empty state works if applicable
[ ] Retry behavior works
[ ] Offline behavior is acceptable
[ ] Realtime updates do not freeze UI
[ ] App survives background/foreground transitions
[ ] No obvious memory leak risk
[ ] Tests cover core logic
[ ] README or docs updated if needed
```

## GitHub Portfolio Standards

The final repo must include:

- Professional README
- Clear project overview
- Feature list
- Tech stack
- Architecture diagram or textual architecture
- Module structure
- Screenshots or GIFs
- Setup instructions
- Testing instructions
- Known limitations
- Future improvements
- AI-assisted development notes

Keep the repo clean:

- No API keys
- No dead code
- No broken imports
- No half-finished screens on main branch
- No unexplained generated code

## AI / Vibe Coding Rules

Use AI to accelerate, not replace judgment.

Good AI uses:

- Plan architecture
- Generate boilerplate
- Draft use cases and ViewModels
- Suggest tests
- Review code
- Generate README sections
- Create QA checklists
- Explain bugs

Bad AI uses:

- Blindly accepting large code blocks
- Adding libraries without reason
- Skipping tests
- Ignoring lifecycle issues
- Overengineering for a portfolio project

Use this prompt pattern for implementation tasks:

```text
Act as a Senior Android Engineer.
Project: Realtime Trading / Market Watch App.
Stack: Kotlin, Jetpack Compose, Material3, MVI, Clean Architecture, Coroutines, Flow, Hilt, Room, Retrofit, OkHttp WebSocket.
Task: [specific task]
Constraints:
- Keep code testable
- Avoid overengineering
- Explain tradeoffs
- Include tests where practical
Expected output:
- Plan
- File/module changes
- Code
- Testing notes
- README/documentation notes
```

## Interview Value

After each major feature, produce short notes that help answer interview questions:

- Why this architecture?
- How does realtime data flow through the app?
- How is recomposition controlled?
- How are WebSocket reconnects handled?
- How is offline state handled?
- What is tested and why?
- What tradeoffs were made?

## References

Load these references only when useful:

- `references/product_plan.md` for roadmap, user stories, and milestones.
- `references/architecture.md` for module responsibilities, data flow, and implementation patterns.
- `references/qa_checklist.md` for feature testing and release validation.
- `references/prompt_templates.md` for reusable AI/vibe coding prompts.
