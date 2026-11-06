"""
GOAL: quick sanity check of the running service (no curl quoting problems on Windows).
Run (service must be up):  python -m app.smoke_test
Expect: the 'strong' donor scores clearly higher than the 'weak' one.
"""
import json
import urllib.request

strong = dict(distance_km=3, coords_known=1, completed_count=6, let_down_count=0, avg_rating=4.8,
              review_count=5, urgency=3, quantity=1, response_delay_min=4, exact_match=1,
              hour_of_day=14, is_weekend=0)
weak = dict(distance_km=70, coords_known=1, completed_count=0, let_down_count=3, avg_rating=3.0,
            review_count=2, urgency=0, quantity=4, response_delay_min=900, exact_match=0,
            hour_of_day=3, is_weekend=1)

req = urllib.request.Request("http://localhost:8001/predict",
                             data=json.dumps({"candidates": [strong, weak]}).encode(),
                             headers={"Content-Type": "application/json"})
print(json.load(urllib.request.urlopen(req)))
