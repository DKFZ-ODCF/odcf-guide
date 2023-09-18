ALTER TABLE parser_component ADD COLUMN optional boolean;

UPDATE parser_component SET optional = false WHERE optional IS NULL;
