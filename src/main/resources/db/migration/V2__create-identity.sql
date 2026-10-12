
create table if not exists identity (
    id uuid default gen_random_uuid() unique not null ,
    authority varchar(50) not null ,
    email varchar(250) unique not null check ( email ~ '^[a-z0-9._%-]+@[a-z0-9.-]+\.[a-z]{2,}$' ) ,
    password varchar not null ,
    status boolean not null default true ,
    firstname varchar not null ,
    lastname varchar not null ,
    patronymic varchar not null ,
    image_id uuid unique references image(id) on delete set null on update cascade ,
    created_at timestamp not null
);