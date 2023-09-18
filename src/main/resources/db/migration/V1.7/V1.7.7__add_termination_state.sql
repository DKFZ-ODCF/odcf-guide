ALTER TABLE submission ADD COLUMN termination_state VARCHAR(255);
UPDATE submission SET termination_state = 'NONE' WHERE termination_state IS NULL;
