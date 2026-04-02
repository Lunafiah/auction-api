# Real-time Bidding API Engine - V1

## Phase 1: Discovery (Completed)
- [x] Define use case and auction type
- [x] Decide on DB Locking vs Queue
- [x] Determine MVP Must-Haves
- [x] Agree on scale and performance targets

## Phase 2: Planning (In Progress)
- [/] Draft Implementation Plan
- [ ] Get User Approval on Architecture
- [ ] Set up Project Infrastructure (Maven, Java 21, Spring Boot Starter)

## Phase 3: Building
- [ ] Step 1: Database & Entities
  - [ ] Configure PostgreSQL Connection
  - [ ] Create User, AuctionItem, BidHistory entities
  - [ ] Seed test data on startup
- [ ] Step 2: Authentication
  - [ ] Implement Spring Security with JWT
  - [ ] Create Login/Register endpoints
- [ ] Step 3: Core Bidding Service & REST
  - [ ] Create REST endpoints to view items
  - [ ] Implement Bid Service with `@Transactional` and Optimistic Locking
- [ ] Step 4: WebSocket Integration
  - [ ] Configure STOMP WebSocket endpoints
  - [ ] Broadcast bid updates to connected clients
  - [ ] Handle OptimisticLockException and send failure message to bidder

## Phase 4: Polish & Testing
- [ ] Write JUnit Concurrent Tests
- [ ] Build basic HTML/JS visual test client
- [ ] Refine error handling and responses

## Phase 5: Handoff
- [ ] Deploy to Render/Railway
- [ ] Document setup instructions
