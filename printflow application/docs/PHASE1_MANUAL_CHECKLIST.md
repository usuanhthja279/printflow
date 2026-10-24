# Phase 1 — owner-led manual test checklist

Use the native app at http://127.0.0.1:3000. Use fictional information only. No Google Workspace messages are sent while local delivery is disabled. Keep passwords, setup/recovery codes and verification/reset links private; do not paste them into chat or screenshots.

Use two separate browser sessions: an ordinary window for the customer, and a private/incognito window for the owner. This prevents one login replacing the other. Your existing owner authenticator is preserved.

## 1. Customer signup and verification

1. Customer window: open `/customer`, choose **Create account**, use a unique fictional address ending **@example.test**, a fictional name/phone and a password of at least 14 characters. Read/check the privacy acknowledgement and continue.
2. Expected: a generic verification-queued message. Signing in before verification must fail.
3. Owner window: sign in at `/business` with your authenticator, choose **LOCAL EMAIL**, then **Load synthetic test messages**. Find the matching fictional recipient and VERIFY message.
4. Privately copy its full link into the customer window. Choose **Continue**, then sign in with the registered email/password.
5. Expected: verified customer workspace. The owner preview is only for fictional @example.test recipients in native DEV/QA; it is not Gmail delivery.

## 2. Customer enquiry and profile

1. In the customer workspace choose **Request a quote**. Contact fields should be filled from the account profile.
2. Use **Guided** mode, choose a product/quantity, add a fictional requirement and check the privacy acknowledgement. Submit once.
3. Expected: enquiry saved and tracking credentials shown. Returning to `/customer` should show the enquiry without re-entering its tracking token.
4. Open **View enquiry**. Expected: submitted requirements/status, without staff follow-up or assignment details.
5. Update organization/address under **Contact details**, save and reload. Expected: the edited details persist.
6. Optional: submit a guest enquiry using the same email, then use **Link an earlier enquiry** with both tracking credentials. A different verified customer must not be able to link it.

## 3. Owner/staff enquiry work

1. Owner window: choose **ENQUIRIES**, search the fictional customer email and open the enquiry.
2. Assign it to an enabled sales account or yourself. Claim it before editing a follow-up/status.
3. Set priority, next action and follow-up date/time. Expected: both action and time are required together; save/reload preserves the plan. An earlier date appears under **Overdue**.
4. Change status forward. Expected: customer status updates, while private internal activity stays out of customer views. Closing removes the follow-up plan.
5. Optional sales check: another sales account must not see a lead assigned to someone else. Owner can review/reassign it.

## 4. Contacts and duplicates

1. Choose **CONTACTS**, enter fictional name/email/phone/organization/address and save.
2. Expected: reusable contact in the list. Editing/saving then reloading preserves details.
3. Enter the same email/phone in the form and choose **Check duplicates**. Expected: suggestions appear; no automatic merge.
4. Only if you deliberately create two fictional duplicates: edit one, choose its surviving contact and **Merge reviewed contacts**. Expected: source is archived, surviving details remain and linked enquiries move to the survivor. Merge requires the same responsible staff and current record revisions.
5. Permission-to-contact checkbox requires an evidence note; it does not send marketing messages.

## 5. Product templates and public pages

1. Choose **PRODUCTS**, select one of the four confirmed product families. Review quantity bounds, optional GSM/sizes and finishes. Keep **UNSURE** among finishes.
2. Use harmless edits for testing; record the original values before changing live prototype configuration. Save creates a new version.
3. Open `/products` and its product page, then **Request this print**. Expected: current description/options/quantity limits are available. Professional mode exposes technical fields; guided mode allows advice.
4. Expected: an enquiry submitted before the edit retains its stored template snapshot/version. Deactivating a product removes it from the public catalogue and rejects new requests for it.

## 6. Password recovery

1. Customer window: sign out, choose **Forgot password**, enter the fictional account email and continue.
2. Owner window: refresh **LOCAL EMAIL** and privately open the matching RESET link in the customer window.
3. Choose a new password and sign in. Expected: new password works, old sessions/password no longer authorize the account. Reusing the reset link fails.
4. Unknown-email recovery must give the same generic response, not reveal whether the account exists.

## 7. Privacy requests

1. Customer workspace: choose **Request export** or **Request erasure**.
2. Expected: OPEN request; an identical open request is not duplicated.
3. Owner: choose **PRIVACY**, review the matching fictional request and record a decision/explanation only after the required work is complete.
4. Customer reload: expected reviewed status/explanation. Recording a review does not automatically export/erase database records; fulfillment/retention decisions remain operator-controlled.

## What to report

For each section write **Pass**, **Fail** or **Not tested**. For a failure, share the route, your role, the last action, expected result and visible error text. A screenshot is useful only after hiding private details and credentials. Use the fictional enquiry ID if needed; never share its tracking token. We will use this list for the review/bug-fix pass after the development handoff.

## Not part of this manual development check

Real Gmail/Workspace delivery, Turnstile credentials/provider success, public DNS/TLS, native QA isolation, production provisioning, backups/restore and legal/privacy sign-off need separate configuration and release checks. Artwork upload/download is disabled; Phase 1 supplies a policy/interface foundation, not the Phase 2 scanning/storage pipeline. These are not claimed tested by a successful local walkthrough.

## Owner-reported results — 23 October 2026

Confirmed by the owner during the walkthrough: customer verification/sign-in; enquiry submission and automatic appearance in the customer workspace; View enquiry; owner assignment/priority/follow-up persistence and Overdue filtering; REVIEWING status visible to the customer while private follow-up details remain hidden. Remaining checks were handed back to Codex for a focused synthetic-account browser run.

## Codex follow-up after owner handoff

Focused real-backend desktop journey passed: signup/verification and customer prefill, enquiry creation, profile address/organization persistence, product save with old-enquiry snapshot preservation, customer owner/staff API denial, contact create/edit and duplicate suggestions, permission evidence rejection/acceptance, privacy request/review visibility and password recovery/new-password login. The owner need not repeat these baseline checks. Contact merging and guest-claim token/ownership negatives retain the earlier integration-test evidence; they were not additionally driven through every UI variant. See PHASE1_DEVELOPMENT_STATUS.md for run details and limitations.
