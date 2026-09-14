
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

import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.sonuSaitring.sonuSaitringManagement.common.exception.BadRequestException;
import com.sonuSaitring.sonuSaitringManagement.common.exception.ExternalServiceException;

@Service
public class LocalFileStorageService implements FileStorageService {

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

        public LocalFileStorageService(
                        @Value("${file.storage.location:uploads}") String storageLocation) {

                this.uploadDirectory = Paths
                                .get(storageLocation)
                                .toAbsolutePath()
                                .normalize();

                try {

                        Files.createDirectories(uploadDirectory);

                } catch (IOException e) {

                        throw new ExternalServiceException(
                                        "File storage is unavailable.");
                }
        }

        @Override
        public String uploadOrganizationLogo(
                        MultipartFile file) {

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

                        throw new BadRequestException(
                                        "Invalid file name.");
                }

                try {

                        Files.createDirectories(logoDirectory);

                        Files.copy(
                                        file.getInputStream(),
                                        targetPath,
                                        StandardCopyOption.REPLACE_EXISTING);

                        return "/uploads/"
                                        + LOGO_DIRECTORY
                                        + "/"
                                        + generatedFileName;

                } catch (IOException e) {

                        throw new ExternalServiceException(
                                        "Unable to store organization logo.");
                }
        }

        @Override
        public void delete(String fileUrl) {

                if (fileUrl == null
                                || fileUrl.isBlank()) {

                        return;
                }

                try {

                        String fileName = extractFileName(fileUrl);

                        Path logoDirectory = uploadDirectory
                                        .resolve(LOGO_DIRECTORY)
                                        .normalize();

                        Path filePath = logoDirectory
                                        .resolve(fileName)
                                        .normalize();

                        if (!filePath.startsWith(logoDirectory)) {
                                return;
                        }

                        Files.deleteIfExists(filePath);

                } catch (IOException e) {

                        throw new ExternalServiceException(
                                        "Unable to delete organization logo.");
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

                if (originalFilename == null
                                || originalFilename.isBlank()) {

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

                if (lastSlash < 0
                                || lastSlash == fileUrl.length() - 1) {

                        throw new BadRequestException(
                                        "Invalid file URL.");
                }

                String fileName = fileUrl.substring(lastSlash + 1);

                if (fileName.contains("..")
                                || fileName.contains("/")
                                || fileName.contains("\\")
                                || fileName.contains(":")) {

                        throw new BadRequestException(
                                        "Invalid file URL.");
                }

                return fileName;
        }
}
