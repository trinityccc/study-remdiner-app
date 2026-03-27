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
  const btn = document.getElementById("signupBtn");
  const spinner = document.getElementById("signupSpinner");
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

  const { data } = await supabase.auth.getSession();
  if (data?.session) {
    window.location.href = "/dashboard.html";
    return;
  }

  const form = document.getElementById("signupForm");
  form?.addEventListener("submit", async (e) => {
    e.preventDefault();
    setError("");

    const btn = document.getElementById("signupBtn");
    if (btn?.disabled) return; // Prevent double-submit spamming Supabase

    const email = document.getElementById("email").value.trim();
    const password = document.getElementById("password").value;
    const confirm = document.getElementById("confirmPassword").value;

    if (!email) return setError("Please enter your email.");
    if (!password) return setError("Please enter your password.");
    if (password !== confirm) return setError("Passwords do not match.");

    try {
      setLoading(true);
      const { data, error } = await supabase.auth.signUp({ email, password });
      if (error) throw error;

      // Supabase may return no session if email confirmation is enabled.
      // Requirement: redirect to login after creating an account.
      const hasSession = Boolean(data?.session);
      const msg = hasSession
        ? "Account created. Redirecting you..."
        : "Account created. If confirmation is enabled, check your email then login.";

      // Surface a friendly message on the login page.
      const url = new URL("/login.html", window.location.origin);
      url.searchParams.set("signup", hasSession ? "success" : "needs_confirmation");
      url.searchParams.set("message", msg);
      window.location.href = url.toString();
    } catch (err) {
      const msg = String(err?.message || "Sign-up failed. Please try again.");
      // Supabase enforces email rate limits; we show the exact error message.
      setError(msg);
    } finally {
      setLoading(false);
    }
  });
});

