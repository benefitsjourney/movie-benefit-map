USE BenefitsJourney;

-- DROP TABLE movie;

CREATE TABLE movie (
    movie_id INT IDENTITY(1,1) NOT NULL,
    source_id NVARCHAR(64) NULL,
    source_url NVARCHAR(512) NULL,
    movie_name NVARCHAR(256) NOT NULL,
    image_url NVARCHAR(512) NULL,
    is_visible BIT NOT NULL CONSTRAINT DF_movie_is_visible DEFAULT 1,
    release_date DATETIME2(0) NULL,
    created_at DATETIME2(0) NOT NULL CONSTRAINT DF_movie_created_at DEFAULT SYSDATETIME(),
    updated_at DATETIME2(0) NOT NULL CONSTRAINT DF_movie_updated_at DEFAULT SYSDATETIME(),

    CONSTRAINT PK_movie PRIMARY KEY (movie_id),
    CONSTRAINT UQ_movie_source_id UNIQUE (source_id)
);
