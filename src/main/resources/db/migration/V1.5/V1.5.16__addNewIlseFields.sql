ALTER TABLE sample ADD COLUMN index_type VARCHAR(255);
ALTER TABLE sample ADD COLUMN protocol VARCHAR(255);

UPDATE sample SET index_type = sample_unknown_values.unknown_values
FROM sample_unknown_values
WHERE sample.id = sample_unknown_values.sample_id AND
      sample_unknown_values.unknown_values_key = 'IndexType' AND
      index_type IS NULL;

UPDATE sample SET library_preparation_kit = sample_unknown_values.unknown_values
FROM sample_unknown_values
WHERE sample.id = sample_unknown_values.sample_id AND
      sample_unknown_values.unknown_values_key = 'LibprepKit' AND
      library_preparation_kit IS NULL;

UPDATE sample SET protocol = sample_unknown_values.unknown_values
FROM sample_unknown_values
WHERE sample.id = sample_unknown_values.sample_id AND
      sample_unknown_values.unknown_values_key = 'Protocol' AND
      protocol IS NULL;

DELETE FROM sample_unknown_values WHERE unknown_values_key = 'IndexType';
DELETE FROM sample_unknown_values WHERE unknown_values_key = 'LibprepKit';
DELETE FROM sample_unknown_values WHERE unknown_values_key = 'Protocol';