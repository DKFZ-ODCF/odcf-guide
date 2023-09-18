ALTER TABLE seq_type ADD COLUMN is_being_processed BOOLEAN;
UPDATE seq_type SET is_being_processed = false WHERE is_being_processed IS NULL;

create table seq_type_requested_value (
    id                           integer not null primary key,
    requested_value              varchar,
    requested_seq_type_id        int references seq_type,
    requester_username           varchar references person,
    origin_submission_identifier varchar(255) references submission,
    created_value_as             varchar,
    date_created                 timestamp,
    last_update                  timestamp,
    state                        varchar(255)
);

create table seq_type_requested_value_used_submissions(
    seq_type_requested_value_id integer not null references seq_type_requested_value,
    used_submissions_identifier varchar not null references submission,
    primary key (seq_type_requested_value_id, used_submissions_identifier)
);

ALTER TABLE IF EXISTS requested_value RENAME TO field_requested_value;
ALTER TABLE IF EXISTS requested_value_used_submissions RENAME TO field_requested_value_used_submissions;

ALTER TABLE field_requested_value_used_submissions RENAME COLUMN requested_value_id TO field_requested_value_id;
ALTER TABLE field_requested_value_used_submissions DROP CONSTRAINT IF EXISTS requested_value_used_submissions_requested_value_id_fkey;
ALTER TABLE field_requested_value_used_submissions
    ADD CONSTRAINT field_requested_value_id_fk FOREIGN KEY (field_requested_value_id) REFERENCES field_requested_value(id);

ALTER TABLE seq_type RENAME COLUMN is_being_processed TO is_requested;
