# On-screen PDF artwork review — 24 October 2026

## Customer and designer experience

Clean PDF, PNG and JPEG artwork has a Preview action in the file list. It opens inside the workspace instead of opening a browser download. The current shared PDF proof is also shown inline automatically. Download remains optional. Historical PDFs can be opened from their file preview; only the current proof auto-loads to avoid starting many PDF workers.

PDF review supports previous/next page, direct page selection, fit-to-width and 75/150/200% zoom. Only the selected page is rendered. Click/tap a location on a page and describe the requested change. The server stores the exact proof ID, page number and normalized coordinates; pins only appear on their own page and remain attached to the old revision when a replacement is shared. Comments request edits by the designer; they do not mutate the original PDF or image.

The customer explicitly marks each successfully displayed page reviewed. Approve is available after all pages of that exact revision are marked and the existing whole-proof/quotation acknowledgement is checked. Requests for changes can be submitted after partial review. The server independently checks the complete, distinct page acknowledgement list before portal PDF approval and stores it immutably in the decision. This is an acknowledgement record, not proof that a person read each word. Existing quote/hash/current-revision/scan/access checks still apply.

## Native implementation and boundaries

- PDF.js 6.4.299 renders canvas content using an explicit local browser worker. No PDF JavaScript, XFA form UI, attachment execution or clickable external-link annotation layer is enabled. Worker/fonts/CMaps/WASM/ICC assets are copied from the pinned package during prebuild/predev; no public CDN is used.
- Existing scoped preview access now admits readable PDFs and returns authenticated no-store application/pdf with inline disposition and sandbox/nosniff headers. Customer access to unshared designer drafts remains denied; clean scans and SHA-256 integrity checks remain required.
- PDF page inspection runs in a separate native Java process with a 96 MiB heap, a ten-second timeout and two concurrent inspection slots. It reads page metadata through PDFBox, rejects unreadable/encrypted/XFA or over-100-page documents, and never executes PDF actions. It is resource isolation, not a claim of a complete OS sandbox. The worker works in native Gradle tests and the runnable Spring Boot jar.
- Preview limit: 20 MiB, 1–100 pages; four million output canvas pixels per page, bounded image-canvas allocation and 30-second load/render timeouts. Closing/replacing/retrying a preview cancels work and terminates the worker. The renderer keeps a document's bounded byte data while open; it renders one page at a time and does not save it to the user's Downloads folder.
- V16 adds derived page_count and immutable reviewed_pages evidence. V17 allows the page count to be populated once for the same clean PDF bytes; it forbids later changes and preserves all prior file-content immutability. Legacy PDF metadata is derived on use. Existing immutable source PDFs/approvals are not rewritten.
- API: GET /api/v1/design/enquiries/{id}/artifacts/{file}/preview now supports application/pdf. Comment payload remains {body,pageNumber,anchorX,anchorY}; page bounds and page association for pins are checked. PDF approval adds reviewedPages:[1,2,...] to the existing {action,note,quoteId,artifactHash,reviewed} payload. No public artwork route is introduced.

A first-login issue discovered while creating the designer was corrected: successful password change already invalidates the session, so the UI returns directly to /business sign-in rather than trying to log out the invalidated session.

## Dedicated local designer

Username: designer.test@jdprintex.local. Role: DESIGNER. Public name: Test Designer. The owner-created account has completed its first-login password change; it remains enabled for manual local testing and has no Contacts grant or owner privileges. Its generated password is stored only in the private, ignored .local/test-designer-credentials.json file; it is not printed in logs or this handbook. Existing credentials are reused on repeat tests. The normal owner staff administration controls can disable/reset this test account.

The browser harness protects that credential file and creates isolated owner/customer fixtures. It archives browser-phase1 synthetic enquiries, disables those fixture accounts and clears their unsent synthetic mail after testing. The persistent test designer remains available. No real customer order is changed, and synthetic UI tests run with real email delivery disabled.

## Focused verification

Native Gradle: one PDF inspector test covers valid page count plus malformed/over-limit rejection; nine design integration checks cover scoped access, clean scan/quote gates, page bounds, page pins, full-page approval/rejection of partial/duplicate lists, page metadata immutability, stale revisions, role isolation, designer tasks and existing evidenced offline approval.

Frontend production build/type checking includes local worker asset generation. Desktop UI evidence: the complete two-page reference/proof/revision journey passed in 24.1 seconds (26.1 seconds including runner setup). It used the real Windows Defender scanner and runnable native jar, not mocked network responses or seeded CLEAN verdicts. It verified the Preview dialog, page navigation/zoom, page-2 pin persistence and page-1 isolation, rejected partial UI approval, fresh review for proof v2, exact final approval and zero browser download events.

Repeat with ./scripts/Test-PdfReviewBrowser.ps1 -Project desktop or -Project mobile after starting native services with -DisableMail. Native Gradle remains the backend workflow; Docker is not used. Phone viewport UI check also passed: 27.9 seconds (37.8 seconds including runner setup), on the final build, with real Defender scanning, reference Preview dialog, page pins, replacement and full-page approval. Its screenshot was visually checked. The final native Gradle check again passed the inspector test and all nine design integration tests; the final frontend production build/type check passed. The remaining restore/health check is recorded below.

Source: [Mozilla PDF.js API](https://mozilla.github.io/pdf.js/api/draft/module-pdfjsLib.html); installed pinned package types are used where the current draft differs.

Final native restore: API health UP, real mail enabled, ephemeral browser fixture credentials removed, dedicated test designer retained with private credential-file ACL. Synthetic PDF enquiries and unsent fixture payloads were archived/cleared; real business records were not modified. No Docker was used.

## Reference artwork workflow
References now use a separate stage-aware component: existing supplied files are visible before quotation; new customer sample/upload/comment actions open after a business artwork request with an issued quotation, or acceptance. The business can review them and assign a designer during quotation. Reference review requests preserve source bytes and send an annotated copy. Final proof approval/production/closure lock further customer reference requests. See CUSTOMER_REVIEW_WORKSPACE.md.
