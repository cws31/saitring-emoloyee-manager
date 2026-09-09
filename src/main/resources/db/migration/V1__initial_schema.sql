-- V1__initial_schema.sql
-- Initial schema for Sonu Saitring Management
-- Generated from the JPA entities in the supplied project source.

CREATE TABLE admin_users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    otp VARCHAR(255),
    otp_generated_time DATETIME(6),
    CONSTRAINT pk_admin_users PRIMARY KEY (id),
    CONSTRAINT uk_admin_users_username UNIQUE (username),
    CONSTRAINT uk_admin_users_email UNIQUE (email)
) ENGINE=InnoDB;

CREATE TABLE employees (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    mobile VARCHAR(15) NOT NULL,
    initial_rate DECIMAL(10,2) NOT NULL,
    blocked BIT NOT NULL,
    created_at DATETIME(6),
    CONSTRAINT pk_employees PRIMARY KEY (id),
    CONSTRAINT uk_employees_mobile UNIQUE (mobile)
) ENGINE=InnoDB;

CREATE TABLE employee_advances (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    amount DOUBLE NOT NULL,
    payment_date DATE NOT NULL,
    note VARCHAR(255),
    CONSTRAINT pk_employee_advances PRIMARY KEY (id),
    CONSTRAINT fk_employee_advances_employee
        FOREIGN KEY (employee_id) REFERENCES employees (id)
) ENGINE=InnoDB;

CREATE TABLE attendances (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    attendance_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6),
    reason VARCHAR(255),
    CONSTRAINT pk_attendances PRIMARY KEY (id),
    CONSTRAINT uk_attendances_employee_date
        UNIQUE (employee_id, attendance_date),
    CONSTRAINT fk_attendances_employee
        FOREIGN KEY (employee_id) REFERENCES employees (id)
) ENGINE=InnoDB;

CREATE TABLE month_closings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    year INT NOT NULL,
    month INT NOT NULL,
    closed_at DATETIME(6),
    CONSTRAINT pk_month_closings PRIMARY KEY (id)
) ENGINE=InnoDB;

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
    hisab_completed BIT NOT NULL,
    CONSTRAINT pk_month_closing_detail PRIMARY KEY (id),
    CONSTRAINT fk_month_closing_detail_month_closing
        FOREIGN KEY (month_closing_id) REFERENCES month_closings (id),
    CONSTRAINT fk_month_closing_detail_employee
        FOREIGN KEY (employee_id) REFERENCES employees (id)
) ENGINE=InnoDB;

CREATE TABLE employee_settlements (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    amount_paid DECIMAL(38,2),
    settlement_date DATE,
    note VARCHAR(255),
    created_at DATETIME(6),
    CONSTRAINT pk_employee_settlements PRIMARY KEY (id),
    CONSTRAINT fk_employee_settlements_employee
        FOREIGN KEY (employee_id) REFERENCES employees (id)
) ENGINE=InnoDB;
