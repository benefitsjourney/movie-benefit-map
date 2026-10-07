USE BenefitsJourney;

-- DROP TABLE benefit;

CREATE TABLE benefit (
    benefit_id INT IDENTITY(1,1) NOT NULL,
    public_id NVARCHAR(200) NULL,
    benefit_name NVARCHAR(100) NOT NULL,
    is_member_only BIT NULL,
    benefit_type NVARCHAR(32) NULL,
    status NVARCHAR(16) NULL,
    start_date DATETIME2(0) NULL,
    end_date DATETIME2(0) NULL,
    official_image_url NVARCHAR(200) NULL,
    source_url NVARCHAR(300) NULL,
    review_note NVARCHAR(100) NULL,
    created_at DATETIME2(0) NOT NULL CONSTRAINT DF_benefit_created_at DEFAULT SYSDATETIME(),
    updated_at DATETIME2(0) NOT NULL CONSTRAINT DF_benefit_updated_at DEFAULT SYSDATETIME(),

    CONSTRAINT PK_benefit PRIMARY KEY (benefit_id),
    CONSTRAINT CK_benefit_type CHECK (benefit_type IN ('REGULAR', 'SPECIAL_BY_DATE', 'SPECIAL_UNTIL_OUT')),
    CONSTRAINT CK_benefit_status CHECK (status IN ('APPROVED', 'EXPIRED'))
    
-- 爬蟲去重索引
CREATE UNIQUE INDEX UQ_benefit_source_url_name
ON benefit (source_url, benefit_name)
WHERE source_url IS NOT NULL;

CREATE INDEX IX_benefit_status ON benefit (status);
);