-- 1. 회원(Users) 테이블
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL
);

CREATE UNIQUE INDEX idx_user_email ON users (email);

-- 2. 상품(Products) 단일 통합 테이블 (SINGLE_TABLE 전략)
CREATE TABLE products (
    id UUID PRIMARY KEY,
    category VARCHAR(31) NOT NULL,
    name VARCHAR(255) NOT NULL,
    brand VARCHAR(100) NOT NULL,
    price INTEGER NOT NULL,
    wireless BOOLEAN NOT NULL,
    description TEXT,
    features JSONB DEFAULT '[]'::jsonb,

    -- [Keyboard 전용 칼럼]
    switch_type VARCHAR(100),
    layout VARCHAR(50),

    -- [Mouse 전용 칼럼]
    max_polling_rate INTEGER,
    max_dpi INTEGER,
    grip_style VARCHAR(100),

    -- [HeadSet 전용 칼럼]
    noise_canceling BOOLEAN,
    has_mic BOOLEAN,
    sound_channel VARCHAR(100),

    -- [Mouse & HeadSet 전용 칼럼]
    weight INTEGER
);

CREATE INDEX idx_product_category_price ON products (category, price);
CREATE INDEX idx_product_features_gin ON products USING GIN (features);

-- 3. 상품 리뷰(Product Reviews) 테이블
CREATE TABLE product_reviews (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rating DOUBLE PRECISION NOT NULL,
    content TEXT NOT NULL
);

CREATE INDEX idx_review_product_rating ON product_reviews (product_id, rating DESC);