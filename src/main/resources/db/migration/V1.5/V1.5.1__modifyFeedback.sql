ALTER TABLE feedback ADD COLUMN submission_identifier varchar(255) REFERENCES submission;
ALTER TABLE feedback ADD COLUMN user_username varchar(255) REFERENCES person;
