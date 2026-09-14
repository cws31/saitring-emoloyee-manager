package com.sonuSaitring.sonuSaitringManagement.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.sonuSaitring.sonuSaitringManagement.common.exception.ResourceNotFoundException;
import com.sonuSaitring.sonuSaitringManagement.owner.entity.Owner;
import com.sonuSaitring.sonuSaitringManagement.owner.repository.OwnerRepository;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Service
public class CurrentOwnerService {

        private static final Logger log = LoggerFactory.getLogger(CurrentOwnerService.class);

        private final OwnerRepository ownerRepository;

        private final Counter authenticatedOwnerLookupCounter;
        private final Counter ownerNotFoundCounter;
        private final Counter unauthenticatedAccessCounter;

        public CurrentOwnerService(
                        OwnerRepository ownerRepository,
                        MeterRegistry meterRegistry) {

                this.ownerRepository = ownerRepository;

                this.authenticatedOwnerLookupCounter = Counter.builder(
                                "security.current_owner.lookup")
                                .description("Number of current owner lookups")
                                .register(meterRegistry);

                this.ownerNotFoundCounter = Counter.builder(
                                "security.current_owner.not_found")
                                .description("Number of current owner lookups where owner was not found")
                                .register(meterRegistry);

                this.unauthenticatedAccessCounter = Counter.builder(
                                "security.current_owner.unauthenticated")
                                .description("Number of attempts to access current owner without authentication")
                                .register(meterRegistry);
        }

        public Long getCurrentOwnerId() {

                Authentication authentication = SecurityContextHolder
                                .getContext()
                                .getAuthentication();

                if (authentication == null ||
                                !authentication.isAuthenticated()) {

                        unauthenticatedAccessCounter.increment();

                        log.warn("Attempt to access current owner without authentication");

                        throw new IllegalStateException(
                                        "No authenticated owner found.");
                }

                Long ownerId;

                try {

                        ownerId = Long.parseLong(
                                        authentication.getPrincipal().toString());

                } catch (NumberFormatException ex) {

                        log.error(
                                        "Authenticated principal contains invalid owner identifier");

                        throw new IllegalStateException(
                                        "Invalid authenticated owner.");
                }

                return ownerId;
        }

        public Owner getCurrentOwner() {

                Long ownerId = getCurrentOwnerId();

                authenticatedOwnerLookupCounter.increment();

                return ownerRepository
                                .findById(ownerId)
                                .orElseThrow(() -> {

                                        ownerNotFoundCounter.increment();

                                        log.error(
                                                        "Authenticated owner was not found ownerId={}",
                                                        ownerId);

                                        return new ResourceNotFoundException(
                                                        "Owner not found with id: " + ownerId);
                                });
        }
}