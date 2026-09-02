package com.example.projectzest.service.impl;

import com.example.projectzest.service.AuditLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Fire-and-forget audit logging. Runs on the dedicated "auditExecutor" pool
 * (see AsyncConfig) so it never blocks or competes with request-handling
 * threads, and intentionally does no database writes here to avoid the
 * transaction/threading pitfalls of async DB access from a non-request thread.
 */
@Slf4j
@Service
public class AuditLogServiceImpl implements AuditLogService {

    @Async("auditExecutor")
    @Override
    public void logProductEvent(String action, Long productId, String performedBy) {
        log.info("AUDIT: action={} productId={} performedBy={}", action, productId, performedBy);
    }
}
