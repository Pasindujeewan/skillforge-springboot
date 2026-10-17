CREATE TABLE user_profiles (
                               id BIGINT PRIMARY KEY,
                               username VARCHAR(30) NOT NULL,
                               display_name VARCHAR(80) NOT NULL,
                               bio VARCHAR(500),
                               avatar_url VARCHAR(500),
                               created_at TIMESTAMPTZ NOT NULL,
                               updated_at TIMESTAMPTZ NOT NULL,

                               CONSTRAINT uk_user_profiles_username UNIQUE (username),

                               CONSTRAINT chk_user_profiles_username
                                   CHECK (username ~ '^[a-zA-Z0-9_]{3,30}$')
    );
