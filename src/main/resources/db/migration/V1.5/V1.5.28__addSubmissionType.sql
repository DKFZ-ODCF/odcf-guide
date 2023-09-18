ALTER TABLE submission ADD COLUMN submission_type VARCHAR(250);

UPDATE submission SET submission_type = 'ApiSubmission' WHERE identifier like 'i%';
UPDATE submission SET submission_type = 'UploadSubmission' WHERE identifier like 'o%';
