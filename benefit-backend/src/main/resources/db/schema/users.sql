USE BenefitsJourney;

-- DROP TABLE users;
-- GO

CREATE TABLE users (
    -- 會員編號
    users_id INT IDENTITY(1,1) PRIMARY KEY,
    -- 一般登入帳號
    account VARCHAR(50) UNIQUE NULL,
    -- BCrypt 加密後的密碼
    password VARCHAR(255) NULL,
    -- 姓名 / 暱稱，需要支援中文
    name NVARCHAR(50) NOT NULL,
    -- Email
    email VARCHAR(255) NOT NULL UNIQUE,
    -- Google OAuth 唯一識別 ID
    -- 不直接設定 UNIQUE，改用下面的 Filtered Unique Index
    google_id VARCHAR(255) NULL,
    -- 生日
    birth_date DATE NOT NULL,
    -- USER / ADMIN
    role VARCHAR(20) NOT NULL,
    -- ACTIVE / INACTIVE / BANNED
    status VARCHAR(20) NOT NULL,
    -- 建立時間
    created_at DATETIME2 NOT NULL,
    -- 修改時間
    updated_at DATETIME2 NOT NULL,
    -- 角色限制
    CONSTRAINT CK_users_role
        CHECK (role IN ('USER', 'ADMIN')),
    -- 帳號狀態限制
    CONSTRAINT CK_users_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'BANNED'))
);
GO

-- Google ID 有值時必須唯一
-- 但允許多筆 NULL
CREATE UNIQUE INDEX UX_users_google_id
ON users(google_id)
WHERE google_id IS NOT NULL;
GO