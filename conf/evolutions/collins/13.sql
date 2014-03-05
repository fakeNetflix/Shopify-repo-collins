# -- asset state schema

# --- !Ups

CREATE TABLE sessions (
  token                    VARCHAR(64) NOT NULL PRIMARY KEY,
  expiry                   BIGINT NOT NULL DEFAULT 0,
  data                     TEXT NOT NULL
);

# --- !Downs

DROP TABLE IF EXISTS sessions;
