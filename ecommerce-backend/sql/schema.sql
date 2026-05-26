-- ============================================================
-- E-Commerce Backend - MySQL Database Schema
-- Production-grade normalized schema with indexing strategy
-- ============================================================

CREATE DATABASE IF NOT EXISTS ecommerce_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE ecommerce_db;

-- ============================================================
-- USERS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    first_name       VARCHAR(100) NOT NULL,
    last_name        VARCHAR(100) NOT NULL,
    email            VARCHAR(150) NOT NULL,
    password         VARCHAR(255) NOT NULL,
    phone            VARCHAR(15),
    role             ENUM('ADMIN','CUSTOMER','SELLER') NOT NULL DEFAULT 'CUSTOMER',
    active           BOOLEAN      NOT NULL DEFAULT FALSE,
    email_verified   BOOLEAN      NOT NULL DEFAULT FALSE,
    profile_image_url VARCHAR(512),
    created_at       DATETIME(6)  NOT NULL,
    updated_at       DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),
    INDEX idx_user_role   (role),
    INDEX idx_user_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- CATEGORIES TABLE (self-referencing hierarchy)
-- ============================================================
CREATE TABLE IF NOT EXISTS categories (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    slug        VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    image_url   VARCHAR(512),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    parent_id   BIGINT,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_categories_name (name),
    UNIQUE KEY uk_categories_slug (slug),
    INDEX idx_category_parent (parent_id),
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id)
        REFERENCES categories(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- PRODUCTS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS products (
    id                BIGINT         NOT NULL AUTO_INCREMENT,
    name              VARCHAR(200)   NOT NULL,
    sku               VARCHAR(100)   NOT NULL,
    description       TEXT,
    price             DECIMAL(12,2)  NOT NULL,
    discount_price    DECIMAL(12,2),
    stock_quantity    INT            NOT NULL DEFAULT 0,
    average_rating    DECIMAL(3,2)   DEFAULT 0.00,
    total_reviews     INT            NOT NULL DEFAULT 0,
    view_count        BIGINT         NOT NULL DEFAULT 0,
    purchase_count    BIGINT         NOT NULL DEFAULT 0,
    active            BOOLEAN        NOT NULL DEFAULT TRUE,
    featured          BOOLEAN        NOT NULL DEFAULT FALSE,
    brand             VARCHAR(100),
    weight            DECIMAL(8,3),
    weight_unit       VARCHAR(50),
    category_id       BIGINT         NOT NULL,
    seller_id         BIGINT         NOT NULL,
    created_at        DATETIME(6)    NOT NULL,
    updated_at        DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_products_sku (sku),
    INDEX idx_product_category     (category_id),
    INDEX idx_product_seller       (seller_id),
    INDEX idx_product_price        (price),
    INDEX idx_product_active       (active),
    INDEX idx_product_name         (name),
    INDEX idx_product_rating       (average_rating),
    INDEX idx_product_purchase     (purchase_count),
    CONSTRAINT fk_product_category FOREIGN KEY (category_id)
        REFERENCES categories(id),
    CONSTRAINT fk_product_seller FOREIGN KEY (seller_id)
        REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- PRODUCT IMAGES TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS product_images (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    image_url     VARCHAR(512) NOT NULL,
    display_order INT          NOT NULL DEFAULT 0,
    `primary`     BOOLEAN      NOT NULL DEFAULT FALSE,
    alt_text      VARCHAR(200),
    product_id    BIGINT       NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6),
    PRIMARY KEY (id),
    INDEX idx_product_image_product (product_id),
    CONSTRAINT fk_product_image FOREIGN KEY (product_id)
        REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- PRODUCT VARIANTS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS product_variants (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    variant_sku      VARCHAR(120)  NOT NULL,
    attribute_name   VARCHAR(50)   NOT NULL,
    attribute_value  VARCHAR(100)  NOT NULL,
    price_adjustment DECIMAL(10,2),
    stock_quantity   INT           NOT NULL DEFAULT 0,
    product_id       BIGINT        NOT NULL,
    created_at       DATETIME(6)   NOT NULL,
    updated_at       DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_variant_sku (variant_sku),
    INDEX idx_variant_product (product_id),
    CONSTRAINT fk_variant_product FOREIGN KEY (product_id)
        REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- ADDRESSES TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS addresses (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    full_name        VARCHAR(100) NOT NULL,
    phone            VARCHAR(15)  NOT NULL,
    address_line1    VARCHAR(300) NOT NULL,
    address_line2    VARCHAR(300),
    city             VARCHAR(100) NOT NULL,
    state            VARCHAR(100) NOT NULL,
    country          VARCHAR(100) NOT NULL,
    pincode          VARCHAR(10)  NOT NULL,
    default_address  BOOLEAN      NOT NULL DEFAULT FALSE,
    address_type     VARCHAR(30)  DEFAULT 'HOME',
    user_id          BIGINT       NOT NULL,
    created_at       DATETIME(6)  NOT NULL,
    updated_at       DATETIME(6),
    PRIMARY KEY (id),
    INDEX idx_address_user    (user_id),
    INDEX idx_address_pincode (pincode),
    CONSTRAINT fk_address_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- CARTS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS carts (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    version    BIGINT      NOT NULL DEFAULT 0,
    user_id    BIGINT      NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_cart_user (user_id),
    CONSTRAINT fk_cart_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- CART ITEMS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS cart_items (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    quantity       INT           NOT NULL,
    price_snapshot DECIMAL(12,2) NOT NULL,
    cart_id        BIGINT        NOT NULL,
    product_id     BIGINT        NOT NULL,
    variant_id     BIGINT,
    created_at     DATETIME(6)   NOT NULL,
    updated_at     DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_cart_product_variant (cart_id, product_id, variant_id),
    INDEX idx_cart_item_cart    (cart_id),
    INDEX idx_cart_item_product (product_id),
    CONSTRAINT fk_cart_item_cart FOREIGN KEY (cart_id)
        REFERENCES carts(id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_item_product FOREIGN KEY (product_id)
        REFERENCES products(id),
    CONSTRAINT fk_cart_item_variant FOREIGN KEY (variant_id)
        REFERENCES product_variants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- ORDERS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS orders (
    id                   BIGINT        NOT NULL AUTO_INCREMENT,
    order_number         VARCHAR(50)   NOT NULL,
    status               ENUM('PENDING','CONFIRMED','SHIPPED','DELIVERED','CANCELLED')
                         NOT NULL DEFAULT 'PENDING',
    subtotal             DECIMAL(12,2) NOT NULL,
    shipping_charge      DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    discount             DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    total_amount         DECIMAL(12,2) NOT NULL,
    coupon_code          VARCHAR(100),
    cancellation_reason  TEXT,
    tracking_number      VARCHAR(500),
    -- Shipping address snapshot
    shipping_full_name   VARCHAR(100)  NOT NULL,
    shipping_phone       VARCHAR(15)   NOT NULL,
    shipping_address_line1 VARCHAR(500) NOT NULL,
    shipping_address_line2 VARCHAR(500),
    shipping_city        VARCHAR(100)  NOT NULL,
    shipping_state       VARCHAR(100)  NOT NULL,
    shipping_country     VARCHAR(100)  NOT NULL,
    shipping_pincode     VARCHAR(10)   NOT NULL,
    user_id              BIGINT        NOT NULL,
    created_at           DATETIME(6)   NOT NULL,
    updated_at           DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_number (order_number),
    INDEX idx_order_user    (user_id),
    INDEX idx_order_status  (status),
    INDEX idx_order_created (created_at),
    CONSTRAINT fk_order_user FOREIGN KEY (user_id)
        REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- ORDER ITEMS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS order_items (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    quantity          INT           NOT NULL,
    unit_price        DECIMAL(12,2) NOT NULL,
    subtotal          DECIMAL(12,2) NOT NULL,
    product_name      VARCHAR(200)  NOT NULL,
    product_sku       VARCHAR(100)  NOT NULL,
    product_image_url VARCHAR(512),
    variant_info      VARCHAR(150),
    order_id          BIGINT        NOT NULL,
    product_id        BIGINT        NOT NULL,
    variant_id        BIGINT,
    created_at        DATETIME(6)   NOT NULL,
    updated_at        DATETIME(6),
    PRIMARY KEY (id),
    INDEX idx_order_item_order   (order_id),
    INDEX idx_order_item_product (product_id),
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id)
        REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_item_product FOREIGN KEY (product_id)
        REFERENCES products(id),
    CONSTRAINT fk_order_item_variant FOREIGN KEY (variant_id)
        REFERENCES product_variants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- PAYMENTS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS payments (
    id                   BIGINT        NOT NULL AUTO_INCREMENT,
    amount               DECIMAL(12,2) NOT NULL,
    status               ENUM('PENDING','COMPLETED','FAILED','REFUNDED','CANCELLED')
                         NOT NULL DEFAULT 'PENDING',
    payment_method       ENUM('CREDIT_CARD','DEBIT_CARD','UPI','NET_BANKING',
                              'WALLET','COD','RAZORPAY','STRIPE') NOT NULL,
    transaction_id       VARCHAR(200),
    gateway_payment_id   VARCHAR(200),
    gateway_order_id     VARCHAR(200),
    gateway_signature    VARCHAR(500),
    failure_reason       TEXT,
    paid_at              DATETIME,
    refunded_at          DATETIME,
    refund_amount        DECIMAL(12,2),
    refund_transaction_id VARCHAR(200),
    order_id             BIGINT        NOT NULL,
    created_at           DATETIME(6)   NOT NULL,
    updated_at           DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_order       (order_id),
    INDEX idx_payment_transaction (transaction_id),
    INDEX idx_payment_status      (status),
    CONSTRAINT fk_payment_order FOREIGN KEY (order_id)
        REFERENCES orders(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- REVIEWS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS reviews (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    rating     INT          NOT NULL CHECK (rating BETWEEN 1 AND 5),
    title      VARCHAR(200) NOT NULL,
    comment    TEXT,
    verified   BOOLEAN      NOT NULL DEFAULT FALSE,
    approved   BOOLEAN      NOT NULL DEFAULT TRUE,
    user_id    BIGINT       NOT NULL,
    product_id BIGINT       NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_review_user_product (user_id, product_id),
    INDEX idx_review_product (product_id),
    INDEX idx_review_user    (user_id),
    INDEX idx_review_rating  (rating),
    CONSTRAINT fk_review_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_review_product FOREIGN KEY (product_id)
        REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- REFRESH TOKENS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    token       VARCHAR(512) NOT NULL,
    expiry_date DATETIME(6)  NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    device_info VARCHAR(200),
    user_id     BIGINT       NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_refresh_token (token),
    INDEX idx_refresh_token_user  (user_id),
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- SEED DATA
-- ============================================================

-- Admin user (password: Admin@123)
INSERT IGNORE INTO users (first_name, last_name, email, password, role, active, email_verified, created_at, updated_at)
VALUES ('Super', 'Admin', 'admin@ecommerce.com',
        '$2a$12$LZkuvNrS0B.V9fkQ7xJpCO1dS/qBv3EywD.G2lc2K.r7cAHU5cjpO',
        'ADMIN', TRUE, TRUE, NOW(), NOW());

-- Root categories
INSERT IGNORE INTO categories (name, slug, description, active, created_at, updated_at) VALUES
('Electronics',    'electronics',    'Electronic devices and accessories',   TRUE, NOW(), NOW()),
('Fashion',        'fashion',        'Clothing, shoes, and accessories',     TRUE, NOW(), NOW()),
('Home & Kitchen', 'home-kitchen',   'Home appliances and kitchenware',      TRUE, NOW(), NOW()),
('Books',          'books',          'Books, e-books, and study materials',  TRUE, NOW(), NOW()),
('Sports',         'sports',         'Sports equipment and fitness gear',    TRUE, NOW(), NOW());

-- Sub-categories
INSERT IGNORE INTO categories (name, slug, description, active, parent_id, created_at, updated_at)
SELECT 'Smartphones', 'smartphones', 'Mobile phones and smartphones', TRUE, id, NOW(), NOW()
FROM categories WHERE slug = 'electronics';

INSERT IGNORE INTO categories (name, slug, description, active, parent_id, created_at, updated_at)
SELECT 'Laptops', 'laptops', 'Laptops and notebooks', TRUE, id, NOW(), NOW()
FROM categories WHERE slug = 'electronics';

INSERT IGNORE INTO categories (name, slug, description, active, parent_id, created_at, updated_at)
SELECT "Men's Clothing", 'mens-clothing', "Clothing for men", TRUE, id, NOW(), NOW()
FROM categories WHERE slug = 'fashion';
