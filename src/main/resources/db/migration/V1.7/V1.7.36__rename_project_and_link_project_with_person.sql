INSERT INTO role VALUES (nextval('hibernate_sequence'), 'PROJECT_OVERVIEW');

ALTER TABLE otp_cached_project RENAME TO project;

CREATE TABLE project_pis (
    project_id INTEGER NOT NULL REFERENCES project,
    pis_username VARCHAR(255) NOT NULL REFERENCES person,
    PRIMARY KEY (pis_username, project_id)
);

ALTER TABLE project DROP COLUMN pis;
