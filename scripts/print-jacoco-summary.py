import csv
from pathlib import Path

p = Path("target/site/jacoco/jacoco.csv")
rows = list(csv.DictReader(p.open(encoding="utf-8")))


def pct(covered: int, missed: int) -> float:
    total = covered + missed
    return 100.0 * covered / total if total else 100.0


focus = [
    "PhoneNumberNormalizer",
    "AuthService",
    "AuthController",
    "PhoneCodeSeedRunner",
    "RefCountry",
    "WorldCountrySeedRunner",
    "JwtUtil",
    "AuthRateLimitService",
    "ConsoleSmsService",
    "IdentityOnboardingController",
    "PersonalityOnboardingController",
    "GlobalExceptionHandler",
]

ti_m = ti_c = tb_m = tb_c = tl_m = tl_c = 0
for r in rows:
    ti_m += int(r["INSTRUCTION_MISSED"])
    ti_c += int(r["INSTRUCTION_COVERED"])
    tb_m += int(r["BRANCH_MISSED"])
    tb_c += int(r["BRANCH_COVERED"])
    tl_m += int(r["LINE_MISSED"])
    tl_c += int(r["LINE_COVERED"])

print(f"{'CLASS':42} {'INSTR%':>8} {'BRANCH%':>8} {'LINE%':>8}  (instr cov/tot | branch cov/tot)")
print("-" * 105)
print(
    f"{'BUNDLE':42} {pct(ti_c, ti_m):7.1f}% {pct(tb_c, tb_m):7.1f}% {pct(tl_c, tl_m):7.1f}%  "
    f"({ti_c}/{ti_c + ti_m} | {tb_c}/{tb_c + tb_m})"
)
print("-" * 105)

for name in focus:
    matches = [r for r in rows if r["CLASS"] == name or r["CLASS"].startswith(name + "$")]
    if not matches:
        print(f"{name:42} NOT IN REPORT")
        continue
    for r in matches:
        im, ic = int(r["INSTRUCTION_MISSED"]), int(r["INSTRUCTION_COVERED"])
        bm, bc = int(r["BRANCH_MISSED"]), int(r["BRANCH_COVERED"])
        lm, lc = int(r["LINE_MISSED"]), int(r["LINE_COVERED"])
        print(
            f"{r['CLASS']:42} {pct(ic, im):7.1f}% {pct(bc, bm):7.1f}% {pct(lc, lm):7.1f}%  "
            f"({ic}/{ic + im} | {bc}/{bc + bm})"
        )

print()
print("JaCoCo CLASS branch gate (min 90%):")
gates = [
    "JwtUtil",
    "AuthRateLimitService",
    "ConsoleSmsService",
    "IdentityOnboardingController",
    "PersonalityOnboardingController",
    "GlobalExceptionHandler",
]
for name in gates:
    r = next((x for x in rows if x["CLASS"] == name), None)
    if not r:
        print(f"  MISSING {name}")
        continue
    bm, bc = int(r["BRANCH_MISSED"]), int(r["BRANCH_COVERED"])
    p = pct(bc, bm)
    status = "PASS" if p >= 90 else "FAIL"
    print(f"  {status} {name}: branch {p:.1f}% ({bc}/{bc + bm})")

bundle_instr = pct(ti_c, ti_m)
print()
print(
    f"Instruction gate (bundle min 5%): {bundle_instr:.1f}% "
    f"{'PASS' if bundle_instr >= 5 else 'FAIL'}"
)
