# Batch 4 visual evidence

These are actual Playwright screenshots from `frontend/e2e/workflow-redesign.spec.ts`, captured against the local UI using synthetic API fixtures. They are not evidence of live payments or customer data.

The 1008 collaboration-revocation screenshots come from `frontend/e2e/batch3-designer-workspace.spec.ts`; they show the same project access revocation state for context during this Batch 4 review.

| Screenshot | View/state | Viewport | Why it matters |
|---|---|---:|---|
| `customer-work-overview-desktop.png` | Customer work overview | 1440 × 900 | Shows ordered work stages, actionable next step and right-side conversation. |
| `customer-work-overview-mobile.png` | Customer work overview | 390 × 844 | Confirms narrow-screen project navigation and overview fit. |
| `customer-finance-readiness-desktop.png` | Customer finance blocked | 1440 × 900 | Shows quotation-bound advance terms, current balance, and invoice/advance blockers without owner controls. |
| `customer-finance-readiness-mobile.png` | Customer finance blocked | 390 × 844 | Checks readable stacked finance readiness cards on phone. |
| `owner-dispatch-and-delivery-readiness-desktop.png` | Owner dispatch rejected | 1440 × 900 | Shows server rejection for missing clean pickup evidence and preserves the entered note. |
| `owner-dispatch-and-delivery-readiness-mobile.png` | Owner dispatch rejected | 390 × 844 | Checks the evidence gate and confirmation control at mobile size. |
| `owner-fulfilment-stepper-desktop.png` | Owner production job drawer | 1440 × 900 | Shows the ordered seven-step fulfilment path and current pickup-evidence step. |
| `owner-fulfilment-stepper-mobile.png` | Owner production job drawer | 390 × 844 | Shows the same stage order in the responsive drawer. |
| `secure-original-photo-preview-desktop.png` | Private pickup-photo preview | 1440 × 900 | Confirms the secure photo opens in a full-screen preview without cropping; fixture image is synthetic. |
| `secure-original-photo-preview-mobile.png` | Private pickup-photo preview | 390 × 844 | Confirms the image and caption fit the mobile preview. |
| `collaboration-revoked-desktop.png` | Designer proof studio after project access revocation | 1440 × 900 | Shows the stale participant and cursor are removed after access is revoked. |
| `collaboration-revoked-mobile.png` | Designer proof studio after project access revocation | 390 × 844 | Confirms the revoked collaboration state at phone size. |

## Visual inspection notes

- No horizontal body overflow appeared in the phone runs.
- Finance text and blocker detail remain readable; the cards stack on mobile.
- The delivery confirmation modal preserves the customer note after its server-side gate rejects dispatch. The error is visible below the confirmation action; scrolling within the modal is needed on the phone.
- The proof overview reserves a fixed conversation rail on desktop and stacks on mobile.
- Photo preview uses contain sizing and shows the full original image. The cyan rectangle is the synthetic fixture, not real artwork/evidence.
- The screenshots are captured at the requested CSS viewport sizes (not device-scaled PNG sizes).
- The fulfilment stepper screenshot is captured inside the production job drawer; its bottom continues below the viewport and the drawer scrolls independently.
- Exactly one step is current in the pending-scan state: Pickup evidence. Dispatch remains pending until the scan passes.
- The desktop proof collaboration view keeps the proof artwork and review controls side-by-side. In the captured mobile view, the proof studio remains vertically scrollable.

## Synthetic fixture states

- Customer work and finance screens use a synthetic accepted quotation, synthetic payment agreement, zero verified payments and no issued invoice.
- Owner delivery screen uses an authorized synthetic READY job with a pickup photo awaiting scan; the server fixture rejects dispatch until a clean scan exists.
- The photo preview fixture is the repository's synthetic image. Its role is to verify aspect ratio, secure preview UI and caption placement.

For test results and the missing real-backend browser harness fixture limitation, see `docs/WORKFLOW_REDESIGN_BATCH4.md`.
