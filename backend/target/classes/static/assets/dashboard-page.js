import { getSupabaseClient } from "./supabaseClient.js";
import { apiCall } from "./api.js";

function $(id) {
  return document.getElementById(id);
}

function applyTheme(mode) {
  const isDark = mode === "dark";
  document.documentElement.classList.toggle("dark", isDark);
  const btn = $("themeToggle");
  if (btn) btn.textContent = isDark ? "🌙 Dark" : "🌞 Light";
}

function initThemeToggle() {
  const saved = localStorage.getItem("themeMode") || "light";
  applyTheme(saved);
  $("themeToggle")?.addEventListener("click", () => {
    const next = document.documentElement.classList.contains("dark") ? "light" : "dark";
    localStorage.setItem("themeMode", next);
    applyTheme(next);
  });
}

function setLoading(isLoading) {
  const btn = $("addReminderBtn");
  if (btn) btn.disabled = isLoading;
  $("loadingOverlay")?.classList.toggle("hidden", !isLoading);
}

function setError(message) {
  const el = $("errorBox");
  if (!el) return;
  el.textContent = message || "";
  el.classList.toggle("hidden", !message);
}

function formatDue(dueDate) {
  if (!dueDate) return "";
  // Expect "YYYY-MM-DDTHH:mm:ss" from backend; show a friendly local-ish format.
  const s = String(dueDate);
  return s.replace("T", " ").replace(":00", "");
}

function toDueDateValue(datetimeLocalValue) {
  if (!datetimeLocalValue) return null;
  // Keep the user-entered local time by appending seconds, without timezone conversion.
  if (datetimeLocalValue.length === 16) return datetimeLocalValue + ":00";
  return datetimeLocalValue;
}

function normalizeText(s) {
  if (s === null || s === undefined) return null;
  const trimmed = String(s).trim();
  return trimmed.length ? trimmed : null;
}

// Toasts for motivation / rewards.
function ensureToastContainer() {
  let el = $("toastContainer");
  if (el) return el;

  el = document.createElement("div");
  el.id = "toastContainer";
  el.className = "toast-container";
  document.body.appendChild(el);
  return el;
}

function showToast(message) {
  const container = ensureToastContainer();
  const toast = document.createElement("div");
  toast.className = "toast animate-fade-in-up";
  toast.textContent = message;
  container.appendChild(toast);
  setTimeout(() => toast.remove(), 3500);
}

const dueBeepTimers = new Map(); // id -> timeoutId
const dueBeepPlayed = new Set(); // id strings
const reminderStatusById = new Map(); // id strings -> "pending" | "completed"

// Many browsers block WebAudio until the user interacts with the page.
// Unlock on first pointer/keyboard interaction.
let audioCtx = null;
let audioReady = false;

function unlockAudio() {
  if (audioReady) return;
  try {
    const AudioContext = window.AudioContext || window.webkitAudioContext;
    if (!AudioContext) return;
    audioCtx = new AudioContext();
    void audioCtx.resume?.();
    audioReady = true;
  } catch {
    audioReady = false;
  }
}

document.addEventListener("pointerdown", unlockAudio, { once: true });
document.addEventListener("keydown", unlockAudio, { once: true });

const dueOverdueTimers = new Map(); // id -> timeoutId
const dueOverduePlayed = new Set(); // id strings

function setCardOverdueState(id, overdue) {
  const card = document.querySelector(`[data-reminder-id="${String(id)}"]`);
  if (!card) return;
  if (!overdue) return;

  const isDark = document.documentElement.classList.contains("dark");
  const roseText = isDark ? "text-rose-200" : "text-rose-700";
  const titleEl = card.querySelector('[data-role="title"]');
  const descEl = card.querySelector('[data-role="desc"]');
  const dueEl = card.querySelector('[data-role="due"]');
  const badgeEl = card.querySelector('[data-role="badge"]');

  // Card border update only (no re-render => no blinking).
  card.className = "glass rounded-2xl p-5 shadow-sm border border-rose-300/70 animate-slide-up-soft";

  if (titleEl) {
    titleEl.className = `text-base md:text-lg font-semibold ${roseText} truncate line-through opacity-90`;
  }
  if (descEl) {
    descEl.className = `mt-2 text-sm ${roseText} line-clamp-3 line-through opacity-90`;
  }
  if (dueEl) {
    dueEl.className = `font-medium ${roseText} line-through opacity-90`;
  }
  if (badgeEl) {
    badgeEl.className = "inline-flex items-center rounded-full bg-rose-100 px-3 py-1 text-xs text-rose-800";
    badgeEl.textContent = "Overdue";
  }
}

