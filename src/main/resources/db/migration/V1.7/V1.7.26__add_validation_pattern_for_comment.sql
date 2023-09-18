INSERT INTO validation VALUES
    ('comment', '[0-9A-Za-z\\\x22\-\/\[\] !#$%&\x27()*+,.:;<=>?@^_`|~{}]*', false,
     'Includes forbidden characters. Allowed: 0-9 A-Z a-z \ " - / [ ] ! # $ % & '' ( ) * + , . : ; < = > ? @ ^ _ ` | ~ { }', nextval('hibernate_sequence'));

INSERT INTO validation_level_fields
VALUES ((SELECT id FROM validation_level WHERE name = 'full'),
        unnest((SELECT array_agg(id) FROM validation)))
ON CONFLICT DO NOTHING;

INSERT INTO validation_level_fields
VALUES ((SELECT id FROM validation_level WHERE name = 'minimal'),
        (SELECT id FROM validation WHERE field = 'comment'));
