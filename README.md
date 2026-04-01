# ⚡ Lightning Bids V1 - Real-Time Auction Engine
A high-performance, real-time auction bidding engine built with **Spring Boot 3**, **PostgreSQL**, and **STOMP WebSockets**.

## 🏗️ Architecture & System Design
This application is built using **Clean Architecture** (Controller-Service-Repository) and is explicitly engineered to handle extreme concurrency and prevent data-loss race collisions.
- **REST API:** Handles immutable state requests and authenticated order execution (`POST /api/auctions/{id}/bid`).
- **Security:** Stateless **JWT authentication** managed universally via a custom Spring Security filter chain.
- **Concurrency Control:** Utilizes strict **PostgreSQL Optimistic Locking (`@Version`)** to guarantee database isolation. If 1,000 users submit a bid at the exact same millisecond, 1 transaction securely commits while the backend physically intercepts the remaining 999, throwing a precise `ObjectOptimisticLockingFailureException` and returning clean HTTP `409 Conflict` rejections.
- **Real-Time Engine:** Uses Spring **STOMP WebSockets** (`/ws`) natively functioning as a one-way megaphone to broadcast successful transactions instantly across all connected client nodes.

---

## 🚀 Getting Started Locally

### Prerequisites
- Java 21+
- Maven
- Docker

### 1. Start the Database
Run the local PostgreSQL instance securely via Docker:
```bash
docker-compose up -d
```

### 2. Boot the Application
The `application.properties` is configured by default to aggressively drop the database tables and securely seed fresh test data upon every initialization.
```bash
mvn spring-boot:run
```

### 3. Concurrency Stress Test (The Frontend)
Navigate to `http://localhost:8080` to access the vanilla JS/CSS **High-Frequency Execution Desk**. 

Register a quick test account, select the active auction, and click the **🚀 STRESS TEST** button to forcefully fire 50 concurrent fetch requests into the exact same millisecond window. The local UI terminal will tally the precise number of transactions that successfully won the underlying lock vs those intercepted by the database engine.

---

## ☁️ Deployment Guide (Railway)
This application supports seamless environment variable injection for CI/CD production deployment.

1. Connect your GitHub repository to [Railway.app](https://railway.app).
2. Provision a PostgreSQL database add-on.
3. Supply the following environment variables to your Spring Boot service:
   - `DB_URL` = `jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}`
   - `DB_USERNAME` = `${{Postgres.PGUSER}}`
   - `DB_PASSWORD` = `${{Postgres.PGPASSWORD}}`
   - `DDL_AUTO` = `update` *(Required to prevent PostgreSQL data loss on server pushes)*

---

## 🗺️ Version 2.0 Architectural Roadmap
While V1 achieves database lock integrity, hitting the physical disk for thousands of concurrent bids causes severe bottlenecks. V2 will introduce scale:
1. **Redis Queueing:** Funnel the instantaneous HTTP firehose through a Redis Cache to rapidly batch-process optimistic lock acquisitions in memory.
2. **JWT Refresh Tokens:** Implement a strict 15-minute Access Token paired with an `HttpOnly` cookie-stored Refresh Token.
3. **Apache Kafka Event Streams:** Decouple the STOMP WebSocket broadcasts into an event stream to allow seamless horizontal pod scaling.
