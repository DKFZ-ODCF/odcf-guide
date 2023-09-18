ALTER TABLE cluster_job ADD COLUMN parent_job_id INTEGER references cluster_job(id);
ALTER TABLE cluster_job_template ADD COLUMN subsequent_job_template_id INTEGER references cluster_job_template(id);

UPDATE runtime_options
SET value = '<PROJECT>/nonOTP/ont/view-by-pid/<PID>/<SAMPLE_TYPE>/<SEQ_TYPE>/<ASID>/'
WHERE name = 'projectPathTemplateNonOtp';
