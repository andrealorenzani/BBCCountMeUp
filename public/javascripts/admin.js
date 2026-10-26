import { api, el, showFeedback } from './api.js';

const MIN_CANDIDATES = 2;
const form = document.querySelector('#event-form');
const candidates = document.querySelector('#candidates');
const feedback = document.querySelector('#feedback');

function addCandidateRow() {
  const n = candidates.children.length + 1;
  candidates.append(
    el('div', { class: 'row g-2 mb-2 candidate-row' },
      el('div', { class: 'col-8' }, el('input', { class: 'form-control', type: 'text', placeholder: `Candidate ${n}`, 'aria-label': `Name of candidate ${n}`, 'data-field': 'name', required: '' })),
      el('div', { class: 'col-4' }, el('input', { class: 'form-control', type: 'number', min: '0', value: '0', 'aria-label': `Initial votes of candidate ${n}`, 'data-field': 'initialVotes' })),
    ),
  );
}

function readForm() {
  return {
    name: form.querySelector('#name').value.trim(),
    description: form.querySelector('#description').value.trim(),
    candidates: [...candidates.children].map((row) => ({
      name: row.querySelector('[data-field="name"]').value.trim(),
      initialVotes: Number(row.querySelector('[data-field="initialVotes"]').value || 0),
    })),
  };
}

document.querySelector('#add-candidate').addEventListener('click', addCandidateRow);

form.addEventListener('submit', async (submit) => {
  submit.preventDefault();
  try {
    const event = await api('/admin/event', { method: 'POST', body: readForm() });
    showFeedback(feedback, `Event "${event.name}" created with ${event.candidates.length} candidates.`, 'success');
  } catch (error) {
    showFeedback(feedback, error.message, 'danger');
  }
});

for (let i = 0; i < MIN_CANDIDATES; i++) addCandidateRow();
