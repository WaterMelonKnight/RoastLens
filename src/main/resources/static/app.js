const textEl = document.getElementById('text');
const domainEl = document.getElementById('domain');
const personaEl = document.getElementById('persona');
const analyzeBtn = document.getElementById('analyzeBtn');
const statusEl = document.getElementById('status');
const resultPanel = document.getElementById('resultPanel');
const refreshContentBtn = document.getElementById('refreshContentBtn');
const statusFilter = document.getElementById('statusFilter');
const languageFilter = document.getElementById('languageFilter');
const symbolFilter = document.getElementById('symbolFilter');
const inventoryMessage = document.getElementById('inventoryMessage');
const contentList = document.getElementById('contentList');
const contentDetail = document.getElementById('contentDetail');

let inventoryItems = [];
let selectedContentId = null;

function element(tag, className, text) {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text !== undefined && text !== null) node.textContent = String(text);
  return node;
}

function formatTimestamp(value) {
  if (!value) return '—';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString();
}

function formatScore(value) {
  const score = Number(value);
  return Number.isFinite(score) ? score.toFixed(2) : '—';
}

function statusBadge(status) {
  const normalized = status || 'UNKNOWN';
  return element('span', `status-badge status-${normalized.toLowerCase()}`, normalized);
}

function showInventoryMessage(message, isError = false) {
  inventoryMessage.textContent = message;
  inventoryMessage.classList.toggle('error', isError);
}

function updateLanguageOptions() {
  const selected = languageFilter.value;
  const languages = [...new Set(inventoryItems.map(item => item.language).filter(Boolean))].sort();
  const options = [element('option', '', 'ALL')];
  options[0].value = 'ALL';
  languages.forEach(language => {
    const option = element('option', '', language);
    option.value = language;
    options.push(option);
  });
  languageFilter.replaceChildren(...options);
  languageFilter.value = languages.includes(selected) ? selected : 'ALL';
}

function filteredContent() {
  const symbol = symbolFilter.value.trim().toLocaleLowerCase();
  return inventoryItems.filter(item =>
    (statusFilter.value === 'ALL' || item.status === statusFilter.value) &&
    (languageFilter.value === 'ALL' || item.language === languageFilter.value) &&
    (!symbol || String(item.symbol || '').toLocaleLowerCase().includes(symbol))
  );
}

function renderContentList() {
  const items = filteredContent();
  contentList.replaceChildren();

  if (!inventoryItems.length) {
    showInventoryMessage('No persisted content yet. Run a FinStream roast request to generate content.');
    return;
  }
  if (!items.length) {
    showInventoryMessage('No content matches the current filters.');
    return;
  }
  showInventoryMessage('');

  items.forEach(item => {
    const button = element('button', 'content-list-item');
    button.type = 'button';
    button.classList.toggle('selected', item.id === selectedContentId);
    button.setAttribute('aria-pressed', String(item.id === selectedContentId));

    const title = element('div', 'item-title');
    title.append(element('span', '', item.symbol || 'Unknown symbol'), statusBadge(item.status));
    const eventType = element('div', 'item-event', item.eventType || 'Unknown event type');
    const meta = element('div', 'item-meta');
    meta.append(
      element('span', '', `Score ${formatScore(item.roastabilityScore)}`),
      element('span', '', item.language || '—'),
      element('time', '', formatTimestamp(item.createdAt))
    );
    button.append(title, eventType, meta);
    button.addEventListener('click', () => loadContentDetail(item.id));
    contentList.appendChild(button);
  });
}

function metadataField(label, value) {
  const wrapper = element('div', 'metadata-field');
  wrapper.append(element('dt', '', label), element('dd', '', value ?? '—'));
  return wrapper;
}

async function copyCandidate(text, button) {
  try {
    await navigator.clipboard.writeText(text);
    button.textContent = 'Copied';
    window.setTimeout(() => { button.textContent = 'Copy'; }, 1500);
  } catch (error) {
    button.textContent = 'Copy failed';
    window.setTimeout(() => { button.textContent = 'Copy'; }, 1500);
  }
}

