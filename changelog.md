# Changelog

## [Unreleased]
### Added
- **Redis Integration**: Added `spring-boot-starter-data-redis` dependency to `pom.xml` and configured `application.properties` to connect to a local Redis instance.
- **Docker Orchestration**: Updated `docker-compose.yml` to spin up a Redis container alongside PostgreSQL and the k6 load testing suite.
- **In-Memory Validation**: Implemented an atomic Redis Lua script in `AuctionService.placeBid()` to evaluate incoming bids in microseconds against memory rather than physical disk.
- **Queueing System**: Bids that pass the Redis validation check are now pushed asynchronously to a Redis List (`auction:bids:queue`).
- **Background Worker**: Created `BidProcessor` service with `@Scheduled` task execution to periodically pop winning bids from the Redis queue and safely persist them to PostgreSQL in the background without blocking API responses.
- **Main Class Annotation**: Added `@EnableScheduling` to `AuctionApiApplication` to allow background tasks.

### Changed
- **Bid Placement Flow**: Re-architected the `placeBid` method to drop `@Transactional(readOnly = false)` since synchronous PostgreSQL writes were eliminated. Database writes are now safely decoupled from the critical path of the bid request.
- **Testing Mocks**: Updated `AuctionServiceTest` to successfully mock `StringRedisTemplate`, `ValueOperations`, `ListOperations`, and `RedisScript` executions to preserve high unit test coverage.

### Fixed
- **Database Scalability Bottleneck**: Prevented thousands of concurrent `SELECT` and `UPDATE` queries from locking the database layer by absorbing the entire "thundering herd" into the Redis Cache and Queue.
