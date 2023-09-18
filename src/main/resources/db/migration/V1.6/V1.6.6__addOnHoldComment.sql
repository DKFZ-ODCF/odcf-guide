ALTER TABLE submission ADD COLUMN on_hold_comment VARCHAR(255);

UPDATE submission SET on_hold_comment = '' WHERE on_hold_comment IS NULL;

ALTER TABLE submission ALTER COLUMN on_hold_comment SET NOT NULL;
