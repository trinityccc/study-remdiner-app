import { getSupabaseClient } from "./supabaseClient.js";

function applyTheme(mode) {
  const isDark = mode === "dark";
  document.documentElement.classList.toggle("dark", isDark);
  const btn = document.getElementById("themeToggle");
  if (btn) btn.textContent = isDark ? "🌙 Dark" : "🌞 Light";
}

function initThemeToggle() {
  const saved = localStorage.getItem("themeMode") || "light";
  applyTheme(saved);
  document.getElementById("themeToggle")?.addEventListener("click", () => {
    const next = document.documentElement.classList.contains("dark") ? "light" : "dark";
    localStorage.setItem("themeMode", next);
    applyTheme(next);
  });
}

function setLoading(isLoading) {
  const btn = document.getElementById("loginBtn");
  const spinner = document.getElementById("loginSpinner");
  if (btn) btn.disabled = isLoading;
  if (spinner) spinner.classList.toggle("hidden", !isLoading);
}

function setError(message) {
  const el = document.getElementById("errorBox");
  if (!el) return;
  el.textContent = message || "";
  el.classList.toggle("hidden", !message);
}

document.addEventListener("DOMContentLoaded", async () => {
  initThemeToggle();
  let supabase = null;
  try {
    supabase = await getSupabaseClient();
  } catch (err) {
    setError(err?.message || "Failed to connect to Supabase.");
    return;
  }

  // Optional feedback after sign-up redirects.
  const params = new URLSearchParams(window.location.search);
  const signupState = params.get("signup");
  const message = params.get("message");
  if (message) {
    setError(message);
  }

  const { data } = await supabase.auth.getSession();
  if (data?.session) {
    window.location.href = "/dashboard.html";
    return;
  }

  const form = document.getElementById("loginForm");
  form?.addEventListener("submit", async (e) => {
    e.preventDefault();
    setError("");

    const btn = document.getElementById("loginBtn");
    if (btn?.disabled) return; // Prevent double-submit

    const email = document.getElementById("email").value.trim();
    const password = document.getElementById("password").value;

    if (!email) {
      setError("Please enter your email.");
      return;
    }
    if (!password) {
      setError("Please enter your password.");
      return;
    }

    try {
      setLoading(true);
      const { data, error } = await supabase.auth.signInWithPassword({
        email,
        password,
      });

      if (error) throw error;
      if (data?.session) {
        window.location.href = "/dashboard.html";
      } else {
        setError("Login succeeded, but no session was found. Try again.");
      }
    } catch (err) {
      setError(err?.message || "Login failed. Please check your credentials.");
    } finally {
      setLoading(false);
    }
  });
});

