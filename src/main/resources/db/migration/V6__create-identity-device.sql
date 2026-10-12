
create table if not exists identity_device (
    identity_id uuid references identity(id) on delete cascade on update cascade ,
    device_id uuid references device(id) on delete cascade on update cascade
);