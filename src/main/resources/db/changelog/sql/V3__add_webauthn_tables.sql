--liquibase formatted sql

-- Passkey (WebAuthn) storage for the optional authentication feature. Column names and shapes match
-- Spring Security's JdbcPublicKeyCredentialUserEntityRepository / JdbcUserCredentialRepository so the
-- built-in JDBC repositories persist registered passkeys here. Created regardless of whether auth is
-- enabled (additive, empty until a passkey is registered) — the tables are inert when auth is off.
--changeset beanbook:v3-add-webauthn-tables

CREATE TABLE user_entities (
    id           TEXT NOT NULL PRIMARY KEY,
    name         TEXT NOT NULL,
    display_name TEXT NOT NULL
);
CREATE UNIQUE INDEX uk_user_entities_name ON user_entities (name);

CREATE TABLE user_credentials (
    credential_id                TEXT NOT NULL PRIMARY KEY,
    user_entity_user_id          TEXT NOT NULL,
    public_key                   BLOB NOT NULL,
    signature_count              INTEGER,
    uv_initialized               INTEGER,
    backup_eligible              INTEGER,
    authenticator_transports     TEXT,
    public_key_credential_type   TEXT,
    backup_state                 INTEGER,
    attestation_object           BLOB,
    attestation_client_data_json BLOB,
    created                      TIMESTAMP,
    last_used                    TIMESTAMP,
    label                        TEXT NOT NULL,
    FOREIGN KEY (user_entity_user_id) REFERENCES user_entities (id) ON DELETE CASCADE
);
CREATE INDEX idx_user_credentials_user_entity ON user_credentials (user_entity_user_id);
