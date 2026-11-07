package com.lifelink.blood_donation.Services;

import com.lifelink.blood_donation.Entities.AuditLog;
import com.lifelink.blood_donation.Entities.Enums.AuditAction;
import com.lifelink.blood_donation.Entities.User;
import com.lifelink.blood_donation.Repositories.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Single writer of the audit trail. Call logAction(...) from any admin action that changes data. */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void logAction(User actor, AuditAction action, String targetType, Long targetId, String details) {
        String safeDetails = details == null ? null
                : (details.length() > 500 ? details.substring(0, 497) + "..." : details);
        auditLogRepository.save(AuditLog.builder()
                .actorId(actor.getId())
                .actorName(actor.getFullName())
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .details(safeDetails)
                .build());
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getPage(AuditAction action, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), size);
        return action == null
                ? auditLogRepository.findAllByOrderByCreatedAtDesc(pageable)
                : auditLogRepository.findByActionOrderByCreatedAtDesc(action, pageable);
    }
}
