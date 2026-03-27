import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

let supabaseSingleton = null;

async function loadPublicConfig() {
  const res = await fetch("/config", { method: "GET" });
  if (!res.ok) {
    // Backend returns ApiError JSON; try to surface its message.
    let payload = null;
    try {
      payload = await res.json();
    } catch {
      // ignore
    }
    const msg =
      payload?.message ||
      payload?.error ||
      payload?.status ||
      `Backend config failed (HTTP ${res.status}).`;
    throw new Error(msg);
  }
  return res.json();
}

export async function getSupabaseClient() {
  if (supabaseSingleton) return supabaseSingleton;

  const cfg = await loadPublicConfig();

  supabaseSingleton = createClient(cfg.supabaseUrl, cfg.supabaseAnonKey, {
    auth: {
      persistSession: true,
      autoRefreshToken: true,
      detectSessionInUrl: true,
    },
  });

  return supabaseSingleton;
}

