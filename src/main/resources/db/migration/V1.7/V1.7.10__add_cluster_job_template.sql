CREATE TABLE cluster_job_template (
    id INT NOT NULL PRIMARY KEY,
    name VARCHAR(255) UNIQUE,
    "group" VARCHAR(255),
    command VARCHAR
);

ALTER TABLE sequencing_technology ADD COLUMN cluster_job_template_id INT;
ALTER TABLE sequencing_technology ADD FOREIGN KEY (cluster_job_template_id) REFERENCES cluster_job_template (id);

ALTER TABLE cluster_job ADD COLUMN submission_identifier varchar(255);
ALTER TABLE cluster_job ADD FOREIGN KEY (submission_identifier) REFERENCES submission (identifier);
ALTER TABLE cluster_job ADD COLUMN command varchar;

INSERT INTO runtime_options VALUES ('autoStartJobs', 'false');
