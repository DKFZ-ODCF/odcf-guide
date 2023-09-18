CREATE TABLE cluster_job (
    id INT NOT NULL PRIMARY KEY,
    remote_id INT,
    job_name VARCHAR(255),
    host_name VARCHAR(255),
    state VARCHAR(255) NOT NULL,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    exit_code INT NOT NULL,
    path_to_log VARCHAR,
    date_created TIMESTAMP,
    last_update TIMESTAMP,
    uuid VARCHAR(255) UNIQUE
);
