CREATE TABLE IF NOT EXISTS wishlist_items (
    id      TEXT NOT NULL PRIMARY KEY,
    name    TEXT NOT NULL,
    roaster TEXT NOT NULL DEFAULT '',
    origin  TEXT NOT NULL DEFAULT '',
    notes   TEXT
);
