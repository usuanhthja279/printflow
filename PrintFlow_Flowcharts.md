# PrintFlow architecture and workflow diagrams

All diagrams are **Mermaid**, editable in Markdown (GitHub/Codex, VS Code Mermaid preview). They show target state unless labelled Phase 0.

## 1. System architecture

```mermaid
flowchart TB
   C[Public website / customer PWA]
   O[Owner and sales portal]
   E[Production operator PWA]
   V[External partner portal]
   C & O & E & V --> FE[Next.js / React]
   FE --> API[Spring Boot modular API]
   API --> CRM[CRM + Enquiry]
   API --> ORDER[Quote + Orders]
   API --> PROD[Production + Stock]
   API --> PAY[Finance + Delivery]
   API --> COLLAB[Collaboration + Approvals]
   CRM & ORDER & PROD & PAY & COLLAB --> DB[(PostgreSQL)]
   API --> OBJ[Private file storage]
   API --> OUTBOX[(Transactional outbox)]
   OUTBOX --> WORK[Integration workers]
   WORK --> CH[WhatsApp / email / social / phone / payments]
   OBJ --> PRE[Isolated Python PDF preflight]
```

## 2. Customer lifecycle and business swimlanes

```mermaid
flowchart LR
  subgraph CUSTOMER[Customer]
    A[Contact / enquiry] --> D[Approve quotation]
    D --> F[Upload / approve proof]
    F --> H[Pay advance or use credit]
    H --> N[Track progress]
    N --> R[Receive order]
  end
  subgraph BUSINESS[Sales, designer, owner]
    B[Capture lead] --> C[Configure and price]
    E[Prepare proofs] --> G[Confirm release]
    O[Notify customer] --> P[Invoice + dispatch]
  end
  subgraph FACTORY[Production]
    I[Prepress] --> J[Paper and plate issue]
    J --> K[Printing]
    K --> L[Finishing]
    L --> M[Quality check]
  end
  A --> B --> C --> D --> E --> F --> G
  H --> G --> I
  M --> O --> N
  M --> P --> R
```

## 3. Quote/proof order release rules

```mermaid
flowchart TD
  A[Enquiry] --> B[Quote v1]
  B --> C{Customer decision}
  C -->|Revision| D[New immutable quote version]
  D --> C
  C -->|Reject| X[Close lead]
  C -->|Accept| E[Order created]
  E --> F[Artwork upload / preflight]
  F --> G{Proof approved?}
  G -->|Changes| H[New artwork version]
  H --> F
  G -->|Approved| I{Payment / credit rule satisfied?}
  I -->|No| J[Collect advance / authorize credit]
  J --> I
  I -->|Yes| K{Materials / machine available?}
  K -->|No| L[Reserve / procure / reschedule]
  L --> K
  K -->|Yes| M[Release production job]
```

## 4. Phase 0 working API sequence

```mermaid
sequenceDiagram
 participant C as Customer browser
 participant N as Next.js proxy
 participant A as Spring API
 participant D as PostgreSQL
 participant O as Owner demo
 C->>N: POST /api/public/enquiries
 N->>A: Forward validated JSON
 A->>D: Insert enquiry + CREATED event
 A-->>N: publicId + random tracking token
 N-->>C: Display ID and token once
 O->>N: GET /api/demo/enquiries, X-Demo-Key
 N->>A: Forward request
 A->>D: Read latest 100 enquiries
 A-->>O: Enquiries (dev/QA only)
 O->>N: PATCH status (key)
 N->>A: PATCH /demo/enquiries/{id}/status
 A->>D: Transaction: status + event
 C->>N: GET /enquiries/{id}, X-Tracking-Token
 N->>A: Forward request
 A->>D: Check token digest, retrieve timeline
 A-->>C: Public-safe status/timeline
```

## 5. External outsourcing and work-sharing

```mermaid
flowchart TD
  A[Production manager] --> B[Choose operation to outsource]
  B --> C[Create limited-scope work package]
  C --> D[Invite approved vendor]
  D --> E{Vendor accepts?}
  E -->|No| D
  E -->|Yes| F[Agree cost / due date]
  F --> G[Release scoped files and purchase order]
  G --> H[Vendor progress + material ledger]
  H --> I[Return materials / output]
  I --> J{Internal QC passes?}
  J -->|No| K[Discrepancy or rework]
  K --> H
  J -->|Yes| L[Accept vendor deliverable]
```

## 6. Social and messaging integration

```mermaid
flowchart TD
  A[WhatsApp webhook] --> V[Webhook verification]
  B[Meta Lead Ads / supported inbox] --> V
  C[Email listener] --> V
  D[Phone provider / manual call log] --> V
  V --> N[Normalize event and deduplicate]
  N --> M[Match contact with confidence]
  M --> X{Verified identity match?}
  X -->|Yes| E[Attach to existing customer/order]
  X -->|No| F[Create unverified lead]
  E & F --> G[Unified inbox + assignment]
  G --> H[Human reviewed quote / response]
  H --> O[(Outbox)]
  O --> P[Provider API]
  P --> Q[Delivery status / retries / audit]
```

## 7. CI/CD and environments

```mermaid
flowchart LR
  A[Feature branch] --> B[Pull request]
  B --> C[Unit / lint / integration / security]
  C --> D{Checks passed?}
  D -->|No| A
  D -->|Yes| E[Immutable build artifact]
  E --> F[Shared DEV]
  F --> G[QA automatic E2E]
  G --> H[UAT owner sign-off]
  H --> I{Release approval?}
  I -->|No| A
  I -->|Yes| J[Production rollout]
  J --> K[Smoke, metrics, rollback decision]
```

## 8. Target ER relationships (high-level)

```mermaid
erDiagram
  CUSTOMER ||--o{ ENQUIRY : submits
  ENQUIRY ||--o{ QUOTE : produces
  QUOTE ||--o{ QUOTE_VERSION : revisions
  QUOTE_VERSION ||--o| ORDER : accepted_as
  ORDER ||--|{ ORDER_ITEM : contains
  ORDER_ITEM ||--o{ ARTWORK_VERSION : uploads
  ARTWORK_VERSION ||--o{ PROOF_APPROVAL : reviews
  ORDER_ITEM ||--o{ PRODUCTION_JOB : requires
  PRODUCTION_JOB ||--|{ PRODUCTION_OPERATION : progresses
  PRODUCTION_OPERATION ||--o{ SUBCONTRACT : may_outsource
  PRODUCTION_OPERATION ||--o{ MATERIAL_ISSUE : consumes
  ORDER ||--o{ PAYMENT : receives
  ORDER ||--o{ INVOICE : bills
  ORDER ||--o{ DELIVERY : dispatches
  ORDER ||--o{ COMMUNICATION : discussed_in
  ORDER ||--o{ TASK : work_tracking
  VENDOR ||--o{ SUBCONTRACT : fulfills
```

## 9. Phase 0 enquiry state machine

```mermaid
stateDiagram-v2
    [*] --> NEW: Customer submits
    NEW --> REVIEWING: Owner begins evaluation
    NEW --> QUOTED: Owner responds directly
    REVIEWING --> QUOTED: Quote prepared externally
    NEW --> CLOSED: Not proceeding
    REVIEWING --> CLOSED: Cancelled
    QUOTED --> CLOSED: Closed
    CLOSED --> [*]
```

**Phase 0 note:** `QUOTED` is manually set; the real quote module does not exist until Phase 2. The frontend and backend are deliberate thin slices rather than claims of production functionality.
