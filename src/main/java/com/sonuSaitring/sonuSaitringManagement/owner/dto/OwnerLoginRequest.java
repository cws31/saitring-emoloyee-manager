package com.sonuSaitring.sonuSaitringManagement.owner.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OwnerLoginRequest {

    @NotBlank(message = "Email or mobile number is required.")
    private String identifier;

    @NotBlank(message = "Password is required.")
    private String password;
}