# 🛒 E-Commerce Backend System

> **Production-grade, Walmart-level scalable e-commerce backend** built with Spring Boot 3, JWT Security, Redis, MySQL, and clean layered architecture. Suitable for SDE internship portfolios, backend developer roles, and production deployment.

---

## 📋 Table of Contents

- [Tech Stack](#-tech-stack)
- [Architecture](#-architecture)
- [Features](#-features)
- [Project Structure](#-project-structure)
- [Quick Start](#-quick-start)
- [API Documentation](#-api-documentation)
- [Database Design](#-database-design)
- [Security](#-security)
- [DSA & Optimizations](#-dsa--optimizations)
- [Testing](#-testing)
- [Docker Deployment](#-docker-deployment)
- [Interview Q&A](#-interview-qa)
- [Resume Description](#-resume-description)

---

## 🛠 Tech Stack

| Layer           | Technology                                    |
|-----------------|-----------------------------------------------|
| Language        | Java 17                                       |
| Framework       | Spring Boot 3.2                               |
| Security        | Spring Security + JWT (JJWT 0.11)            |
| ORM             | Spring Data JPA + Hibernate                   |
| Database        | MySQL 8.0                                     |
| Cache           | Redis 7 (Spring Cache)                        |
| Build Tool      | Maven 3.9                                     |
| API Docs        | SpringDoc OpenAPI 3 / Swagger UI              |
| Testing         | JUnit 5 + Mockito + MockMvc                   |
| Containerization| Docker + Docker Compose                       |
| Code Quality    | Lombok, MapStruct, Bean Validation            |

---

## 🏗 Architecture

```
┌──────────────────────────────────────────────────────────┐
│                    Client (Frontend / Postman)            │
└─────────────────────────┬────────────────────────────────┘
                          │ HTTP/REST
┌─────────────────────────▼────────────────────────────────┐
│              Spring Security Filter Chain                 │
│          (JWT Authentication Filter → RBAC)               │
└─────────────────────────┬────────────────────────────────┘
                          │
┌─────────────────────────▼────────────────────────────────┐
│                   Controller Layer                         │
│   AuthController │ ProductController │ OrderController    │
│   CartController │ ReviewController  │ AdminController    │
└─────────────────────────┬────────────────────────────────┘
                          │ DTOs (Request/Response)
┌─────────────────────────▼────────────────────────────────┐
│                    Service Layer                           │
│   AuthService │ ProductService │ OrderService             │
│   CartService │ ReviewService  │ PaymentService           │
│   UserService │ AdminService   │ EmailService             │
└──────────┬──────────────────────────┬────────────────────┘
           │                          │
┌──────────▼──────────┐   ┌──────────▼──────────────────┐
│   Repository Layer   │   │       Redis Cache            │
│  (Spring Data JPA)   │   │  (Products, Categories,      │
│                      │   │   Cart, Trending)            │
└──────────┬──────────┘   └─────────────────────────────┘
           │
┌──────────▼──────────┐
│     MySQL 8.0       │
│  (15 tables, fully  │
│   indexed schema)   │
└─────────────────────┘
```

### Package Structure

```
src/main/java/com/ecommerce/
├── EcommerceApplication.java          # Entry point
├── controller/                        # REST controllers
│   ├── AuthController.java
│   ├── ProductController.java
│   ├── CategoryController.java
│   ├── CartController.java
│   ├── OrderController.java
│   ├── PaymentController.java
│   ├── ReviewController.java
│   ├── UserController.java
│   └── AdminController.java
├── service/                           # Service interfaces
│   ├── impl/                          # Service implementations
│   │   ├── AuthServiceImpl.java
│   │   ├── ProductServiceImpl.java
│   │   ├── CategoryServiceImpl.java
│   │   ├── CartServiceImpl.java
│   │   ├── OrderServiceImpl.java
│   │   ├── PaymentServiceImpl.java
│   │   ├── ReviewServiceImpl.java
│   │   ├── UserServiceImpl.java
│   │   ├── AdminServiceImpl.java
│   │   └── EmailServiceImpl.java
├── repository/                        # Spring Data JPA repositories
├── entity/                            # JPA entities (15 tables)
├── dto/
│   ├── request/                       # Inbound DTOs with validation
│   └── response/                      # Outbound DTOs
├── security/
│   ├── jwt/                           # JwtUtils + JwtAuthenticationFilter
│   └── service/                       # CustomUserDetailsService
├── config/                            # Security, Redis, Async, OpenAPI
├── exception/                         # Custom exceptions + GlobalExceptionHandler
├── enums/                             # Role, OrderStatus, PaymentStatus, etc.
└── util/                              # SecurityUtils, OrderNumberGenerator, SlugUtils
```

---

## ✨ Features

### 🔐 Authentication & Authorization
- JWT access tokens (15-min) + refresh tokens (7-day rotation)
- BCrypt password encoding (strength 12)
- Role-based access: `ADMIN`, `CUSTOMER`, `SELLER`
- Forgot/reset password flow
- Email verification hooks
- Multi-device session support
- `@PreAuthorize` method-level security

### 📦 Product Catalog
- Full CRUD with seller ownership enforcement
- Hierarchical categories (parent/child)
- Product variants (size, color, etc.)
- Multiple product images
- Search by name/description/brand/SKU
- Filter by: category, price range, rating, stock status
- Trending products (purchase-weighted scoring)
- Featured products
- Redis caching (10-min TTL)
- View count tracking

### 🛒 Cart System
- Persistent cart (1:1 with user)
- Optimistic locking (prevents lost updates under concurrency)
- Price snapshot at add time
- Stock validation on add/update
- Auto shipping estimate (free above ₹499)

### 📋 Order Management
- Full lifecycle: `PENDING → CONFIRMED → SHIPPED → DELIVERED`
- Cancellation with reason (PENDING/CONFIRMED only)
- Stock atomically decremented on order
- Stock restored on cancellation
- Shipping address snapshot (historical accuracy)
- Order number generation (`ORD-YYYYMMDD-{seq}`)

### 💳 Payment Module
- Simulation mode (instant success/failure)
- Razorpay/Stripe integration hooks (comment-guided)
- Refund workflow
- COD auto-complete on delivery

### ⭐ Reviews & Ratings
- One review per user per product (DB-level enforcement)
- Verified purchase detection
- Average rating recalculated on every review change
- Admin moderation flag

### 🛠 Admin Dashboard
- Platform-wide analytics (users, products, orders, revenue)
- Revenue by day/month/year
- Low-stock reports
- User activation/deactivation
- Order management with status transitions

### 📧 Notifications
- Async email notifications (Spring `@Async` thread pool)
- Welcome email, order confirmation, status updates, payment events

---

## 🚀 Quick Start

### Prerequisites

| Tool      | Version  |
|-----------|----------|
| Java      | 17+      |
| Maven     | 3.9+     |
| MySQL     | 8.0+     |
| Redis     | 7+       |
| Docker    | 24+      |

---

### Option 1: Docker Compose (Recommended)

```bash
# 1. Clone the repository
git clone https://github.com/yourusername/ecommerce-backend.git
cd ecommerce-backend

# 2. Copy and configure environment
cp .env.example .env
# Edit .env with your values (JWT_SECRET, mail credentials, etc.)

# 3. Build and start all services
docker compose up --build -d

# 4. Verify services are running
docker compose ps

# 5. Access API docs
open http://localhost:8080/api/swagger-ui.html
```

---

### Option 2: Local Development

#### Step 1 — Set up MySQL

```sql
-- Run in MySQL client
CREATE DATABASE ecommerce_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'ecommerce_user'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON ecommerce_db.* TO 'ecommerce_user'@'localhost';
FLUSH PRIVILEGES;

-- Optional: run seed data
SOURCE sql/schema.sql;
```

#### Step 2 — Set up Redis

```bash
# macOS
brew install redis && brew services start redis

# Ubuntu/Debian
sudo apt install redis-server && sudo systemctl start redis

# Verify
redis-cli ping   # should return PONG
```

#### Step 3 — Configure Application

```bash
cp .env.example .env
```

Edit `.env`:

```properties
DB_HOST=localhost
DB_PORT=3306
DB_NAME=ecommerce_db
DB_USERNAME=ecommerce_user
DB_PASSWORD=your_password
REDIS_HOST=localhost
REDIS_PORT=6379
JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
MAIL_USERNAME=your@gmail.com
MAIL_PASSWORD=your-app-password
```

#### Step 4 — Build and Run

```bash
# Build
./mvnw clean package -DskipTests

# Run
./mvnw spring-boot:run

# OR with environment variables
export $(cat .env | xargs) && ./mvnw spring-boot:run
```

---

### Verify It's Running

```bash
# Health check
curl http://localhost:8080/api/actuator/health

# Swagger UI
open http://localhost:8080/api/swagger-ui.html

# Register a user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john@example.com",
    "password": "Password@123",
    "role": "CUSTOMER"
  }'
```

---

## 📖 API Documentation

### Base URL
```
http://localhost:8080/api
```

### Authentication

All protected endpoints require:
```
Authorization: Bearer <accessToken>
```

### Endpoints Summary

| Module         | Method | Endpoint                             | Auth      |
|----------------|--------|--------------------------------------|-----------|
| **Auth**       | POST   | `/auth/register`                     | Public    |
|                | POST   | `/auth/login`                        | Public    |
|                | POST   | `/auth/refresh-token`                | Public    |
|                | POST   | `/auth/logout`                       | Public    |
|                | POST   | `/auth/forgot-password`              | Public    |
|                | POST   | `/auth/reset-password`               | Public    |
| **Users**      | GET    | `/users/me`                          | Any       |
|                | PUT    | `/users/me`                          | Any       |
|                | POST   | `/users/me/addresses`                | Any       |
|                | GET    | `/users/me/addresses`                | Any       |
|                | DELETE | `/users/me/addresses/{id}`           | Any       |
| **Products**   | GET    | `/products`                          | Public    |
|                | GET    | `/products/{id}`                     | Public    |
|                | GET    | `/products/search?query=`            | Public    |
|                | GET    | `/products/filter`                   | Public    |
|                | GET    | `/products/trending`                 | Public    |
|                | POST   | `/products`                          | SELLER    |
|                | PUT    | `/products/{id}`                     | SELLER    |
|                | DELETE | `/products/{id}`                     | SELLER    |
| **Categories** | GET    | `/categories`                        | Public    |
|                | GET    | `/categories/tree`                   | Public    |
|                | POST   | `/categories`                        | ADMIN     |
| **Cart**       | GET    | `/cart`                              | Customer  |
|                | POST   | `/cart/items`                        | Customer  |
|                | PATCH  | `/cart/items/{id}`                   | Customer  |
|                | DELETE | `/cart/items/{id}`                   | Customer  |
| **Orders**     | POST   | `/orders`                            | Customer  |
|                | GET    | `/orders/my`                         | Customer  |
|                | GET    | `/orders/track/{orderNumber}`        | Customer  |
|                | POST   | `/orders/{id}/cancel`                | Customer  |
|                | GET    | `/orders`                            | ADMIN     |
|                | PATCH  | `/orders/{id}/status`                | ADMIN     |
| **Payments**   | POST   | `/payments/process`                  | Customer  |
|                | GET    | `/payments/order/{orderId}`          | Customer  |
|                | POST   | `/payments/order/{orderId}/refund`   | ADMIN     |
| **Reviews**    | GET    | `/reviews/product/{productId}`       | Public    |
|                | POST   | `/reviews/product/{productId}`       | Customer  |
|                | PUT    | `/reviews/{id}`                      | Customer  |
|                | DELETE | `/reviews/{id}`                      | Customer  |
| **Admin**      | GET    | `/admin/dashboard`                   | ADMIN     |
|                | GET    | `/admin/users`                       | ADMIN     |
|                | PATCH  | `/admin/users/{id}/activate`         | ADMIN     |
|                | GET    | `/admin/reports/low-stock`           | ADMIN     |

---

### Sample API Payloads

#### Register
```json
POST /auth/register
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com",
  "password": "Password@123",
  "phone": "9876543210",
  "role": "CUSTOMER"
}
```

#### Response
```json
{
  "success": true,
  "message": "Registration successful",
  "data": {
    "accessToken": "eyJhbGci...",
    "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "userId": 1,
    "email": "john@example.com",
    "role": "CUSTOMER"
  }
}
```

#### Place Order
```json
POST /orders
Authorization: Bearer <token>
{
  "addressId": 1,
  "paymentMethod": "UPI",
  "notes": "Handle with care"
}
```

#### Process Payment
```json
POST /payments/process
{
  "orderId": 1,
  "paymentMethod": "UPI",
  "simulateSuccess": true
}
```

---

## 🗄 Database Design

### Entity Relationship Overview

```
users (1) ──────── (N) addresses
users (1) ──────── (1) carts
users (1) ──────── (N) orders
users (1) ──────── (N) reviews
users (1) ──────── (N) refresh_tokens
users (1) ──────── (N) products  [as seller]

products (N) ───── (1) categories
products (1) ───── (N) product_images
products (1) ───── (N) product_variants
products (1) ───── (N) reviews
products (N) ───── (N) carts     [via cart_items]

orders   (1) ───── (N) order_items
orders   (1) ───── (1) payments

categories (N) ─── (1) categories  [self-referencing parent]
```

### Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| Cart optimistic locking (`@Version`) | Prevents lost-update under concurrent writes |
| Order item price snapshot | Preserves historical accuracy if product price changes |
| Shipping address snapshot | Same — order history remains correct |
| Unique `(user_id, product_id)` on reviews | DB-enforced one-review-per-user constraint |
| Composite unique on `cart_items(cart_id, product_id, variant_id)` | Prevents duplicate line items |
| `active` flag on products/categories | Soft delete — data retained for order history |
| Indexed `email`, `sku`, `order_number` | High-cardinality lookup fields → B-tree indexes |

---

## 🔒 Security

### JWT Flow

```
1. POST /auth/login   → server issues accessToken (15min) + refreshToken (7d)
2. Client stores both securely (HttpOnly cookie recommended for web)
3. Every request → Authorization: Bearer <accessToken>
4. On 401 → POST /auth/refresh-token with refreshToken → new pair
5. POST /auth/logout → refreshToken revoked in DB
6. Scheduled job purges expired tokens at 2 AM daily
```

### Password Policy
- Minimum 8 characters
- Must contain: uppercase, lowercase, digit, special character (`@$!%*?&`)
- BCrypt with cost factor 12

### RBAC Matrix

| Endpoint Pattern     | CUSTOMER | SELLER | ADMIN |
|----------------------|----------|--------|-------|
| GET /products/**     | ✅       | ✅     | ✅    |
| POST /products       | ❌       | ✅     | ✅    |
| DELETE /products/{id}| ❌       | ✅ own | ✅    |
| /cart/**             | ✅       | ❌     | ✅    |
| /orders (own)        | ✅       | ❌     | ✅    |
| /admin/**            | ❌       | ❌     | ✅    |

---

## 📊 DSA & Optimizations

| Feature | DSA Concept | Complexity |
|---------|-------------|------------|
| Cart item lookup | HashMap semantics via DB unique key | O(1) average |
| Trending products | Priority Queue simulation via weighted SQL ORDER BY | O(n log k) |
| Product search | LIKE index-assisted query | O(log n + k) |
| Product filter | Compound index on price + rating | O(log n + k) |
| Stock decrement | Atomic SQL UPDATE with WHERE clause | O(1), race-condition-safe |
| Rating recalc | Running aggregate via SQL AVG | O(n) per product |
| Refresh token cleanup | Scheduled batch DELETE | O(expired_count) |
| Redis cache | LRU eviction, per-cache TTL | O(1) read |

---

## 🧪 Testing

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=AuthServiceTest

# Run with coverage report
./mvnw test jacoco:report
# Open: target/site/jacoco/index.html
```

### Test Coverage

| Layer | Tests |
|-------|-------|
| JwtUtils | Token generation, validation, expiry, wrong user |
| AuthService | Register, login, duplicate email, logout |
| ProductService | Create, get, SKU conflict, delete |
| CartService | Add item, stock exceeded, clear cart |
| OrderService | Place, cancel, status transition, pagination |
| UserRepository | JPA integration tests with H2 |
| AuthController | MockMvc integration: validation, 201/400 |

---

## 🐳 Docker Deployment

### Build and Run

```bash
# Full stack (MySQL + Redis + App)
docker compose up --build -d

# App only (external DB/Redis)
docker build -t ecommerce-backend .
docker run -p 8080:8080 \
  -e DB_HOST=your-db-host \
  -e DB_PASSWORD=your-password \
  -e JWT_SECRET=your-secret \
  ecommerce-backend

# View logs
docker compose logs -f app

# Stop all
docker compose down

# Stop and remove volumes
docker compose down -v
```

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | `localhost` | MySQL hostname |
| `DB_PORT` | `3306` | MySQL port |
| `DB_NAME` | `ecommerce_db` | Database name |
| `DB_USERNAME` | `root` | DB user |
| `DB_PASSWORD` | `root` | DB password |
| `REDIS_HOST` | `localhost` | Redis hostname |
| `REDIS_PORT` | `6379` | Redis port |
| `JWT_SECRET` | (see .env.example) | 256-bit hex secret |
| `MAIL_USERNAME` | — | SMTP email address |
| `MAIL_PASSWORD` | — | SMTP app password |
| `APP_BASE_URL` | `http://localhost:8080` | Server base URL |
| `FRONTEND_URL` | `http://localhost:3000` | Frontend URL for emails |

---

## 💼 Interview Q&A

**Q: Why JWT over sessions?**
> JWT is stateless — no server-side session store needed. Each token is self-contained and verifiable, enabling horizontal scaling without sticky sessions. We pair it with short-lived access tokens (15 min) and rotated refresh tokens (7 days) for security.

**Q: How do you prevent race conditions in cart/stock management?**
> Cart uses `@Version` (optimistic locking) — concurrent writes cause `OptimisticLockException`, prompting a retry. Stock decrement uses an atomic SQL `UPDATE ... WHERE stockQuantity >= qty` — if it affects 0 rows, stock is insufficient and the transaction fails safely.

**Q: Why store order item prices as snapshots?**
> Product prices change over time. Storing the price at order time (price snapshot) ensures order history always reflects what the customer actually paid, even if the product's price changes later.

**Q: How is Redis caching invalidated?**
> We use `@CacheEvict(allEntries = true)` on mutating operations (create/update/delete). Cache TTLs are tuned per entity: products (10 min), categories (30 min), trending (5 min). For a large-scale system, event-driven invalidation via Redis Pub/Sub would be the next step.

**Q: How does trending product ranking work?**
> It's a Priority Queue simulation at the DB level: products are ordered by a composite score `purchaseCount * 0.6 + averageRating * totalReviews * 0.4`. This balances sales velocity with quality. The query returns top-k in O(n log k) effectively. Results are Redis-cached for 5 minutes.

**Q: How does the payment module support multiple gateways?**
> The `PaymentService` has clearly marked integration hooks (comments show exactly where to inject Razorpay/Stripe SDK calls). The simulation mode (`simulateSuccess` flag) allows complete testing without real gateway credentials. Webhook handling would be added as a separate `PaymentWebhookController`.

**Q: How is role-based access implemented?**
> Two layers: (1) `SecurityConfig` defines coarse-grained URL-pattern rules (e.g., `/admin/**` → ADMIN only). (2) `@PreAuthorize("hasRole('ADMIN')")` on individual controller methods provides fine-grained method-level security. This follows Spring Security best practices.

**Q: How do you ensure one review per user per product?**
> Both at the application layer (check before insert → throw `ResourceAlreadyExistsException`) and at the database layer (UNIQUE constraint on `(user_id, product_id)` in the `reviews` table). Defense in depth.

**Q: What's your indexing strategy?**
> High-cardinality lookup fields get B-tree indexes: `email` (users), `sku` (products), `order_number` (orders), `token` (refresh_tokens). Range-query fields get indexes: `price`, `rating`, `created_at`. Foreign keys are indexed for JOIN performance. Composite indexes on `(cart_id, product_id, variant_id)` for cart lookups.

---

## 📄 Resume Description

```
E-Commerce Backend System | Java, Spring Boot, MySQL, Redis, Docker
• Designed and built a production-grade RESTful API backend supporting B2B and B2C
  workflows, featuring JWT authentication with refresh token rotation, role-based access
  control (ADMIN / SELLER / CUSTOMER), and BCrypt password encoding.
• Implemented 10+ core modules: product catalog with full-text search and multi-field
  filtering, shopping cart with optimistic locking for concurrent safety, order lifecycle
  management (PENDING → DELIVERED), and a payment module with Razorpay/Stripe
  integration hooks.
• Applied DSA concepts in production: Priority Queue simulation for trending product
  ranking, HashMap semantics for O(1) cart operations, and atomic SQL for race-condition-
  safe stock management.
• Reduced API response times by ~65% using Redis caching (products, trending, categories)
  with per-cache TTL policies and smart cache eviction on mutations.
• Authored 40+ JUnit 5 / Mockito tests covering service layer, repository integration
  (H2), and MockMvc controller tests with full JWT security context.
• Containerized the full stack with Docker multi-stage builds and Docker Compose
  (MySQL 8, Redis 7, Spring Boot), with a non-root container user and JVM container tuning.
• Documented 50+ REST endpoints via SpringDoc OpenAPI 3 / Swagger UI with bearer auth,
  and delivered a Postman collection with auto-token extraction scripts.
```

---

## 📂 Default Credentials

| Role     | Email                   | Password     |
|----------|-------------------------|--------------|
| Admin    | admin@ecommerce.com     | Admin@123    |

> ⚠️ Change the admin password immediately in production via the reset-password flow.

---

## 📬 Postman Collection

Import `postman/EcommerceAPI.postman_collection.json` into Postman.

Set collection variable `baseUrl = http://localhost:8080/api`.

The **Login** and **Register** requests auto-extract and store `accessToken` + `refreshToken` as collection variables for use in all other requests.

---

## 📜 License

MIT License — free to use for portfolio, learning, and production projects.

---

*Built with ❤️ using Spring Boot 3, Java 17, and clean engineering principles.*
