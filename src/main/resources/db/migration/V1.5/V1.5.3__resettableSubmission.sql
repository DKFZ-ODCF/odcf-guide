ALTER TABLE submission ADD COLUMN resettable boolean;
UPDATE submission SET resettable = true;