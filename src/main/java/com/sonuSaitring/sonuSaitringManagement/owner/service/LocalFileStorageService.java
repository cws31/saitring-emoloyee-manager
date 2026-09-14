package com.sonuSaitring.sonuSaitringManagement.owner.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.sonuSaitring.sonuSaitringManagement.common.exception.BadRequestException;
import com.sonuSaitring.sonuSaitringManagement.common.exception.ExternalServiceException;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Service
public class LocalFileStorageService implements FileStorageService {

        private static final Logger log = LoggerFactory.getLogger(LocalFileStorageService.class);

        private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

        private static final String LOGO_DIRECTORY = "organization-logos";

        private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
                        "image/jpeg",
                        "image/png",
                        "image/webp");

        private static final Map<String, String> CONTENT_TYPE_TO_EXTENSION = Map.of(
                        "image/jpeg", ".jpg",
                        "image/png", ".png",
                        "image/webp", ".webp");

        private static final Tika TIKA = new Tika();

        private final Path uploadDirectory;

        private final Counter uploadSuccessCounter;
        private final Counter uploadFailureCounter;
        private final Counter deleteSuccessCounter;
        private final Counter deleteFailureCounter;

        private final Timer uploadTimer;
        private final Timer deleteTimer;

        public LocalFileStorageService(
                        @Value("${file.storage.location:uploads}") String storageLocation,
                        MeterRegistry meterRegistry) {

                this.uploadDirectory = Paths
                                .get(storageLocation)
                                .toAbsolutePath()
                                .normalize();

                this.uploadSuccessCounter = Counter.builder(
                                "file.upload.success")
                                .description("Successful file uploads")
                                .tag("type", "organization_logo")
                                .register(meterRegistry);

                this.uploadFailureCounter = Counter.builder(
                                "file.upload.failure")
                                .description("Failed file uploads")
                                .tag("type", "organization_logo")
                                .register(meterRegistry);

                this.deleteSuccessCounter = Counter.builder(
                                "file.delete.success")
                                .description("Successful file deletions")
                                .tag("type", "organization_logo")
                                .register(meterRegistry);

                this.deleteFailureCounter = Counter.builder(
                                "file.delete.failure")
                                .description("Failed file deletions")
                                .tag("type", "organization_logo")
                                .register(meterRegistry);

                this.uploadTimer = Timer.builder(
                                "file.upload.duration")
                                .description("Time taken to upload files")
                                .tag("type", "organization_logo")
                                .register(meterRegistry);

                this.deleteTimer = Timer.builder(
                                "file.delete.duration")
                                .description("Time taken to delete files")
                                .tag("type", "organization_logo")
                                .register(meterRegistry);

                try {

                        Files.createDirectories(uploadDirectory);

                        log.info(
                                        "File storage initialized successfully");

                } catch (IOException e) {

                        log.error(
                                        "Unable to initialize file storage errorType={}",
                                        e.getClass().getSimpleName(),
                                        e);

                        throw new ExternalServiceException(
                                        "File storage is unavailable.");
                }
        }

        @Override
        public String uploadOrganizationLogo(
                        MultipartFile file) {

                long start = System.nanoTime();

                try {

                        log.info(
                                        "Organization logo upload started size={} contentType={}",
                                        file != null ? file.getSize() : 0,
                                        file != null ? file.getContentType() : "unknown");

                        String detectedContentType = validateLogo(file);

                        String extension = CONTENT_TYPE_TO_EXTENSION
                                        .get(detectedContentType);

                        String generatedFileName = UUID.randomUUID() + extension;

                        Path logoDirectory = uploadDirectory
                                        .resolve(LOGO_DIRECTORY)
                                        .normalize();

                        Path targetPath = logoDirectory
                                        .resolve(generatedFileName)
                                        .normalize();

                        if (!targetPath.startsWith(logoDirectory)) {

                                uploadFailureCounter.increment();

                                log.error(
                                                "File upload rejected because generated path escaped logo directory");

                                throw new BadRequestException(
                                                "Invalid file name.");
                        }

                        try {

                                Files.createDirectories(
                                                logoDirectory);

                                Files.copy(
                                                file.getInputStream(),
                                                targetPath,
                                                StandardCopyOption.REPLACE_EXISTING);

                                String fileUrl = "/uploads/"
                                                + LOGO_DIRECTORY
                                                + "/"
                                                + generatedFileName;

                                uploadSuccessCounter.increment();

                                log.info(
                                                "Organization logo uploaded successfully size={} detectedContentType={}",
                                                file.getSize(),
                                                detectedContentType);

                                return fileUrl;

                        } catch (IOException e) {

                                uploadFailureCounter.increment();

                                log.error(
                                                "Unable to store organization logo errorType={}",
                                                e.getClass().getSimpleName(),
                                                e);

                                throw new ExternalServiceException(
                                                "Unable to store organization logo.");
                        }

                } catch (BadRequestException ex) {

                        uploadFailureCounter.increment();

                        log.warn(
                                        "Organization logo upload rejected reason={}",
                                        ex.getMessage());

                        throw ex;

                } finally {

                        uploadTimer.record(
                                        System.nanoTime() - start,
                                        TimeUnit.NANOSECONDS);
                }
        }

        @Override
        public void delete(String fileUrl) {

                if (fileUrl == null ||
                                fileUrl.isBlank()) {

                        log.debug(
                                        "File deletion skipped because file URL was empty");

                        return;
                }

                long start = System.nanoTime();

                try {

                        String fileName = extractFileName(fileUrl);

                        Path logoDirectory = uploadDirectory
                                        .resolve(LOGO_DIRECTORY)
                                        .normalize();

                        Path filePath = logoDirectory
                                        .resolve(fileName)
                                        .normalize();

                        if (!filePath.startsWith(logoDirectory)) {

                                deleteFailureCounter.increment();

                                log.warn(
                                                "File deletion rejected because path escaped logo directory");

                                return;
                        }

                        boolean deleted = Files.deleteIfExists(filePath);

                        if (deleted) {

                                deleteSuccessCounter.increment();

                                log.info(
                                                "Organization logo deleted successfully");

                        } else {

                                log.debug(
                                                "Organization logo did not exist during deletion");
                        }

                } catch (BadRequestException ex) {

                        deleteFailureCounter.increment();

                        log.warn(
                                        "Organization logo deletion rejected reason={}",
                                        ex.getMessage());

                        throw ex;

                } catch (IOException e) {

                        deleteFailureCounter.increment();

                        log.error(
                                        "Unable to delete organization logo errorType={}",
                                        e.getClass().getSimpleName(),
                                        e);

                        throw new ExternalServiceException(
                                        "Unable to delete organization logo.");

                } finally {

                        deleteTimer.record(
                                        System.nanoTime() - start,
                                        TimeUnit.NANOSECONDS);
                }
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

                } catch (IOException e) {

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

        private String extractFileName(
                        String fileUrl) {

                int lastSlash = fileUrl.lastIndexOf('/');

                if (lastSlash < 0 ||
                                lastSlash == fileUrl.length() - 1) {

                        throw new BadRequestException(
                                        "Invalid file URL.");
                }

                String fileName = fileUrl.substring(lastSlash + 1);

                if (fileName.contains("..") ||
                                fileName.contains("/") ||
                                fileName.contains("\\") ||
                                fileName.contains(":")) {

                        throw new BadRequestException(
                                        "Invalid file URL.");
                }

                return fileName;
        }
}