ALTER TABLE seq_type ADD COLUMN is_hidden_for_user BOOLEAN;

UPDATE seq_type SET is_hidden_for_user = false WHERE is_displayed_for_user = true;
UPDATE seq_type SET is_hidden_for_user = true WHERE is_displayed_for_user = false;

ALTER TABLE seq_type DROP COLUMN IF EXISTS is_displayed_for_user;
