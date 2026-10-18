USE BenefitsJourney;

-- DROP TABLE crawler_review;

CREATE TABLE crawler_review (
    crawler_review_id INT IDENTITY(1,1) NOT NULL,
    crawler_type NVARCHAR(32) NOT NULL,
    entity_id INT NOT NULL,
    field_name NVARCHAR(64) NOT NULL,
    crawler_value NVARCHAR(512) NULL,
    previous_value NVARCHAR(512) NULL,
    review_status NVARCHAR(32) NULL,
    reviewed_at DATETIME2(0) NULL,
    created_at DATETIME2(0) NOT NULL CONSTRAINT DF_crawler_review_created_at DEFAULT SYSDATETIME(),
    updated_at DATETIME2(0) NOT NULL CONSTRAINT DF_crawler_review_updated_at DEFAULT SYSDATETIME(),

    CONSTRAINT PK_crawler_review PRIMARY KEY (crawler_review_id)
);
