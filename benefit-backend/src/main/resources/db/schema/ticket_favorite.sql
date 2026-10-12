USE BenefitsJourney;
GO

-- DROP TABLE ticket_favorite;
-- GO

-- DROP TABLE dbo.ticket_favorite;
-- GO

-- 票券轉讓收藏表 ticket_favorite

-- CREATE TABLE dbo.ticket_favorite (
CREATE TABLE ticket_favorite (
    ticket_favorite_id INT IDENTITY(1,1) NOT NULL,
    users_id INT NOT NULL,
    ticket_listing_id INT NOT NULL,
    created_at DATETIME2(0) NOT NULL
        CONSTRAINT DF_ticket_favorite_created_at
        DEFAULT SYSDATETIME(),

    CONSTRAINT PK_ticket_favorite
        PRIMARY KEY (ticket_favorite_id),

    -- 同一會員不能重複收藏同一筆票券
    CONSTRAINT UQ_ticket_favorite_users_listing
        UNIQUE (users_id, ticket_listing_id),

    CONSTRAINT FK_ticket_favorite_users
        FOREIGN KEY (users_id)
        REFERENCES dbo.users(users_id),

    CONSTRAINT FK_ticket_favorite_ticket_listing
        FOREIGN KEY (ticket_listing_id)
        REFERENCES dbo.ticket_listing(ticket_listing_id)
);
GO