function parseDueLocalDate(dueDateStr) {
  if (!dueDateStr) return null;
  const s = String(dueDateStr);
  const d = new Date(s);
  if (Number.isNaN(d.getTime())) return null;
  return d;
}

function playDueBeep() {
  try {
    const AudioContext = window.AudioContext || window.webkitAudioContext;
    if (!AudioContext) return;
    const ctx = audioCtx || new AudioContext();
    audioCtx = ctx;
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    void ctx.resume?.();

    // "Ring" for longer using a simple tone.
    osc.type = "square";
    osc.frequency.value = 880;
    gain.gain.value = 0.07;

    osc.connect(gain);
    gain.connect(ctx.destination);

    osc.start();
    setTimeout(() => {
      osc.stop();
      // Keep context alive for future beeps.
    }, 5000);
  } catch {
    // Browser may block audio; ignore.
  }
}

function scheduleDueSounds(pendingReminders) {
  const now = Date.now();
  const pendingIds = new Set(pendingReminders.map((r) => String(r.id)));

  // Cancel timers for reminders that are no longer pending.
  for (const [id, timeoutId] of dueBeepTimers.entries()) {
    if (!pendingIds.has(id)) {
      clearTimeout(timeoutId);
      dueBeepTimers.delete(id);
    }
  }

  // Cancel overdue styling timers for reminders that are no longer pending.
  for (const [id, timeoutId] of dueOverdueTimers.entries()) {
    if (!pendingIds.has(id)) {
      clearTimeout(timeoutId);
      dueOverdueTimers.delete(id);
      dueOverduePlayed.delete(id);
    }
  }

  pendingReminders.forEach((r) => {
    const id = String(r.id);
    reminderStatusById.set(id, "pending");
    const dueDate = parseDueLocalDate(r.due_date);
    if (!dueDate) return;

    const dueMs = dueDate.getTime();

    // Overdue styling (red + strikethrough) should update without re-render/blinking.
    if (Date.now() > dueMs) {
      if (!dueOverduePlayed.has(id)) {
        dueOverduePlayed.add(id);
        setCardOverdueState(id, true);
      }
    } else {
      if (!dueOverduePlayed.has(id) && !dueOverdueTimers.has(id)) {
        const delayOverdue = Math.max(0, dueMs - now + 100);
        const timeoutId = setTimeout(() => {
          const statusNow = reminderStatusById.get(id);
          if (statusNow !== "pending") return; // moved to completed in time
          dueOverduePlayed.add(id);
          setCardOverdueState(id, true);
          dueOverdueTimers.delete(id);
        }, delayOverdue);
        dueOverdueTimers.set(id, timeoutId);
      }
    }

    // Sound + toast only after "due date past for 3 seconds".
    if (!dueBeepPlayed.has(id) && !dueBeepTimers.has(id)) {
      const duePlus3 = dueMs + 3000;
      const delay = Math.max(0, duePlus3 - now);

      const timeoutId = setTimeout(() => {
        const statusNow = reminderStatusById.get(id);
        if (statusNow !== "pending") return; // moved to completed in time
        dueBeepPlayed.add(id);
        playDueBeep();
        showToast("⏰ Due time passed! Keep going 💖");
        dueBeepTimers.delete(id);
      }, delay);

      dueBeepTimers.set(id, timeoutId);
    }
  });
}

function renderReminders(reminders) {
  const pending = reminders.filter((r) => r.status !== "completed");
  const completed = reminders.filter((r) => r.status === "completed");

  renderList(pending, "pendingReminderList", "🌷 No pending reminders.");
  renderList(completed, "completedReminderList", "🎀 No completed tasks yet.");

  // After re-render, schedule beeps for reminders whose due date has passed.
  scheduleDueSounds(pending);
}

