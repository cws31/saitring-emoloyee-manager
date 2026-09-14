package com.sonuSaitring.sonuSaitringManagement.owner.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OwnerLoginResponse {

    private boolean otpRequired;

    private String message;

    private String token;

    private String tokenType;

    private Long ownerId;

    private String ownerName;

    private String organizationName;

    private String username;

    private String logoUrl;
}