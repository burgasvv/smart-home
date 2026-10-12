
create table if not exists device (
    id uuid default gen_random_uuid() unique not null ,
    category_id uuid references category(id) on delete set null on update cascade ,
    brand_id uuid references brand(id) on delete set null on update cascade ,
    model varchar unique not null ,
    specifications text not null ,
    in_progress boolean not null default true ,
    created_at timestamp not null
);