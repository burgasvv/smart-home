
create table if not exists category (
    id uuid default gen_random_uuid() unique not null ,
    name varchar unique not null ,
    description text unique not null ,
    created_at timestamp not null
);