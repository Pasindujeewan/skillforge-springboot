CREATE TABLE registration_sagas (
    id UUID PRIMARY KEY,
    auth_user_id BIGINT,
    status VARCHAR(32) NOT NULL,
    failure_reason VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_registration_sagas_auth_user
        FOREIGN KEY (auth_user_id) REFERENCES auth_user (id)
);

CREATE INDEX idx_registration_sagas_status_updated_at
    ON registration_sagas (status, updated_at);
