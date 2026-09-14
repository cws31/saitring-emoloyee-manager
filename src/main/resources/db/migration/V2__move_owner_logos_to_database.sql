CREATE TABLE owner_logos (
    id BIGINT NOT NULL AUTO_INCREMENT,

    owner_id BIGINT NOT NULL,

    logo_data LONGBLOB NOT NULL,

    content_type VARCHAR(100) NOT NULL,

    created_at DATETIME(6) NOT NULL,

    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT pk_owner_logos
        PRIMARY KEY (id),

    CONSTRAINT uk_owner_logos_owner_id
        UNIQUE (owner_id),

    CONSTRAINT fk_owner_logos_owner
        FOREIGN KEY (owner_id)
        REFERENCES owners(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE

) ENGINE=InnoDB;