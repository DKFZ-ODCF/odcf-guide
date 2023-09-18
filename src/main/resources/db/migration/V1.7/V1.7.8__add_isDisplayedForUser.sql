ALTER TABLE seq_type ADD COLUMN is_displayed_for_user BOOLEAN;
UPDATE seq_type SET is_displayed_for_user = true WHERE is_displayed_for_user IS NULL;
