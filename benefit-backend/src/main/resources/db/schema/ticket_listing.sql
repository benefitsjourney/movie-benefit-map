USE BenefitsJourney;
GO

-- DROP TABLE ticket_listing;
-- GO

-- DROP TABLE dbo.ticket_listing;
-- GO

-- 票券轉讓刊登表 ticket_listing

-- CREATE TABLE dbo.ticket_listing (
CREATE TABLE ticket_listing (
    ticket_listing_id INT IDENTITY(1,1) NOT NULL,
    title NVARCHAR(100) NOT NULL,
    description NVARCHAR(500) NULL,
    users_id INT NOT NULL,
    cinema_id INT NOT NULL,
    movie_id INT NOT NULL,
    ticket_showtime DATETIME2(0) NOT NULL,
    status NVARCHAR(20) NOT NULL,
    created_at DATETIME2(0) NOT NULL
        CONSTRAINT DF_ticket_listing_created_at
        DEFAULT SYSDATETIME(),
    updated_at DATETIME2(0) NULL,

    CONSTRAINT PK_ticket_listing
        PRIMARY KEY (ticket_listing_id),

    CONSTRAINT FK_ticket_listing_users
        FOREIGN KEY (users_id)
        REFERENCES dbo.users(users_id),

    CONSTRAINT FK_ticket_listing_cinema
        FOREIGN KEY (cinema_id)
        REFERENCES dbo.cinema(cinema_id),

    CONSTRAINT FK_ticket_listing_movie
        FOREIGN KEY (movie_id)
        REFERENCES dbo.movie(movie_id),

    CONSTRAINT CK_ticket_listing_status
        CHECK (status IN ('ACTIVE', 'HIDDEN', 'CLOSED', 'DELETED'))
);
GO
