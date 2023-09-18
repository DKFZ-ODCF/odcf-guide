ALTER TABLE person ADD COLUMN accepted_data_security_statement_date TIMESTAMP;

/*this ist for documentation and actually not necessary*/
UPDATE person SET accepted_data_security_statement_date = NULL
    WHERE accepted_data_security_statement_date IS NOT NULL;