ALTER TABLE cluster_job ADD COLUMN restarted_job_id INTEGER references cluster_job(id);
ALTER TABLE cluster_job ADD COLUMN visible_for_user BOOLEAN;
ALTER TABLE cluster_job_template ADD COLUMN cluster_job_visible_for_user BOOLEAN;
ALTER TABLE cluster_job_template ADD COLUMN maximum_runtime INT;
ALTER TABLE cluster_job_template ADD COLUMN estimated_runtime_per_sample INT;
ALTER TABLE sequencing_technology ADD COLUMN check_external_metadata_source BOOLEAN;

UPDATE cluster_job SET visible_for_user = TRUE WHERE visible_for_user IS NULL;
UPDATE cluster_job_template SET cluster_job_visible_for_user = TRUE WHERE cluster_job_visible_for_user IS NULL;
UPDATE cluster_job_template SET maximum_runtime = 1440 WHERE maximum_runtime IS NULL;
UPDATE cluster_job_template SET estimated_runtime_per_sample = 0 WHERE estimated_runtime_per_sample IS NULL;
UPDATE sequencing_technology SET check_external_metadata_source = TRUE WHERE check_external_metadata_source IS NULL;
