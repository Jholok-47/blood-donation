"""
GOAL: create a synthetic training set (data/training_data.csv) so the model can be trained
without hand-entering thousands of rows.

HONESTY NOTE (say this in the defense): real LifeLink data is far too small to train on, so labels
here are drawn from an ASSUMED behavioural model (nearer, more reliable, faster-responding donors
are likelier to complete; night-time and far-away/low-urgency cases are riskier; plus a hidden
"donor reliability" trait and random noise so the problem is not trivially separable).
The model therefore learns those assumptions; retrain on real rows (export from request_assigns)
once enough accumulate.

Run:  python -m app.generate_synthetic_data [--rows 6000] [--seed 42]
"""
import argparse
import numpy as np
import pandas as pd
from .features import DATA_PATH, FEATURES, TARGET


def sigmoid(z):
    return 1.0 / (1.0 + np.exp(-z))


def generate(n: int, seed: int) -> pd.DataFrame:
    rng = np.random.default_rng(seed)

    trait = rng.normal(0, 1, n)  # hidden donor reliability, never given to the model

    urgency = rng.choice([0, 1, 2, 3], size=n, p=[0.15, 0.35, 0.30, 0.20])
    quantity = rng.choice([1, 2, 3, 4, 5, 6], size=n, p=[0.45, 0.25, 0.15, 0.08, 0.05, 0.02])

    coords_known = (rng.random(n) > 0.08).astype(int)
    distance = np.clip(rng.gamma(2.0, 8.0, n), 0.2, 150.0)
    distance = np.where(coords_known == 1, distance, 50.0)

    completed = rng.poisson(np.exp(0.6 + 0.3 * trait))
    let_down = rng.poisson(np.exp(-0.9 - 0.7 * trait))
    review_count = rng.binomial(completed, 0.6)
    avg_rating = np.clip(4.2 + 0.35 * trait + rng.normal(0, 0.5, n) / np.sqrt(np.maximum(review_count, 1)), 1, 5)
    avg_rating = np.where(review_count > 0, np.round(avg_rating, 2), 4.0)

    delay = np.clip(rng.lognormal(3.0 - 0.2 * trait - 0.15 * urgency, 0.9, n), 1, 1440)
    hour_w = np.array([0.3] * 7 + [1.0] * 15 + [0.3] * 2)
    hour = rng.choice(24, size=n, p=hour_w / hour_w.sum())
    weekend = (rng.random(n) < 2 / 7).astype(int)
    exact = (rng.random(n) < 0.55).astype(int)

    night = ((hour < 6) | (hour >= 23)).astype(int)
    z = (
        1.9
        + 0.55 * trait
        - 0.035 * np.maximum(distance - 5, 0)
        - 0.03 * np.maximum(distance - 20, 0) * (urgency <= 1)  # far + not urgent -> drops out
        - 0.9 * (distance > 40)                                  # threshold effect: very far donors rarely come
        - 0.8 * (delay > 360)                                    # slow responders tend to go silent
        - 0.7 * ((let_down >= 2) & (urgency <= 1))             # repeat let-downs on non-urgent cases
        + 0.6 * ((completed >= 3) & (let_down == 0))            # proven donors
        - 0.25 * (1 - coords_known)
        + 0.25 * urgency
        - 0.12 * (quantity - 1)
        - 0.45 * np.log1p(delay / 15.0)
        - 0.50 * night
        - 0.20 * weekend
        + 0.15 * exact
        + rng.normal(0, 0.45, n)
    )
    y = (rng.random(n) < sigmoid(z)).astype(int)

    df = pd.DataFrame({
        "distance_km": np.round(distance, 2),
        "coords_known": coords_known,
        "completed_count": completed,
        "let_down_count": let_down,
        "avg_rating": avg_rating,
        "review_count": review_count,
        "urgency": urgency,
        "quantity": quantity,
        "response_delay_min": np.round(delay, 1),
        "exact_match": exact,
        "hour_of_day": hour,
        "is_weekend": weekend,
        TARGET: y,
    })
    return df[FEATURES + [TARGET]]


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--rows", type=int, default=8000)
    ap.add_argument("--seed", type=int, default=42)
    args = ap.parse_args()
    data = generate(args.rows, args.seed)
    DATA_PATH.parent.mkdir(parents=True, exist_ok=True)
    data.to_csv(DATA_PATH, index=False)
    print(f"Wrote {len(data)} rows to {DATA_PATH}  (completion rate {data[TARGET].mean():.1%})")
