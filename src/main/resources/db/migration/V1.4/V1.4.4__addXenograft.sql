ALTER TABLE sample ADD COLUMN xenograft BOOLEAN;
UPDATE sample SET xenograft = FALSE WHERE xenograft IS NULL;
ALTER TABLE sample ALTER COLUMN xenograft SET NOT NULL;
