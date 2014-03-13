# -- asset state schema

# --- !Ups

CREATE TABLE users (
  id             INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
  username       VARCHAR(128) NOT NULL DEFAULT '' UNIQUE KEY,
  password       VARCHAR(256) NOT NULL DEFAULT '',
  salt           VARCHAR(64) NOT NULL DEFAULT '',
  roles          TEXT NOT NULL,
  type           VARCHAR(64) NOT NULL DEFAULT ''
);

CREATE INDEX user_type_idx ON users (type);

# --- !Downs

DROP TABLE IF EXISTS users;