function renderList(list, containerId, emptyMessage) {
  const container = $(containerId);
  if (!container) return;
  container.innerHTML = "";

  if (!list.length) {
    container.innerHTML = `
      <div class="glass rounded-2xl p-6 text-center text-sm text-gray-600 animate-fade-in-up">
        ${emptyMessage}
      </div>
    `;
    return;
  }

  list.forEach((r, idx) => {
    const due = formatDue(r.due_date);

    const card = document.createElement("div");
    card.className =
      "glass rounded-2xl p-5 shadow-sm border border-white/60 animate-slide-up-soft";
    card.style.animationDelay = `${idx * 60}ms`;
    card.dataset.reminderId = String(r.id);

    const typeLabel =
      r.type === "exam" ? "Exam" : r.type === "revision" ? "Revision" : "Assignment";
    const isDark = document.documentElement.classList.contains("dark");
    const dueDateObj = r.due_date ? parseDueLocalDate(r.due_date) : null;
    const overdue =
      r.status !== "completed" && dueDateObj && Date.now() > dueDateObj.getTime();

    const titleClass = overdue
      ? isDark
        ? "text-rose-200"
        : "text-rose-700"
      : isDark
        ? "text-gray-100"
        : "text-gray-800";

    const descClass = overdue
      ? isDark
        ? "text-rose-200"
        : "text-rose-700"
      : isDark
        ? "text-gray-300"
        : "text-gray-600";

    if (overdue) {
      card.className =
        "glass rounded-2xl p-5 shadow-sm border border-rose-300/70 animate-slide-up-soft";
    }

    const statusBadgeHtml = overdue
      ? `<span data-role="badge" class="inline-flex items-center rounded-full bg-rose-100 px-3 py-1 text-xs text-rose-800">Overdue</span>`
      : r.status === "completed"
        ? `<span data-role="badge" class="inline-flex items-center rounded-full bg-emerald-100 px-3 py-1 text-xs text-emerald-800">Completed</span>`
        : `<span data-role="badge" class="inline-flex items-center rounded-full bg-pink-100 px-3 py-1 text-xs text-pink-800">Pending</span>`;

    card.innerHTML = `
      <div class="flex items-start justify-between gap-4">
        <div class="min-w-0">
          <h3 data-role="title" class="text-base md:text-lg font-semibold ${titleClass} truncate ${overdue ? "line-through opacity-90" : ""}">${r.title}</h3>
          ${r.description ? `<p data-role="desc" class="mt-2 text-sm ${descClass} line-clamp-3 ${overdue ? "line-through opacity-90" : ""}">${r.description}</p>` : ""}
          <div class="mt-3 flex flex-wrap items-center gap-2">
            <span class="inline-flex items-center rounded-full bg-lavender/50 px-3 py-1 text-xs text-purple-700">
              ${typeLabel}
            </span>
            <span class="inline-flex items-center rounded-full bg-blue/50 px-3 py-1 text-xs text-blue-700">
              Priority: ${capitalize(r.priority)}
            </span>
            ${statusBadgeHtml}
          </div>
          <div class="mt-3 text-sm text-gray-600">
            Due: <span data-role="due" class="font-medium ${overdue ? (isDark ? "text-rose-200" : "text-rose-700") : "text-gray-800"} ${overdue ? "line-through opacity-90" : ""}">${due || "Not set"}</span>
          </div>
        </div>
        <div class="flex flex-col items-end gap-2">
          <button
            class="btn-glow rounded-xl border border-pink-200 bg-pink-50 px-3 py-2 text-xs font-semibold text-pink-700 hover:bg-pink-100"
            data-action="toggle-status"
            data-id="${r.id}"
          >
            ${r.status === "completed" ? "Mark Pending 🤍" : "Mark Completed ✅"}
          </button>
          <button
            class="btn-glow rounded-xl border border-rose-200 bg-rose-50 px-3 py-2 text-xs font-semibold text-rose-700 hover:bg-rose-100"
            data-action="delete"
            data-id="${r.id}"
          >
            Delete 🗑️
          </button>
        </div>
      </div>
    `;

    container.appendChild(card);
  });
}

function capitalize(s) {
  return s ? String(s).charAt(0).toUpperCase() + String(s).slice(1) : "";
}

function applyFilters(reminders) {
  const type = $("filterType")?.value || "all";
  const priority = $("filterPriority")?.value || "all";

  return reminders.filter((r) => {
    const typeOk = type === "all" || r.type === type;
    const priorityOk = priority === "all" || r.priority === priority;
    return typeOk && priorityOk;
  });
}

