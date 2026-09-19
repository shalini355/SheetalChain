#!/usr/bin/env python3
"""
===============================================================================
SheetalChain - Simulated IoT Telemetry Generator
===============================================================================
Simulates IoT sensors attached to micro-cold-storage units used by small farmers.
Streams real-time temperature, humidity, and location telemetry to the Spring Boot
backend (POST /api/telemetry).

Features:
- Pure Python 3 standard library (no external packages required)
- Auto-discovers existing units or registers demo units on launch
- Simulates normal cooling (2.0°C - 6.0°C) and periodic cooling failures (> 8.0°C)
- Passes Cedar security header 'X-User-Role: MANAGER' for telemetry ingestion
===============================================================================
"""

import argparse
import json
import random
import sys
import time
from urllib.request import Request, urlopen
from urllib.error import URLError, HTTPError

# Ensure UTF-8 stdout encoding on Windows consoles
if hasattr(sys.stdout, "reconfigure"):
    try:
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass

# ANSI Color Codes for Terminal Output
COLOR_GREEN = "\033[92m"
COLOR_RED = "\033[91m"
COLOR_YELLOW = "\033[93m"
COLOR_CYAN = "\033[96m"
COLOR_BOLD = "\033[1m"
COLOR_RESET = "\033[0m"

# Default Demo Units to bootstrap if backend table is empty
DEMO_UNITS = [
    {
        "farmerId": "FARMER-101",
        "farmerName": "Ramesh Kumar",
        "location": "Nashik, Maharashtra",
        "baseLat": 19.9975,
        "baseLon": 73.7898
    },
    {
        "farmerId": "FARMER-102",
        "farmerName": "Suresh Patel",
        "location": "Baramati, Maharashtra",
        "baseLat": 18.1507,
        "baseLon": 74.5772
    },
    {
        "farmerId": "FARMER-103",
        "farmerName": "Sunita Patil",
        "location": "Sangli, Maharashtra",
        "baseLat": 16.8524,
        "baseLon": 74.5815
    }
]

def make_request(url, method="GET", body=None, headers=None):
    """Sends an HTTP request using urllib standard library."""
    if headers is None:
        headers = {}

    headers["X-User-Role"] = "MANAGER"
    
    data = None
    if body is not None:
        data = json.dumps(body).encode("utf-8")
        headers["Content-Type"] = "application/json"

    req = Request(url, data=data, headers=headers, method=method)
    try:
        with urlopen(req) as resp:
            resp_body = resp.read().decode("utf-8")
            return resp.status, json.loads(resp_body) if resp_body else {}
    except HTTPError as e:
        err_body = e.read().decode("utf-8")
        try:
            parsed_err = json.loads(err_body)
        except Exception:
            parsed_err = {"raw": err_body}
        return e.code, parsed_err
    except URLError as e:
        return 500, {"error": f"Connection failed: {e.reason}"}

def bootstrap_units(base_url):
    """Ensures cold storage units exist on the backend."""
    print(f"{COLOR_CYAN}[LOOKUP] Checking registered cold storage units on backend...{COLOR_RESET}")
    status, units = make_request(f"{base_url}/api/units")
    
    if status != 200:
        print(f"{COLOR_RED}[ERROR] Failed to fetch units from {base_url}/api/units (Status: {status}){COLOR_RESET}")
        sys.exit(1)

    if not isinstance(units, list):
        units = []

    if len(units) > 0:
        print(f"{COLOR_GREEN}[FOUND] Found {len(units)} existing unit(s) registered on backend.{COLOR_RESET}\n")
        return units

    print(f"{COLOR_YELLOW}[BOOTSTRAP] No units found. Auto-registering demo cold storage units...{COLOR_RESET}")
    registered = []
    for demo in DEMO_UNITS:
        payload = {
            "farmerId": demo["farmerId"],
            "farmerName": demo["farmerName"],
            "location": demo["location"]
        }
        status, new_unit = make_request(f"{base_url}/api/units", method="POST", body=payload)
        if status in (200, 201):
            new_unit["baseLat"] = demo["baseLat"]
            new_unit["baseLon"] = demo["baseLon"]
            registered.append(new_unit)
            print(f"  [REGISTERED] {new_unit['farmerName']} ({new_unit['location']}) -> ID: {new_unit['unitId']}")
        else:
            print(f"  [ERROR] Failed to register {demo['farmerName']}: {new_unit}")

    print(f"{COLOR_GREEN}[SUCCESS] Auto-registration complete! Total units: {len(registered)}{COLOR_RESET}\n")
    return registered

