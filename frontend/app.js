const state = {
  role: 'MANAGER',
  farmerId: 'FARMER-101',
  units: [],
  alerts: [],
  charts: new Map(),
  previousStatuses: new Map(),
  knownAlertIds: new Set(),
};

const els = {
  unitCount: document.getElementById('unit-count'),
  riskCount: document.getElementById('risk-count'),
  alertCount: document.getElementById('alert-count'),
  unitList: document.getElementById('unit-list'),
  alertFeed: document.getElementById('alert-feed'),
  alertCountLabel: document.getElementById('alert-count-label'),
  roleButtons: [...document.querySelectorAll('.role-btn')],
};

async function request(path, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
  if (state.role === 'FARMER') {
    headers['X-User-Role'] = 'FARMER';
    headers['X-Farmer-Id'] = state.farmerId;
  } else {
    headers['X-User-Role'] = 'MANAGER';
  }

  const response = await fetch(path, { ...options, headers });
  const text = await response.text();
  return text ? JSON.parse(text) : {};
}

function getLatestTemperature(unitId) {
  const unitTelemetry = state.telemetryMap?.[unitId] || [];
  if (!unitTelemetry.length) return { value: 0, timestamp: null };
  const latest = [...unitTelemetry].sort((a, b) => b.timestamp - a.timestamp)[0];
  return { value: latest.temperature, timestamp: latest.timestamp };
}

async function loadData() {
  const units = await request('/api/units');
  state.units = Array.isArray(units) ? units : [];

  const telemetryMap = {};
  for (const unit of state.units) {
    const telemetry = await request(`/api/telemetry/${unit.unitId}`);
    telemetryMap[unit.unitId] = Array.isArray(telemetry) ? telemetry : [];
  }
  state.telemetryMap = telemetryMap;

  const alerts = await request('/api/alerts');
  state.alerts = Array.isArray(alerts) ? alerts : [];

  render();
}

function formatAlertGroups(alerts) {
  const groups = [];
  (Array.isArray(alerts) ? alerts : []).forEach((alert) => {
    const existing = groups.find((group) => (
      group.alert.unitId === alert.unitId
      && group.alert.source === alert.source
      && group.alert.message === alert.message
    ));
    if (existing) {
      existing.count += 1;
    } else {
      groups.push({ alert, count: 1 });
    }
  });
  return groups;
}

function createChart(unitId, readings) {
  const canvas = document.querySelector(`[data-chart-id="${unitId}"]`);
  if (!canvas || typeof Chart === 'undefined') return;
  const ordered = [...readings].sort((a, b) => a.timestamp - b.timestamp).slice(-20);
  const chartData = {
    labels: ordered.map((reading) => new Date(reading.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })),
    datasets: [{
      data: ordered.map((reading) => reading.temperature),
      borderColor: '#72e3c2',
      backgroundColor: 'rgba(114, 227, 194, 0.12)',
      borderWidth: 2,
      pointRadius: 2,
      pointHoverRadius: 5,
      pointBackgroundColor: '#f4c95d',
      fill: true,
      tension: 0.35,
    }],
  };
  const existing = state.charts.get(unitId);
  if (existing) {
    existing.data = chartData;
    existing.update('none');
    return;
  }
  state.charts.set(unitId, new Chart(canvas, {
    type: 'line',
    data: chartData,
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: { legend: { display: false }, tooltip: { displayColors: false } },
      scales: {
        x: { display: false },
        y: {
          min: -5,
          max: 15,
          ticks: { color: '#8fa9ad', maxTicksLimit: 4, callback: (value) => `${value}°` },
          grid: { color: 'rgba(255,255,255,0.07)' },
        },
      },
    },
  }));
}

function bindUnitControls() {
  document.querySelectorAll('.temperature-slider').forEach((slider) => {
    const value = document.querySelector(`[data-value-for="${slider.dataset.unitId}"]`);
    slider.addEventListener('input', () => {
      value.textContent = `${Number(slider.value).toFixed(1)}°C`;
    });
    slider.addEventListener('change', async () => {
      slider.disabled = true;
      await request('/api/telemetry', {
        method: 'POST',
        body: JSON.stringify({
          unitId: slider.dataset.unitId,
          temperature: Number(slider.value),
          humidity: 86,
          latitude: 19.99,
          longitude: 73.78,
        }),
      });
      await loadData();
    });
  });
}

