package com.sonuSaitring.sonuSaitringManagement.owner.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OwnerProfileResponse {

    private Long id;

    private String ownerName;

    private String organizationName;

    private String email;

    private String mobileNumber;

    private String addressLine1;

    private String addressLine2;

    private String city;

    private String state;

    private String country;

    private String postalCode;

    private String website;

    private boolean logoExists;
}