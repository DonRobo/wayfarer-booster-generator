CREATE TABLE atomic_card_id
(
    id               serial PRIMARY KEY,
    atomic_card_name text NOT NULL UNIQUE REFERENCES atomic_card (full_name) DEFERRABLE
);
