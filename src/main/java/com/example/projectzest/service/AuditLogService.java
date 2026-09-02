package com.example.projectzest.service;

/**
 * Records product mutation events. Implemented asynchronously (see impl)
 * because writing an audit trail entry is not something the caller needs
 * to wait for - it must never slow down or fail the actual CRUD response.
 */
public interface AuditLogService {
    void logProductEvent(String action, Long productId, String performedBy);
}
