> Superseded signup/mail policy: new transactional mail now allows any real recipient by owner request. Guest acceptance sends customer-only temporary credentials with forced activation; signup retry verification is repaired. See CUSTOMER_SIGNUP_CORRECTION.md.

# Temporary Gmail delivery — native local development

Temporary sender and first recipient: your-test-inbox@example.com. Replace this mailbox with the business address later. Delivery is enabled for the selected test recipient. Google SMTP accepted the setup email on 24 October 2026; inbox receipt and the UI-driven end-to-end journey await user confirmation.

## One private setup step

Enable Google 2-Step Verification for the selected account, then create an app password for J.D.Printex at https://myaccount.google.com/apppasswords. Google may restrict app passwords for some managed/security configurations. Never use the normal Google password or paste an app password into chat. Credentials are read from system SPRING_MAIL_USERNAME and SPRING_MAIL_PASSWORD; SPRING_MAIL_PASSWROD is supported as a compatibility alias. The setup script uses these without prompting when present, and does not copy system credentials into .env.

Open PowerShell in the application folder and run:

```powershell
.\scripts\Enable-LocalGmail.ps1 -Sender 'your-test-inbox@example.com' -TestRecipient 'your-test-inbox@example.com'
```

The script prompts for the app password with hidden input, sends one requested setup test over smtp.gmail.com:587 with STARTTLS and certificate validation, and saves private settings only if Google accepts the message. It then restarts native services using the compiled Gradle build. Confirm receipt in Inbox/Spam. A provider accepting SMTP is not proof of inbox delivery.

Local app delivery is restricted to the selected recipient and messages created after the activation timestamp. Earlier queued cleanup/test messages are held, and fictional .test/.invalid recipients are held. Keep these restrictions during local testing. Links point to http://127.0.0.1:3000 and work only on this computer; no public deployment or domain activation is implied. Production requires HTTPS; the HTTP exception is limited to exact loopback URLs in DEV and cannot apply to prod.

## Verify through the application

After receiving the setup email, create a new enquiry with the selected real inbox. Confirm the acknowledgement arrives, then issue its quotation and check that it threads with the acknowledgement. New verification/password-reset requests also need to use the selected inbox. Previously queued messages are not replayed. Owner delivery can be disabled again with APP_MAIL_ENABLED=false and a native restart.

Prefer system environment credentials. APP_MAIL_PASSWORD in ignored .env remains a fallback when system variables are absent. Preserve APP_MAIL_ENCRYPTION_KEY. Recipient restriction is APP_MAIL_ALLOWED_RECIPIENTS; activation boundary is APP_MAIL_SEND_AFTER. Switching the sender later should use the same setup script with the new mailbox and a newly issued credential. Do not commit .env or disclose it in screenshots.

Google instructions: https://support.google.com/mail/answer/185833 and https://support.google.com/a/answer/176600 . SMTP and future OAuth are different integrations; this step does not connect Google customer sign-in, read the mailbox, sync replies or enable WhatsApp. Automated synthetic browser fixtures intentionally refuse to run when real delivery is enabled.

Verification: Gradle build passed, PowerShell script syntax parsed successfully. Four focused mail checks cover recipient/activation requirements, production/remote-HTTP rejection and holding historical/non-allowed recipients without contacting SMTP. System credentials were detected without printing values; Google accepted the setup test. Inbox receipt and app-created message delivery remain to be confirmed.

Current verification: native Gradle build/unit tests passed with system-credential and alias coverage. Local delivery supports acknowledgement, quotation, artwork-request and account verification/reset emails. Proof-published, production and delivery milestone email events are not yet wired; this is not a complete lifecycle notification implementation. Mail remains restricted to your-test-inbox@example.com.

24 October lifecycle-mail continuation: V13 adds one-time customer setup with quotation, lifecycle customer/team notifications, owner Email activity, proof-gated manually confirmed production/QC/dispatch/delivery milestones, delivery issue holds and durable 48-hour completion. This supersedes earlier “milestone emails/manual operations/timer not implemented” notes only for this bounded slice. Photos/GPS, stock/payment production controls, inbound Gmail sync and multi-page artwork viewer remain pending. Scope, APIs and verification: WORKFLOW_EMAIL_VERIFICATION.md.

24 October signup correction: V14 repairs repeated unverified signup/resend, binds the latest chosen password to verification, and creates guest access at intake acceptance. Customer-only email carries a 24-hour temporary password with forced activation/change and privacy consent; existing passwords are retained. New mail is allowed to any real address by explicit owner request; held/synthetic mail is not replayed. This supersedes earlier recipient-only/link-only account policy. See CUSTOMER_SIGNUP_CORRECTION.md.
