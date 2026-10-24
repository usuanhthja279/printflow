# Batch 2 visual test report

**Feature:** J.D.Printex Owner Dashboard & Production Command Centre
**Source revision:** `48ef74a` (`Implement Batch 2 owner production command centre`)
**Capture date:** 2026-10-25
**Route:** `/business`
**Role:** `OWNER`
**Data:** Synthetic Playwright API fixtures; no live customer or business records were used.

## Viewports and evidence

The browser ran the same evidence scenarios at a 1440 × 900 desktop CSS viewport and a 390 × 844 mobile CSS viewport (iPhone 13 emulation). Mobile PNGs use device scale factor 3, so their pixel dimensions are three times the CSS viewport. Each image in `desktop/` and `mobile/` is a real local-browser screenshot, and `index.html` provides a labeled gallery with the significance of each state. `comparison/` contains four before/after images based on the Batch 1 baseline captures, plus a desktop contact sheet.

The inspected states cover the owner dashboard and action inbox, production board and job drawer, release readiness, assignment, printing, hold and resume, quality check and rework, dispatch readiness, missing delivery evidence, scan status and an empty queue. Mobile inspection also covered assignment, hold, rework, and pending scan evidence. The desktop board displayed all six lanes without horizontal clipping. The mobile board used its single-lane layout without viewport overflow. Release and dispatch blockers were visible with their reasons in the job drawer.

Screenshot images were visually inspected after the final browser run. The evidence is intended to guide the next redesign pass; synthetic fixture content and the different baseline fixture/viewport sizes mean that before/after comparisons are visual rather than data-identical pixel diffs.

## Reproduction

From `printflow application/frontend`:

```powershell
npm.cmd run test:e2e -- e2e/batch2-evidence.spec.ts --project=desktop --project=mobile
```

The test intercepts API requests with fixtures and captures the final views; it does not send mail, create provider payments, or write business records.
