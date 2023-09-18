ALTER TABLE file ADD COLUMN base_count int;
ALTER TABLE file ADD COLUMN cycle_count int;

ALTER TABLE technical_sample DROP COLUMN IF EXISTS  base_count;
ALTER TABLE technical_sample DROP COLUMN IF EXISTS  cycle_count;
ALTER TABLE technical_sample DROP COLUMN IF EXISTS lane;
ALTER TABLE technical_sample ADD COLUMN lane int;
ALTER TABLE technical_sample DROP COLUMN IF EXISTS read_count;
ALTER TABLE technical_sample ADD COLUMN read_count int;