async function refresh(state) {
  setError("");
  const { reminders } = state;
  const auth = state.session?.access_token;
  if (!auth) return;

  state.loading = true;
  setLoading(true);
  try {
    const data = await apiCall("/reminders", {
      method: "GET",
      token: state.session.access_token,
    });
    state.reminders = data || [];
    renderReminders(applyFilters(state.reminders));
  } catch (err) {
    setError(err?.message || "Failed to load reminders.");
  } finally {
    state.loading = false;
    setLoading(false);
  }
}

document.addEventListener("DOMContentLoaded", async () => {
  let supabase = null;
  try {
    supabase = await getSupabaseClient();
  } catch (err) {
    setError(err?.message || "Failed to connect to Supabase.");
    return;
  }

  const state = {
    session: null,
    reminders: [],
    loading: false,
  };

  initThemeToggle();

  const { data } = await supabase.auth.getSession();
  if (!data?.session) {
    window.location.href = "/login.html";
    return;
  }
  state.session = data.session;

  $("welcomeEmail").textContent = data.session.user.email || "there";
  $("logoutBtn")?.addEventListener("click", async () => {
    await supabase.auth.signOut();
    window.location.href = "/login.html";
  });

  $("filterType")?.addEventListener("change", () => {
    renderReminders(applyFilters(state.reminders));
  });
  $("filterPriority")?.addEventListener("change", () => {
    renderReminders(applyFilters(state.reminders));
  });

  $("pendingReminderList")?.addEventListener("click", async (e) => {
    const btn = e.target.closest("button[data-action]");
    if (!btn) return;

    const action = btn.getAttribute("data-action");
    const id = btn.getAttribute("data-id");
    if (!id) return;

    setError("");
    setLoading(true);
    try {
      if (action === "delete") {
        await apiCall(`/reminders/${id}`, { method: "DELETE", token: state.session.access_token });
      } else if (action === "toggle-status") {
        // We can't rely on data state in the button, so compute status from button text.
        const current = btn.textContent.includes("Mark Completed") ? "pending" : "completed";
        const next = current === "pending" ? "completed" : "pending";
        await apiCall(`/reminders/${id}`, {
          method: "PUT",
          token: state.session.access_token,
          body: { status: next },
        });

        if (next === "completed") {
          const messages = [
            "🎁 Gift unlocked! You earned this badge.",
            "🏆 Achievement unlocked! Keep shining.",
            "💖 Nice work! Your future self thanks you.",
            "✨ Badge earned! Soft progress is still progress."
          ];
          const pick = messages[Math.floor(Math.random() * messages.length)];
          showToast(pick);
        }
      }
      await refresh(state);
    } catch (err) {
      setError(err?.message || "Action failed.");
    } finally {
      setLoading(false);
    }
  });

  $("completedReminderList")?.addEventListener("click", async (e) => {
    const btn = e.target.closest("button[data-action]");
    if (!btn) return;

    const action = btn.getAttribute("data-action");
    const id = btn.getAttribute("data-id");
    if (!id) return;

    setError("");
    setLoading(true);
    try {
      if (action === "delete") {
        await apiCall(`/reminders/${id}`, { method: "DELETE", token: state.session.access_token });
      } else if (action === "toggle-status") {
        const current = btn.textContent.includes("Mark Completed") ? "pending" : "completed";
        const next = current === "pending" ? "completed" : "pending";
        await apiCall(`/reminders/${id}`, {
          method: "PUT",
          token: state.session.access_token,
          body: { status: next },
        });
      }
      await refresh(state);
    } catch (err) {
      setError(err?.message || "Action failed.");
    } finally {
      setLoading(false);
    }
  });

  $("reminderForm")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    setError("");

    const title = $("title").value.trim();
    const description = normalizeText($("description").value);
    const type = $("type").value;
    const priority = $("priority").value;
    const due = toDueDateValue($("dueDate").value);

    if (!title) return setError("Title is required.");

    setLoading(true);
    try {
      await apiCall("/reminders", {
        method: "POST",
        token: state.session.access_token,
        body: {
          title,
          description,
          type,
          priority,
          due_date: due,
          status: "pending",
        },
      });
      $("reminderForm").reset();
      $("status").value = "pending";
      await refresh(state);
    } catch (err) {
      setError(err?.message || "Could not create reminder.");
    } finally {
      setLoading(false);
    }
  });

  // Initial load
  await refresh(state);
});

