USE BenefitsJourney;

-- INT IDENTITY預設快取1000個號碼，重啟後未使用的號碼會跳過；關閉快取可避免重啟造成跳號
ALTER DATABASE SCOPED CONFIGURATION SET IDENTITY_CACHE = OFF;

-- DROP TABLE crawler_log;

CREATE TABLE crawler_log (
    crawler_log_id INT IDENTITY(1,1) NOT NULL,
    crawler_type NVARCHAR(32) NOT NULL,
    crawler_status NVARCHAR(16) NOT NULL,
    fetched_count INT NOT NULL,
    created_count INT NOT NULL,
    updated_count INT NOT NULL,
    skipped_count INT NOT NULL,
    pending_review_count INT NOT NULL,
    message NVARCHAR(1024) NULL,
    executed_at DATETIME2(0) NOT NULL,
    finished_at DATETIME2(0) NOT NULL,

    CONSTRAINT PK_crawler_log PRIMARY KEY (crawler_log_id)
);
