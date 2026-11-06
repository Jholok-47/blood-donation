"""
GOAL: single source of truth for the model's input features.
Generator, trainer and API all import FEATURES, so column order/names can never drift apart.
The Java side (MlCandidateFeatures) must send exactly these names.
"""
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent.parent
DATA_PATH = BASE_DIR / "data" / "training_data.csv"
MODEL_PATH = BASE_DIR / "model" / "donor_model.joblib"
METRICS_PATH = BASE_DIR / "model" / "metrics.json"

TARGET = "completed"  # 1 = selected donor completed the donation, 0 = withdrew / went silent

FEATURES = [
    "distance_km",        # haversine donor -> patient (50 if coordinates missing)
    "coords_known",       # 1 if both donor and patient coordinates exist
    "completed_count",    # past COMPLETED assignments
    "let_down_count",     # past DECLINED assignments + UNRESPONSIVE releases
    "avg_rating",         # average review rating (4.0 if no reviews)
    "review_count",
    "urgency",            # 0 LOW, 1 MEDIUM, 2 HIGH, 3 CRITICAL
    "quantity",           # units the request needs
    "response_delay_min", # minutes between open-call notification and "I can donate" (capped 1440)
    "exact_match",        # 1 if donor blood group == requested group
    "hour_of_day",        # 0-23 when the ranking/assignment happens
    "is_weekend",         # 1 Sat/Sun
]
