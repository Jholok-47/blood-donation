"""
GOAL: REST microservice that Spring Boot calls to score accepted donors.
POST /predict  {candidates:[{...12 features...}]} -> {model_version, probabilities:[0..1,...]}
GET  /health   -> model loaded?  (used by Spring for the admin status check + fallback)

Run:  uvicorn app.main:app --port 8001
"""
import joblib
import pandas as pd
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

from .features import FEATURES, MODEL_PATH

app = FastAPI(title="LifeLink donor-completion model")
_bundle = joblib.load(MODEL_PATH) if MODEL_PATH.exists() else None


class Candidate(BaseModel):
    distance_km: float = Field(ge=0)
    coords_known: int = Field(ge=0, le=1)
    completed_count: int = Field(ge=0)
    let_down_count: int = Field(ge=0)
    avg_rating: float = Field(ge=0, le=5)
    review_count: int = Field(ge=0)
    urgency: int = Field(ge=0, le=3)
    quantity: int = Field(ge=1)
    response_delay_min: float = Field(ge=0)
    exact_match: int = Field(ge=0, le=1)
    hour_of_day: int = Field(ge=0, le=23)
    is_weekend: int = Field(ge=0, le=1)


class PredictRequest(BaseModel):
    candidates: list[Candidate] = Field(max_length=500)


@app.get("/health")
def health():
    return {"status": "UP" if _bundle else "NO_MODEL", "model_version": _bundle["version"] if _bundle else None}


@app.post("/predict")
def predict(req: PredictRequest):
    if _bundle is None:
        raise HTTPException(status_code=503, detail="Model not trained yet - run python -m app.train_model")
    if not req.candidates:
        return {"model_version": _bundle["version"], "probabilities": []}
    frame = pd.DataFrame([c.model_dump() for c in req.candidates])[FEATURES]
    probs = _bundle["model"].predict_proba(frame)[:, 1]
    return {"model_version": _bundle["version"], "probabilities": [round(float(p), 4) for p in probs]}
