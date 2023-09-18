ALTER TABLE person ADD COLUMN account_disabled boolean;

UPDATE person SET account_disabled = false WHERE account_disabled IS NULL;
