ALTER TABLE sample ADD COLUMN tissue VARCHAR(255);
ALTER TABLE sample ADD COLUMN requested_sequencing_info VARCHAR(255);
ALTER TABLE sample ADD COLUMN base_material VARCHAR(255);
ALTER TABLE sample ADD COLUMN requested_lanes DOUBLE PRECISION;

ALTER TABLE submission ADD COLUMN type VARCHAR(255);
ALTER TABLE submission RENAME COLUMN external_data_availibilty_date TO external_data_availability_date;
ALTER TABLE submission RENAME COLUMN otrs_ticket_number TO ticket_number;

UPDATE sample SET requested_lanes = 0;
UPDATE sample SET base_material = '';
UPDATE sample SET requested_sequencing_info = '';
UPDATE sample SET tissue = '';

ALTER TABLE sample ALTER COLUMN tissue SET NOT NULL;
ALTER TABLE sample ALTER COLUMN requested_sequencing_info SET NOT NULL;
ALTER TABLE sample ALTER COLUMN base_material SET NOT NULL;
ALTER TABLE sample ALTER COLUMN requested_lanes SET NOT NULL;
