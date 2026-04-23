from app.schema import VitalsRequest
from typing import Optional


# ── Standard SIRS thresholds (Adult 18+) ──────────────────────────────────
ADULT = dict(hr_high=90, hr_low=None, temp_high=38.3, temp_low=36.0,
             rr_high=20, bp_sys_low=100, spo2_low=94)

# ── Pediatric thresholds (age-weighted, simplified for 1–18y) ─────────────
PEDIATRIC = dict(hr_high=140, hr_low=60, temp_high=38.5, temp_low=36.0,
                 rr_high=30, bp_sys_low=70, spo2_low=94)

# ── Neonatal thresholds (0–28 days) ──────────────────────────────────────
NEONATAL = dict(hr_high=180, hr_low=100, temp_high=38.0, temp_low=36.5,
                rr_high=60,  bp_sys_low=60, spo2_low=90)


def _thresholds(vitals: VitalsRequest) -> dict:
    g = vitals.age_group.upper()
    if g == "NEONATAL":  return NEONATAL
    if g == "PEDIATRIC": return PEDIATRIC
    return ADULT


def apply_age_adaptive_rules(vitals: VitalsRequest) -> Optional[dict]:
    """
    Returns a dict with risk_score / risk_level / confidence if any
    CRITICAL threshold is breached, else None (fall through to model).
    """
    t = _thresholds(vitals)
    flags = 0

    if vitals.spo2 < t["spo2_low"] - 4:           flags += 2   # heavy weight
    elif vitals.spo2 < t["spo2_low"]:              flags += 1
    if vitals.systolic_bp < t["bp_sys_low"]:       flags += 2
    if vitals.heart_rate > t["hr_high"] + 20:      flags += 2
    elif vitals.heart_rate > t["hr_high"]:         flags += 1
    if t["hr_low"] and vitals.heart_rate < t["hr_low"]: flags += 2
    if vitals.temperature > t["temp_high"] + 1:    flags += 1
    if vitals.temperature < t["temp_low"]:         flags += 2   # hypothermia = bad
    if vitals.respiratory_rate > t["rr_high"] + 8: flags += 2
    elif vitals.respiratory_rate > t["rr_high"]:   flags += 1

    # Only override model if clearly critical (≥4 flags)
    if flags >= 6:
        return {"risk_score": 0.92, "risk_level": "CRITICAL", "confidence": 0.97}
    if flags >= 4:
        return {"risk_score": 0.78, "risk_level": "HIGH",     "confidence": 0.90}
    return None


def apply_rules(vitals: VitalsRequest) -> Optional[dict]:
    """Simple fallback rules used when model is unavailable."""
    t = _thresholds(vitals)
    flags = sum([
        vitals.heart_rate > t["hr_high"],
        vitals.temperature > t["temp_high"],
        vitals.respiratory_rate > t["rr_high"],
        vitals.systolic_bp < t["bp_sys_low"],
        vitals.spo2 < t["spo2_low"],
    ])
    if flags >= 4: return {"risk_score": 0.88, "risk_level": "CRITICAL", "confidence": 0.85}
    if flags >= 2: return {"risk_score": 0.65, "risk_level": "HIGH",     "confidence": 0.80}
    return None
