USE BenefitsJourney;

CREATE TABLE login_log (
    login_log_id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NULL,
    login_time DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    login_status VARCHAR(20) NOT NULL,
    ip_address VARCHAR(45) NULL,
    user_agent VARCHAR(500) NULL,

    CONSTRAINT FK_login_log_Users
        FOREIGN KEY (user_id)
        REFERENCES Users(users_id),

    CONSTRAINT CK_login_log_LoginStatus
        CHECK (login_status IN ('SUCCESS', 'FAILED'))
);