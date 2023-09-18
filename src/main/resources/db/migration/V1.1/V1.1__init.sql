create sequence hibernate_sequence;

create table basic_seq_type (
    id int not null primary key,
    name varchar(255),
    constraint basic_seq_type_name unique (name)
);

create table seq_type (
    id int not null primary key,
    name varchar(255),
    need_antibody_target boolean not null,
    single_cell boolean not null,
    tagmentation boolean not null,
    basic_seq_type_id int references basic_seq_type,
    constraint seq_type_name unique (name)
);

create table import_source_data (
    identifier varchar(255) not null primary key,
    formatted_identifier varchar(255),
    import_date timestamp not null,
    json_content varchar(255),
    times_used_for_reset int,
    constraint import_source_data_formatted_identifier unique (formatted_identifier)
);

create table person (
    username varchar(255) not null primary key,
    department varchar(255),
    first_name varchar(255),
    is_admin boolean not null,
    last_name varchar(255),
    mail varchar(255)
);

create table submission (
    identifier varchar(255) not null primary key,
    closed_date timestamp,
    closed_user varchar(255),
    export_date timestamp,
    external_data_available_for_merging boolean,
    external_data_availibilty_date timestamp,
    lock_date timestamp,
    lock_user varchar(255),
    origin_projects varchar(255),
    otrs_ticket_number varchar(255),
    removal_date timestamp,
    removal_user varchar(255),
    sequencing_type varchar(255),
    status varchar(255),
    terminate_date timestamp,
    uuid varchar(255),
    submitter_username varchar(255) references person,
    constraint submission_uuid unique (uuid)
);

create table technical_sample (
    id int not null primary key,
    barcode varchar(255),
    base_count varchar(255),
    center varchar(255),
    cycle_count varchar(255),
    external_submission_id varchar(255),
    instrument_model varchar(255),
    instrument_platform varchar(255),
    lane varchar(255),
    pipeline_version varchar(255),
    read_count varchar(255),
    run_date varchar(255),
    run_id varchar(255),
    sequencing_kit varchar(255)
);

create table sample (
    id int not null primary key,
    antibody_target varchar(255),
    comment varchar(255),
    library_layout varchar(255),
    pid varchar(255),
    project varchar(255),
    sample_identifier varchar(255),
    sample_identifier_odcf varchar(255),
    sample_type varchar(255),
    sex varchar(255),
    single_cell boolean,
    tagmentation boolean,
    tagmentation_library varchar(255),
    seq_type_id int references seq_type,
    submission_identifier varchar(255) references submission,
    antibody varchar(255),
    library_preparation_kit varchar(255),
    phenotype varchar(255),
    species varchar(255),
    technical_sample_id int references technical_sample
);

create table sample_unknown_values (
    sample_id int not null references sample,
    unknown_values varchar(255),
    unknown_values_key varchar(255) not null,
    primary key (sample_id, unknown_values_key)
);

create table file (
    id int not null primary key,
    file_name varchar(255),
    md5 varchar(255),
    read_number varchar(255),
    sample_id int null references sample
);
