CREATE TABLE role (
    id INT NOT NULL PRIMARY KEY,
    name VARCHAR(255) unique
);

INSERT INTO role VALUES (nextval('hibernate_sequence'), 'ADMIN');
INSERT INTO role VALUES (nextval('hibernate_sequence'), 'USER');
INSERT INTO role VALUES (nextval('hibernate_sequence'), 'ILSE_IMPORT');

CREATE TABLE person_roles (
    person_username VARCHAR(255) NOT NULL REFERENCES person,
    roles_id INTEGER NOT NULL REFERENCES role,
    PRIMARY KEY (person_username, roles_id)
);
