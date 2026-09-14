package com.sonuSaitring.sonuSaitringManagement.owner.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "owners")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Owner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String ownerName;

    @Column(nullable = false, length = 150)
    private String organizationName;

    /*
     * Primary login identifier when owner chooses email login.
     */
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    /*
     * Primary login identifier when owner chooses mobile login.
     * International format:
     * +919876543210
     */
    @Column(name = "mobile_number", nullable = false, unique = true, length = 20)
    private String mobileNumber;

    /*
     * Password is the first authentication factor.
     *
     * NEVER store the raw password.
     * It must always contain a BCrypt/PasswordEncoder hash.
     */
    @Column(nullable = false)
    private String password;

    /*
     * Legacy field.
     *
     * Logo is now stored in owner_logos.
     * This can be removed later after the database migration
     * is completed.
     */
    @Column(length = 1000)
    private String logoUrl;

    @Column(name = "address_line1", length = 255)
    private String addressLine1;

    @Column(name = "address_line2", length = 255)
    private String addressLine2;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(length = 100)
    private String country;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(length = 255)
    private String website;
}