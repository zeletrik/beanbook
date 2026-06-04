-- Seed data for Bean Book development/testing.
-- Run manually: sqlite3 beanbook.db < src/main/resources/db/seed/seed-data.sql
-- This file is NOT referenced by Liquibase migrations.

INSERT INTO bean_purchases (id, name, roaster, origin, price_per_unit, weight_grams,
    purchase_date, roast_date, roast_level, process, notes, grind_settings,
    image_data, rating, opened_date, finished_date)
VALUES (
    lower(hex(randomblob(4))) || '-' || lower(hex(randomblob(2))) || '-4' ||
        substr(lower(hex(randomblob(2))),2) || '-' || substr('89ab',abs(random()) % 4 + 1, 1) ||
        substr(lower(hex(randomblob(2))),2) || '-' || lower(hex(randomblob(6))),
    'Yirgacheffe Natural', 'Square Mile', 'Ethiopia',
    '18.50', 250, '2025-03-10', '2025-03-05', 'LIGHT', 'NATURAL',
    'Blueberry and jasmine notes', '4 clicks Comandante',
    NULL, 5, '2025-03-12', '2025-04-01'
);

INSERT INTO bean_purchases (id, name, roaster, origin, price_per_unit, weight_grams,
    purchase_date, roast_date, roast_level, process, notes, grind_settings,
    image_data, rating, opened_date, finished_date)
VALUES (
    lower(hex(randomblob(4))) || '-' || lower(hex(randomblob(2))) || '-4' ||
        substr(lower(hex(randomblob(2))),2) || '-' || substr('89ab',abs(random()) % 4 + 1, 1) ||
        substr(lower(hex(randomblob(2))),2) || '-' || lower(hex(randomblob(6))),
    'Huila Washed', 'Onyx Coffee Lab', 'Colombia',
    '22.00', 340, '2025-04-01', '2025-03-28', 'MEDIUM', 'WASHED',
    NULL, '20 on Niche Zero',
    NULL, 4, '2025-04-05', NULL
);

INSERT INTO bean_purchases (id, name, roaster, origin, price_per_unit, weight_grams,
    purchase_date, roast_date, roast_level, process, notes, grind_settings,
    image_data, rating, opened_date, finished_date)
VALUES (
    lower(hex(randomblob(4))) || '-' || lower(hex(randomblob(2))) || '-4' ||
        substr(lower(hex(randomblob(2))),2) || '-' || substr('89ab',abs(random()) % 4 + 1, 1) ||
        substr(lower(hex(randomblob(2))),2) || '-' || lower(hex(randomblob(6))),
    'Sumatra Mandheling', 'Intelligentsia', 'Indonesia',
    '16.00', 250, '2025-04-15', '2025-04-10', 'DARK', 'WASHED',
    'Earthy, low acidity', NULL,
    NULL, 3, '2025-04-20', '2025-05-10'
);

INSERT INTO bean_purchases (id, name, roaster, origin, price_per_unit, weight_grams,
    purchase_date, roast_date, roast_level, process, notes, grind_settings,
    image_data, rating, opened_date, finished_date)
VALUES (
    lower(hex(randomblob(4))) || '-' || lower(hex(randomblob(2))) || '-4' ||
        substr(lower(hex(randomblob(2))),2) || '-' || substr('89ab',abs(random()) % 4 + 1, 1) ||
        substr(lower(hex(randomblob(2))),2) || '-' || lower(hex(randomblob(6))),
    'Gesha Honey', 'Onyx Coffee Lab', 'Panama',
    '42.00', 100, '2025-05-02', '2025-04-29', 'LIGHT', 'HONEY',
    NULL, NULL,
    NULL, NULL, NULL, NULL
);

INSERT INTO bean_purchases (id, name, roaster, origin, price_per_unit, weight_grams,
    purchase_date, roast_date, roast_level, process, notes, grind_settings,
    image_data, rating, opened_date, finished_date)
VALUES (
    lower(hex(randomblob(4))) || '-' || lower(hex(randomblob(2))) || '-4' ||
        substr(lower(hex(randomblob(2))),2) || '-' || substr('89ab',abs(random()) % 4 + 1, 1) ||
        substr(lower(hex(randomblob(2))),2) || '-' || lower(hex(randomblob(6))),
    'Sidama Washed', 'Square Mile', 'Ethiopia',
    '19.00', 250, '2025-05-20', '2025-05-16', 'MEDIUM', 'WASHED',
    NULL, '18 clicks Comandante',
    NULL, 4, '2025-05-22', NULL
);
