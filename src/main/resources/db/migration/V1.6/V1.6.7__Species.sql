ALTER TABLE sample ADD COLUMN species_with_strain VARCHAR;

INSERT INTO validation VALUES ('species', 'dropdown', true,
                               '- choose element from list' ||
                               '<br>- main species for the same individual must match (including OTP)' ||
                               '<br>- all species for the same sample entries must match (including OTP)' ||
                               '<br>- xenograft samples must include two species');

DELETE FROM meta_data_column WHERE column_name = 'species';
INSERT INTO meta_data_column VALUES ('species with strain', 'species', 'SPECIES', 115, 'speciesWithStrain', 'Sample');
