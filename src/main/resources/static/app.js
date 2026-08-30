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
    if (item.status === 'GENERATED') title.append(statusBadge(item.reviewStatus || 'PENDING'));
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

function approvedSection(item) {
  if (item.status !== 'GENERATED' || item.reviewStatus !== 'APPROVED' || !String(item.reviewedText || '').trim()) return null;
  const section = element('section', 'approved-section');
  const cardUrl = `/api/v1/content/${encodeURIComponent(item.id)}/card.svg?v=${encodeURIComponent(item.updatedAt || '')}`;
  const preview = element('img', 'card-preview');
  preview.src = cardUrl;
  preview.alt = `Approved RoastLens image card for ${item.symbol || 'market event'}`;
  const filename = `roastlens-${safeFilename(item.symbol)}-${safeFilename(item.eventType)}`;
  const svgButton = element('button', 'secondary-button', 'Download SVG'); svgButton.type = 'button';
  const pngButton = element('button', 'secondary-button', 'Download PNG'); pngButton.type = 'button';
  svgButton.addEventListener('click', () => downloadSvg(cardUrl, `${filename}.svg`, svgButton));
  pngButton.addEventListener('click', () => downloadPng(cardUrl, `${filename}.png`, pngButton));
  const actions = element('div', 'output-actions'); actions.append(svgButton, pngButton);
  section.append(
    element('h3', '', 'Approved Output'),
    element('h4', '', 'Approved text'),
    element('p', 'approved-text', item.reviewedText || ''),
    element('p', 'review-meta', `Reviewed: ${formatTimestamp(item.reviewedAt)}`),
    preview,
    actions
  );
  return section;
}

function safeFilename(value) {
  const clean = String(value || 'market').replace(/[^a-z0-9_-]+/gi, '-').replace(/^-+|-+$/g, '');
  return clean || 'market';
}

function saveBlob(blob, filename) {
  const link = document.createElement('a');
  const url = URL.createObjectURL(blob);
  link.href = url; link.download = filename; document.body.appendChild(link); link.click(); link.remove();
  window.setTimeout(() => URL.revokeObjectURL(url), 0);
}

async function fetchCard(url) {
  const response = await fetch(url, { cache: 'no-store' });
  if (!response.ok) throw new Error('Card download is unavailable.');
  return response.blob();
}

async function downloadSvg(url, filename, button) {
  button.disabled = true;
  try { saveBlob(await fetchCard(url), filename); } finally { button.disabled = false; }
}

async function downloadPng(url, filename, button) {
  button.disabled = true;
  let objectUrl;
  try {
    objectUrl = URL.createObjectURL(await fetchCard(url));
    const image = new Image();
    await new Promise((resolve, reject) => { image.onload = resolve; image.onerror = reject; image.src = objectUrl; });
    const canvas = document.createElement('canvas'); canvas.width = 1200; canvas.height = 1200;
    canvas.getContext('2d').drawImage(image, 0, 0, 1200, 1200);
    const blob = await new Promise(resolve => canvas.toBlob(resolve, 'image/png'));
    if (!blob) throw new Error('PNG conversion failed.');
    saveBlob(blob, filename);
  } finally {
    if (objectUrl) URL.revokeObjectURL(objectUrl);
    button.disabled = false;
  }
}

async function submitReview(item, action, body, button, message) {
  button.disabled = true;
  message.textContent = `${action === 'approve' ? 'Approving' : 'Rejecting'}…`;
  try {
    const response = await fetch(`/api/v1/content/${encodeURIComponent(item.id)}/${action}`, {
      method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body)
    });
    const data = await response.json();
    if (!response.ok) throw new Error(data.error || 'Review request failed.');
    inventoryItems = inventoryItems.map(value => value.id === data.id ? data : value);
    renderContentList();
    renderContentDetail(data);
  } catch (error) {
    message.textContent = error.message;
    message.classList.add('error');
    button.disabled = false;
  }
}

