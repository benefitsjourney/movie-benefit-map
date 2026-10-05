USE BenefitsJourney;

-- DROP TABLE cinema;

CREATE TABLE cinema (
    cinema_id INT IDENTITY(1,1) NOT NULL,
    source_id NVARCHAR(64) NOT NULL,
    cinema_name NVARCHAR(64) NOT NULL,
    cinema_address NVARCHAR(256) NULL,
    latitude DECIMAL(10,6) NULL,
    longitude DECIMAL(10,6) NULL,
    official_website_url NVARCHAR(512) NULL,
    county NVARCHAR(32) NOT NULL,
    is_visible BIT NOT NULL CONSTRAINT DF_cinema_is_visible DEFAULT 1,
    is_active BIT NOT NULL CONSTRAINT DF_cinema_is_active DEFAULT 1,
    created_at DATETIME2 NOT NULL CONSTRAINT DF_cinema_created_at DEFAULT SYSDATETIME(),
    updated_at DATETIME2 NOT NULL CONSTRAINT DF_cinema_updated_at DEFAULT SYSDATETIME(),

    CONSTRAINT PK_cinema PRIMARY KEY (cinema_id),
    CONSTRAINT UQ_cinema_source_id UNIQUE (source_id)
);