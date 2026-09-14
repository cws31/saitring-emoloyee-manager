package com.sonuSaitring.sonuSaitringManagement.owner.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OwnerRegistrationResponse {

    private Long id;

    private String ownerName;

    private String organizationName;

    private String email;

    private String mobileNumber;

    private String logoUrl;
}