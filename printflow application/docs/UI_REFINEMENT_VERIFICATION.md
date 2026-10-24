# Account UI refinement verification

23 October 2026. Native Java 21, Gradle, PostgreSQL and Next.js; no Docker.

- Comments 1/7: owner verification and customer forms now have consistent field/action gaps and full-width primary buttons.
- Comments 2/9: business header uses a circular account dropdown with profile/security and logout; customer header omits staff sign-in and keeps Request a quote.
- Comment 3: dedicated module navigation spacing separates tabs from metrics.
- Comments 4/5: enquiry details use a native modal with blurred background, internal scrolling, keyboard dismissal, focus restoration and consistent assignment/follow-up typography.
- Comment 6: email/password remains available. Google/Microsoft sign-in and explicit linking are implemented but disabled until private provider configuration and live testing. Microsoft currently uses an explicit single tenant; see provider setup documentation.
- Comment 8: resend verification moved into collapsed verification-email help rather than the main sign-in action row.

Actual checks: frontend typecheck and production build passed. Gradle build passed with 17 unit tests; four focused social-account integration tests passed; one configured OAuth state/PKCE integration test passed. Six Playwright layout checks passed across desktop/mobile in 34.4 seconds. Synthetic screenshots were inspected for customer login, mobile MFA and enquiry modal. No actual owner MFA was read or changed. These checks do not establish live provider connectivity or production readiness.

Focused real-backend regression: `scripts/Test-PhaseOneBrowser.ps1 -Project desktop` passed (one journey, 16.1 seconds; fixture setup/cleanup separate). Customer verification, enquiry/profile, CRM, product snapshot and password recovery remained functional. Synthetic fixture accounts were disabled and their private fixture file removed.

Header follow-up: logged-out visitors see Staff / owner sign in linking to /business, including on the customer page. Authenticated sessions show the circular account menu instead.
