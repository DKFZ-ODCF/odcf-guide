ALTER TABLE sample ADD COLUMN read1length BIGINT;
ALTER TABLE sample ADD COLUMN read2length BIGINT;

UPDATE sample SET read1length = -1 where read1length IS NULL;
UPDATE sample SET read2length = -1 where read2length IS NULL;

ALTER TABLE sample ALTER COLUMN read1length SET NOT NULL;
ALTER TABLE sample ALTER COLUMN read2length SET NOT NULL;
