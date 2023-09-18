ALTER TABLE feedback ADD COLUMN ticket VARCHAR;
UPDATE feedback SET ticket = '' WHERE ticket IS NULL;
ALTER TABLE feedback ALTER COLUMN ticket SET NOT NULL;

UPDATE feedback SET message = '' WHERE message = 'The user did not put in a message.';
