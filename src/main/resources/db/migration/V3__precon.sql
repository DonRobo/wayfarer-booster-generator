CREATE TABLE precon
(
    id   serial PRIMARY KEY,
    name text UNIQUE NOT NULL
);

CREATE TABLE precon_card
(
    id        serial primary key,
    precon_id int  NOT NULL REFERENCES precon (id) DEFERRABLE,
    card_name text NOT NULL references atomic_card (full_name) DEFERRABLE,
    commander bool NOT NULL,
    count     int  NOT NULL
);
