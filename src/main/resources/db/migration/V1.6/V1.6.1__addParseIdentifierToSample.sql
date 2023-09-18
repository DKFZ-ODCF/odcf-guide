ALTER TABLE sample ADD COLUMN parse_identifier VARCHAR;

UPDATE sample SET parse_identifier = '' where parse_identifier IS NULL;

ALTER TABLE sample ALTER COLUMN parse_identifier SET NOT NULL;

INSERT INTO meta_data_column
VALUES ('parse identifier', null, null, 11, '', '');

INSERT INTO validation VALUES ('parseIdentifier', '^[a-zA-Z0-9.()_:\/+%&:^-]+$', false, 'ERROR');
