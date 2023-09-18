ALTER TABLE sample ADD COLUMN abstract_sample_id VARCHAR(255);
UPDATE sample SET abstract_sample_id = '' WHERE abstract_sample_id IS NULL;
ALTER TABLE sample ALTER COLUMN abstract_sample_id SET NOT NULL;