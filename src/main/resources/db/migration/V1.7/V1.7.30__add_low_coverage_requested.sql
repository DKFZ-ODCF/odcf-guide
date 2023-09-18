ALTER TABLE seq_type ADD COLUMN low_coverage_requestable BOOLEAN;
UPDATE seq_type SET low_coverage_requestable = true WHERE name in ('WHOLE_GENOME');
UPDATE seq_type SET low_coverage_requestable = false WHERE low_coverage_requestable IS NULL;

ALTER TABLE sample ADD COLUMN low_coverage_requested BOOLEAN;
UPDATE sample SET low_coverage_requested = false WHERE low_coverage_requested IS NULL;

INSERT INTO meta_data_column VALUES ('low coverage requested', '', null, 185, '', '', '');
