"""
GOAL: train and evaluate the donor-completion model, then save it (model/donor_model.joblib) with
metrics (model/metrics.json). Compares a logistic-regression baseline with a calibrated gradient-
boosting model; the boosted model is what the API serves. Probabilities are calibrated so that
0.80 really means "about 80% of such donors complete".

Run:  python -m app.train_model
"""
import json
from datetime import datetime, timezone

import joblib
import pandas as pd
from sklearn.calibration import CalibratedClassifierCV
from sklearn.ensemble import HistGradientBoostingClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import accuracy_score, brier_score_loss, log_loss, roc_auc_score
from sklearn.model_selection import train_test_split
from sklearn.pipeline import make_pipeline
from sklearn.preprocessing import StandardScaler

from .features import DATA_PATH, FEATURES, METRICS_PATH, MODEL_PATH, TARGET


def evaluate(model, X, y):
    p = model.predict_proba(X)[:, 1]
    return {
        "roc_auc": round(roc_auc_score(y, p), 4),
        "accuracy": round(accuracy_score(y, p >= 0.5), 4),
        "brier": round(brier_score_loss(y, p), 4),
        "log_loss": round(log_loss(y, p), 4),
    }


def main():
    if not DATA_PATH.exists():
        raise SystemExit(f"{DATA_PATH} not found - run: python -m app.generate_synthetic_data")
    df = pd.read_csv(DATA_PATH)
    X, y = df[FEATURES], df[TARGET]
    X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.2, random_state=42, stratify=y)

    baseline = make_pipeline(StandardScaler(), LogisticRegression(max_iter=1000)).fit(X_tr, y_tr)
    model = CalibratedClassifierCV(
        HistGradientBoostingClassifier(max_depth=3, learning_rate=0.05, max_iter=200, min_samples_leaf=60, l2_regularization=1.0, random_state=42),
        method="isotonic", cv=3,
    ).fit(X_tr, y_tr)

    metrics = {
        "model_version": datetime.now(timezone.utc).strftime("gbm-%Y%m%d-%H%M"),
        "trained_on": "synthetic",
        "rows": int(len(df)),
        "positive_rate": round(float(y.mean()), 4),
        "baseline_logistic_regression": evaluate(baseline, X_te, y_te),
        "gradient_boosting_calibrated": evaluate(model, X_te, y_te),
        "features": FEATURES,
    }
    MODEL_PATH.parent.mkdir(parents=True, exist_ok=True)
    joblib.dump({"model": model, "features": FEATURES, "version": metrics["model_version"]}, MODEL_PATH)
    METRICS_PATH.write_text(json.dumps(metrics, indent=2))
    print(json.dumps(metrics, indent=2))
    print(f"\nSaved model to {MODEL_PATH}")


if __name__ == "__main__":
    main()
