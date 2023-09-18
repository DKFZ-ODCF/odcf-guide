ALTER TABLE meta_data_column ADD COLUMN reflection_property_name varchar;
ALTER TABLE meta_data_column ADD COLUMN reflection_class_name varchar;

UPDATE meta_data_column SET reflection_class_name = 'Sample',
                            reflection_property_name = 'antibody'
WHERE column_name = 'antibody';

UPDATE meta_data_column SET reflection_class_name = 'Sample',
                            reflection_property_name = 'antibodyTarget'
WHERE column_name = 'antibody target';

UPDATE meta_data_column SET reflection_class_name = 'File',
                            reflection_property_name = 'baseCount'
WHERE column_name = 'base count';

UPDATE meta_data_column SET reflection_class_name = 'TechnicalSample',
                            reflection_property_name = 'center',
                            export_name = 'CENTER_NAME'
WHERE column_name = 'center';

UPDATE meta_data_column SET reflection_class_name = 'Sample',
                            reflection_property_name = 'comment'
WHERE column_name = 'comment';

UPDATE meta_data_column SET reflection_class_name = 'File',
                            reflection_property_name = 'cycleCount'
WHERE column_name = 'cycle count';

UPDATE meta_data_column SET reflection_class_name = 'File',
                            reflection_property_name = 'fileName',
                            export_name = 'FASTQ_FILE'
WHERE column_name = 'fastq file name';

UPDATE meta_data_column SET reflection_class_name = 'Submission',
                            reflection_property_name = 'identifier'
WHERE column_name = 'ilse no';

UPDATE meta_data_column SET reflection_class_name = 'TechnicalSample',
                            reflection_property_name = 'barcode'
WHERE column_name = 'index';

UPDATE meta_data_column SET reflection_class_name = 'TechnicalSample',
                            reflection_property_name = 'instrumentModel'
WHERE column_name = 'instrument model';

UPDATE meta_data_column SET reflection_class_name = 'TechnicalSample',
                            reflection_property_name = 'instrumentPlatform'
WHERE column_name = 'instrument platform';

UPDATE meta_data_column SET reflection_class_name = 'TechnicalSample',
                            reflection_property_name = 'lane'
WHERE column_name = 'lane no';

UPDATE meta_data_column SET reflection_class_name = 'Sample',
                            reflection_property_name = 'libraryPreparationKit'
WHERE column_name = 'lib prep kit';

UPDATE meta_data_column SET reflection_class_name = 'File',
                            reflection_property_name = 'md5',
                            export_name = 'MD5'
WHERE column_name = 'md5 sum';

UPDATE meta_data_column SET reflection_class_name = 'Sample',
                            reflection_property_name = 'phenotype'
WHERE column_name = 'phenotype';

UPDATE meta_data_column SET reflection_class_name = 'Sample',
                            reflection_property_name = 'pid'
WHERE column_name = 'patient id';

UPDATE meta_data_column SET reflection_class_name = 'Sample',
                            reflection_property_name = 'project'
WHERE column_name = 'project';

UPDATE meta_data_column SET reflection_class_name = 'TechnicalSample',
                            reflection_property_name = 'readCount'
WHERE column_name = 'read count';

UPDATE meta_data_column SET reflection_class_name = 'File',
                            reflection_property_name = 'readNumber',
                            export_name = 'READ'
WHERE column_name = 'read number';

UPDATE meta_data_column SET reflection_class_name = 'TechnicalSample',
                            reflection_property_name = 'runDate'
WHERE column_name = 'run date';

UPDATE meta_data_column SET reflection_class_name = 'TechnicalSample',
                            reflection_property_name = 'runId'
WHERE column_name = 'run id';

UPDATE meta_data_column SET reflection_class_name = 'Sample',
                            reflection_property_name = 'importIdentifier'
WHERE column_name = 'sample name';

UPDATE meta_data_column SET reflection_class_name = 'Sample',
                            reflection_property_name = 'sampleType'
WHERE column_name = 'sample type';

UPDATE meta_data_column SET reflection_class_name = 'TechnicalSample',
                            reflection_property_name = 'sequencingKit'
WHERE column_name = 'sequencing kit';

UPDATE meta_data_column SET reflection_class_name = 'Sample',
                            reflection_property_name = 'libraryLayout'
WHERE column_name = 'sequencing read type';

UPDATE meta_data_column SET reflection_class_name = 'Sample',
                            reflection_property_name = 'seqType'
WHERE column_name = 'sequencing type';

UPDATE meta_data_column SET reflection_class_name = 'Sample',
                            reflection_property_name = 'sex'
WHERE column_name = 'sex';

UPDATE meta_data_column SET reflection_class_name = 'Sample',
                            reflection_property_name = 'singleCell'
WHERE column_name = 'single cell';

UPDATE meta_data_column SET reflection_class_name = 'Sample',
                            reflection_property_name = 'species'
WHERE column_name = 'species';

UPDATE meta_data_column SET reflection_class_name = 'Sample',
                            reflection_property_name = 'tagmentation'
WHERE column_name = 'tagmentation';

UPDATE meta_data_column SET reflection_class_name = 'Sample',
                            reflection_property_name = 'tagmentationLibrary'
WHERE column_name = 'tagmentation library';


UPDATE meta_data_column SET reflection_class_name = '', reflection_property_name = '' WHERE reflection_class_name is null;
ALTER TABLE meta_data_column ALTER COLUMN reflection_class_name SET NOT NULL;
ALTER TABLE meta_data_column ALTER COLUMN reflection_property_name SET NOT NULL;

INSERT INTO runtime_options VALUES ('tsvBasePath', '/icgc/dkfzlsdf/dmg/seq_center_inbox/external_data_guide/');
INSERT INTO runtime_options VALUES ('otpImportLink', 'https://otp.dkfz.de/otp/metadataImport?ticketNumber=TICKET_NUMBER&paths=FILE_PATH&directoryStructure=ABSOLUTE_PATH"');
