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
    finished_date  TEXT
);
