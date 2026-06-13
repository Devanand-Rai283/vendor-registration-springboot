package com.streetvendor.common.audit;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TestAuditableEntityRepository extends JpaRepository<TestAuditableEntity, Long> {
}
