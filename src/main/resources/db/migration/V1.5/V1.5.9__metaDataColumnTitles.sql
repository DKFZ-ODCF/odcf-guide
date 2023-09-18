CREATE TABLE meta_data_column (
    column_name VARCHAR(255) PRIMARY KEY,
    import_aliases VARCHAR(255),
    export_name VARCHAR(255)
);

INSERT INTO meta_data_column VALUES ('antibody', null, null);
INSERT INTO meta_data_column VALUES ('antibody target', null, null);
INSERT INTO meta_data_column VALUES ('base count', null, null);
INSERT INTO meta_data_column VALUES ('center', 'center name', 'center name');
INSERT INTO meta_data_column VALUES ('comment', null, null);
INSERT INTO meta_data_column VALUES ('cycle count', null, null);
INSERT INTO meta_data_column VALUES ('fastq file name', 'fastq file', 'fastq file');
INSERT INTO meta_data_column VALUES ('fastq generator', 'bcl2fastq version;pipeline version', null);
INSERT INTO meta_data_column VALUES ('ilse no', null, null);
INSERT INTO meta_data_column VALUES ('index', 'barcode', null);
INSERT INTO meta_data_column VALUES ('instrument model', null, null);
INSERT INTO meta_data_column VALUES ('instrument platform', null, null);
INSERT INTO meta_data_column VALUES ('lane no', null, null);
INSERT INTO meta_data_column VALUES ('lib prep kit', 'library preparation kit', null);
INSERT INTO meta_data_column VALUES ('md5 sum', 'md5', 'md5');
INSERT INTO meta_data_column VALUES ('phenotype', null, null);
INSERT INTO meta_data_column VALUES ('patient id', 'pid', null);
INSERT INTO meta_data_column VALUES ('project', null, null);
INSERT INTO meta_data_column VALUES ('read count', null, null);
INSERT INTO meta_data_column VALUES ('read number', 'mate;read', 'read');
INSERT INTO meta_data_column VALUES ('run date', null, null);
INSERT INTO meta_data_column VALUES ('run id', null, null);
INSERT INTO meta_data_column VALUES ('sample name', 'sample id', null);
INSERT INTO meta_data_column VALUES ('sample type', 'biomaterial id', 'biomaterial id');
INSERT INTO meta_data_column VALUES ('sequencing kit', null, null);
INSERT INTO meta_data_column VALUES ('sequencing read type', 'library layout', null);
INSERT INTO meta_data_column VALUES ('sequencing type', 'seq type', null);
INSERT INTO meta_data_column VALUES ('sex', 'gender', null);
INSERT INTO meta_data_column VALUES ('single cell', null, null);
INSERT INTO meta_data_column VALUES ('species', null, null);
INSERT INTO meta_data_column VALUES ('tagmentation', 'tagmentation based library', 'tagmentation based library');
INSERT INTO meta_data_column VALUES ('tagmentation library', 'customer library', 'customer library');
