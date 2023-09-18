ALTER TABLE person ADD COLUMN organizational_unit VARCHAR(255);

UPDATE person SET organizational_unit = '' WHERE organizational_unit IS NULL;
