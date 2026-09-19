#!/usr/bin/env python3
"""
Strands AI Monitoring Agent for SheetalChain Cold Storage Network.

Architecture Note:
Built using the Strands Agents SDK tool/model architecture patterns (@tool decorators
and Strands tool specifications) with deterministic multi-reading trend analysis as the
core decision engine — not live LLM-based reasoning.
"""

import os
import sys
import time
import requests
from typing import List, Dict, Any, Optional
from dataclasses import dataclass

# Ensure UTF-8 encoding for standard output on Windows terminals to prevent UnicodeEncodeError
if sys.platform == "win32":
    try:
        if hasattr(sys.stdout, "reconfigure"):
            sys.stdout.reconfigure(encoding="utf-8", errors="replace")
        if hasattr(sys.stderr, "reconfigure"):
            sys.stderr.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass

# Strands Agents SDK imports (used for tool schema decorations and agent model specs)
try:
    import strands
    from strands import tool
    from strands.models import Model
    STRANDS_AVAILABLE = True
except ImportError:
    STRANDS_AVAILABLE = False
    print("⚠️ Strands Agents SDK not found. Install via 'pip install strands-agents'")


BACKEND_URL = os.getenv("BACKEND_URL", "http://localhost:8080")
MANAGER_HEADER = {"X-User-Role": "MANAGER"}
POLL_INTERVAL_SECONDS = 5.0

# Store recent alert states to prevent duplicate spamming for unchanged critical states
alerted_states: Dict[str, str] = {}


# ============================================================================
# Strands Agent Tools (Decorated with @tool)
# ============================================================================
@tool
def fetch_active_units() -> List[Dict[str, Any]]:
    """
    Tool: Fetches the list of active cold storage units from the Spring Boot backend.
    """
    try:
        url = f"{BACKEND_URL}/api/units"
        response = requests.get(url, headers=MANAGER_HEADER, timeout=3.0)
        if response.status_code == 200:
            return response.json()
    except Exception as e:
        print(f"❌ [Agent Tool Error] Failed to fetch units: {e}")
    return []


@tool
def fetch_unit_telemetry(unit_id: str) -> List[Dict[str, Any]]:
    """
    Tool: Fetches the recent telemetry readings for a specific cold storage unit.
    """
    try:
        url = f"{BACKEND_URL}/api/telemetry/{unit_id}"
        response = requests.get(url, headers=MANAGER_HEADER, timeout=3.0)
        if response.status_code == 200:
            return response.json()
    except Exception as e:
        print(f"❌ [Agent Tool Error] Failed to fetch telemetry for unit {unit_id}: {e}")
    return []


@tool
def post_agent_alert(
    unit_id: str,
    temperature: float,
    severity: str,
    message: str,
    reasoning: str
) -> bool:
    """
    Tool: Posts an independently flagged AI Agent alert to the backend endpoint POST /api/alerts/agent-flag.
    """
    try:
        url = f"{BACKEND_URL}/api/alerts/agent-flag"
        payload = {
            "unitId": unit_id,
            "temperature": temperature,
            "severity": severity,
            "message": message,
            "reasoning": reasoning
        }
        response = requests.post(url, json=payload, headers=MANAGER_HEADER, timeout=3.0)
        if response.status_code in (200, 201):
            return True
        else:
            print(f"❌ [Agent Tool Error] Alert post failed with status {response.status_code}: {response.text}")
    except Exception as e:
        print(f"❌ [Agent Tool Error] Exception posting alert: {e}")
    return False


# ============================================================================
# Multi-Reading Spoilage Analysis Engine (Velocity & Trend Evaluation)
# ============================================================================
@dataclass
class RiskEvaluation:
    assessment: str        # SAFE, WATCH, or CRITICAL
    trend_description: str # e.g. "rising over last 3 readings (+0.7°C/step)"
    latest_temp: float
    reasoning: str


