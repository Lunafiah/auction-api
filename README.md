# Real-Time Auction API

A robust, enterprise-grade Real-Time Bidding and Auction API engine built with Java and Spring Boot. This portfolio-ready application maps Node.js/Express architectural concepts to the Spring Boot ecosystem, demonstrating advanced backend patterns for high-concurrency environments.

## 🚀 Key Features

*   **Real-Time Bidding via WebSockets:** Push real-time bid updates to all connected clients instantly.
*   **Zero-Conflict Transactions:** Strict database transaction management using Optimistic/Pessimistic locking to handle concurrent bid requests without race conditions.
*   **Secure Stateless Auth:** JWT-based authentication (`JwtAuthenticationFilter`) for securing endpoints and managing user sessions.
*   **Relational Data Integrity:** Built with Spring Data JPA and Hibernate, configured for PostgreSQL.

## 🛠️ Tech Stack

*   **Language:** Java (JDK 21)
*   **Framework:** Spring Boot
*   **Database:** PostgreSQL (Port `5432`)
*   **ORM:** Hibernate / Spring Data JPA
*   **Build Tool:** Maven

---

## 💻 Getting Started

Follow these instructions to set up the project on your local machine.

### Prerequisites

Ensure you have the following installed before starting:
1.  [Java Development Kit (JDK 21)](https://adoptium.net/)
2.  [Apache Maven](https://maven.apache.org/) (or use the included `mvnw` wrapper)
3.  [PostgreSQL](https://www.postgresql.org/download/) (running on the default port `5432`)

### 1. Database Setup

You need a running PostgreSQL database for the application to connect to. 
1. Open pgAdmin or your terminal.
2. Create a new database for the application:
   ```sql
   CREATE DATABASE auction_db;
   ```
*(If your local Postgres uses a custom username/password, update them in the `src/main/resources/application.properties` file).*

### 2. Clone the Repository

```bash
git clone https://github.com/YOUR_USERNAME/auction-api.git
cd auction-api
```

### 3. Build & Run the Application

You can run the application directly using Maven:

```bash
# Clean and compile the project
mvn clean install

# Run the Spring Boot application
mvn spring-boot:run
```

If successful, the API runs on `http://localhost:8080`.

---

## 📡 API Architecture Highlights

This API demonstrates:
1.  **Concurrency Control:** Ensuring that if 1,000 users bid on an item at the exact same millisecond, the database handles the locks perfectly without losing or duplicating data.
2.  **Spring Security Integration:** Securing routes seamlessly while integrating custom `UserDetailsServiceImpl` for database-backed user validation.
3.  **Clean Code:** Transitioning from Express.js unstructured paradigms to Spring Boot's Model-Controller-Service-Repository organized layering.

## 📝 License
This project is for portfolio and educational purposes.