def generate_telemetry_reading(unit, spike_probability=0.25):
    """Generates a realistic telemetry reading, occasionally simulating a temperature spike."""
    is_spike = random.random() < spike_probability

    if is_spike:
        # Temperature breach > 8.0°C (triggers backend Alert)
        temperature = round(random.uniform(8.5, 13.8), 2)
    else:
        # Normal safe cooling temperature <= 8.0°C
        temperature = round(random.uniform(2.1, 6.2), 2)

    humidity = round(random.uniform(80.0, 94.0), 1)

    # Slight GPS jitter (+/- 0.001 deg) to simulate real sensor readings
    base_lat = unit.get("baseLat", 19.9975)
    base_lon = unit.get("baseLon", 73.7898)
    lat = round(base_lat + random.uniform(-0.001, 0.001), 4)
    lon = round(base_lon + random.uniform(-0.001, 0.001), 4)

    return {
        "unitId": unit["unitId"],
        "temperature": temperature,
        "humidity": humidity,
        "latitude": lat,
        "longitude": lon
    }, is_spike

def run_simulation(base_url, interval, max_cycles, spike_prob):
    """Main simulation loop streaming telemetry to the Spring Boot REST API."""
    units = bootstrap_units(base_url)
    if not units:
        print(f"{COLOR_RED}[ERROR] No units available for telemetry streaming.{COLOR_RESET}")
        return

    print(f"{COLOR_BOLD}{COLOR_CYAN}========================================================================={COLOR_RESET}")
    print(f"{COLOR_BOLD}[START] STARTING IOT TELEMETRY SIMULATION LOOP{COLOR_RESET}")
    print(f"   Backend Endpoint : {base_url}/api/telemetry")
    print(f"   Interval         : Every {interval} seconds")
    print(f"   Mode             : {'Single Batch (--once)' if max_cycles == 1 else 'Continuous Loop'}")
    print(f"{COLOR_BOLD}{COLOR_CYAN}========================================================================={COLOR_RESET}\n")

    cycle = 1
    try:
        while True:
            timestamp_str = time.strftime("%H:%M:%S")
            print(f"{COLOR_BOLD}--- Cycle #{cycle} [{timestamp_str}] ---{COLOR_RESET}")

            for unit in units:
                reading, is_spike = generate_telemetry_reading(unit, spike_probability=spike_prob)
                status, response = make_request(f"{base_url}/api/telemetry", method="POST", body=reading)

                farmer_name = unit.get("farmerName", "Farmer")
                temp = reading["temperature"]
                hum = reading["humidity"]
                unit_id = unit["unitId"][:8] + "..."

                if status in (200, 201):
                    if is_spike or temp > 8.0:
                        print(f"  {COLOR_RED}{COLOR_BOLD}[ALERT TRIGGERED]{COLOR_RESET} Unit {unit_id} ({farmer_name}): "
                              f"{COLOR_RED}{temp}°C{COLOR_RESET} | {hum}% Hum | "
                              f"Spoilage Risk Breach (>8.0°C)")
                    else:
                        print(f"  {COLOR_GREEN}[SAFE]{COLOR_RESET}            Unit {unit_id} ({farmer_name}): "
                              f"{COLOR_GREEN}{temp}°C{COLOR_RESET} | {hum}% Hum | Optimal Cooling")
                else:
                    print(f"  {COLOR_RED}[ERROR {status}]{COLOR_RESET} Unit {unit_id}: {response}")

            if max_cycles and cycle >= max_cycles:
                print(f"\n{COLOR_GREEN}[COMPLETE] Simulation completed after {max_cycles} cycle(s).{COLOR_RESET}")
                break

            cycle += 1
            time.sleep(interval)

    except KeyboardInterrupt:
        print(f"\n{COLOR_YELLOW}[STOPPED] IoT Simulator stopped by user.{COLOR_RESET}")

def main():
    parser = argparse.ArgumentParser(description="SheetalChain IoT Telemetry Simulator")
    parser.add_argument("--url", default="http://localhost:8080", help="Spring Boot backend base URL")
    parser.add_argument("--interval", type=float, default=3.0, help="Interval in seconds between simulation cycles")
    parser.add_argument("--count", type=int, default=0, help="Number of cycles to run (0 for infinite loop)")
    parser.add_argument("--once", action="store_true", help="Run a single telemetry cycle and exit")
    parser.add_argument("--spike-prob", type=float, default=0.30, help="Probability (0.0 to 1.0) of temperature spike breach")

    args = parser.parse_args()
    max_cycles = 1 if args.once else args.count

    run_simulation(args.url, args.interval, max_cycles, args.spike_prob)

if __name__ == "__main__":
    main()
