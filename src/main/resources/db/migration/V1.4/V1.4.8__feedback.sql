create table feedback
(
    id int not null primary key,
    date timestamp not null,
    rating varchar(255) not null,
    message varchar
);
