TRUNCATE table precon_card;

ALTER TABLE precon_card
    drop column precon_id;

alter table precon
    drop column id;
alter table precon
    drop constraint precon_name_key;

alter table precon
    add constraint precon_pk
        primary key (name);

-- noinspection SqlAddNotNullColumn
alter table precon_card
    add column precon text NOT NULL REFERENCES precon (name);

alter table precon_card
    drop column id;
ALTER TABLE precon_card
    add constraint precon_card_pk primary key (card_name, precon);
