CREATE TABLE card_printing
(
    uuid             UUID PRIMARY KEY,
    face_name        text NOT NULL,
    full_name        text NOT NULL REFERENCES atomic_card (full_name) DEFERRABLE,
    set_code         text NOT NULL,
    collector_number text NOT NULL
);

CREATE TABLE card_printing_price
(
    card_printing       UUID      NOT NULL PRIMARY KEY REFERENCES card_printing (uuid) DEFERRABLE,
    price_eur           numeric(10, 2),
    price_usd           numeric(10, 2),
    price_eur_last_week numeric(10, 2),
    price_usd_last_week numeric(10, 2),
    updated_on          timestamp NOT NULL
);
