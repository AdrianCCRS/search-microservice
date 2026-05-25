const API_BASE = "/api/search";

const searchInput = document.getElementById("searchInput");
const searchBtn = document.getElementById("searchBtn");
const suggestionsEl = document.getElementById("suggestions");
const statusEl = document.getElementById("status");
const resultsEl = document.getElementById("results");
const apiUrlEl = document.getElementById("apiUrl");

let debounceTimer = null;
let abortController = null;

apiUrlEl.textContent = API_BASE;

searchInput.addEventListener("input", handleInput);
searchInput.addEventListener("keydown", handleKeydown);
searchBtn.addEventListener("click", handleSearch);
document.addEventListener("click", handleClickOutside);

function handleInput() {
  clearTimeout(debounceTimer);
  const q = searchInput.value.trim();

  if (q.length < 2) {
    hideSuggestions();
    return;
  }

  debounceTimer = setTimeout(() => fetchSuggestions(q), 250);
}

async function fetchSuggestions(q) {
  try {
    const res = await fetch(`${API_BASE}/suggest?q=${encodeURIComponent(q)}`);
    if (!res.ok) {
      hideSuggestions();
      return;
    }
    const suggestions = await res.json();
    if (!Array.isArray(suggestions) || suggestions.length === 0) {
      hideSuggestions();
      return;
    }
    renderSuggestions(suggestions, q);
  } catch {
    hideSuggestions();
  }
}

function renderSuggestions(suggestions, query) {
  suggestionsEl.innerHTML = "";
  const regex = new RegExp(`(${escapeRegex(query)})`, "gi");

  suggestions.forEach(function (s) {
    const li = document.createElement("li");
    li.className = "suggestion-item";
    li.innerHTML = s.replace(regex, "<mark>$1</mark>");
    li.addEventListener("mousedown", function (e) {
      e.preventDefault();
      searchInput.value = s;
      hideSuggestions();
      doSearch(s);
    });
    suggestionsEl.appendChild(li);
  });

  suggestionsEl.classList.remove("hidden");
}

function hideSuggestions() {
  suggestionsEl.classList.add("hidden");
}

function handleKeydown(e) {
  if (e.key === "Enter") {
    e.preventDefault();
    hideSuggestions();
    doSearch(searchInput.value.trim());
  }
}

function handleSearch() {
  hideSuggestions();
  doSearch(searchInput.value.trim());
}

async function doSearch(q) {
  if (!q) {
    showStatus("Escribe algo para buscar", "empty");
    resultsEl.innerHTML = "";
    return;
  }

  showStatus("", "loading");
  resultsEl.innerHTML = "";

  if (abortController) abortController.abort();
  abortController = new AbortController();

  try {
    const res = await fetch(`${API_BASE}?q=${encodeURIComponent(q)}`, {
      signal: abortController.signal,
    });

    if (!res.ok) {
      if (res.status === 429) {
        showStatus("Demasiadas peticiones. Espera un momento.", "error");
      } else {
        showStatus("Error al buscar. Intenta de nuevo.", "error");
      }
      return;
    }

    const data = await res.json();
    hideStatus();

    if (!Array.isArray(data) || data.length === 0) {
      showStatus(`Sin resultados para "${q}"`, "empty");
      return;
    }

    renderResults(data);
  } catch (err) {
    if (err.name === "AbortError") return;
    showStatus("No se pudo conectar con el servicio de búsqueda.", "error");
  }
}

function renderResults(docs) {
  hideStatus();
  resultsEl.innerHTML = docs.map(renderCard).join("");
}

function renderCard(doc) {
  var stars = "";
  if (doc.rating != null) {
    var full = Math.round(doc.rating);
    for (var i = 0; i < 5; i++) {
      stars += i < full ? "\u2605" : "\u2606";
    }
  }

  return (
    '<div class="card">' +
    '  <div class="card-header">' +
    '    <span class="card-title">' + escapeHtml(doc.name || "Sin nombre") + "</span>" +
    '    <span class="card-price">' +
    (doc.price != null ? "$" + formatPrice(doc.price) : "") +
    "    </span>" +
    "  </div>" +
    (doc.description
      ? '  <p class="card-description">' + escapeHtml(doc.description) + "</p>"
      : "") +
    '  <div class="card-meta">' +
    (doc.category
      ? '    <span class="badge badge-category">' + escapeHtml(doc.category) + "</span>"
      : "") +
    (doc.brand
      ? '    <span class="badge badge-brand">' + escapeHtml(doc.brand) + "</span>"
      : "") +
    (doc.available === true
      ? '    <span class="badge badge-available">Disponible</span>'
      : '    <span class="badge badge-unavailable">Agotado</span>') +
    (stars
      ? '    <span class="rating"><span class="rating-star">' + stars + "</span>" + (doc.rating != null ? doc.rating.toFixed(1) : "") + "</span>"
      : "") +
    "  </div>" +
    "</div>"
  );
}

function showStatus(msg, type) {
  statusEl.textContent = msg;
  statusEl.className = "status visible " + type;
}

function hideStatus() {
  statusEl.textContent = "";
  statusEl.className = "status";
}

function handleClickOutside(e) {
  if (!suggestionsEl.contains(e.target) && e.target !== searchInput) {
    hideSuggestions();
  }
}

function formatPrice(price) {
  if (typeof price === "number") return price.toFixed(2);
  return price;
}

function escapeHtml(str) {
  return String(str)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

function escapeRegex(str) {
  return str.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}
