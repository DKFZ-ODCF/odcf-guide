CREATE TABLE parser (
    id INT NOT NULL PRIMARY KEY,
    parser_regex VARCHAR,
    project VARCHAR(255)
);

CREATE TABLE parser_field (
    id INT NOT NULL PRIMARY KEY,
    parser_id INT NULL,
    field_name VARCHAR(255),
    field_regex VARCHAR,
    order_of_components VARCHAR(255)
);

CREATE TABLE parser_component (
    id INT NOT NULL PRIMARY KEY,
    parser_field_id INT NULL,
    component_name VARCHAR(255),
    component_regex VARCHAR,
    number_of_digits INT,
    parser_mapping_string VARCHAR
);
