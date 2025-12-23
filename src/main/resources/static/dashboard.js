// ---------- CONFIG ----------
const API_BASE = "";
const username = localStorage.getItem("username");
if (!username) window.location.href = "login.html";

document.getElementById("userDisplay").textContent = username;

// Theme (dark/light)
const themeToggle = document.getElementById("themeToggle");
function applyTheme() {
  const theme = localStorage.getItem("theme") || "light";
  if (theme === "dark") document.body.classList.add("dark"), themeToggle.textContent = "Light";
  else document.body.classList.remove("dark"), themeToggle.textContent = "Dark";
}
applyTheme();
themeToggle.addEventListener("click", () => {
  const current = localStorage.getItem("theme") || "light";
  localStorage.setItem("theme", current === "light" ? "dark" : "light");
  applyTheme();
});

// Logout
function logout() { localStorage.removeItem("username"); window.location.href = "login.html"; }

// helpers
function buildQuery(params) {
  const esc = encodeURIComponent;
  return Object.keys(params)
    .filter(k => params[k] !== undefined && params[k] !== null && params[k] !== "")
    .map(k => esc(k) + "=" + esc(params[k]))
    .join("&");
}

async function fetchFilteredExpenses(filters) {
  try {
    const q = buildQuery(filters);
    const url = `${API_BASE}/expenses/filter?${q}`;
    const res = await fetch(url);
    return await res.json();
  } catch (err) {
    console.error("Fetch failed", err);
    return [];
  }
}

function computeSummaryFromList(list) {
  let totalIncome = 0, totalExpense = 0;
  const breakdown = {};
  list.forEach(e => {
    const amt = Number(e.amount || 0);
    const cat = e.category ? e.category.name : "Uncategorized";
    if (cat.toLowerCase() === "income") totalIncome += amt;
    else totalExpense += amt;
    if (cat.toLowerCase() !== "income") {
      breakdown[cat] = (breakdown[cat] || 0) + amt;
    }
  });
  return { totalIncome, totalExpense, breakdown };
}

function renderRecent(list) {
  const table = document.getElementById("recentExpenses");
  table.innerHTML = "";
  list.slice(-8).reverse().forEach(exp => {
    table.innerHTML += `
      <tr>
        <td>${exp.category ? exp.category.name : ''}</td>
        <td>${exp.description}</td>
        <td>₹${exp.amount}</td>
        <td>${exp.date ? exp.date.substring(0,10) : ''}</td>
        <td>
          <button class="btn btn-sm btn-secondary action-btn" onclick="openEditModal(${exp.id}, '${exp.category ? exp.category.name : ''}', '${(exp.description||'').replace(/'/g, "\\'")}', ${exp.amount}, '${exp.date ? exp.date.substring(0,10) : ''}')">Edit</button>
          <button class="btn btn-sm btn-danger action-btn" onclick="deleteExpense(${exp.id})">Delete</button>
        </td>
      </tr>
    `;
  });
}

// chart instances
let pieChartInstance = null, barChartInstance = null;
function renderCharts(breakdown) {
  if (pieChartInstance) pieChartInstance.destroy();
  if (barChartInstance) barChartInstance.destroy();
  const labels = Object.keys(breakdown);
  const values = Object.values(breakdown);
  pieChartInstance = new Chart(document.getElementById("pieChart"), {
    type: "pie",
    data: { labels, datasets: [{ data: values, backgroundColor: ["rgba(255,99,132,0.6)","rgba(54,162,235,0.6)","rgba(255,206,86,0.6)","rgba(75,192,192,0.6)","rgba(153,102,255,0.6)"] }] },
    options: { responsive: true }
  });
  barChartInstance = new Chart(document.getElementById("barChart"), {
    type: "bar",
    data: { labels, datasets: [{ label: "Amount Spent", data: values, backgroundColor: "rgba(54,162,235,0.6)" }] },
    options: { responsive: true }
  });
}

// apply filters
async function applyFilters() {
  const from = document.getElementById("filterFrom").value;
  const to = document.getElementById("filterTo").value;
  const month = document.getElementById("filterMonth").value;
  const year = document.getElementById("filterYear").value;
  const category = document.getElementById("filterCategory").value;
  const filters = { username };
  if (from) filters.from = from;
  if (to) filters.to = to;
  if (month) filters.month = month;
  if (year) filters.year = year;
  if (category) filters.category = category;
  const list = await fetchFilteredExpenses(filters);
  const summary = computeSummaryFromList(list);
  document.getElementById("totalIncome").textContent = `₹${summary.totalIncome}`;
  document.getElementById("totalExpense").textContent = `₹${summary.totalExpense}`;
  document.getElementById("balanceValue").textContent = `₹${summary.totalIncome - summary.totalExpense}`;
  renderRecent(list);
  renderCharts(summary.breakdown);
}

