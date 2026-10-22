USE BenefitsJourney;

-- INT IDENTITY預設快取1000個號碼，重啟後未使用的號碼會跳過；關閉快取可避免重啟造成跳號
ALTER DATABASE SCOPED CONFIGURATION SET IDENTITY_CACHE = OFF;

-- DROP TABLE cinema_movie_review;

CREATE TABLE cinema_movie_review (
    cinema_movie_review_id INT IDENTITY(1,1) NOT NULL,
    crawler_type NVARCHAR(32) NOT NULL,
    entity_id INT NOT NULL,
    field_name NVARCHAR(64) NOT NULL,
    crawler_value NVARCHAR(512) NULL,
    previous_value NVARCHAR(512) NULL,
    review_status NVARCHAR(32) NULL,
    reviewed_at DATETIME2(0) NULL,
    created_at DATETIME2(0) NOT NULL CONSTRAINT DF_cinema_movie_review_created_at DEFAULT SYSDATETIME(),
    updated_at DATETIME2(0) NOT NULL CONSTRAINT DF_cinema_movie_review_updated_at DEFAULT SYSDATETIME(),

    CONSTRAINT PK_cinema_movie_review PRIMARY KEY (cinema_movie_review_id)
);
