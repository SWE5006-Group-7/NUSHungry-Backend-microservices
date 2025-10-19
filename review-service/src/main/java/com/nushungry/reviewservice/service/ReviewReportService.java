package com.nushungry.reviewservice.service;

import com.nushungry.reviewservice.document.ReviewReportDocument;
import com.nushungry.reviewservice.dto.CreateReportRequest;
import com.nushungry.reviewservice.dto.HandleReportRequest;
import com.nushungry.reviewservice.dto.ReportResponse;
import com.nushungry.reviewservice.enums.ReportStatus;
import com.nushungry.reviewservice.exception.ResourceNotFoundException;
import com.nushungry.reviewservice.exception.ValidationException;
import com.nushungry.reviewservice.repository.ReviewReportRepository;
import com.nushungry.reviewservice.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewReportService {

    private final ReviewReportRepository reviewReportRepository;
    private final ReviewRepository reviewRepository;

    @Transactional
    public ReportResponse createReport(String reviewId, CreateReportRequest request, String reporterId, String reporterName) {
        log.info("Creating report for review ID: {} by user: {}", reviewId, reporterId);

        if (!reviewRepository.existsById(reviewId)) {
            throw new ResourceNotFoundException("Review", "id", reviewId);
        }

        if (reviewReportRepository.existsByReviewIdAndReporterId(reviewId, reporterId)) {
            throw new ValidationException("You have already reported this review");
        }

        ReviewReportDocument report = ReviewReportDocument.builder()
                .reviewId(reviewId)
                .reporterId(reporterId)
                .reporterName(reporterName)
                .reason(request.getReason())
                .description(request.getDescription())
                .status(ReportStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        ReviewReportDocument savedReport = reviewReportRepository.save(report);
        log.info("Report created with ID: {}", savedReport.getId());

        return mapToResponse(savedReport);
    }

    public List<ReportResponse> getReportsByReviewId(String reviewId) {
        log.info("Getting reports for review ID: {}", reviewId);
        List<ReviewReportDocument> reports = reviewReportRepository.findByReviewId(reviewId);
        return reports.stream().map(this::mapToResponse).toList();
    }

    public Page<ReportResponse> getReportsByStatus(ReportStatus status, Pageable pageable) {
        log.info("Getting reports by status: {}", status);
        Page<ReviewReportDocument> reports = reviewReportRepository.findByStatus(status, pageable);
        return reports.map(this::mapToResponse);
    }

    @Transactional
    public ReportResponse handleReport(String reportId, HandleReportRequest request, String handlerUserId) {
        log.info("Handling report ID: {} by user: {}", reportId, handlerUserId);

        ReviewReportDocument report = reviewReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report", "id", reportId));

        report.setStatus(request.getStatus());
        report.setHandledBy(handlerUserId);
        report.setHandledAt(LocalDateTime.now());
        report.setHandleNote(request.getHandleNote());

        ReviewReportDocument updatedReport = reviewReportRepository.save(report);
        log.info("Report handled successfully");

        return mapToResponse(updatedReport);
    }

    private ReportResponse mapToResponse(ReviewReportDocument document) {
        return ReportResponse.builder()
                .id(document.getId())
                .reviewId(document.getReviewId())
                .reporterId(document.getReporterId())
                .reporterName(document.getReporterName())
                .reason(document.getReason())
                .description(document.getDescription())
                .status(document.getStatus())
                .handledBy(document.getHandledBy())
                .handledAt(document.getHandledAt())
                .handleNote(document.getHandleNote())
                .createdAt(document.getCreatedAt())
                .build();
    }
}
