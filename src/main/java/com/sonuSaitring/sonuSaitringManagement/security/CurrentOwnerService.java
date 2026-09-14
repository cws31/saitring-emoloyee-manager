package com.sonuSaitring.sonuSaitringManagement.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.sonuSaitring.sonuSaitringManagement.common.exception.ResourceNotFoundException;
import com.sonuSaitring.sonuSaitringManagement.owner.entity.Owner;
import com.sonuSaitring.sonuSaitringManagement.owner.repository.OwnerRepository;

@Service
public class CurrentOwnerService {

    private final OwnerRepository ownerRepository;

    public CurrentOwnerService(
            OwnerRepository ownerRepository) {
        this.ownerRepository = ownerRepository;
    }

    public Long getCurrentOwnerId() {

        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            throw new IllegalStateException(
                    "No authenticated owner found.");
        }

        return Long.parseLong(
                authentication.getPrincipal().toString());
    }

    public Owner getCurrentOwner() {

        Long ownerId = getCurrentOwnerId();

        return ownerRepository
                .findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Owner not found with id: " + ownerId));
    }
}