ALTER TABLE person ADD COLUMN is_organizational_unit_leader boolean;

UPDATE person SET is_organizational_unit_leader = false WHERE is_organizational_unit_leader IS NULL;

INSERT INTO runtime_options VALUES ('organizationalUnitsPath', '');
INSERT INTO runtime_options VALUES ('organizationalUnitLeaderPath', '');
