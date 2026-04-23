# API Performance Load Test Results

**Date**: 2026-04-23
**Test Tool**: Grafana k6
**Target Architecture**: Spring Boot + Redis Queueing + PostgreSQL

## 🔍 Test Configuration
- **Virtual Users (VUs)**: 100 max concurrent
- **Duration**: 90 seconds (including ramp-up and ramp-down)
- **Scenarios**:
  - Ramp up to 50 VUs (10s)
  - Sustain 50 VUs (30s)
  - Spike to 100 VUs (10s)
  - Sustain 100 VUs (30s)
  - Ramp down to 0 VUs (10s)

## ⚡ Performance Test Results
*   **Total API Calls (Iterations)**: `5,854` requests
*   **Throughput**: `~64.05` requests per second
*   **Data Received**: `3.1 MB` (`34 kB/s`)
*   **Data Sent**: `2.0 MB` (`22 kB/s`)

### 📊 Response Times
| Metric | Time |
| --- | --- |
| Average | `32.72ms` |
| Median | `28.03ms` |
| Minimum | `15.79ms` |
| 90th Percentile (p90) | `48.62ms` |
| 95th Percentile (p95) | `60.78ms` |
| Maximum | `808.10ms` (Initial JVM warm-up spike) |

### 🛡️ Reliability & Stability Metrics
*   **HTTP 200 OK or 409 Conflict**: `100.00%` (`5,853` out of `5,853` bid requests)
*   **HTTP 500 Server Error**: **`0.00%`** (`0` out of `5,853`)

## 🚨 Conclusion
**Status: PASS ✅**

The transition to the asynchronous Redis queueing architecture has completely eliminated the database locking bottlenecks observed in earlier iterations. Out of 5,853 concurrent requests peaking at 100 virtual users, exactly 0 returned an HTTP 500 error. The 95th percentile response time remained under 61 milliseconds, ensuring a highly responsive end-user experience without dropping any data.
