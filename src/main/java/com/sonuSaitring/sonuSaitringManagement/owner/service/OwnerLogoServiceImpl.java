package com.sonuSaitring.sonuSaitringManagement.owner.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.sonuSaitring.sonuSaitringManagement.common.exception.BadRequestException;
import com.sonuSaitring.sonuSaitringManagement.common.exception.ExternalServiceException;
import com.sonuSaitring.sonuSaitringManagement.owner.entity.OwnerLogo;
import com.sonuSaitring.sonuSaitringManagement.owner.repository.OwnerLogoRepository;

@Service
public class OwnerLogoServiceImpl implements OwnerLogoService {

    private static final Logger log = LoggerFactory.getLogger(OwnerLogoServiceImpl.class);

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp");

    private static final Map<String, String> CONTENT_TYPE_TO_EXTENSION = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp");

    private static final Tika TIKA = new Tika();

    private final OwnerLogoRepository ownerLogoRepository;

    public OwnerLogoServiceImpl(
            OwnerLogoRepository ownerLogoRepository) {

        this.ownerLogoRepository = ownerLogoRepository;
    }

    @Override
    @Transactional
    public void saveLogo(
            Long ownerId,
            MultipartFile file) {

        long start = System.nanoTime();

        try {

            String contentType = validateLogo(file);

            byte[] logoData;

            try {
                logoData = file.getBytes();
            } catch (IOException ex) {

                log.error(
                        "Unable to read owner logo ownerId={} errorType={}",
                        ownerId,
                        ex.getClass().getSimpleName(),
                        ex);

                throw new ExternalServiceException(
                        "Unable to process organization logo.");
            }

            OwnerLogo logo = ownerLogoRepository
                    .findByOwnerId(ownerId)
                    .orElseGet(OwnerLogo::new);

            LocalDateTime now = LocalDateTime.now();

            logo.setOwnerId(ownerId);
            logo.setLogoData(logoData);
            logo.setContentType(contentType);

            if (logo.getCreatedAt() == null) {
                logo.setCreatedAt(now);
            }

            logo.setUpdatedAt(now);

            ownerLogoRepository.save(logo);

            log.info(
                    "Organization logo saved successfully ownerId={} size={} contentType={}",
                    ownerId,
                    file.getSize(),
                    contentType);

        } finally {

            log.debug(
                    "Organization logo processing completed ownerId={} durationMs={}",
                    ownerId,
                    (System.nanoTime() - start) / 1_000_000);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public OwnerLogo getLogo(Long ownerId) {

        return ownerLogoRepository
                .findByOwnerId(ownerId)
                .orElseThrow(() -> new BadRequestException(
                        "Organization logo not found."));
    }

    @Override
    @Transactional
    public void deleteLogo(Long ownerId) {

        ownerLogoRepository.deleteByOwnerId(ownerId);

        log.info(
                "Organization logo deleted ownerId={}",
                ownerId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(Long ownerId) {

        return ownerLogoRepository
                .findByOwnerId(ownerId)
                .isPresent();
    }

    private String validateLogo(
            MultipartFile file) {

        if (file == null || file.isEmpty()) {

            throw new BadRequestException(
                    "Organization logo is required.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {

            throw new BadRequestException(
                    "Organization logo must not exceed 5 MB.");
        }

        String originalFilename = file.getOriginalFilename();

        if (originalFilename == null ||
                originalFilename.isBlank()) {

            throw new BadRequestException(
                    "Invalid logo file name.");
        }

        String detectedContentType;

        try (InputStream inputStream = file.getInputStream()) {

            detectedContentType = TIKA.detect(inputStream);

        } catch (IOException ex) {

            throw new BadRequestException(
                    "Unable to validate uploaded logo.");
        }

        if (!ALLOWED_CONTENT_TYPES
                .contains(detectedContentType)) {

            throw new BadRequestException(
                    "Only JPEG, PNG and WebP images are allowed.");
        }

        if (!CONTENT_TYPE_TO_EXTENSION
                .containsKey(detectedContentType)) {

            throw new BadRequestException(
                    "Unsupported image format.");
        }

        return detectedContentType;
    }
}