// clear filters
function clearFilters() {
  document.getElementById("filterFrom").value = "";
  document.getElementById("filterTo").value = "";
  document.getElementById("filterMonth").value = "";
  document.getElementById("filterYear").value = "";
  document.getElementById("filterCategory").value = "";
  initDashboard();
}

// init: fetch all and populate categories
async function initDashboard() {
  const all = await fetchFilteredExpenses({ username });
  const summary = computeSummaryFromList(all);
  document.getElementById("totalIncome").textContent = `₹${summary.totalIncome}`;
  document.getElementById("totalExpense").textContent = `₹${summary.totalExpense}`;
  document.getElementById("balanceValue").textContent = `₹${summary.totalIncome - summary.totalExpense}`;
  renderRecent(all);
  renderCharts(summary.breakdown);
  const catSelect = document.getElementById("filterCategory");
  catSelect.innerHTML = `<option value="">All</option>`;
  Object.keys(summary.breakdown).forEach(cat => {
    const opt = document.createElement("option");
    opt.value = cat;
    opt.text = cat;
    catSelect.appendChild(opt);
  });
}

// delete
async function deleteExpense(id) {
  if (!confirm("Are you sure you want to delete this expense?")) return;
  try {
    const res = await fetch(`${API_BASE}/expenses/delete/${id}`, { method: "DELETE" });
    const text = await res.text();
    alert(text);
    initDashboard();
  } catch (err) {
    console.error(err);
    alert("Delete failed");
  }
}

// EDIT feature
let editingId = null;
function openEditModal(id, category, description, amount, date) {
  editingId = id;
  document.getElementById("editCategory").value = category || "";
  document.getElementById("editDescription").value = description || "";
  document.getElementById("editAmount").value = amount || "";
  document.getElementById("editDate").value = date || "";
  const modal = new bootstrap.Modal(document.getElementById('editModal'));
  modal.show();
}

document.getElementById("editForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const payload = {
    username,
    amount: Number(document.getElementById("editAmount").value),
    category: document.getElementById("editCategory").value,
    description: document.getElementById("editDescription").value,
    date: document.getElementById("editDate").value
  };
  try {
    const res = await fetch(`${API_BASE}/expenses/update/${editingId}`, {
      method: "PUT", headers: { "Content-Type": "application/json" }, body: JSON.stringify(payload)
    });
    const text = await res.text();
    alert(text);
    const modal = bootstrap.Modal.getInstance(document.getElementById('editModal'));
    modal.hide();
    initDashboard();
  } catch (err) { console.error(err); alert("Update failed"); }
});

// ADD INCOME (uses existing /expenses/add endpoint with category "Income")
function openAddIncomeModal() {
  const modal = new bootstrap.Modal(document.getElementById('addIncomeModal'));
  modal.show();
}
document.getElementById("addIncomeForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const description = document.getElementById("incomeDescription").value.trim();
  const amount = Number(document.getElementById("incomeAmount").value);
  const date = document.getElementById("incomeDate").value;
  if (!description || !amount || !date) { alert("Fill all fields"); return; }
  const payload = { username, amount, category: "Income", description, date };
  try {
    const res = await fetch(`${API_BASE}/expenses/add`, {
      method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(payload)
    });
    const text = await res.text();
    alert(text);
    const modal = bootstrap.Modal.getInstance(document.getElementById('addIncomeModal'));
    modal.hide();
    initDashboard();
  } catch (err) { console.error(err); alert("Failed to add income"); }
});

// EXPORT CSV (client-side quick export using current filters)
async function exportCSV() {
  const from = document.getElementById("filterFrom").value;
  const to = document.getElementById("filterTo").value;
  const month = document.getElementById("filterMonth").value;
  const year = document.getElementById("filterYear").value;
  const category = document.getElementById("filterCategory").value;
  const filters = { username };
  if (from) filters.from = from;
  if (to) filters.to = to;
  if (month) filters.month = month;
  if (year) filters.year = year;
  if (category) filters.category = category;

  // prefer server-side CSV endpoint if you want (below I use client-side to keep file local)
  const list = await fetchFilteredExpenses(filters);
  if (!list || !list.length) { alert("No data to export"); return; }

  const rows = [];
  rows.push(["id","username","category","description","amount","date"]);
  list.forEach(r => {
    rows.push([r.id, (r.user && r.user.username) || username, (r.category && r.category.name) || "", r.description || "", r.amount || 0, r.date ? r.date.substring(0,10) : ""]);
  });

  const csvContent = rows.map(e => e.map(v => `"${String(v).replace(/"/g,'""')}"`).join(",")).join("\n");
  const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  const fname = `expenses_${username}_${new Date().toISOString().slice(0,10)}.csv`;
  a.href = url; a.download = fname; a.style.display = "none";
  document.body.appendChild(a); a.click(); document.body.removeChild(a); URL.revokeObjectURL(url);
}

// initialize
initDashboard();
