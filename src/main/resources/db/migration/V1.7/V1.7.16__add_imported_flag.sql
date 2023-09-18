ALTER TABLE submission ADD imported_external BOOLEAN;

UPDATE submission SET imported_external = false WHERE imported_external IS NULL;
