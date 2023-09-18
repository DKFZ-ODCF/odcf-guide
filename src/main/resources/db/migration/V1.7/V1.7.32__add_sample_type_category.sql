ALTER TABLE sample ADD COLUMN sample_type_category VARCHAR(255);
UPDATE sample SET sample_type_category = 'UNDEFINED' WHERE sample_type_category IS NULL;

ALTER TABLE seq_type ADD COLUMN need_sample_type_category BOOLEAN;
UPDATE seq_type SET need_sample_type_category = false WHERE need_sample_type_category IS NULL;
