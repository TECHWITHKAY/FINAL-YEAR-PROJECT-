package com.ghana.commoditymonitor.service;

import com.ghana.commoditymonitor.dto.request.PriceRecordApprovalDto;
import com.ghana.commoditymonitor.dto.request.PriceRecordRequestDto;
import com.ghana.commoditymonitor.dto.response.PendingSubmissionResponseDto;
import com.ghana.commoditymonitor.dto.response.PriceRecordResponseDto;
import com.ghana.commoditymonitor.entity.*;
import com.ghana.commoditymonitor.enums.PriceRecordStatus;
import com.ghana.commoditymonitor.exception.BusinessRuleException;
import com.ghana.commoditymonitor.exception.ResourceNotFoundException;
import com.ghana.commoditymonitor.exception.ValidationException;
import com.ghana.commoditymonitor.repository.*;
import com.ghana.commoditymonitor.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PriceRecordService {

    private final PriceRecordRepository priceRecordRepository;
    private final CommodityRepository commodityRepository;
    private final MarketRepository marketRepository;
    private final UserRepository userRepository;
    private final PriceRecordAuditRepository auditRepository;

    @Transactional
    public PriceRecordResponseDto createPriceRecord(PriceRecordRequestDto request, UserPrincipal submitter) {
        log.info("Creating new price record for commodity: {} in market: {} by user: {}", 
                 request.commodityId(), request.marketId(), submitter.username());

        Commodity commodity = commodityRepository.findById(request.commodityId())
                .orElseThrow(() -> new ResourceNotFoundException("Commodity", "id", request.commodityId()));

        Market market = marketRepository.findById(request.marketId())
                .orElseThrow(() -> new ResourceNotFoundException("Market", "id", request.marketId()));

        User submitterUser = userRepository.findById(submitter.id())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", submitter.id()));

        PriceRecord priceRecord = PriceRecord.builder()
                .commodity(commodity)
                .market(market)
                .price(request.price())
                .recordedDate(request.recordedDate())
                .source(request.source())
                .submittedBy(submitterUser)
                .build();

        if (submitter.isAdmin()) {
            priceRecord.setStatus(PriceRecordStatus.APPROVED);
            priceRecord.setReviewedBy(submitterUser);
            priceRecord.setReviewedAt(OffsetDateTime.now());
        } else if (submitter.isFieldAgent()) {
            priceRecord.setStatus(PriceRecordStatus.PENDING);
        } else {
            priceRecord.setStatus(PriceRecordStatus.PENDING);
        }

        PriceRecord savedRecord = priceRecordRepository.save(priceRecord);

        PriceRecordAudit audit = PriceRecordAudit.builder()
                .priceRecord(savedRecord)
                .action("SUBMITTED")
                .performedBy(submitterUser)
                .newPrice(request.price())
                .build();
        auditRepository.save(audit);

        return mapToResponse(savedRecord);
    }

    @Transactional
    public PriceRecordResponseDto approvePriceRecord(Long id, PriceRecordApprovalDto dto, UserPrincipal reviewer) {
        log.info("Reviewing price record {} by user: {}", id, reviewer.username());

        PriceRecord priceRecord = priceRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PriceRecord", "id", id));

        if (priceRecord.getStatus() != PriceRecordStatus.PENDING) {
            throw new BusinessRuleException("Only PENDING records can be reviewed");
        }

        User reviewerUser = userRepository.findById(reviewer.id())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", reviewer.id()));

        if (dto.approved()) {
            priceRecord.setStatus(PriceRecordStatus.APPROVED);
            priceRecord.setReviewedBy(reviewerUser);
            priceRecord.setReviewedAt(OffsetDateTime.now());

            PriceRecordAudit audit = PriceRecordAudit.builder()
                    .priceRecord(priceRecord)
                    .action("APPROVED")
                    .performedBy(reviewerUser)
                    .build();
            auditRepository.save(audit);
        } else {
            if (!StringUtils.hasText(dto.rejectionReason())) {
                throw new ValidationException("Rejection reason is required when rejecting a price record");
            }

            priceRecord.setStatus(PriceRecordStatus.REJECTED);
            priceRecord.setRejectionReason(dto.rejectionReason());
            priceRecord.setReviewedBy(reviewerUser);
            priceRecord.setReviewedAt(OffsetDateTime.now());

            PriceRecordAudit audit = PriceRecordAudit.builder()
                    .priceRecord(priceRecord)
                    .action("REJECTED")
                    .performedBy(reviewerUser)
                    .note(dto.rejectionReason())
                    .build();
            auditRepository.save(audit);
        }

        PriceRecord updatedRecord = priceRecordRepository.save(priceRecord);
        return mapToResponse(updatedRecord);
    }

    public List<PendingSubmissionResponseDto> getPendingRecords() {
        log.debug("Fetching all pending price records");
        List<PriceRecord> pendingRecords = priceRecordRepository.findByStatus(PriceRecordStatus.PENDING);
        
        return pendingRecords.stream()
                .map(this::mapToPendingResponse)
                .collect(Collectors.toList());
    }

    public List<PriceRecordResponseDto> getMySubmissions(Long userId) {
        log.debug("Fetching submissions for user: {}", userId);
        List<PriceRecord> submissions = priceRecordRepository.findBySubmittedByIdAndStatus(userId, PriceRecordStatus.PENDING);
        
        return submissions.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public PriceRecordResponseDto getPriceRecordById(Long id) {
        log.debug("Fetching price record with id: {}", id);
        return priceRecordRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("PriceRecord", "id", id));
    }

    public Page<PriceRecordResponseDto> getAllPriceRecords(Pageable pageable) {
        log.debug("Fetching all price records with pagination");
        return priceRecordRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    public List<PriceRecordResponseDto> getPriceRecordsByCommodity(Long commodityId) {
        log.debug("Fetching price records for commodity id: {}", commodityId);
        return priceRecordRepository.findByCommodityId(commodityId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<PriceRecordResponseDto> getPriceRecordsByMarket(Long marketId) {
        log.debug("Fetching price records for market id: {}", marketId);
        return priceRecordRepository.findByMarketId(marketId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public PriceRecordResponseDto updatePriceRecord(Long id, PriceRecordRequestDto request) {
        log.info("Updating price record with id: {}", id);
        PriceRecord priceRecord = priceRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PriceRecord", "id", id));

        Commodity commodity = commodityRepository.findById(request.commodityId())
                .orElseThrow(() -> new ResourceNotFoundException("Commodity", "id", request.commodityId()));

        Market market = marketRepository.findById(request.marketId())
                .orElseThrow(() -> new ResourceNotFoundException("Market", "id", request.marketId()));

        priceRecord.setCommodity(commodity);
        priceRecord.setMarket(market);
        priceRecord.setPrice(request.price());
        priceRecord.setRecordedDate(request.recordedDate());
        priceRecord.setSource(request.source());

        PriceRecord updatedRecord = priceRecordRepository.save(priceRecord);
        return mapToResponse(updatedRecord);
    }

    @Transactional
    public void deletePriceRecord(Long id) {
        log.info("Deleting price record with id: {}", id);
        if (!priceRecordRepository.existsById(id)) {
            throw new ResourceNotFoundException("PriceRecord", "id", id);
        }
        priceRecordRepository.deleteById(id);
    }

    private PriceRecordResponseDto mapToResponse(PriceRecord record) {
        return PriceRecordResponseDto.builder()
                .id(record.getId())
                .commodityId(record.getCommodity().getId())
                .commodityName(record.getCommodity().getName())
                .marketId(record.getMarket().getId())
                .marketName(record.getMarket().getName())
                .cityName(record.getMarket().getCity().getName())
                .price(record.getPrice())
                .recordedDate(record.getRecordedDate())
                .source(record.getSource())
                .status(record.getStatus() != null ? record.getStatus().name() : null)
                .submittedByUsername(record.getSubmittedBy() != null ? record.getSubmittedBy().getUsername() : null)
                .reviewedByUsername(record.getReviewedBy() != null ? record.getReviewedBy().getUsername() : null)
                .reviewedAt(record.getReviewedAt())
                .rejectionReason(record.getRejectionReason())
                .createdAt(record.getCreatedAt())
                .build();
    }

    private PendingSubmissionResponseDto mapToPendingResponse(PriceRecord record) {
        long daysPending = ChronoUnit.DAYS.between(record.getCreatedAt(), OffsetDateTime.now());
        
        return PendingSubmissionResponseDto.builder()
                .id(record.getId())
                .commodityId(record.getCommodity().getId())
                .commodityName(record.getCommodity().getName())
                .marketId(record.getMarket().getId())
                .marketName(record.getMarket().getName())
                .cityName(record.getMarket().getCity().getName())
                .price(record.getPrice())
                .recordedDate(record.getRecordedDate())
                .source(record.getSource())
                .status(record.getStatus().name())
                .submittedByUsername(record.getSubmittedBy() != null ? record.getSubmittedBy().getUsername() : null)
                .daysPending(daysPending)
                .createdAt(record.getCreatedAt())
                .build();
    }
}
