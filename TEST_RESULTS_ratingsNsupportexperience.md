# Test results — feature/ratingsNsupportexperience

Date: 2026-11-22

## JaCoCo (backend)

Command: `mvnw test jacoco:report` + `scripts/print-jacoco-summary.py`

| Metric | Coverage | Gate |
|--------|----------|------|
| Instructions (bundle) | **98.5%** (41520/42137) | ≥5% PASS; proceed threshold ≥90% **PASS** |
| Branches (bundle) | **95.2%** (3923/4122) | — |
| Lines (bundle) | **98.6%** | — |

### CLASS branch gate (min 90%)

- PASS JwtUtil: 100.0%
- PASS AuthRateLimitService: 90.9%
- PASS ConsoleSmsService: 100.0%
- PASS IdentityOnboardingController: 92.9%
- PASS PersonalityOnboardingController: 100.0%
- PASS GlobalExceptionHandler: 100.0%

Full HTML report (local): `target/site/jacoco/index.html`

## Decision

Coverage **above 90%** → proceed with push / MR / merge to `main`.
