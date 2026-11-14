---
name: database-administrator
description: Expert Database Administrator (DBA) specializing in relational schema design, 3NF normalization, index optimization (B-Tree, GIN, GiST), SQL query performance tuning (EXPLAIN ANALYZE), ACID transaction isolation, connection pooling, and backup/restore strategies. Use when designing schemas, tuning queries, or optimizing database reliability.
---

# Database Administrator (DBA)

## Overview

Comprehensive runbook and operational guidelines for relational database engineering, performance tuning, indexing, concurrency management, and database reliability (PostgreSQL focus, compatible with Spring Boot / Hibernate / Supabase).

---

## 1. Relational Schema Design & Normalization

### Normal Forms
- **1NF (First Normal Form)**: Atomic values per cell, unique column names, each row uniquely identifiable via primary key.
- **2NF (Second Normal Form)**: 1NF satisfied + no partial dependencies (all non-key columns depend on the whole primary key).
- **3NF (Third Normal Form)**: 2NF satisfied + no transitive dependencies (non-key columns depend *only* on the candidate key: "the key, the whole key, and nothing but the key").
- **BCNF (Boyce-Codd Normal Form)**: Stricter version of 3NF for tables with multiple overlapping candidate keys.

### Pragmatic Denormalization
Denormalize selectively only when:
- Profiling proves repeated complex multi-table joins create bottlenecks under high read ratios (>95% reads).
- Aggregated counts or summaries are frequently requested; update via triggers or asynchronous workers (e.g., cached `follower_count`, `order_total_cents`).
- Retaining historic snapshot data (e.g., copying `unit_price_at_sale` into `order_items` rather than referencing changing `products.price`).

### Data Types Best Practices
- **Primary Keys**: Prefer `UUID v7` (time-ordered, index-friendly) or `BIGINT GENERATED ALWAYS AS IDENTITY` over random `UUID v4` to prevent B-Tree page fragmentation and random I/O.
- **Strings**: Use `TEXT` or `VARCHAR(n)` with domain check constraints. Avoid arbitrary limits unless required by business rules.
- **Timestamps**: Always use `TIMESTAMPTZ` (`TIMESTAMP WITH TIME ZONE`). Store UTC internally.
- **Financial & Monetary**: Never use floating point (`FLOAT`, `DOUBLE`). Always use `NUMERIC(precision, scale)` or integer representations (e.g. cents in `BIGINT`).
- **Semi-structured Data**: Use `JSONB` for unstructured attributes, dynamic configuration, or third-party webhooks. Always define GIN or expression indexes when querying inside JSONB.

---

## 2. Advanced Indexing Strategies

### Index Types & Use Cases
- **B-Tree (Default)**: Best for equality (`=`), range queries (`<`, `<=`, `>`, `>=`, `BETWEEN`), and sorting (`ORDER BY`).
- **GIN (Generalized Inverted Index)**: Best for array containment (`@>`), full-text search (`tsvector`), and JSONB containment (`column ? 'key'`, `column @> '{"status": "active"}'`).
- **GiST (Generalized Search Tree)**: Geometric data (PostGIS), range types (`tsrange`, `daterange`), and nearest-neighbor search.
- **BRIN (Block Range Index)**: Extremely compact indexes for massive append-only sequential tables (e.g., audit logs, time-series metrics ordered by timestamp).

### Multi-Column (Composite) Indexing & Column Order
- Follow the **Equality First, Range Later, Sort Last** rule:
  ```sql
  -- Efficient for: WHERE tenant_id = 'x' AND created_at >= '2026-01-01' ORDER BY created_at DESC
  CREATE INDEX idx_orders_tenant_created 
  ON orders (tenant_id, created_at DESC);
  ```

### Partial & Covering Indexes
- **Partial Indexes**: Index only active or frequently queried subsets, saving disk space and memory:
  ```sql
  -- Index only active subscriptions
  CREATE INDEX idx_active_subscriptions 
  ON subscriptions (user_id) 
  WHERE status = 'active';
  ```
