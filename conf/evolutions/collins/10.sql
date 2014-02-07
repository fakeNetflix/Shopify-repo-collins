# -- asset type labels

# --- !Ups

ALTER TABLE asset_type ADD COLUMN label VARCHAR(255) NOT NULL DEFAULT '';
UPDATE asset_type SET name='SERVER_NODE', label='Server Node' WHERE id=1;
UPDATE asset_type SET name='SERVER_CHASSIS', label='Server Chassis' WHERE id=2;
UPDATE asset_type SET name='RACK', label='Rack' WHERE id=3;
UPDATE asset_type SET name='DATA_ROW', label='Data Center Row' WHERE id=4;
UPDATE asset_type SET name='DATA_ROOM', label='Data Center Room' WHERE id=5;
UPDATE asset_type SET name='DATA_CENTER', label='Data Center' WHERE id=6;
UPDATE asset_type SET name='SWITCH', label='Switch' WHERE id=7;
UPDATE asset_type SET name='ROUTER', label='Router' WHERE id=8;
UPDATE asset_type SET name='FIREWALL', label='Firewall' WHERE id=9;
UPDATE asset_type SET name='PATCH_PANEL', label='Patch Panel' WHERE id=10;
UPDATE asset_type SET name='LOAD_BALANCER', label='Load Balancer' WHERE id=11;
UPDATE asset_type SET name='DDOS_MITIGATE', label='DDOS Mitigator' WHERE id=12;
UPDATE asset_type SET name='SERIAL_SERVER', label='Console Server' WHERE id=13;
UPDATE asset_type SET name='POWER_CIRCUIT', label='Power Circuit' WHERE id=14;
UPDATE asset_type SET name='POWER_STRIP', label='Power Strip' WHERE id=15;
UPDATE asset_type SET name='PDU', label='PDU' WHERE id=16;
UPDATE asset_type SET name='CONFIGURATION', label='Configuration' WHERE id=17;


# --- !Downs

ALTER TABLE asset_type DROP COLUMN label;
UPDATE asset_type SET name='SERVER_NODE',  WHERE id=1;
UPDATE asset_type SET name='SERVER_CHASSIS',  WHERE id=2;
UPDATE asset_type SET name='RACK',  WHERE id=3;
UPDATE asset_type SET name='DATA_ROW',  WHERE id=4;
UPDATE asset_type SET name='DATA_ROOM',  WHERE id=5;
UPDATE asset_type SET name='DATA_CENTER',  WHERE id=6;
UPDATE asset_type SET name='SWITCH',  WHERE id=7;
UPDATE asset_type SET name='ROUTER',  WHERE id=8;
UPDATE asset_type SET name='FIREWALL',  WHERE id=9;
UPDATE asset_type SET name='PATCH_PANEL',  WHERE id=10;
UPDATE asset_type SET name='LOAD_BALANCER',  WHERE id=11;
UPDATE asset_type SET name='DDOS_MITIGATE',  WHERE id=12;
UPDATE asset_type SET name='SERIAL_SERVER',  WHERE id=13;
UPDATE asset_type SET name='POWER_CIRCUIT',  WHERE id=14;
UPDATE asset_type SET name='POWER_STRIP',  WHERE id=15;
UPDATE asset_type SET name='PDU',  WHERE id=16;
UPDATE asset_type SET name='CONFIGURATION',  WHERE id=17;


