ALTER TABLE submission ADD COLUMN custom_name VARCHAR(255);
ALTER TABLE submission ADD COLUMN comment VARCHAR;

UPDATE submission SET custom_name = '' WHERE custom_name IS NULL;
UPDATE submission SET comment = '' WHERE comment IS NULL;

