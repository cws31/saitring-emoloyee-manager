package com.sonuSaitring.sonuSaitringManagement.owner.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OwnerRegistrationRequest {

        @NotBlank(message = "Owner name is required.")
        @Size(min = 2, max = 100, message = "Owner name must be between 2 and 100 characters.")
        private String ownerName;

        @NotBlank(message = "Organization name is required.")
        @Size(min = 2, max = 150, message = "Organization name must be between 2 and 150 characters.")
        private String organizationName;

        @NotBlank(message = "Email is required.")
        @Email(message = "Please provide a valid email address.")
        @Size(max = 150, message = "Email must not exceed 150 characters.")
        private String email;

        @NotBlank(message = "Mobile number is required.")
        @Pattern(regexp = "^\\+[1-9]\\d{7,14}$", message = "Mobile number must be in international format, for example +919876543210.")
        private String mobileNumber;

        @NotBlank(message = "Password is required.")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters.")
        private String password;
}