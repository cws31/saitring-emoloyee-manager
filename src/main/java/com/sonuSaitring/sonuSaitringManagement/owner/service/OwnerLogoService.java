package com.sonuSaitring.sonuSaitringManagement.owner.service;

import org.springframework.web.multipart.MultipartFile;

import com.sonuSaitring.sonuSaitringManagement.owner.entity.OwnerLogo;

public interface OwnerLogoService {

    void saveLogo(Long ownerId, MultipartFile file);

    OwnerLogo getLogo(Long ownerId);

    void deleteLogo(Long ownerId);

    boolean exists(Long ownerId);
}