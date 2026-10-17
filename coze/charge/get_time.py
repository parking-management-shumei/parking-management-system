from runtime import Args
from typings.get_time.get_time import Input, Output
import os
import requests

def handler(args: Args[Input]) -> Output:
    # Configure these in the Coze runtime/secret settings; do not commit credentials.
    base = os.environ.get("PARKING_API_URL", "").rstrip("/")
    token = os.environ.get("COZE_SERVICE_TOKEN", "")
    if not base or len(token) < 32:
        raise ValueError("Parking callback URL/service token is not configured")
    response = requests.get(base + "/api/integration/time",
        params={"car_number": args.input.car_number},
        headers={"Authorization": "Bearer " + token}, timeout=8)
    response.raise_for_status()
    data = response.json()
    return {key: data[key] for key in ['time']}
