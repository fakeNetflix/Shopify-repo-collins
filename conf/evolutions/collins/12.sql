# -- asset state schema

# --- !Ups

CREATE TABLE hierarchy (
  id                            INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
  priority                      INTEGER NOT NULL DEFAULT -1,
  asset_tag                     VARCHAR(64) NOT NULL DEFAULT '',
  child_tag                     VARCHAR(64) NOT NULL DEFAULT '',
  child_label                   VARCHAR(255) NOT NULL DEFAULT '',
  child_start                   INTEGER NOT NULL DEFAULT -1,
  child_end                     INTEGER NOT NULL DEFAULT -1
);

CREATE INDEX hierarchy_idx ON hierarchy (priority);

# --- !Downs

DROP TABLE IF EXISTS hierarchy;
