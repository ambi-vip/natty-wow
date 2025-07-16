# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a DDD (Domain-Driven Design) project template built on the Wow framework. It's a multi-module Kotlin project using Gradle for build management, targeting JDK 17.

## Build System

This project uses Gradle with Kotlin DSL (`.gradle.kts` files) and requires JDK 17 or higher.

### Common Development Commands

#### Build and Run
```bash
# Start the server (main application)
./gradlew server:run

# Build the server distribution
./gradlew server:installDist

# Build all modules
./gradlew build
```

#### Testing
```bash
# Run all tests
./gradlew test

# Run tests for a specific module
./gradlew domain:test

# Run domain model tests with coverage
./gradlew domain:check

# Verify test coverage (requires 80% minimum)
./gradlew domain:jacocoTestCoverageVerification
```

#### Code Quality
```bash
# Run code style checks
./gradlew detekt

# Run tests with retry (useful in CI)
./gradlew test --continue
```

#### Single Test Execution
```bash
# Run a specific test class
./gradlew domain:test --tests "site.weixing.natty.domain.SomeTestClass"

# Run a specific test method
./gradlew domain:test --tests "site.weixing.natty.domain.SomeTestClass.testMethod"
```

## Project Architecture

### Module Structure
- **api/**: API layer - Commands, Domain Events, Query View Models (the "published language")
- **domain/**: Domain layer - Aggregates, business logic, domain rules
- **server/**: Host service - Application entry point, controllers, configuration
- **security/**: Security module - Authentication and authorization
- **dependencies/**: Dependency management BOM
- **bom/**: Project Bill of Materials
- **code-coverage-report/**: Test coverage reporting

### Key Framework Dependencies
- **Wow Framework**: Core DDD framework (version 5.21.1)
- **Spring Boot**: 3.5.3 (WebFlux for reactive programming)
- **MongoDB**: Primary data store (reactive driver)
- **Redis**: Caching and session management
- **Kotlin**: Primary language with Spring support
- **CoSID**: Distributed ID generation
- **CoSec**: Security framework
- **Simba**: Distributed locking

### Application Entry Point
- Main class: `site.weixing.natty.server.ServerKt`
- Default port: 8080
- Swagger UI available at: http://localhost:8080/swagger-ui.html

## Development Workflow

### Code Generation
The project uses KSP (Kotlin Symbol Processing) with the Wow compiler for code generation:
```bash
# Trigger code generation
./gradlew kspKotlin
```

### Testing Strategy
- Unit tests in each module's `src/test/kotlin`
- Domain module requires 80% test coverage
- Use JUnit 5 with Kotlin test DSL
- Mock dependencies using MockK

### Configuration
- Application configs in `server/src/main/resources/`
- Environment-specific configs: `application-{profile}.yaml`
- JVM args configured in `server/build.gradle.kts`

## Project-Specific Conventions

### Package Structure
- Base package: `site.weixing.natty`
- Domain models in `domain/src/main/kotlin/site/weixing/natty/domain/`
- API definitions in `api/src/main/kotlin/site/weixing/natty/api/`
- Server components in `server/src/main/kotlin/site/weixing/natty/server/`

### Key Components
- **Dictionary Management**: Core business capability with CRUD operations
- **File Storage**: File upload and management system
- **User Management**: Authentication and user services
- **Compensation**: Saga pattern implementation for distributed transactions

### Build Configuration
- Chinese mirror repositories configured for faster dependency resolution
- Parallel builds enabled (`org.gradle.parallel=true`)
- Build cache enabled with 5GB limit
- Incremental compilation for faster builds