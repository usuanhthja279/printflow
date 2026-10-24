# Customer signup and acceptance credentials — 24 October 2026

## Corrected behaviour

Self-signup queues a verification email. Until verification, the customer cannot sign in. Retrying signup for an existing unverified password account now issues a fresh verification link instead of silently doing nothing. The latest chosen password is stored as a candidate BCrypt hash bound to that verification; it replaces the pending account password only after mailbox ownership is proved. Resend carries the latest candidate forward, old links fail, and verified accounts/passwords are never overwritten by another signup attempt.

Accepting a guest enquiry now provisions the customer account at intake acceptance, rather than waiting for quotation. The private customer acceptance email contains the email username and a cryptographically random temporary password, valid for 24 hours. Staff notifications never include that credential. Database/API/activity logs do not expose plaintext temporary passwords; the unsent customer email payload is encrypted and removed after sending/expiry.

Temporary sign-in grants only a restricted activation session. The customer must choose a different permanent password and accept the privacy notice before reading enquiries or placing normal customer API requests. Activation verifies the mailbox through possession of the mailed credential, rotates access, invalidates the temporary password/setup tokens and signs the customer into the UI with the new password. The optional one-time setup link remains another activation path. Expired temporary credentials cannot authenticate or keep a session alive.

Existing verified customer passwords stay unchanged. Existing unverified password signups receive a fresh verification link in the acceptance/quotation access instructions, not a misleading disabled-account message. Pending identity ownership is linked without switching an enquiry to another customer. Staff-account email collisions and previously disabled verified accounts are not promoted/reactivated. Provider-signup browser binding remains required where applicable.

## Mail scope

At the owner's explicit request, new transactional mail may now go to any real address entered in this local app (APP_MAIL_ALLOWED_RECIPIENTS=*). Fictional .test/.invalid recipients and old held messages remain blocked. APP_MAIL_SEND_AFTER was advanced when broadening the recipient rule; held messages are not replayed. Native development and existing rate limits remain in place. Gmail credentials continue to come from system environment variables.

## UI test

- New signup: Create account → fill details/password/consent → open fresh verification email → Continue → sign in with the latest chosen password.
- Retry unverified signup: submit again with the same email; use the newest verification link. A verified account uses normal Sign in/Forgot password instead.
- Guest enquiry: submit with a new email → owner/designer accepts → read the customer acceptance email → sign in with the temporary credentials → Activate your customer account → choose a new password and consent → customer workspace.
- An already registered but unverified customer gets a fresh verification/setup link; the existing password is never emailed or reset by staff acceptance.

V14 adds candidate verification hashes and temporary credential expiry. Evidence is recorded below after final checks. Supersedes the earlier link-only automatic account creation policy, following the owner's explicit request for emailed temporary credentials.

Verification: native Gradle build and 23 unit checks passed; four focused workflow/identity integration checks passed, including signup/resend/replay/password preservation, customer-only temporary credentials, restricted activation, expired credentials and the existing production/delivery checks. A real native desktop signup journey passed in 41.1 seconds including runner setup: retry signup with the latest password, verify, sign in; accept another guest enquiry, retrieve its synthetic customer-only mail, sign in with temporary credentials, confirm profile API is blocked, activate and view the linked enquiry. Synthetic accounts/credentials were cleaned; real owner access was not changed. The selected real Gmail account was confirmed to be unverified with an expired prior verification email; no actual password was inspected or changed.

Live check: the fresh verification request returned HTTP 202 and its outbox entry reached SENT (SMTP accepted). Customer verification still requires opening the newest email link. Real delivery is enabled for any real address; historical held emails remain held.
