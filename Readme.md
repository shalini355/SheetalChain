<div align="center">
🌾 SheetalChain
Shared Cold Storage Monitoring — Built for Small Farmers

A hardware-agnostic monitoring platform that prevents negligence-driven spoilage in shared community cold storage facilities.

Built for First Commit — Bharat Builds Tour 2026 Track: Build It (Open Source / Local Stack) + Best UI

Java Spring Boot Python LocalStack Show Image

</div>
🎯 The Problem

India loses an estimated 15–20% of its fruits and vegetables post-harvest every year. But that number hides an important detail: most of the loss doesn't happen in one place.

Stage	Typical Loss
Farm / field	15–20%
Packaging	15–20%
Transportation	30–40%
Marketing / selling	30–40%

Most of it comes from structural problems — poor transport infrastructure, market access, and the simple fact that individual small farmers almost never own cold storage. India has only around 6,700 cold storage facilities nationwide, and the ones that exist are typically shared, community-level infrastructure run through Farmer Producer Organizations (FPOs), village co-operatives, or government schemes like PM-KUSUM and APMC mandis.

SheetalChain doesn't claim to fix all of that. We're solving one clear, well-defined problem:

Where shared cold storage already exists, silent temperature failures go undetected — until it's too late. No alerts, no accountability, no visibility for the farmers whose produce is at risk.

💡 The Solution

SheetalChain is a real-time monitoring and alerting platform for shared cold storage facilities — built so that every farmer sharing a facility can trust it's being watched properly, and every temperature failure is caught the moment it happens, not after the produce is already spoiled.

Because the storage is shared across many farmers, the platform is built around two roles:

👨‍🌾 Farmer — sees only their own stored batch's live status and alerts
🧑‍💼 Manager (e.g. an FPO coordinator) — full facility oversight across every unit, every farmer
🔌 A Note on "Hardware-Agnostic" (not "no hardware, ever")

This prototype uses simulated telemetry instead of physical IoT sensors — not because the concept doesn't need sensors, but because building and deploying real hardware wasn't feasible in a 4-day hackathon sprint. The backend's POST /api/telemetry endpoint doesn't care where a reading comes from — a Python script or a real temperature sensor sends the exact same JSON payload. The architecture was deliberately built so that swapping simulated data for real IoT hardware later requires zero changes to the core system. What we've built is the software backbone a real deployment would need — proven and working, ready for real sensors when the infrastructure allows it.

✨ Features
📡 Live Telemetry Ingestion — real-time temperature, humidity & GPS data per storage unit
🧠 Dual-Layer Alerting — a fast backend threshold check and an independent AI monitoring agent that analyzes multi-reading temperature trends (not just a single spike) to tell a real thermal failure apart from a brief door-open blip
🔐 Role-Based Access Control — Cedar-policy-inspired authorization: Farmers see only their own unit, Managers see everything
☁️ Local AWS Stack — LocalStack + DynamoDB, zero cloud cost, runs entirely offline
📊 Live Dashboard — real-time status cards, live temperature charts, an interactive temperature slider for on-demand demo control, and a live alert feed
🤖 Explainable AI Reasoning — every AI-flagged alert comes with a plain-language explanation of why (e.g. "rising 0.6°C/step over last 4 readings")
🏗️ Architecture
┌─────────────────────┐        ┌──────────────────────┐
│  Mock Telemetry      │──POST─▶│  Spring Boot Backend  │◀────── Real IoT sensors
│  Generator (Python)  │        │  (REST API)           │        (future — same endpoint)
└─────────────────────┘        └──────────┬───────────┘
                                            │
                    ┌───────────────────────┼───────────────────────┐
                    ▼                       ▼                       ▼
          ┌──────────────────┐   ┌──────────────────┐   ┌──────────────────┐
          │  LocalStack       │   │  Role-Based       │   │  AI Monitoring   │
          │  (DynamoDB)       │   │  Access Control    │   │  Agent (Python)  │
          └──────────────────┘   └──────────────────┘   └──────────────────┘
                                            │
                                            ▼
                                  ┌──────────────────┐
                                  │  Live Dashboard    │
                                  │  (Manager/Farmer)  │
                                  └──────────────────┘
🛠️ Tech Stack
Layer	Technology
Backend	Java 21, Spring Boot 3.x, AWS SDK for Java
Local Cloud Simulation	LocalStack (DynamoDB)
Authorization	Cedar-policy-inspired RBAC/ABAC
AI Monitoring Agent	Python, Strands Agents SDK architecture, trend-velocity analysis
IoT Simulation	Python
Frontend	HTML/JS dashboard with live charts
Containerization	Docker
🚀 Getting Started
Prerequisites
Java 21+, Maven 3.9+
Docker Desktop
Python 3.12+
A free LocalStack Personal Auth Token
1. Start LocalStack
bash
localstack auth set-token YOUR_LOCALSTACK_TOKEN
localstack start -d
2. Run the backend (also serves the dashboard)
bash
cd backend
mvn spring-boot:run

Visit http://localhost:8080 — the live dashboard loads automatically.

3. Start the telemetry simulator
bash
python scripts/iot_simulator.py
4. Start the AI monitoring agent
bash
python ai-agent/monitoring_agent.py

Watch the dashboard update live as telemetry streams in and alerts fire.

🎥 Demo Highlights

The demo video shows:

LocalStack + Spring Boot running together, tables created live
The dashboard updating in real time as telemetry streams in
A manual temperature spike (via the dashboard slider) triggering both the backend's rule-based alert and the AI agent's trend-based alert
Switching between Manager and Farmer views to show role-based access in action — a Farmer only ever sees their own unit
⚠️ Scope & Limitations — Being Upfront

We'd rather tell you this than have it come up in Q&A:

This is a prototype. Telemetry is simulated, not read from physical sensors — see the "Hardware-Agnostic" note above for why, and why that's a deliberate, low-risk choice rather than a shortcut.
We don't address transport or market-stage losses (30–40% each), which are larger contributors to India's post-harvest waste than storage-stage losses. SheetalChain is scoped specifically to the storage/monitoring layer.
We don't solve the access problem. Most farmers still don't have access to any cold storage, shared or otherwise. SheetalChain assumes a facility already exists and makes its use safer and more accountable.
No real-world pilot yet. Real deployment would need testing with an actual FPO or storage facility, usability testing with non-technical users, and integration work with real sensor hardware.

We see SheetalChain as a focused, working proof-of-concept — a real software backbone that's ready to plug into real IoT sensors and real shared cold storage facilities as that infrastructure grows.

📂 Project Structure
sheetalchain/
├── backend/               # Spring Boot REST API + dashboard hosting
├── scripts/               # Python mock IoT telemetry generator
├── ai-agent/              # AI monitoring agent (trend-based risk analysis)
├── cedar-policies/        # Role-based authorization policy definitions
└── README.md
🙋 Author

Shalini Yadav Final-year B.Tech CSE @ Pranveer Singh Institute of Technology GitHub · LinkedIn

<div align="center">

Built in 4 days for First Commit — Bharat Builds Tour 2026 🌾

</div>