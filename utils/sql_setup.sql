CREATE TABLE users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    firstname VARCHAR(255),
    lastname VARCHAR(255),
    username VARCHAR(255),
    password VARCHAR(255),
    time_stamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sessions (
    user_id int,
    session_id VARCHAR(128),
    user_agent VARCHAR(512),
    expiry_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE permissions (
    user_id int,
    filepath VARCHAR(255) PRIMARY KEY,
    perms int
);