- **Covering Indexes (`INCLUDE`)**: Eliminate table fetches by fulfilling queries purely from index scans (Index-Only Scan):
  ```sql
  CREATE INDEX idx_users_email_covering 
  ON users (email) 
  INCLUDE (id, full_name, password_hash);
  ```

---

## 3. SQL Query Optimization & Execution Analysis

### Interpreting `EXPLAIN (ANALYZE, BUFFERS)`
Always analyze slow queries with buffers:
```sql
EXPLAIN (ANALYZE, BUFFERS, VERBOSE, TIMING)
SELECT u.id, u.email, COUNT(o.id)
FROM users u
JOIN orders o ON o.user_id = u.id
WHERE u.status = 'active' AND o.created_at >= NOW() - INTERVAL '30 days'
GROUP BY u.id, u.email;
```

### Key Red Flags in Query Plans
- **Seq Scan on Large Tables**: Missing or unutilized index.
- **High `shared read` vs `shared hit`**: Buffer pool misses; data read from slow disk rather than RAM (`shared_buffers`).
- **External Sort / Disk Spill**: Sort operation exceeded `work_mem`; memory spill to disk.
- **High Row Estimate Divergence**: Huge gap between `rows=10` (planner estimate) and `actual rows=100000`. Indicates stale statistics; run `ANALYZE table_name;`.
- **N+1 Query Syndrome**: Application issuing 1 root query followed by N child queries. In Spring Boot / Hibernate, fix using `JOIN FETCH`, `@EntityGraph`, or projection DTOs.

---

## 4. ACID Transactions & Concurrency Control

### ANSI Isolation Levels in PostgreSQL
1. **Read Committed (Default)**: Statements see data committed before statement began. Vulnerable to non-repeatable reads and phantom reads.
2. **Repeatable Read**: Statements see data committed before *transaction* began (snapshot isolation). Prevents non-repeatable reads; raises serialization failure on concurrent conflict.
3. **Serializable**: True serializability using SSI (Serializable Snapshot Isolation). Requires application retry loops on conflict.

### Explicit Locking & Concurrency
- **Pessimistic Row Locking**:
  ```sql
  -- Acquire lock, skipping rows locked by other concurrent workers
  SELECT * FROM job_queue 
  WHERE status = 'pending' 
  ORDER BY priority DESC 
  LIMIT 1 
  FOR UPDATE SKIP LOCKED;
  ```
- **Advisory Locks**: Application-level distributed locking without table overhead:
  ```sql
  SELECT pg_try_advisory_xact_lock(hashtext('nightly_billing_job'));
  ```

---

## 5. Reliability, Connection Pooling & Maintenance

### Connection Pooling (HikariCP & PgBouncer)
- **Golden Rule**: PostgreSQL processes each connection as a dedicated OS process. Too many connections degrade performance due to context switching and RAM saturation.
- **Pool Size Sizing Formula**:
  $$\text{Connections} = ((\text{Core Count} \times 2) + \text{Effective Spindle Count})$$
- In Spring Boot:
  ```properties
  spring.datasource.hikari.maximum-pool-size=20
  spring.datasource.hikari.minimum-idle=5
  spring.datasource.hikari.idle-timeout=300000
  spring.datasource.hikari.connection-timeout=20000
  ```
- Use PgBouncer in **transaction mode** for web workloads and serverless instances.

### Routine Maintenance & Autovacuum Tuning
- Monitor table bloat and vacuum health:
  ```sql
  SELECT relname, n_dead_tup, last_vacuum, last_autovacuum, last_analyze 
  FROM pg_stat_user_tables 
  ORDER BY n_dead_tup DESC;
  ```
- Adjust autovacuum for high-update tables to avoid freeze locks and bloat:
  ```sql
  ALTER TABLE high_throughput_events SET (
    autovacuum_vacuum_scale_factor = 0.05,
    autovacuum_vacuum_threshold = 500
  );
  ```

### Backup & Disaster Recovery
- **Logical Backups**: `pg_dump -Fc -Z 6 -d dbname > backup.dump` for schema and portable backups.
- **Physical Backups & PITR (Point-in-Time Recovery)**: Continuous WAL archiving via tools like pgBackRest or cloud WAL archiving. Enables restoring the database to any exact second before data corruption.
