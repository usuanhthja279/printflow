package com.printflow.enquiry;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnquiryEventRepository extends JpaRepository<EnquiryEvent, UUID> {
    List<EnquiryEvent> findByEnquiryIdOrderByCreatedAtAsc(UUID enquiryId);
}
