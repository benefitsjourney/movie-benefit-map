
CREATE TABLE users (
    -- Primary key
    users_id INT IDENTITY(1,1) PRIMARY KEY,

    -- Account: required for normal registration, nullable for Google login
    account NVARCHAR(50) NULL,

    -- Password: nullable for Google login
    password NVARCHAR(255) NULL,

    -- Member name
    name NVARCHAR(50) NOT NULL,

    -- Email
    email NVARCHAR(255) NOT NULL,

    -- Google account ID
    google_id NVARCHAR(255) NULL,

    -- Birthday: optional
    birth_date DATE NULL,

    -- Role
    role NVARCHAR(20) NOT NULL,

    -- Member status
    status NVARCHAR(20) NOT NULL,

    -- Timestamps
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL,

    -- Role constraint
    CONSTRAINT CK_users_role
        CHECK (role IN ('USER', 'ADMIN')),

    -- Status constraint
    CONSTRAINT CK_users_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'BANNED', 'DELETED'))
);
GO

-- Non-null accounts must be unique.
-- Multiple Google members can have a NULL account.
CREATE UNIQUE INDEX UX_users_account
ON users(account)
WHERE account IS NOT NULL;
GO

-- Non-null Google IDs must be unique.
CREATE UNIQUE INDEX UX_users_google_id
ON users(google_id)
WHERE google_id IS NOT NULL;
GO

-- Email must be unique.
CREATE UNIQUE INDEX UX_users_email
ON users(email);
GO