function renderContentDetail(item) {
  const header = element('header', 'detail-header');
  const badges = element('div', 'badge-row');
  badges.append(statusBadge(item.status), element('span', 'meta-chip', item.language || 'Unknown language'));
  header.append(element('h3', '', item.symbol || 'Unknown symbol'), element('p', '', item.eventType || 'Unknown event type'), badges);

  const metadata = element('dl', 'metadata-grid');
  [
    ['Source', item.source],
    ['Source event ID', item.sourceEventId],
    ['Event time', formatTimestamp(item.eventTime)],
    ['Detected at', formatTimestamp(item.detectedAt)],
    ['Content score', formatScore(item.roastabilityScore)],
    ['Language', item.language],
    ['Status', item.status],
    ['Created', formatTimestamp(item.createdAt)],
    ['Updated', formatTimestamp(item.updatedAt)]
  ].forEach(([label, value]) => metadata.appendChild(metadataField(label, value)));

  const candidatesSection = element('section', 'candidates-section');
  const candidates = Array.isArray(item.candidates) ? item.candidates : [];
  candidatesSection.appendChild(element('h3', 'candidates-heading', `Candidates (${candidates.length})`));
  if (!candidates.length) {
    candidatesSection.appendChild(element('p', 'no-candidates', 'No candidates were persisted for this content item.'));
  } else {
    candidates.forEach((candidate, index) => {
      const card = element('article', 'candidate-card');
      const cardHead = element('div', 'candidate-card-head');
      const label = element('span', '', `Candidate ${index + 1}`);
      const copy = element('button', 'copy-button', 'Copy');
      copy.type = 'button';
      copy.addEventListener('click', () => copyCandidate(String(candidate.text || ''), copy));
      cardHead.append(label, copy);
      const text = element('p', 'candidate-text', candidate.text || '');
      const candidateMeta = element('div', 'candidate-meta');
      candidateMeta.append(
        element('span', 'meta-chip', `style: ${candidate.style || '—'}`),
        element('span', 'meta-chip', `risk: ${candidate.riskLevel || '—'}`)
      );
      card.append(cardHead, text, candidateMeta);
      candidatesSection.appendChild(card);
    });
  }
  contentDetail.replaceChildren(header, metadata, candidatesSection);
}

async function loadContentDetail(id) {
  selectedContentId = id;
  renderContentList();
  contentDetail.replaceChildren(element('p', 'detail-placeholder', 'Loading details…'));
  try {
    const response = await fetch(`/api/v1/content/${encodeURIComponent(id)}`);
    if (!response.ok) throw new Error('Unable to load content details.');
    const item = await response.json();
    if (selectedContentId === id) renderContentDetail(item);
  } catch (error) {
    if (selectedContentId === id) {
      const errorBox = element('div', 'detail-placeholder');
      errorBox.append(element('h3', '', 'Details unavailable'), element('p', '', 'Please try selecting this item again.'));
      contentDetail.replaceChildren(errorBox);
    }
  }
}

async function loadContent() {
  refreshContentBtn.disabled = true;
  showInventoryMessage('Loading content…');
  try {
    const response = await fetch('/api/v1/content?limit=20');
    if (!response.ok) throw new Error('Unable to load Content Inventory.');
    const data = await response.json();
    inventoryItems = Array.isArray(data) ? data : [];
    updateLanguageOptions();
    renderContentList();
    if (selectedContentId && inventoryItems.some(item => item.id === selectedContentId)) {
      await loadContentDetail(selectedContentId);
    }
  } catch (error) {
    inventoryItems = [];
    contentList.replaceChildren();
    showInventoryMessage('Content Inventory could not be loaded. Please try again.', true);
  } finally {
    refreshContentBtn.disabled = false;
  }
}

async function loadMeta() {
  try {
    const resp = await fetch('/api/meta');
    if (!resp.ok) {
      throw new Error('Failed to load metadata');
    }
    const data = await resp.json();

    domainEl.replaceChildren();
    personaEl.replaceChildren();

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
  evidenceList.replaceChildren();
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
loadContent();

refreshContentBtn.addEventListener('click', loadContent);
statusFilter.addEventListener('change', renderContentList);
languageFilter.addEventListener('change', renderContentList);
symbolFilter.addEventListener('input', renderContentList);
