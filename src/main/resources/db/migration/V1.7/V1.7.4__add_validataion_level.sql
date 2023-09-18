ALTER TABLE validation DROP CONSTRAINT validation_pkey;
ALTER TABLE validation ADD COLUMN id INT;
UPDATE validation SET id = nextval('hibernate_sequence');
ALTER TABLE validation ADD PRIMARY KEY (id);

CREATE TABLE validation_level (
    id INT NOT NULL PRIMARY KEY,
    name VARCHAR(255)
);

CREATE TABLE validation_level_fields (
    validation_level_id int NOT NULL,
    fields_id int NOT NULL,
    PRIMARY KEY (validation_level_id, fields_id),
    FOREIGN KEY (validation_level_id) REFERENCES validation_level(id),
    FOREIGN KEY (fields_id) REFERENCES validation(id)
);

INSERT INTO validation_level VALUES (nextval('hibernate_sequence'), 'minimal');
INSERT INTO validation_level VALUES (nextval('hibernate_sequence'), 'full');

INSERT INTO validation_level_fields
VALUES ((SELECT id FROM validation_level WHERE name = 'minimal'),
        (SELECT id FROM validation WHERE field = 'project'));

INSERT INTO validation_level_fields
VALUES ((SELECT id FROM validation_level WHERE name = 'full'),
        unnest((SELECT array_agg(id) FROM validation)));
