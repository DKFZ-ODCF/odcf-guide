ALTER TABLE sequencing_technology ADD COLUMN validation_level_id INT;
ALTER TABLE sequencing_technology ADD FOREIGN KEY (validation_level_id) REFERENCES validation_level (id);

ALTER TABLE validation_level ADD COLUMN default_object BOOLEAN;
UPDATE validation_level SET default_object = false where default_object is null;
UPDATE validation_level SET default_object = true where name = 'full';

ALTER TABLE sequencing_technology ADD COLUMN default_object BOOLEAN;
UPDATE sequencing_technology SET default_object = false where default_object is null;
INSERT INTO sequencing_technology (id, name, import_aliases_string, default_object) VALUES (nextval('hibernate_sequence'), 'unknown', '', true);

ALTER TABLE submission ADD COLUMN validation_level_id INT;
ALTER TABLE submission ADD FOREIGN KEY (validation_level_id) REFERENCES validation_level (id);
UPDATE submission SET validation_level_id = validation_level.id FROM validation_level WHERE validation_level.name = 'full';
ALTER TABLE submission ADD COLUMN sequencing_technology_id INT;
ALTER TABLE submission ADD FOREIGN KEY (sequencing_technology_id) REFERENCES sequencing_technology (id);
UPDATE submission SET sequencing_technology_id = sequencing_technology.id FROM sequencing_technology
     WHERE sequencing_technology.name = 'unknown' AND submission.submission_type = 'ApiSubmission';
