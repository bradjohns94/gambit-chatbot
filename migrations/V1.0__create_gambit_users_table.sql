/* Unique ID Sequence for gambit user IDs */
CREATE SEQUENCE gambit_users_id_seq;

/* Update the "updated_at" column of a changed row */
CREATE OR REPLACE FUNCTION trigger_set_timestamp()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

/* Gambit User Table
 * The core, internal user that users for other clients map to. Contains
 * any special conditions associated with their name and any permissions
 * they may have.
 */
CREATE TABLE gambit_users(
  id          INT PRIMARY KEY DEFAULT nextval('gambit_users_id_seq'), /* Unique Identifier */
  nickname    TEXT UNIQUE NOT NULL,  /* Human-readable name to map to */
  is_admin    BOOLEAN NOT NULL DEFAULT false, /* Permission level */
  prefix      TEXT, /* Any text for the bot to prepend before their username */
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(), /* Time the row was created */
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW() /* Last time the row was changed */
);

/* Update updated_at any time we update a gambit_user */
CREATE TRIGGER set_timestamp
BEFORE UPDATE ON gambit_users
FOR EACH ROW
EXECUTE PROCEDURE trigger_set_timestamp();