function renderContentDetail(item) {
  const header = element('header', 'detail-header');
  const badges = element('div', 'badge-row');
  badges.append(statusBadge(item.status));
  if (item.status === 'GENERATED') badges.append(statusBadge(item.reviewStatus || 'PENDING'));
  badges.append(element('span', 'meta-chip', item.language || 'Unknown language'));
  header.append(element('h3', '', item.symbol || 'Unknown symbol'), element('p', '', item.eventType || 'Unknown event type'), badges);

  const metadata = element('dl', 'metadata-grid');
  [
    ['Source', item.source], ['Source event ID', item.sourceEventId], ['Event time', formatTimestamp(item.eventTime)],
    ['Detected at', formatTimestamp(item.detectedAt)], ['Content score', formatScore(item.roastabilityScore)],
    ['Language', item.language], ['Generation status', item.status], ['Review status', item.status === 'GENERATED' ? (item.reviewStatus || 'PENDING') : '—'],
    ['Created', formatTimestamp(item.createdAt)], ['Updated', formatTimestamp(item.updatedAt)]
  ].forEach(([label, value]) => metadata.appendChild(metadataField(label, value)));

  const candidatesSection = element('section', 'candidates-section');
  const candidates = Array.isArray(item.candidates) ? item.candidates : [];
  candidatesSection.appendChild(element('h4', 'candidates-heading', `Step 1: Choose a candidate (${candidates.length})`));
  let selectedId = item.selectedCandidateId || (item.status === 'GENERATED' && candidates.length ? candidates[0].id : null);
  const selectedCandidate = candidates.find(candidate => candidate.id === selectedId);
  let finalText = item.reviewStatus === 'APPROVED'
    ? (item.reviewedText ?? '')
    : (selectedCandidate?.text ?? '');
  const textarea = element('textarea', 'review-text');
  textarea.rows = 6;
  textarea.maxLength = 4000;
  const approve = element('button', 'approve-button', 'Approve');
  approve.type = 'button';
  approve.disabled = !selectedId;

  if (!candidates.length) {
    candidatesSection.appendChild(element('p', 'no-candidates', 'No candidates were persisted for this content item.'));
  } else {
    candidates.forEach((candidate, index) => {
      const card = element('article', 'candidate-card');
      card.tabIndex = 0;
      card.setAttribute('role', 'radio');
      card.setAttribute('aria-checked', String(candidate.id === selectedId));
      if (candidate.id === selectedId) card.classList.add('candidate-selected');
      const cardHead = element('div', 'candidate-card-head');
      const selection = element('label', 'candidate-choice');
      const radio = element('input');
      radio.type = 'radio'; radio.name = `candidate-${item.id}`; radio.value = candidate.id;
      radio.checked = candidate.id === selectedId;
      selection.append(radio, document.createTextNode(` Candidate ${index + 1}`));
      const selectCandidate = () => {
        selectedId = candidate.id; finalText = candidate.text || ''; textarea.value = finalText; textarea.disabled = false; approve.disabled = false;
        candidatesSection.querySelectorAll('.candidate-card').forEach(node => {
          node.classList.remove('candidate-selected');
          node.setAttribute('aria-checked', 'false');
        });
        candidatesSection.querySelectorAll('input[type="radio"]').forEach(node => { node.checked = false; });
        radio.checked = true;
        card.classList.add('candidate-selected');
        card.setAttribute('aria-checked', 'true');
      };
      radio.addEventListener('change', selectCandidate);
      card.addEventListener('click', event => {
        if (event.target.closest('.copy-button')) return;
        selectCandidate();
      });
      card.addEventListener('keydown', event => {
        if (event.target.closest('.copy-button')) return;
        if (event.key === 'Enter' || event.key === ' ') {
          event.preventDefault();
          selectCandidate();
        }
      });
      const copy = element('button', 'copy-button', 'Copy'); copy.type = 'button';
      copy.addEventListener('click', event => {
        event.stopPropagation();
        copyCandidate(String(candidate.text || ''), copy);
      });
      cardHead.append(selection, copy);
      const text = element('p', 'candidate-text', candidate.text || '');
      const candidateMeta = element('div', 'candidate-meta');
      candidateMeta.append(element('span', 'meta-chip', `style: ${candidate.style || '—'}`), element('span', 'meta-chip', `risk: ${candidate.riskLevel || '—'}`));
      card.append(cardHead, text, candidateMeta); candidatesSection.appendChild(card);
    });
  }

  const nodes = [header, metadata];
  if (item.status !== 'GENERATED') {
    nodes.push(candidatesSection);
    nodes.push(element('p', 'not-reviewable', 'This item is not reviewable.'));
  } else if (candidates.length) {
    const review = element('section', 'review-workspace');
    review.append(element('h3', '', 'Human Review'), candidatesSection, element('h4', 'review-step', 'Step 2: Final text'), element('label', 'visually-hidden', 'Final text'));
    textarea.value = finalText || ''; textarea.placeholder = 'Select a candidate to edit its final text'; textarea.disabled = !selectedId;
    const message = element('p', 'review-message');
    const reject = element('button', 'reject-button', 'Reject'); reject.type = 'button';
    approve.addEventListener('click', () => submitReview(item, 'approve', { candidateId: selectedId, reviewedText: textarea.value }, approve, message));
    reject.addEventListener('click', () => submitReview(item, 'reject', {}, reject, message));
    const actions = element('div', 'review-actions'); actions.append(approve, reject);
    review.append(textarea, actions, message);
    if (item.reviewStatus === 'REJECTED') {
      review.append(element('p', 'rejection-detail', item.rejectionReason ? `Rejection reason: ${item.rejectionReason}` : 'This content was rejected.'));
    }
    nodes.push(review);
  } else {
    nodes.push(candidatesSection);
  }
  const approved = approvedSection(item); if (approved) nodes.push(approved);
  contentDetail.replaceChildren(...nodes);
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
