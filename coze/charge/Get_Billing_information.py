from runtime import Args
from typings.Get_Billing_information.Get_Billing_information import Input, Output
import os
import requests

def handler(args: Args[Input]) -> Output:
    # Configure these in the Coze runtime/secret settings; do not commit credentials.
    base = os.environ.get("PARKING_API_URL", "").rstrip("/")
    token = os.environ.get("COZE_SERVICE_TOKEN", "")
    if not base or len(token) < 32:
        raise ValueError("Parking callback URL/service token is not configured")
    response = requests.get(base + "/api/integration/billing",
        params={"numberplate": args.input.numberplate},
        headers={"Authorization": "Bearer " + token}, timeout=8)
    response.raise_for_status()
    data = response.json()
    return {key: data[key] for key in ['reg1', 'reg2', 'reg3', 'unit', 'free_time']}
