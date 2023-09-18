ALTER TABLE submission ADD COLUMN date_created TIMESTAMP;
ALTER TABLE submission ADD COLUMN last_update TIMESTAMP;

UPDATE submission SET date_created = '1970-01-01 00:00:00' WHERE date_created IS NULL;
UPDATE submission SET last_update = '1970-01-01 00:00:00' WHERE last_update IS NULL;

ALTER TABLE submission ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE submission ALTER COLUMN last_update SET NOT NULL;

ALTER TABLE sample ADD COLUMN date_created TIMESTAMP;
ALTER TABLE sample ADD COLUMN last_update TIMESTAMP;
ALTER TABLE sample ADD COLUMN uuid VARCHAR(255) UNIQUE;

UPDATE sample SET date_created = '1970-01-01 00:00:00' WHERE date_created IS NULL;
UPDATE sample SET last_update = '1970-01-01 00:00:00' WHERE last_update IS NULL;
UPDATE sample SET uuid = uuid_generate_v4() WHERE uuid IS NULL;

ALTER TABLE sample ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE sample ALTER COLUMN last_update SET NOT NULL;
ALTER TABLE sample ALTER COLUMN uuid SET NOT NULL;

ALTER TABLE technical_sample ADD COLUMN date_created TIMESTAMP;
ALTER TABLE technical_sample ADD COLUMN last_update TIMESTAMP;
ALTER TABLE technical_sample ADD COLUMN uuid VARCHAR(255) UNIQUE;

UPDATE technical_sample SET date_created = '1970-01-01 00:00:00' WHERE date_created IS NULL;
UPDATE technical_sample SET last_update = '1970-01-01 00:00:00' WHERE last_update IS NULL;
UPDATE technical_sample SET uuid = uuid_generate_v4() WHERE uuid IS NULL;

ALTER TABLE technical_sample ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE technical_sample ALTER COLUMN last_update SET NOT NULL;
ALTER TABLE technical_sample ALTER COLUMN uuid SET NOT NULL;

ALTER TABLE file ADD COLUMN date_created TIMESTAMP;
ALTER TABLE file ADD COLUMN last_update TIMESTAMP;
ALTER TABLE file ADD COLUMN uuid VARCHAR(255) UNIQUE;

UPDATE file SET date_created = '1970-01-01 00:00:00' WHERE date_created IS NULL;
UPDATE file SET last_update = '1970-01-01 00:00:00' WHERE last_update IS NULL;
UPDATE file SET uuid = uuid_generate_v4() WHERE uuid IS NULL;

ALTER TABLE file ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE file ALTER COLUMN last_update SET NOT NULL;
ALTER TABLE file ALTER COLUMN uuid SET NOT NULL;