def evaluate_telemetry_trend(readings: List[Dict[str, Any]]) -> RiskEvaluation:
    """
    Evaluates spoilage risk with multi-reading trend analysis & temperature velocity.

    Distinguishes:
    1. Flat High (> 8.0°C sustained) -> CRITICAL
    2. Fast Rising Trend (escalating towards or past 8.0°C) -> CRITICAL / WATCH
    3. Transient Blip (temp > 8.0°C but dropping sharply) -> WATCH / SAFE
    4. Stable Safe (< 8.0°C) -> SAFE
    """
    if not readings:
        return RiskEvaluation(
            assessment="SAFE",
            trend_description="no readings available",
            latest_temp=0.0,
            reasoning="No sensor data received yet."
        )

    # Telemetry comes sorted descending by timestamp (latest first)
    latest = readings[0]
    current_temp = float(latest.get("temperature", 0.0))

    # Get last 3 to 5 readings in chronological order (oldest to newest)
    sample_window = readings[:min(len(readings), 5)]
    sample_window.reverse()
    temps = [float(r.get("temperature", 0.0)) for r in sample_window]

    if len(temps) < 2:
        if current_temp > 8.0:
            return RiskEvaluation(
                assessment="CRITICAL",
                trend_description="single reading above threshold",
                latest_temp=current_temp,
                reasoning=f"Current temperature {current_temp}°C breaches 8.0°C safety threshold."
            )
        return RiskEvaluation(
            assessment="SAFE",
            trend_description="single reading normal",
            latest_temp=current_temp,
            reasoning=f"Current temperature {current_temp}°C within safe range."
        )

    # Compute temperature changes between successive readings
    diffs = [temps[i] - temps[i-1] for i in range(1, len(temps))]
    avg_delta = sum(diffs) / len(diffs)
    total_delta = temps[-1] - temps[0]
    reading_count = len(temps)

    is_rising = total_delta > 0.3 and avg_delta > 0.1
    is_falling = total_delta < -0.3 and avg_delta < -0.1

    # Describe trend string
    if is_rising:
        trend_str = f"rising over last {reading_count} readings (+{avg_delta:.1f}°C/step)"
    elif is_falling:
        trend_str = f"recovering/falling over last {reading_count} readings ({avg_delta:.1f}°C/step)"
    else:
        trend_str = f"stable over last {reading_count} readings (avg {sum(temps)/len(temps):.1f}°C)"

    # Risk decision logic
    if current_temp > 8.0:
        if is_falling and current_temp < 9.0:
            # Temp exceeded threshold slightly but is actively recovering (e.g., door was opened briefly)
            assessment = "WATCH"
            reasoning = (
                f"Temperature is {current_temp}°C (above 8.0°C threshold), but actively falling ({avg_delta:.1f}°C/step). "
                f"Likely transient door open event. Monitoring recovery."
            )
        else:
            # High and sustained or rising
            assessment = "CRITICAL"
            reasoning = (
                f"Temperature {current_temp}°C exceeds 8.0°C safety threshold and is {trend_str}. "
                f"High spoilage risk detected for perishable cold chain assets. Immediate cooling intervention required."
            )
    elif current_temp >= 6.5 and is_rising:
        # Temperature is approaching unsafe threshold rapidly
        assessment = "WATCH"
        reasoning = (
            f"Temperature is {current_temp}°C and rapidly rising (+{avg_delta:.1f}°C/step) across last {reading_count} readings. "
            f"Predictive anomaly detected before threshold breach."
        )
    else:
        assessment = "SAFE"
        reasoning = f"Temperature {current_temp}°C is within safe operating range (4°C-8°C). Trend: {trend_str}."

    return RiskEvaluation(
        assessment=assessment,
        trend_description=trend_str,
        latest_temp=current_temp,
        reasoning=reasoning
    )


# ============================================================================
# Strands Monitoring Agent Service Class
# ============================================================================
class StrandsMonitoringAgent:
    """
    Monitoring Agent service utilizing Strands SDK tool patterns for unit inspection,
    trend evaluation, and alert posting.
    """

    def run_cycle(self):
        """
        Executes a single monitoring cycle across all units using Strands tools.
        """
        units = fetch_active_units()
        if not units:
            print("ℹ️ [Agent] No active units retrieved from backend.")
            return

        for unit in units:
            unit_id = unit.get("unitId")
            location = unit.get("location", "Unknown Location")

            readings = fetch_unit_telemetry(unit_id)
            eval_result = evaluate_telemetry_trend(readings)

            # Format requirement:
            # [Agent] Unit e709ecbd (Nashik) — Current: 9.2°C, Trend: rising over last 3 readings — Assessment: CRITICAL — Recommending immediate action
            console_msg = (
                f"[Agent] Unit {unit_id[:8]} ({location}) — Current: {eval_result.latest_temp:.1f}°C, "
                f"Trend: {eval_result.trend_description} — Assessment: {eval_result.assessment}"
            )
            if eval_result.assessment == "CRITICAL":
                console_msg += " — Recommending immediate action"
            elif eval_result.assessment == "WATCH":
                console_msg += " — Elevated risk watch"
            else:
                console_msg += " — Conditions normal"

            print(console_msg)

            # Trigger backend alert for CRITICAL assessment (with deduplication)
            if eval_result.assessment == "CRITICAL":
                last_alert = alerted_states.get(unit_id)
                if last_alert != "CRITICAL":
                    alert_msg = f"AI Agent Spoilage Risk: Temperature {eval_result.latest_temp:.1f}°C ({eval_result.trend_description})"
                    success = post_agent_alert(
                        unit_id=unit_id,
                        temperature=eval_result.latest_temp,
                        severity="CRITICAL",
                        message=alert_msg,
                        reasoning=eval_result.reasoning
                    )
                    if success:
                        print(f"  └─ 🚨 [Agent Alert Posted] Logged AI_AGENT alert to backend for unit {unit_id[:8]}")
                        alerted_states[unit_id] = "CRITICAL"
            else:
                # Reset alerted state when unit returns to SAFE or WATCH
                alerted_states[unit_id] = eval_result.assessment


def main():
    print("=================================================")
    print("🤖 Strands AI Cold Storage Monitoring Agent Active")
    print(f"📡 Backend: {BACKEND_URL} | Interval: {POLL_INTERVAL_SECONDS}s")
    print("=================================================")

    agent_runner = StrandsMonitoringAgent()

    try:
        while True:
            agent_runner.run_cycle()
            time.sleep(POLL_INTERVAL_SECONDS)
    except KeyboardInterrupt:
        print("\n🛑 Strands AI Agent shut down safely.")


if __name__ == "__main__":
    main()
