from pydantic import BaseModel, Field
from typing import Optional


class VitalsRequest(BaseModel):
    age:              int   = Field(..., ge=0,  le=120)
    heart_rate:       float = Field(..., ge=20, le=300)
    spo2:             float = Field(..., ge=0,  le=100)
    respiratory_rate: float = Field(..., ge=1,  le=100)
    systolic_bp:      float = Field(..., ge=40, le=300)
    diastolic_bp:     float = Field(..., ge=20, le=200)
    temperature:      float = Field(..., ge=28, le=45)
    age_group:        str   = Field(default="ADULT")   # NEONATAL / PEDIATRIC / ADULT
    emergency_type:   Optional[str] = None


class PredictionResponse(BaseModel):
    risk_score:   float
    risk_level:   str
    confidence:   float = 0.0
    source:       str
    top_features: list[str] = []


class ExplainRequest(BaseModel):
    vitals:      dict
    risk_score:  float
    risk_level:  str
    age_group:   str = "ADULT"
    medical_history: Optional[str] = None


class ExplainResponse(BaseModel):
    explanation:       str
    treatment_recs:    str
    paramedic_guidance: str
