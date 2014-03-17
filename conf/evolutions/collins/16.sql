# -- asset type labels

# --- !Ups

ALTER TABLE hierarchy ADD COLUMN child_id BIGINT NOT NULL DEFAULT -1;
UPDATE hierarchy SET hierarchy.child_id=(SELECT asset.id FROM asset WHERE asset.tag=hierarchy.child_tag);
ALTER TABLE hierarchy DROP COLUMN child_tag;
ALTER TABLE hierarchy DROP COLUMN child_label;



# --- !Downs

ALTER TABLE hierarchy DROP COLUMN child_id;
ALTER TABLE hierarchy ADD COLUMN child_tag VARCHAR(64) NOT NULL DEFAULT '' ;
ALTER TABLE hierarchy ADD COLUMN child_label VARCHAR(255) NOT NULL DEFAULT '' ;

