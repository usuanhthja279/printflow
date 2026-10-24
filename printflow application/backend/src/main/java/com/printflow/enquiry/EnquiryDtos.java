package com.printflow.enquiry;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class EnquiryDtos {
    private EnquiryDtos() {}
    public record CreateEnquiry(
        @NotBlank @Size(max=140) String customerName,
        @NotBlank @Email @Size(max=254) String email,
        @NotBlank @Pattern(regexp="^[+0-9() .-]{7,32}$") String phone,
        @NotBlank String productCode,
        @NotNull @Min(1) @Max(10000000) Integer quantity,
        @Size(max=60) String size,
        @Min(40) @Max(600) Integer paperGsm,
        @Size(max=24) String printSides,
        @Size(max=80) String finish,
        @Size(max=2000) String notes,
        Boolean privacyConsent,@Size(max=32) String privacyVersion,@Size(max=200) String website,@Size(max=2048) String challengeToken,java.time.LocalDate requestedDate
    ) {
        public CreateEnquiry(String n,String e,String p,String c,Integer q,String s,Integer g,String sides,String f,String notes,Boolean consent,String privacy,String website,String challenge){this(n,e,p,c,q,s,g,sides,f,notes,consent,privacy,website,challenge,null);}
        public CreateEnquiry(String name,String email,String phone,String product,Integer quantity,String size,Integer gsm,String sides,String finish,String notes){this(name,email,phone,product,quantity,size,gsm,sides,finish,notes,null,null,null,null,null);}
    }
    public record CreatedEnquiry(UUID publicId, String trackingToken, String status, Instant createdAt) {}
    public record EventView(String status, Instant at) {}
    public record TrackingView(UUID publicId, String productCode, int quantity, String status,
                               Instant createdAt, List<EventView> events,String closureKind,String closureMessage) {}
    public record DemoEnquiry(UUID id, UUID publicId, String customerName, String email, String phone,
        String productCode, int quantity, String size, Integer paperGsm, String printSides,
        String finish, String notes, String status, Instant createdAt) {}
    public record UpdateStatus(@NotNull EnquiryStatus status) {}
}
