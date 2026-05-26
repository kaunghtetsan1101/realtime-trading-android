# AI Prompt Templates

## Feature Planning Prompt

```text
Act as a Product Owner and Senior Android Engineer.
I am building a native Android realtime trading watchlist app for my GitHub portfolio.
Plan the feature: [feature name].
Include:
- User story
- Acceptance criteria
- MVP scope
- Module ownership
- Risks
- QA checklist
Keep it realistic and senior-level.
```

## Architecture Prompt

```text
Act as a Senior Android Architect.
Design the architecture for [feature] using Kotlin, Jetpack Compose, MVI, Clean Architecture, Coroutines, Flow, Hilt, Room, and WebSocket.
Include:
- Module placement
- Data flow
- UI state/events/effects
- Error handling
- Testing strategy
- Tradeoffs
```

## Implementation Prompt

```text
Act as a Senior Android Developer.
Implement [specific task] for a realtime trading app.
Constraints:
- Kotlin
- Jetpack Compose
- MVI
- Clean Architecture
- Testable code
- No overengineering
Output:
- Files to create/change
- Code
- Explanation
- Test notes
```

## Code Review Prompt

```text
Act as a strict Android code reviewer.
Review this code for:
- Architecture violations
- Compose performance issues
- Coroutine/Flow lifecycle issues
- Testability
- Naming/readability
- Overengineering
- Missing error states
Suggest concrete improvements.
```

## README Prompt

```text
Act as a technical writer for a Senior Android Engineer GitHub portfolio.
Write a README section for [feature/project].
Make it recruiter-friendly and engineering-focused.
Include tech stack, architecture, screenshots placeholders, setup, tests, and known limitations.
```
