ALTER TABLE aliases DROP CONSTRAINT aliases_pkey;
ALTER TABLE aliases ADD PRIMARY KEY (aliased_name);
