from runtime import Args
from typings.Parking_charging_system.Parking_charging_system import Input, Output
from decimal import Decimal, ROUND_HALF_UP
from math import ceil

def calculate_price(total_seconds, reg1, reg2, reg3, unit, free_time):
    """Grace threshold, started-unit billing, cap for each 24h block. Matches Java BillingService for unit=60."""
    seconds = int(total_seconds)
    unit = int(unit)
    free_time = int(free_time)
    first, cap, rate = (Decimal(str(v)) for v in (reg1, reg2, reg3))
    if seconds < 0 or unit <= 0 or 1440 % unit != 0 or free_time < 0:
        raise ValueError("Invalid duration, billing unit or grace period")
    if not all(v.is_finite() and v >= 0 for v in (first, cap, rate)):
        raise ValueError("Invalid fee")
    if seconds <= free_time * 60:
        return 0.0
    days, remainder = divmod(seconds, 86400)
    full = min(cap, first + max(0, 1440 // unit - 1) * rate)
    partial = min(cap, first + max(0, ceil(remainder / (unit * 60)) - 1) * rate) if remainder else Decimal(0)
    return float((days * full + partial).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP))

def handler(args: Args[Input]) -> Output:
    i = args.input
    return {"price": calculate_price(i.total_seconds, i.reg1, i.reg2, i.reg3, i.unit, i.free_time)}
