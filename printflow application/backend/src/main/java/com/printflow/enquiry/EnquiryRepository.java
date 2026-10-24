package com.printflow.enquiry;

import java.util.Optional;
import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;

public interface EnquiryRepository extends JpaRepository<Enquiry, UUID> {
    Optional<Enquiry> findByPublicId(UUID publicId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Enquiry e where e.id = :id")
    Optional<Enquiry> findForUpdate(UUID id);
    List<Enquiry> findTop100ByOrderByCreatedAtDesc();
    @Query("select e from Enquiry e where e.assignedStaffId is null or e.assignedStaffId = :staffId order by e.createdAt desc")
    List<Enquiry> findVisible(UUID staffId, Pageable pageable);
}
