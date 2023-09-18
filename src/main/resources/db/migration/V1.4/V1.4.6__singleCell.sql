ALTER TABLE sample ADD COLUMN single_cell_plate VARCHAR(255);
ALTER TABLE sample ADD COLUMN single_cell_well_position VARCHAR(255);

UPDATE sample SET single_cell_plate = '' WHERE single_cell_plate IS NULL;
UPDATE sample SET single_cell_well_position = '' WHERE single_cell_well_position IS NULL;

ALTER TABLE sample ALTER COLUMN single_cell_plate SET NOT NULL;
ALTER TABLE sample ALTER COLUMN single_cell_well_position SET NOT NULL;