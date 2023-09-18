create table seq_type_import_aliases (
   seq_type_id int not null,
   import_aliases varchar(255) not null unique,
   primary key (seq_type_id, import_aliases)
);
