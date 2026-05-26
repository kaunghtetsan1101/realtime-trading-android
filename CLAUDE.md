# Realtime Trading / Market Watch App

Act as a Product Owner, Senior Android Engineer, QA Engineer, and Technical Writer.

## Goal
Build a production-style Native Android portfolio app for realtime market watching and trading simulation.

## Tech Stack
- Kotlin
- Jetpack Compose
- Material3
- Coroutines
- Flow / StateFlow / SharedFlow
- MVI
- Clean Architecture
- Multi-module Gradle
- Hilt
- Retrofit / OkHttp / WebSocket
- Room
- JUnit
- MockK or Mockito
- Turbine
- Compose UI Testing

## Rules
- Plan before coding.
- Build feature by feature.
- Do not generate the whole app at once.
- Keep architecture clean and realistic.
- Use small commits.
- Explain tradeoffs.
- Add tests for ViewModels, use cases, repositories, and Flow logic.
- Keep README updated.
- Avoid overengineering.
- Prioritize portfolio quality.

## App Features
- Realtime market watchlist
- WebSocket mock streaming
- Asset detail screen
- Search assets
- Favorite/watchlist persistence
- Offline caching
- Error/loading/empty states
- Dark mode
- Mock order/trading screen

## Architecture
Use this module structure:

app
core-common
core-ui
core-designsystem
core-network
core-database
domain
data
feature-watchlist
feature-market-detail
feature-search
feature-settings

## Development Workflow
For every feature:
1. Write user story
2. Write acceptance criteria
3. Design data flow
4. Implement domain/data/UI
5. Add tests
6. Run QA checklist
7. Update README

## Definition of Done
A feature is done only when:
- It works
- It has loading/error/empty states
- It is testable
- It has reasonable test coverage
- It follows architecture
- It is documented