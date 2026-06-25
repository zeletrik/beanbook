--liquibase formatted sql

-- Second-level origin (region / sub-origin), e.g. "Huila" for a Colombia bean. Additive and nullable
-- so existing rows are unaffected.
--changeset beanbook:v2-add-origin-region
ALTER TABLE bean_purchases ADD COLUMN region TEXT;
