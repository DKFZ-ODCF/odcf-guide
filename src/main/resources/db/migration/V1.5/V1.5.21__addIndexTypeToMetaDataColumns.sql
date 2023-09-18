INSERT INTO meta_data_column
VALUES ('index type', 'index kit', null, 231, 'indexType', 'Sample');

UPDATE meta_data_column SET reflection_class_name = 'Sample',
                            reflection_property_name = 'xenograft'
WHERE column_name = 'xenograft';

UPDATE meta_data_column SET reflection_class_name = 'Sample',
                            reflection_property_name = 'singleCellPlate'
WHERE column_name = 'plate';

UPDATE meta_data_column SET reflection_class_name = 'Sample',
                            reflection_property_name = 'singleCellWellPosition'
WHERE column_name = 'well position';

UPDATE meta_data_column SET reflection_class_name = 'TechnicalSample',
                            reflection_property_name = 'pipelineVersion'
WHERE column_name = 'fastq generator';
