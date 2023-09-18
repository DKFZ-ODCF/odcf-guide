CREATE TABLE requested_value (
    id INT NOT NULL PRIMARY KEY,
    field_name VARCHAR(255),
    class_name VARCHAR(255),
    requested_value VARCHAR,
    requester_username VARCHAR REFERENCES person(username),
    origin_submission_identifier VARCHAR(255) REFERENCES submission(identifier),
    created_value_as VARCHAR,
    date_created TIMESTAMP,
    last_update TIMESTAMP,
    state VARCHAR(255)
);

CREATE TABLE requested_value_used_submissions (
    requested_value_id INT NOT NULL,
    used_submissions_identifier VARCHAR NOT NULL,
    PRIMARY KEY (requested_value_id, used_submissions_identifier),
    FOREIGN KEY (requested_value_id) REFERENCES requested_value(id),
    FOREIGN KEY (used_submissions_identifier) REFERENCES submission(identifier)
);
