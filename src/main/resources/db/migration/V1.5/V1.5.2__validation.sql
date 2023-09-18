create table validation (
    field varchar(255) primary key,
    regex varchar(255) not null,
    required boolean not null
);

insert into validation values ('project', 'dropdown', true);
insert into validation values ('pid', '^[A-Za-z0-9_+-]{3,42}$', true);
insert into validation values ('sampleType', '^[a-z0-9+-]*[0-9]+$', true);
insert into validation values ('oldSampleType', '^[a-z0-9+-]+$', true);
insert into validation values ('sex', 'dropdown', true);
insert into validation values ('basicSeqType', 'dropdown', true);
insert into validation values ('seqType', 'dropdown', true);
insert into validation values ('tagmentationLibrary', '^\d{1,3}$', false);
insert into validation values ('singleCellPlate', '^[a-zA-Z0-9]*$', false);
insert into validation values ('singleCellWellPosition', '^[a-zA-Z0-9]*$', false);