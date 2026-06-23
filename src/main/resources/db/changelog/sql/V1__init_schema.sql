--liquibase formatted sql

-- Consolidated v1 baseline: the full schema in a single changeset.
-- Supersedes the original incremental V1–V6 (create + roast_profile + used_as +
-- user_preferences + wishlist_items + tags). Safe to consolidate pre-release because the
-- database had only been exercised locally; a clean DB recreates from this baseline.
-- splitStatements:true so all three CREATE TABLEs in this one changeset are executed
-- individually (SQLite's JDBC runs only the first statement of a multi-statement string).

--changeset beanbook:v1-init-schema splitStatements:true endDelimiter:;
CREATE TABLE IF NOT EXISTS bean_purchases (
    id             TEXT    NOT NULL PRIMARY KEY,
    name           TEXT    NOT NULL,
    roaster        TEXT    NOT NULL,
    origin         TEXT    NOT NULL,
    price_per_unit TEXT    NOT NULL,
    weight_grams   INTEGER NOT NULL,
    purchase_date  TEXT    NOT NULL,
    roast_date     TEXT    NOT NULL,
    roast_level    TEXT    NOT NULL,
    process        TEXT    NOT NULL,
    notes          TEXT,
    grind_settings TEXT,
    image_data     BLOB,
    rating         INTEGER,
    opened_date    TEXT,
    finished_date  TEXT,
    roast_profile  TEXT    NOT NULL DEFAULT 'OMNI',
    used_as        TEXT,
    tags           TEXT,
    url            TEXT
);
CREATE TABLE IF NOT EXISTS user_preferences (
    key   TEXT NOT NULL PRIMARY KEY,
    value TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS wishlist_items (
    id      TEXT NOT NULL PRIMARY KEY,
    name    TEXT NOT NULL,
    roaster TEXT NOT NULL DEFAULT '',
    origin  TEXT NOT NULL DEFAULT '',
    notes   TEXT,
    url     TEXT
);
