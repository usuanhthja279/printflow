# Customer Google and Microsoft sign-in setup

J.D.Printex · Native Gradle development · configuration required, not connected

Email/password signup/login remains available. Google and Microsoft buttons are disabled until the corresponding OAuth client credentials are configured. Google Workspace SMTP configuration does not configure Google sign-in; these are separate integrations. No live provider login is claimed by the local mapping or redirect tests.

## Local configuration

Register an authorized OAuth/OpenID Connect application in each provider's console. No app registration, account consent, purchase or DNS change was performed by this implementation. Keep populated credentials in ignored local environment files or deployment secrets; never paste them into chat, source or screenshots.

Set `APP_PUBLIC_BASE_URL=http://127.0.0.1:3000` for native local development. Register these exact redirect URIs for the provider being configured:

- Google: `http://127.0.0.1:3000/api/public/sso/callback/google`.
- Microsoft: `http://127.0.0.1:3000/api/public/sso/callback/microsoft`.

Production uses the approved HTTPS public origin, for example `https://jdprintex.in`, with the same callback paths. Do not expose the private API port as the callback. Browser requests/callbacks stay behind the frontend `/api` proxy. Update the registered URIs when choosing the actual deployment hostname.

Google requires private `APP_GOOGLE_CLIENT_ID` and `APP_GOOGLE_CLIENT_SECRET`. Microsoft requires `APP_MICROSOFT_CLIENT_ID`, `APP_MICROSOFT_CLIENT_SECRET`, and an explicit tenant UUID in `APP_MICROSOFT_TENANT_ID`.

The initial Microsoft implementation intentionally validates one explicit tenant issuer. It does not claim arbitrary multi-tenant or personal-account support. Choose/test the tenant and app audience together; supporting other audiences requires a reviewed issuer/tenant strategy rather than using `common` and weakening validation.

Set `APP_SESSION_SAME_SITE=lax` when enabling either provider. Password-only development defaults to Strict. The OAuth callback is a cross-site top-level GET and needs the existing session to validate state; configured providers reject startup if the cookie remains Strict. HttpOnly, production Secure cookies, session rotation and CSRF on all session mutations remain required.

Keep `APP_CUSTOMER_ACCOUNTS_ENABLED` and privacy/email release controls consistent with the active environment. New Microsoft signup and any changed/unverified provider email require configured email verification. Real delivery stays disabled until separately configured and verified; native fictional @example.test verification uses the owner-only local preview.

## Account behavior

Only customer accounts use provider sign-in. Staff/owner authentication and owner MFA remain separate.

Spring Security performs authorization-code/OIDC validation, including state, nonce and PKCE. Provider access/refresh tokens are not retained for provider API access. External identities are keyed by provider, issuer and subject, never by email alone.

A new user completes name, phone, email and current privacy acknowledgement after provider authentication. A Google-verified unchanged email can create a verified customer. Microsoft email is not treated as ownership proof; its verification link must be opened in the same browser used for signup. The browser/session binding prevents an untrusted email claim and an unrelated recipient's click from silently linking the provider. Unbound resend requests cannot remove this requirement.

An email matching an existing account never automatically links or grants access. Sign in normally, open Your profile, confirm the current password and explicitly connect the provider. Provider-only accounts can use email password recovery to set a password before connecting another provider. Role changes, disable and password changes continue to revoke stale sessions. No provider flow can promote an account to owner/staff.

## Provider checks before describing as connected

After configuring each provider, verify consent/callback with an approved test account, signup completion, logout/re-login, existing-account collision handling and explicit linking. Test cancellation, invalid state, wrong tenant, unverified email, duplicate linking and disabled accounts. Confirm that query codes/tokens, secrets and identity claims do not enter logs.

Local tests cover configuration validation, state/nonce/PKCE redirect construction, rejection of an unsolicited callback, customer-only identity mapping, email-collision refusal, password-confirmed linking and same-browser email verification. They do not substitute for live Google/Microsoft consent/token/signature tests.

Official references:

- https://docs.spring.io/spring-security/reference/servlet/oauth2/index.html
- https://developers.google.com/identity/openid-connect/openid-connect
- https://learn.microsoft.com/en-us/entra/identity-platform/id-token-claims-reference
