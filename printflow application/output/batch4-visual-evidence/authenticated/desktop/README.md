# Batch 4 authenticated desktop journey

Captured from the local application with a synthetic customer, owner, designer, printer and delivery worker. Every image is an actual 1440×900 application screenshot from the Playwright journey. Synthetic credentials and tracking material were masked by the test. SMTP was disabled; no external mail or real payment was made.

The journey exercised enquiry intake, quotation, customer activation, design assignment, proof feedback and approval, advance/balance records, invoice preview, production hold/resume, print/finish/QC and rework, dispatch blocking until a scanned pickup image, transit, delivery, and the customer gallery.

## Review result

The test did not fully pass. It reached delivery and captured the customer photo gallery, then failed when the third, delivery-stage thumbnail displayed **Preview unavailable**. Pickup and transit thumbnails loaded. This is retained as a real UI defect in `34-customer-delivery-photo-gallery.png`; the accessibility snapshot is in `delivery-gallery-accessibility.txt`. The full-size delivery-photo preview and final email activity capture were not reached. Do not treat this package as full Batch 4 acceptance evidence.

The prior mobile journey also did not complete; its latest attempt stopped before owner MFA and only produced blank failure captures. Those files are intentionally not included. No authenticated mobile acceptance is claimed.

## Screenshot index

| Image | What it shows |
|---|---|
| `01-owner-enquiry-queue.png` | Owner intake queue and authenticated workspace. |
| `02-designer-routing-availability.png` | Automatic designer routing and availability. |
| `03-public-print-studio.png` – `05-enquiry-saved-with-reference.png` | Public quote request from guided print selection through saved enquiry. |
| `06-quotation-issued.png` – `08-quotation-pdf-preview.png` | Quotation issuance, customer activation and PDF preview. |
| `09-designer-assigned-automatically.png` – `13-revised-proof-approved.png` | Assignment, artwork feedback, revised proof and approval. |
| `14-advance-and-balance-agreement.png` – `16-production-assignment.png` | Payment agreements/records, invoice PDF and production assignment. |
| `17-owner-production-board.png` – `27-ready-for-dispatch.png` | Production queue, hold/resume, printing, finishing, QC/rework and READY. |
| `28-assigned-delivery-task.png` – `33-delivery-confirmed.png` | Delivery assignment, blocked dispatch, scanned evidence and delivery confirmation. |
| `34-customer-delivery-photo-gallery.png` | Customer evidence gallery; delivery thumbnail failure is visible. |

`journey-failure.json` records the exact final assertion failure. The temporary Playwright directory is not required to review these captures.
