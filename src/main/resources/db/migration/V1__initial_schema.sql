
CREATE TABLE owners (
    id BIGINT NOT NULL AUTO_INCREMENT,

    owner_name VARCHAR(100) NOT NULL,
    organization_name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    logo_url VARCHAR(1000),

    CONSTRAINT pk_owners
        PRIMARY KEY (id),

    CONSTRAINT uk_owners_email
        UNIQUE (email),

    CONSTRAINT uk_owners_username
        UNIQUE (username)

) ENGINE=InnoDB;



CREATE TABLE employees (
    id BIGINT NOT NULL AUTO_INCREMENT,

    name VARCHAR(100) NOT NULL,
    mobile VARCHAR(15) NOT NULL,
    initial_rate DECIMAL(10,2) NOT NULL,
    blocked BIT NOT NULL,
    created_at DATETIME(6),

    owner_id BIGINT NOT NULL,

    CONSTRAINT pk_employees
        PRIMARY KEY (id),

    CONSTRAINT uk_employee_owner_mobile
        UNIQUE (owner_id, mobile),

    CONSTRAINT fk_employee_owner
        FOREIGN KEY (owner_id)
        REFERENCES owners(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE

) ENGINE=InnoDB;

CREATE INDEX idx_employee_owner_id
    ON employees(owner_id);



CREATE TABLE employee_advances (
    id BIGINT NOT NULL AUTO_INCREMENT,

    employee_id BIGINT NOT NULL,
    owner_id BIGINT NOT NULL,

    amount DOUBLE NOT NULL,
    payment_date DATE NOT NULL,
    note VARCHAR(255),

    CONSTRAINT pk_employee_advances
        PRIMARY KEY (id),

    CONSTRAINT fk_advance_employee
        FOREIGN KEY (employee_id)
        REFERENCES employees(id),

    CONSTRAINT fk_advance_owner
        FOREIGN KEY (owner_id)
        REFERENCES owners(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE

) ENGINE=InnoDB;

CREATE INDEX idx_advance_owner_id
    ON employee_advances(owner_id);

CREATE INDEX idx_advance_owner_payment_date
    ON employee_advances(owner_id, payment_date);



CREATE TABLE attendances (
    id BIGINT NOT NULL AUTO_INCREMENT,

    employee_id BIGINT NOT NULL,
    owner_id BIGINT NOT NULL,

    attendance_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6),
    reason VARCHAR(255),

    CONSTRAINT pk_attendances
        PRIMARY KEY (id),

    CONSTRAINT uk_attendance_owner_employee_date
        UNIQUE (owner_id, employee_id, attendance_date),

    CONSTRAINT fk_attendance_employee
        FOREIGN KEY (employee_id)
        REFERENCES employees(id),

    CONSTRAINT fk_attendance_owner
        FOREIGN KEY (owner_id)
        REFERENCES owners(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE

) ENGINE=InnoDB;

CREATE INDEX idx_attendance_owner_id
    ON attendances(owner_id);



CREATE TABLE month_closings (
    id BIGINT NOT NULL AUTO_INCREMENT,

    year INT NOT NULL,
    month INT NOT NULL,
    closed_at DATETIME(6),

    total_employees BIGINT NOT NULL DEFAULT 0,
    total_payable DECIMAL(38,2) NOT NULL DEFAULT 0.00,
    total_over_advance DECIMAL(38,2) NOT NULL DEFAULT 0.00,

    owner_id BIGINT NOT NULL,

    CONSTRAINT pk_month_closings
        PRIMARY KEY (id),

    CONSTRAINT uk_month_closing_owner_year_month
        UNIQUE (owner_id, year, month),

    CONSTRAINT fk_month_closing_owner
        FOREIGN KEY (owner_id)
        REFERENCES owners(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE

) ENGINE=InnoDB;

CREATE INDEX idx_month_closing_owner_id
    ON month_closings(owner_id);



CREATE TABLE month_closing_detail (
    id BIGINT NOT NULL AUTO_INCREMENT,

    month_closing_id BIGINT,
    employee_id BIGINT,

    employee_name VARCHAR(255),

    total_presences DECIMAL(38,2),
    rate DECIMAL(38,2),
    total_earning DECIMAL(38,2),
    total_advance DECIMAL(38,2),
    previous_balance DECIMAL(38,2),
    extra_money DECIMAL(38,2),
    net_payable DECIMAL(38,2),
    amount_paid DECIMAL(38,2),
    remaining_balance DECIMAL(38,2),

    hisab_completed BIT NOT NULL DEFAULT 0,

    CONSTRAINT pk_month_closing_detail
        PRIMARY KEY (id),

    CONSTRAINT fk_month_closing_detail_month_closing
        FOREIGN KEY (month_closing_id)
        REFERENCES month_closings(id),

    CONSTRAINT fk_month_closing_detail_employee
        FOREIGN KEY (employee_id)
        REFERENCES employees(id)

) ENGINE=InnoDB;



CREATE TABLE employee_settlements (
    id BIGINT NOT NULL AUTO_INCREMENT,

    employee_id BIGINT NOT NULL,
    owner_id BIGINT NOT NULL,

    amount_paid DECIMAL(38,2),
    settlement_date DATE,
    note VARCHAR(255),
    created_at DATETIME(6),

    CONSTRAINT pk_employee_settlements
        PRIMARY KEY (id),

    CONSTRAINT fk_employee_settlement_employee
        FOREIGN KEY (employee_id)
        REFERENCES employees(id),

    CONSTRAINT fk_settlement_owner
        FOREIGN KEY (owner_id)
        REFERENCES owners(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE

) ENGINE=InnoDB;

CREATE INDEX idx_settlement_owner_id
    ON employee_settlements(owner_id);

CREATE INDEX idx_settlement_owner_date
    ON employee_settlements(owner_id, settlement_date);



CREATE TABLE owner_login_otps (
    id BIGINT NOT NULL AUTO_INCREMENT,

    owner_id BIGINT NOT NULL,

    otp_hash VARCHAR(255) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    used BIT NOT NULL DEFAULT 0,
    attempts INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,

    CONSTRAINT pk_owner_login_otps
        PRIMARY KEY (id),

    CONSTRAINT fk_owner_login_otp_owner
        FOREIGN KEY (owner_id)
        REFERENCES owners(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE

) ENGINE=InnoDB;

CREATE INDEX idx_owner_login_otp_owner_id
    ON owner_login_otps(owner_id);

CREATE INDEX idx_owner_login_otp_owner_used_created
    ON owner_login_otps(owner_id, used, created_at);