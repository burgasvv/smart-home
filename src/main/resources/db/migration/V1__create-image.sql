
create table if not exists image (
    id uuid default gen_random_uuid() unique not null ,
    name varchar ,
    content_type varchar not null check ( content_type like 'image/%') ,
    data bytea not null ,
    created_at timestamp not null
);