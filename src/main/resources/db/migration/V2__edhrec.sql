CREATE TABLE edhrec_theme
(
    url        text PRIMARY KEY,
    commander  text      NOT NULL REFERENCES atomic_card (full_name) DEFERRABLE,
    partner    text REFERENCES atomic_card (full_name) DEFERRABLE,
    theme      text      NOT NULL,
    fetched_on timestamp NOT NULL
);

CREATE TABLE edhrec_recommendation
(
    theme        text REFERENCES edhrec_theme (url),
    card         text NOT NULL REFERENCES atomic_card (full_name) DEFERRABLE,
    synergyScore INT  NOT NULL,
    usageScore   INT  NOT NULL,
    PRIMARY KEY (theme, card)
);
