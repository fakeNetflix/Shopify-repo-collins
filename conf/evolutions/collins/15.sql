# -- asset type labels

# --- !Ups

ALTER TABLE hierarchy ADD COLUMN asset_id BIGINT NOT NULL DEFAULT -1;
UPDATE hierarchy SET hierarchy.asset_id=(SELECT asset.id FROM asset WHERE asset.tag=hierarchy.asset_tag);
ALTER TABLE hierarchy DROP COLUMN asset_tag;




# --- !Downs

ALTER TABLE hierarchy DROP COLUMN asset_id;
ALTER TABLE hierarchy ADD COLUMN asset_tag VARCHAR(64) NOT NULL DEFAULT '' ;


