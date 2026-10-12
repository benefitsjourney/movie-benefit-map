USE BenefitsJourney;
GO

-- DROP TABLE exchange_listing;
-- GO

-- DROP TABLE dbo.exchange_listing;
-- GO

-- 交換刊登表 exchange_listing

-- CREATE TABLE dbo.exchange_listing (
CREATE TABLE exchange_listing (
    exchange_listing_id INT IDENTITY(1,1) NOT NULL,
    title NVARCHAR(100) NOT NULL,
    description NVARCHAR(500) NULL,
    status NVARCHAR(20) NOT NULL,
    use_benefit_id INT NOT NULL,
    created_at DATETIME2(0) NOT NULL
        CONSTRAINT DF_exchange_listing_created_at
        DEFAULT SYSDATETIME(),
    updated_at DATETIME2(0) NULL,

    CONSTRAINT PK_exchange_listing
        PRIMARY KEY (exchange_listing_id),

-- 一筆持有紀錄最多只能對應一筆交換刊登
    CONSTRAINT UQ_exchange_listing_use_benefit_id
        UNIQUE (use_benefit_id),

    CONSTRAINT FK_exchange_listing_use_benefit
        FOREIGN KEY (use_benefit_id)
        REFERENCES dbo.use_benefit(use_benefit_id),

    CONSTRAINT CK_exchange_listing_status
        CHECK (status IN ('ACTIVE', 'HIDDEN', 'CLOSED', 'DELETED'))
);
GO