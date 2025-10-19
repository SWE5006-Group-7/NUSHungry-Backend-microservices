package com.nushungry.reviewservice.enums;

public enum ReportStatus {
    PENDING("待处理"),
    APPROVED("已批准"),
    REJECTED("已驳回"),
    IGNORED("已忽略");

    private final String description;

    ReportStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
