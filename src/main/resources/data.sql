-- Seed timestamps use 2026-11-04T14:51:22Z as epoch milliseconds.
-- Top-level categories.
INSERT INTO category (id, parent_id, name, version, created_by, created_at_utc_ms, updated_by, updated_at_utc_ms)
VALUES (1, NULL, 'Food', 0, 'system', 1793803882746, NULL, NULL);

INSERT INTO category (id, parent_id, name, version, created_by, created_at_utc_ms, updated_by, updated_at_utc_ms)
VALUES (2, NULL, 'Clothes', 0, 'system', 1793803882746, NULL, NULL);

-- Sub-categories. parent_id points to the owning top-level category.
INSERT INTO category (id, parent_id, name, version, created_by, created_at_utc_ms, updated_by, updated_at_utc_ms)
VALUES (3, 1, 'Cake', 0, 'system', 1793803882746, NULL, NULL);

INSERT INTO category (id, parent_id, name, version, created_by, created_at_utc_ms, updated_by, updated_at_utc_ms)
VALUES (4, 2, 'Shoe', 0, 'system', 1793803882746, NULL, NULL);

INSERT INTO category (id, parent_id, name, version, created_by, created_at_utc_ms, updated_by, updated_at_utc_ms)
VALUES (5, 2, 'T-Shirts', 0, 'system', 1793803882746, NULL, NULL);