function render() {
  const visibleUnits = state.units;
  const riskUnits = visibleUnits.filter((unit) => {
    const temp = getLatestTemperature(unit.unitId).value;
    return temp > 8;
  }).length;

  els.unitCount.textContent = String(visibleUnits.length);
  els.riskCount.textContent = String(riskUnits);
  els.alertCount.textContent = String(Array.isArray(state.alerts) ? state.alerts.length : 0);

  state.charts.forEach((chart) => chart.destroy());
  state.charts.clear();
  els.unitList.innerHTML = visibleUnits.map((unit) => {
    const latest = getLatestTemperature(unit.unitId);
    const temp = latest.value;
    const alerting = temp > 8;
    const humidity = state.telemetryMap?.[unit.unitId]?.slice(-1)[0]?.humidity || 0;
    const lastSeen = latest.timestamp ? new Date(latest.timestamp).toLocaleTimeString() : 'No data';
    const previousStatus = state.previousStatuses.get(unit.unitId);
    const changedToRisk = previousStatus === 'safe' && alerting;
    state.previousStatuses.set(unit.unitId, alerting ? 'risk' : 'safe');
    return `
      <article class="unit-card ${alerting ? 'risk' : ''} ${changedToRisk ? 'status-pulse' : ''}">
        <div class="unit-top">
          <div>
            <div class="unit-name">${unit.farmerName}</div>
            <div class="meta">${unit.location}</div>
          </div>
          <span class="badge ${alerting ? 'risk' : 'safe'}">${alerting ? 'Risk' : 'Safe'}</span>
        </div>
        <div class="meta">Unit: ${unit.unitId} • Farmer: ${unit.farmerId}</div>
        <div class="metric-row">
          <div class="metric-box"><span>Temp</span><strong>${temp.toFixed(1)}°C</strong></div>
          <div class="metric-box"><span>Humidity</span><strong>${humidity.toFixed(0)}%</strong></div>
          <div class="metric-box"><span>Last seen</span><strong>${lastSeen}</strong></div>
        </div>
        <div class="unit-chart"><canvas data-chart-id="${unit.unitId}" aria-label="Temperature history for ${unit.unitId}"></canvas></div>
        <div class="temperature-control">
          <div class="control-label"><span>Temperature control</span><strong data-value-for="${unit.unitId}">${temp.toFixed(1)}°C</strong></div>
          <input class="temperature-slider" data-unit-id="${unit.unitId}" type="range" min="-5" max="15" step="0.1" value="${temp.toFixed(1)}" aria-label="Set temperature for ${unit.unitId}" />
          <div class="slider-scale"><span>-5°C</span><span>Safe ≤ 8°C</span><span>15°C</span></div>
        </div>
      </article>
    `;
  }).join('') || '<p class="meta">No units available.</p>';

  const alertGroups = formatAlertGroups(state.alerts).slice(0, 6);
  els.alertCountLabel.textContent = `${alertGroups.length} signal${alertGroups.length === 1 ? '' : 's'}`;
  els.alertFeed.innerHTML = alertGroups.map(({ alert, count }) => {
    const level = alert.severity === 'HIGH' || alert.severity === 'CRITICAL' ? 'high' : 'medium';
    const time = new Date(alert.timestamp).toLocaleTimeString();
    const isNew = !state.knownAlertIds.has(alert.alertId);
    state.knownAlertIds.add(alert.alertId);
    return `
      <div class="alert-item ${level} ${isNew ? 'alert-enter' : ''}">
        <h4>${alert.message}</h4>
        <p>Unit ${alert.unitId} · ${alert.source || 'BACKEND_RULE'} · ${time}${count > 1 ? ` · ${count} similar alerts` : ''}</p>
      </div>
    `;
  }).join('') || '<p class="meta">No active alerts.</p>';

  bindUnitControls();
  visibleUnits.forEach((unit) => createChart(unit.unitId, state.telemetryMap?.[unit.unitId] || []));
}

function setRole(role) {
  state.role = role;
  if (role === 'FARMER') {
    state.farmerId = 'FARMER-101';
  }
  els.roleButtons.forEach((button) => {
    const isActive = button.dataset.role === role;
    button.classList.toggle('active', isActive);
  });
  loadData();
}

els.roleButtons.forEach((button) => {
  button.addEventListener('click', () => setRole(button.dataset.role));
});

loadData();
setInterval(loadData, 5000);
