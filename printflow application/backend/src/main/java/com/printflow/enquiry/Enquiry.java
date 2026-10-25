package com.printflow.enquiry;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "enquiries")
public class Enquiry {
    @Id public UUID id;
    @Column(name="public_id",nullable=false,unique=true) public UUID publicId;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name="tracking_token_sha256",nullable=false,length=64) public String trackingTokenSha256;
    @Column(name="customer_name",nullable=false,length=140) public String customerName;
    @Column(nullable=false,length=254) public String email;
    @Column(nullable=false,length=32) public String phone;
    @Column(name="product_code",nullable=false,length=32) public String productCode;
    @Column(nullable=false) public int quantity;
    @Column(length=60) public String size;
    @Column(name="paper_gsm") public Integer paperGsm;
    @Column(name="print_sides",length=24) public String printSides;
    @Column(length=80) public String finish;
    @Column(length=2000) public String notes;
    @Column(name="intended_use",length=240) public String intendedUse;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=24) public EnquiryStatus status;
    @Column(name="created_at",nullable=false) public Instant createdAt;
    @Column(name="updated_at",nullable=false) public Instant updatedAt;
    @Column(name="assigned_staff_id") public UUID assignedStaffId;
    @Column(nullable=false,length=24) public String source="WEBSITE";
    @Column(nullable=false,length=12) public String priority="NORMAL";
    @Column(name="next_action",length=500) public String nextAction;
    @Column(name="follow_up_at") public Instant followUpAt;
    @Column(name="customer_user_id") public UUID customerUserId;
    @Column(name="contact_id") public UUID contactId;
    @Column(name="template_revision") public Long templateRevision;
    @Column(name="template_snapshot",columnDefinition="text") public String templateSnapshot;
    @Column(name="privacy_version",length=32) public String privacyVersion;
    @Version @Column(nullable=false) public long revision;

    @Column(name="requested_date") public java.time.LocalDate requestedDate;
    @Column(name="closure_kind") public String closureKind;
    @Column(name="closure_message") public String closureMessage;
    protected Enquiry() { }
}
