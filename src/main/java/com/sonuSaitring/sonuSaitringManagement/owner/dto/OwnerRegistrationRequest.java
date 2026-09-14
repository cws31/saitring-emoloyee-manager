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

        @NotBlank(message = "Username is required.")
        @Size(min = 4, max = 50, message = "Username must be between 4 and 50 characters.")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username may contain only letters, numbers and underscore.")
        private String username;

        @NotBlank(message = "Password is required.")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters.")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Password must contain at least one letter and one number.")
        private String password;
}