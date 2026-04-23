from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from app.schema import VitalsRequest, PredictionResponse, ExplainRequest, ExplainResponse
from app.inference.xgboost_model import get_model
from app.triage.rule_engine import apply_rules, apply_age_adaptive_rules
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("vigilai-ai")

app = FastAPI(
    title="VigilAI ML Service",
    description="Sepsis & Emergency Prediction — XGBoost + Age-Adaptive Rules",
    version="2.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


def map_risk(prob: float) -> str:
    if prob > 0.80: return "CRITICAL"
    if prob > 0.60: return "HIGH"
    if prob > 0.30: return "MEDIUM"
    return "LOW"


def top_features(vitals: VitalsRequest) -> list[str]:
    """Return human-readable SHAP-style feature contributions."""
    features = []
    if vitals.spo2 < 92:
        features.append(f"SpO2 critically low ({vitals.spo2}%) ↓")
    if vitals.systolic_bp < 100:
        features.append(f"Hypotension ({vitals.systolic_bp} mmHg) ↓")
    if vitals.heart_rate > 100:
        features.append(f"Tachycardia ({vitals.heart_rate} bpm) ↑")
    if vitals.respiratory_rate > 22:
        features.append(f"Tachypnea ({vitals.respiratory_rate}/min) ↑")
    if vitals.temperature > 38.3:
        features.append(f"Fever ({vitals.temperature}°C) ↑")
    if vitals.temperature < 36.0:
        features.append(f"Hypothermia ({vitals.temperature}°C) ↓")
    if not features:
        features.append("Vitals within borderline range")
    return features[:5]


@app.get("/health")
def health():
    return {"status": "healthy", "service": "VigilAI AI Service v2.0"}


@app.post("/predict", response_model=PredictionResponse)
def predict(vitals: VitalsRequest):
    logger.info(f"Predict: age={vitals.age}, HR={vitals.heart_rate}, "
                f"SpO2={vitals.spo2}, RR={vitals.respiratory_rate}, "
                f"BP={vitals.systolic_bp}/{vitals.diastolic_bp}, "
                f"Temp={vitals.temperature}, group={vitals.age_group}")

    # 1. Age-adaptive rule engine (overrides model for extreme values)
    rule_result = apply_age_adaptive_rules(vitals)
    if rule_result:
        feats = top_features(vitals)
        return PredictionResponse(
            risk_score=rule_result["risk_score"],
            risk_level=rule_result["risk_level"],
            confidence=rule_result.get("confidence", 0.95),
            source="RULE_ENGINE",
            top_features=feats,
        )

    # 2. XGBoost model prediction
    try:
        model = get_model()
        features = [
            vitals.age,
            vitals.heart_rate,
            vitals.spo2,
            vitals.respiratory_rate,
            vitals.systolic_bp,
            vitals.diastolic_bp,
            vitals.temperature,
        ]
        prob, _ = model.predict(features)
        level    = map_risk(float(prob))
        feats    = top_features(vitals)

        return PredictionResponse(
            risk_score=float(prob),
            risk_level=level,
            confidence=float(prob),
            source="XGBOOST_MODEL",
            top_features=feats,
        )

    except Exception as e:
        logger.warning(f"Model unavailable ({e}), falling back to rules")
        rule_fallback = apply_rules(vitals)
        if rule_fallback:
            return PredictionResponse(**rule_fallback, source="RULE_ENGINE_FALLBACK",
                                      top_features=top_features(vitals))
        return PredictionResponse(
            risk_score=0.5, risk_level="MEDIUM",
            confidence=0.5, source="FALLBACK",
            top_features=["Unable to compute — manual review required"],
        )


@app.post("/explain", response_model=ExplainResponse)
def explain(req: ExplainRequest):
    """Generate clinical explanation + treatment recs + paramedic guidance."""
    v   = req.vitals
    lvl = req.risk_level
    age = v.get("age", 30)
    age_group = v.get("age_group", "ADULT")

    # ── Explanation ───────────────────────────────────────────────
    explanation = (
        f"The AI model assessed a {req.risk_score*100:.0f}% {lvl} sepsis risk for this "
        f"{age_group.lower()} patient (age {age}). "
        f"Key contributing signals: HR={v.get('heart_rate')} bpm, "
        f"Temp={v.get('temperature')}°C, RR={v.get('respiratory_rate')}/min, "
        f"BP={v.get('systolic_bp')}/{v.get('diastolic_bp')} mmHg, "
        f"SpO2={v.get('spo2')}%. "
    )
    if v.get('spo2', 100) < 92:
        explanation += "Oxygen desaturation is a critical early sepsis indicator. "
    if v.get('systolic_bp', 120) < 100:
        explanation += "Hypotension suggests haemodynamic compromise — possible septic shock. "
    if v.get('heart_rate', 70) > 110:
        explanation += "Tachycardia reflects compensatory response to reduced perfusion. "

    # ── Treatment Recommendations ─────────────────────────────────
    recs = ["IMMEDIATE ACTIONS:"]
    if v.get('spo2', 100) < 92:
        recs.append("• O₂ supplementation — high-flow mask, target SpO2 ≥ 94%")
    if v.get('systolic_bp', 120) < 100:
        recs.append("• IV fluid resuscitation — NS/LR 30 mL/kg over 3h (reduce for neonates)")
    if v.get('temperature', 37) > 38.3:
        recs.append("• Blood cultures ×2 before antibiotics — broad-spectrum empirical coverage")
        recs.append("• Antipyretics (Paracetamol 15 mg/kg in children)")

    if age_group == "NEONATAL":
        recs += [
            "• Neonatal ICU transfer — STAT",
            "• Glucose check q30 min — neonatal hypoglycaemia risk",
            "• Avoid aggressive fluids — 10 mL/kg bolus max",
        ]
    elif age_group == "PEDIATRIC":
        recs += [
            "• PALS sepsis pathway activation",
            "• Weight-based dosing — check Broselow tape",
            "• Paediatric surgery consult if abdominal source suspected",
        ]
    else:
        recs += [
            "• Sepsis-3 bundle: lactate, cultures, antibiotics within 1h",
            "• ICU consult for vasopressors if MAP < 65 mmHg",
            "• Hourly urine output monitoring (target > 0.5 mL/kg/h)",
        ]

    if lvl == "CRITICAL":
        recs.append("⚠️ CRITICAL — Do NOT delay transfer. Alert receiving ICU immediately.")

    # ── Paramedic Guidance ────────────────────────────────────────
    paramedic = (
        f"EN-ROUTE PROTOCOL ({age_group}):\n"
        f"1. O₂ @ {'1-2' if age_group == 'NEONATAL' else '2-3'} L/min — target SpO2 94%\n"
        f"2. IV {'24G' if age_group == 'NEONATAL' else '18G'} — "
        f"NS {'10' if age_group == 'NEONATAL' else '30'} mL/kg\n"
        f"3. Vitals every 5 min — document all trends\n"
        f"4. {'Keep warm, avoid excessive handling' if age_group == 'NEONATAL' else 'Cool if Temp > 39.5°C'}\n"
        f"5. Pre-notify hospital: ETA + current vitals + risk={lvl}\n"
        f"Current snapshot: HR={v.get('heart_rate')}, Temp={v.get('temperature')}°C, "
        f"RR={v.get('respiratory_rate')}, BP={v.get('systolic_bp')}/{v.get('diastolic_bp')}, "
        f"SpO2={v.get('spo2')}%"
    )

    return ExplainResponse(
        explanation=explanation,
        treatment_recs="\n".join(recs),
        paramedic_guidance=paramedic,
    )
