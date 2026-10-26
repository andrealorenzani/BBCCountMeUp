import { api, el, showFeedback } from './api.js';

const form = document.querySelector('#vote-form');
const feedback = document.querySelector('#feedback');
let currentEvent = null;

async function init() {
  try {
    currentEvent = await api('/voter/event');
  } catch (error) {
    document.querySelector('#event-title').textContent = 'Voting is closed';
    showFeedback(feedback, error.message, 'warning');
    return;
  }
  document.querySelector('#event-title').textContent = currentEvent.name;
  document.querySelector('#event-description').textContent = currentEvent.description;
  const candidates = document.querySelector('#candidates');
  currentEvent.candidates.forEach(({ id, name }, index) => {
    const inputId = `candidate-${id}`;
    const input = el('input', { class: 'form-check-input', type: 'radio', name: 'candidate', id: inputId, value: String(id), ...(index === 0 ? { required: '' } : {}) });
    candidates.append(el('div', { class: 'form-check' }, input, el('label', { class: 'form-check-label', for: inputId }, name)));
  });
  form.hidden = false;
}

form.addEventListener('submit', async (submit) => {
  submit.preventDefault();
  const voter = form.elements.voter.value.trim();
  const selected = form.querySelector('input[name="candidate"]:checked');
  if (!voter || !selected) {
    showFeedback(feedback, 'Enter your voter id and select a candidate.', 'warning');
    return;
  }
  try {
    await api('/voter/vote', { method: 'POST', body: { voter, candidateId: Number(selected.value), eventId: currentEvent.id } });
    showFeedback(feedback, 'Thank you, your vote has been counted!', 'success');
  } catch (error) {
    showFeedback(feedback, error.message, 'danger');
  }
});

init();
