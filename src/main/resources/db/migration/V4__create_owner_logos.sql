ALTER TABLE owners
    ADD COLUMN mobile_number VARCHAR(20) NULL,
    ADD COLUMN address_line1 VARCHAR(255) NULL,
    ADD COLUMN address_line2 VARCHAR(255) NULL,
    ADD COLUMN city VARCHAR(100) NULL,
    ADD COLUMN state VARCHAR(100) NULL,
    ADD COLUMN country VARCHAR(100) NULL,
    ADD COLUMN postal_code VARCHAR(20) NULL,
    ADD COLUMN website VARCHAR(255) NULL;

ALTER TABLE owners
    ADD CONSTRAINT uk_owners_mobile_number
        UNIQUE (mobile_number);