# Real-time Bidding API Engine - Version 1

## Goal Description
Build a high-performance, real-time auction API using Java and Spring Boot. The system must handle frequent concurrent bids (100-500 connections) reliably, utilizing PostgreSQL with Optimistic Locking (`@Version`) to ensure strict transactional integrity and prevent race conditions. 

## User Review Required
> [!IMPORTANT]
> Please review this technical approach, specifically the translation from Node.js concepts to Spring Boot, and the decisions needed before we start coding (Java version, Build tool).

## Proposed Architecture & Technical Approach

We will use Clean Architecture with a Controller-Service-Repository pattern. Here is how this maps from your Node.js/Express experience to Spring Boot:
- **Controllers** (like Express routers): Handle incoming HTTP requests and WebSocket messages.
- **Services** (like Express controller logic): Contain the business rules, handle database transactions, and execute the locking logic.
- **Repositories** (like Prisma/Mongoose): Interfaces that handle direct database interactions via Spring Data JPA.

### 1. Database Schema
We will need three core entities in PostgreSQL:
- **User**: `id`, `username`, `password` (hashed).
- **AuctionItem**: `id`, `name`, `description`, `startingPrice`, `currentHighestBid`, `highestBidderId`, `endTime`, `version` (for Optimistic Locking).
- **BidHistory**: `id`, `auctionItemId`, `userId`, `bidAmount`, `timestamp`.

### 2. Core Components

#### Security & Auth (JWT)
- Configure Spring Security to use JWTs for stateless authentication.
- Endpoints: `POST /api/auth/register`, `POST /api/auth/login`.

#### REST API (HTTP)
- `GET /api/auctions` - List all active auctions.
- `GET /api/auctions/{id}` - Get details for a specific auction.
- We will use an initializer (`CommandLineRunner`) to seed the database with test items on application boot.

#### WebSocket Integration
- Configure Spring WebSocket with STOMP (Simple Text Oriented Messaging Protocol).
- Clients will subscribe to `/topic/auctions/{id}` to receive real-time updates when new bids are accepted.
- Clients will send new bids to `/app/auctions/{id}/bid`.

#### Concurrency & Locking Engine
- We will use `@Version` on the `AuctionItem` entity.
- When a user places a bid, the Service layer fetches the item. If the bid is valid (higher than current), it attempts to update the item.
- If two users bid in the exact same millisecond, the first to commit updates the `version`. The second transaction will automatically throw an `ObjectOptimisticLockingFailureException`.
- We will catch this exception inside the service, notify the user (via WebSocket) that their bid failed due to a newer bid, and broadcast the new highest bid to the room.

## Complexity Estimate
**Complexity: Medium to Ambitious.** 
While building CRUD endpoints is straightforward, properly configuring stateless Spring Security with JWTs and managing WebSocket sessions across transactions adds significant complexity. Handling the Optimistic Locking failure gracefully and tying it back to the precise WebSocket client is the ambitious "portfolio-grade" core of this project.

## Prerequisites & Decisions Needed
Before we move to Phase 3 (Building), I need your decision on:
1. **Build Tool**: Maven or Gradle? (I recommend **Maven** for beginners coming from Node/NPM, as the `pom.xml` maps conceptually well to `package.json`).
2. **Java Version**: Java 17 or 21? (I recommend **Java 21** as it is the latest LTS and supports Virtual Threads, though we'll stick to standard Tomcat threads for V1 as requested).
3. **Database Setup**: Do you have PostgreSQL installed locally on Windows, or do you prefer using Docker?

## Verification Plan

### Automated Tests
- Write JUnit tests mocking concurrent requests entirely in-memory to verify the Optimistic Locking behaves perfectly under load.

### Manual Verification
- Build a simple HTML/vanilla-JS test client to open multiple WebSocket connections locally. We will literally spam the bid button to visually prove the locks prevent race conditions.
