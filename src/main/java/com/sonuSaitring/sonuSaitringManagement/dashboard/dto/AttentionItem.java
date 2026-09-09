package com.sonuSaitring.sonuSaitringManagement.dashboard.dto;

public record AttentionItem(
        String type,
        String message,
        long count,
        String severity) {
}