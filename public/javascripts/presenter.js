import { api, el } from './api.js';

const REFRESH_RETRY_MS = 5000;
let chart = null;
let chartType = 'doughnut';
let latest = { event: null, results: [] };

function render() {
  const { event, results } = latest;
  document.querySelector('#event-title h2').textContent = event ? event.name : 'No event is open yet';
  document.querySelector('#event-description').textContent = event ? event.description : '';
  renderTable(results);
  renderChart(results);
}

function renderTable(results) {
  const total = results.reduce((sum, { votes }) => sum + votes, 0);
  const rows = results.map(({ name, votes }) => {
    const share = total === 0 ? 0 : (votes * 100) / total;
    return el('tr', {}, el('td', {}, name), el('td', { class: 'text-end' }, String(votes)), el('td', { class: 'text-end' }, `${share.toFixed(1)}%`));
  });
  document.querySelector('#results-table tbody').replaceChildren(...rows);
}

function renderChart(results) {
  // Chart.js is a deferred classic script placed before this module, so it has run by now.
  if (chart && chart.config.type !== chartType) {
    chart.destroy();
    chart = null;
  }
  const labels = results.map(({ name }) => name);
  const data = results.map(({ votes }) => votes);
  if (!chart) {
    chart = new Chart(document.querySelector('#chart'), {
      type: chartType,
      data: { labels, datasets: [{ label: 'Votes', data }] },
      options: { maintainAspectRatio: false, plugins: { legend: { display: chartType !== 'bar' } } },
    });
  } else {
    chart.data.labels = labels;
    chart.data.datasets[0].data = data;
    chart.update();
  }
}

function setupChartTypes() {
  document.querySelector('#chart-types').addEventListener('click', ({ target }) => {
    if (!target.dataset.type) return;
    chartType = target.dataset.type;
    document.querySelectorAll('#chart-types button').forEach((button) => button.classList.toggle('active', button === target));
    renderChart(latest.results);
  });
}

function liveUpdates() {
  const status = document.querySelector('#live-status');
  const scheme = location.protocol === 'https:' ? 'wss' : 'ws';
  const socket = new WebSocket(`${scheme}://${location.host}/presenter/websocket`);
  socket.addEventListener('open', () => { status.textContent = 'Live: updates every few seconds'; });
  socket.addEventListener('message', ({ data }) => {
    latest = JSON.parse(data);
    render();
  });
  socket.addEventListener('close', () => {
    status.textContent = 'Connection lost, retrying…';
    setTimeout(liveUpdates, REFRESH_RETRY_MS);
  });
}

async function init() {
  setupChartTypes();
  try {
    const [event, results] = await Promise.all([api('/presenter/event'), api('/presenter/result')]);
    latest = { event, results };
  } catch {
    // No event yet: the live updates will fill the page as soon as one is opened.
  }
  render();
  liveUpdates();
}

init();
