const textEl = document.getElementById('text');
const domainEl = document.getElementById('domain');
const personaEl = document.getElementById('persona');
const analyzeBtn = document.getElementById('analyzeBtn');
const statusEl = document.getElementById('status');
const resultPanel = document.getElementById('resultPanel');

async function loadMeta() {
  try {
    const resp = await fetch('/api/meta');
    if (!resp.ok) {
      throw new Error('Failed to load metadata');
    }
    const data = await resp.json();

    domainEl.innerHTML = '';
    personaEl.innerHTML = '';

    (data.domains || []).forEach(d => {
      const option = document.createElement('option');
      option.value = d;
      option.textContent = d;
      domainEl.appendChild(option);
    });

    (data.personas || []).forEach(p => {
      const option = document.createElement('option');
      option.value = p;
      option.textContent = p;
      personaEl.appendChild(option);
    });
  } catch (err) {
    statusEl.textContent = `Meta load error: ${err.message}`;
  }
}

function renderResult(data) {
  document.getElementById('summary').textContent = data.summary || '';
  document.getElementById('counterPoint').textContent = data.counterPoint || '';
  document.getElementById('confidenceNote').textContent = data.confidenceNote || '';
  document.getElementById('disclaimer').textContent = data.disclaimer || '';
  document.getElementById('styleMeta').textContent = JSON.stringify(data.styleMeta || {}, null, 2);

  const evidenceList = document.getElementById('evidencePoints');
  evidenceList.innerHTML = '';
  (data.evidencePoints || []).forEach(item => {
    const li = document.createElement('li');
    li.textContent = item;
    evidenceList.appendChild(li);
  });

  resultPanel.hidden = false;
}

analyzeBtn.addEventListener('click', async () => {
  const text = textEl.value.trim();
  if (!text) {
    statusEl.textContent = 'Please input text first.';
    return;
  }

  analyzeBtn.disabled = true;
  statusEl.textContent = 'Generating...';

  try {
    const resp = await fetch('/api/analyze', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        text,
        domain: domainEl.value,
        persona: personaEl.value
      })
    });

    const data = await resp.json();
    if (!resp.ok) {
      throw new Error(data.error || 'Request failed');
    }

    renderResult(data);
    statusEl.textContent = 'Done.';
  } catch (err) {
    statusEl.textContent = `Analyze error: ${err.message}`;
  } finally {
    analyzeBtn.disabled = false;
  }
});

loadMeta();
