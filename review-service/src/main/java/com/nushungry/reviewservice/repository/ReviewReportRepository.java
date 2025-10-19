package com.nushungry.reviewservice.repository;

import com.nushungry.reviewservice.document.ReviewReportDocument;
import com.nushungry.reviewservice.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewReportRepository extends MongoRepository<ReviewReportDocument, String> {

    List<ReviewReportDocument> findByReviewId(String reviewId);

    Page<ReviewReportDocument> findByStatus(ReportStatus status, Pageable pageable);

    boolean existsByReviewIdAndReporterId(String reviewId, String reporterId);

    long countByReviewIdAndStatus(String reviewId, ReportStatus status);
}
