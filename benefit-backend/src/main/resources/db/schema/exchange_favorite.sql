USE BenefitsJourney;
GO

-- DROP TABLE exchange_favorite;
-- GO

-- DROP TABLE dbo.exchange_favorite;
-- GO


-- 交換收藏表 exchange_favorite

-- CREATE TABLE dbo.exchange_favorite (
CREATE TABLE exchange_favorite (
    exchange_favorite_id INT IDENTITY(1,1) NOT NULL,
    users_id INT NOT NULL,
    exchange_listing_id INT NOT NULL,
    created_at DATETIME2(0) NOT NULL
        CONSTRAINT DF_exchange_favorite_created_at
        DEFAULT SYSDATETIME(),

    CONSTRAINT PK_exchange_favorite
        PRIMARY KEY (exchange_favorite_id),

    -- 同一會員不能重複收藏同一筆交換刊登
    CONSTRAINT UQ_exchange_favorite_users_listing
        UNIQUE (users_id, exchange_listing_id),

    CONSTRAINT FK_exchange_favorite_users
        FOREIGN KEY (users_id)
        REFERENCES dbo.users(users_id),

    CONSTRAINT FK_exchange_favorite_exchange_listing
        FOREIGN KEY (exchange_listing_id)
        REFERENCES dbo.exchange_listing(exchange_listing_id)
);
GO