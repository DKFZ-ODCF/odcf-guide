ALTER TABLE parser_field  ADD COLUMN column_mapping VARCHAR;

UPDATE parser_field SET column_mapping = field_name;
UPDATE parser_field SET field_name = 'patient_id' where field_name = 'pid';
UPDATE parser_field SET field_name = 'sample_type' where field_name = 'sampleType';

UPDATE parser_field SET column_mapping = '' WHERE column_mapping IS NULL;
ALTER TABLE parser_field ALTER COLUMN column_mapping SET NOT NULL;
