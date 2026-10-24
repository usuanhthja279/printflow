package com.printflow.enquiry;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="enquiry_events")
public class EnquiryEvent {
    @Id public UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="enquiry_id") public Enquiry enquiry;
    @Column(name="event_type",nullable=false,length=40) public String eventType;
    @Column(name="from_status",length=24) public String fromStatus;
    @Column(name="to_status",nullable=false,length=24) public String toStatus;
    @Column(name="created_at",nullable=false) public Instant createdAt;
    protected EnquiryEvent() { }
    public static EnquiryEvent of(Enquiry enquiry, EnquiryStatus previous, EnquiryStatus next, String action) {
        EnquiryEvent event = new EnquiryEvent();
        event.id = UUID.randomUUID(); event.enquiry = enquiry; event.eventType = action;
        event.fromStatus = previous == null ? null : previous.name(); event.toStatus=next.name();
        event.createdAt=Instant.now(); return event;
    }
}
