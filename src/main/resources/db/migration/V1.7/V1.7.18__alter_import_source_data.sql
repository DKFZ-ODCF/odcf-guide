ALTER TABLE import_source_data RENAME COLUMN identifier TO submission_identifier;

ALTER TABLE import_source_data ADD COLUMN date_created TIMESTAMP;
ALTER TABLE import_source_data ADD COLUMN last_update TIMESTAMP;

UPDATE import_source_data SET date_created = import_date;
UPDATE import_source_data SET last_update = import_date;

UPDATE import_source_data SET date_created = '1970-01-01 00:00:00' WHERE date_created IS NULL;
UPDATE import_source_data SET last_update = '1970-01-01 00:00:00' WHERE last_update IS NULL;

ALTER TABLE import_source_data ALTER COLUMN date_created SET NOT NULL;
ALTER TABLE import_source_data ALTER COLUMN last_update SET NOT NULL;


ALTER TABLE import_source_data DROP COLUMN IF EXISTS formatted_identifier;
ALTER TABLE import_source_data DROP COLUMN IF EXISTS import_date;
ALTER TABLE import_source_data DROP COLUMN IF EXISTS times_used_for_reset;
