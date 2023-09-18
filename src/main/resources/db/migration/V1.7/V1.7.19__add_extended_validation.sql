ALTER TABLE seq_type ADD COLUMN basic_seq_type VARCHAR;
UPDATE seq_type SET basic_seq_type = 'RNA' WHERE basic_seq_type_id = (SELECT id from basic_seq_type WHERE name = 'RNA');
UPDATE seq_type SET basic_seq_type = 'DNA' WHERE basic_seq_type_id = (SELECT id from basic_seq_type WHERE name = 'DNA');
ALTER TABLE seq_type DROP COLUMN IF EXISTS basic_seq_type_id;

DELETE FROM validation WHERE field = 'basicSeqType';

ALTER TABLE seq_type ADD COLUMN need_lib_prep_kit BOOLEAN;
UPDATE seq_type SET need_lib_prep_kit = true WHERE name in ('RNA', 'WHOLE_GENOME_BISULFITE', 'EXOME');
UPDATE seq_type SET need_lib_prep_kit = false WHERE need_lib_prep_kit IS NULL;

INSERT INTO validation VALUES ('center', 'dropdown', true, 'choose element from list', nextval('hibernate_sequence'));
INSERT INTO validation VALUES ('runId', '[a-zA-Z0-9.()_:\/+%&:^-]+', true, 'Includes forbidden characters. Allowed: a-z A-Z 0-9 . ( ) _ : / + % & : ^ -', nextval('hibernate_sequence'));
INSERT INTO validation VALUES ('runDate', '\d{4}-\d{2}-\d{2}', true, 'Please enter the date in the format YYYY-MM-DD', nextval('hibernate_sequence'));
INSERT INTO validation VALUES ('instrumentModelWithSequencingKit', 'dropdown', true, 'choose element from list', nextval('hibernate_sequence'));
INSERT INTO validation VALUES ('pipelineVersion', 'dropdown', true, 'choose element from list', nextval('hibernate_sequence'));

INSERT INTO validation VALUES ('libraryPreparationKit', 'pattern', true, 'A libPrepKit is required for this seqType.', nextval('hibernate_sequence'));

UPDATE validation SET regex = '[1-8]+', required = true, description = 'Number between 1-8' WHERE field = 'lane';

INSERT INTO validation_level_fields
VALUES ((SELECT id FROM validation_level WHERE name = 'full'),
        unnest((SELECT array_agg(id) FROM validation)))
ON CONFLICT DO NOTHING;
