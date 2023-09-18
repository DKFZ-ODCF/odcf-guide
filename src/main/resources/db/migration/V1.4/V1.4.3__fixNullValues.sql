UPDATE sample SET antibody = '' WHERE antibody IS NULL;
UPDATE sample SET antibody_target = '' WHERE antibody_target IS NULL;
UPDATE sample SET tagmentation_library = '' WHERE tagmentation_library IS NULL;
UPDATE sample SET library_preparation_kit = '' WHERE library_preparation_kit IS NULL;
UPDATE sample SET phenotype = '' WHERE phenotype IS NULL;
UPDATE sample SET species = '' WHERE species IS NULL;
