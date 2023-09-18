create table otp_cached_project
(
    id                   int          not null primary key,
    latest_Update        timestamp    not null,
    name                 varchar(255) not null,
    unix_group           varchar(255) not null,
    pis                  varchar(255) not null,
    seq_types            varchar(255) not null,
    last_data_received   varchar(255) not null,
    path_project_folder  varchar(255) not null,
    path_analysis_folder varchar(255) not null,
    size_project_folder  bigint       not null,
    size_analysis_folder bigint       not null,
    closed               boolean      not null
);