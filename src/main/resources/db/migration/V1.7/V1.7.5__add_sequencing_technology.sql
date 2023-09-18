CREATE TABLE sequencing_technology (
    id INT NOT NULL PRIMARY KEY,
    name VARCHAR(255) unique,
    import_aliases_string VARCHAR(255)
);
