package com.sonuSaitring.sonuSaitringManagement.owner.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    String uploadOrganizationLogo(MultipartFile file);

    void delete(String fileUrl);
}