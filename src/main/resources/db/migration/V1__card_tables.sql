CREATE TABLE atomic_card
(
    full_name   text PRIMARY KEY,
    card_json   jsonb  not null,
    names       text[] not null,
    first_name  text   not null,
    scryfall_id text   NOT NULL UNIQUE
);

create table mtgjson_update
(
    id           serial PRIMARY KEY,
    release_date text                    not null,
    version      text                    not null,
    updated_on   timestamp default now() not null
);

