'use strict';

/**
 * Car Jam Solver — cloud services API (leaderboard + save sync).
 *
 * Security model (no accounts, no passwords, nothing to phish):
 *  - The app generates a 256-bit random SYNC KEY on first launch. The server
 *    stores ONLY its SHA-256 hash as the save id — the key itself is never
 *    persisted, so a database leak cannot reveal any credential.
 *  - Saves are addressed by hash(syncKey); knowing the hash is useless for
 *    reading or writing anyone's save.
 *  - Economy values are clamped to the same absolute caps as the client AND
 *    delta-limited by elapsed time (e.g. coins can grow ≤ 50k/hour) — a fake
 *    "999,999,999 coins" save is shrunk to something plausible, permanently.
 *  - Leaderboard ratings are recomputed from raw components, never accepted.
 *  - Purchases NEVER sync: no-ads is granted exclusively by Google Play's own
 *    restore flow on the client, so the cloud cannot mint entitlements.
 *  - The MongoDB URI lives only in this process's environment.
 */

const crypto = require('crypto');
const express = require('express');
const mongoose = require('mongoose');

const app = express();
app.use(express.json({ limit: '20kb' }));
app.disable('x-powered-by');

const sha256 = (s) => crypto.createHash('sha256').update(String(s)).digest('hex');

// ---------------------------------------------------------------- models

const playerSchema = new mongoose.Schema(
  {
    deviceId: { type: String, required: true, unique: true, index: true },
    name: { type: String, default: 'Racer' },
    avatarId: { type: Number, default: 0 },
    maxLevel: { type: Number, default: 1 },
    totalCoinsEarned: { type: Number, default: 0 },
    dailyStreak: { type: Number, default: 0 },
    wins: { type: Number, default: 0 },
    losses: { type: Number, default: 0 },
    rating: { type: Number, default: 0, index: true },
  },
  { timestamps: true },
);
const Player = mongoose.model('Player', playerSchema);

const saveSchema = new mongoose.Schema(
  {
    saveId: { type: String, required: true, unique: true, index: true },
    lastDeviceId: { type: String, default: '' },
    name: { type: String, default: 'Racer' },
    avatarId: { type: Number, default: 0 },
    coins: { type: Number, default: 0 },
    gems: { type: Number, default: 0 },
    maxLevel: { type: Number, default: 1 },
    totalCoinsEarned: { type: Number, default: 0 },
    dailyStreak: { type: Number, default: 0 },
    lastClaimDay: { type: Number, default: -1 },
    wins: { type: Number, default: 0 },
    losses: { type: Number, default: 0 },
    practiceWins: { type: Number, default: 0 },
    referredBy: { type: String, default: '' },
    welcomed: { type: Boolean, default: false },
  },
  { timestamps: true },
);
const Save = mongoose.model('Save', saveSchema);

// ---------------------------------------------------------------- helpers

const clampInt = (v, lo, hi) => Math.min(hi, Math.max(lo, Math.floor(Number(v) || 0)));
const cleanName = (s) => {
  const t = String(s == null ? '' : s)
    .replace(/[^\p{L}\p{N} .'_-]/gu, '')
    .trim()
    .slice(0, 14);
  return t || 'Racer';
};
// MUST match Prefs.rating() in the app — clients send components, never a score.
const ratingOf = (p) =>
  p.maxLevel * 120 + Math.floor(p.totalCoinsEarned / 5) + p.dailyStreak * 10;

const publicEntry = (p, rank) => ({
  rank,
  name: p.name,
  avatarId: p.avatarId,
  rating: p.rating,
  maxLevel: p.maxLevel,
  deviceId: p.deviceId,
});

/**
 * Anti-abuse wallet clamp: gains are capped to per-hour rates (+slack) measured
 * against the previously stored value. Spending (decreases) is always allowed.
 */
const economyClamp = (prev, elapsedH, raw, perHour, slack, cap) => {
  let v = clampInt(raw, 0, cap);
  if (prev != null && v > prev) {
    const budget = prev + Math.floor(perHour * elapsedH + slack);
    v = Math.min(v, budget);
  }
  return v;
};

const canonicalSave = (doc) => ({
  name: doc.name,
  avatarId: doc.avatarId,
  coins: doc.coins,
  gems: doc.gems,
  maxLevel: doc.maxLevel,
  totalCoinsEarned: doc.totalCoinsEarned,
  dailyStreak: doc.dailyStreak,
  lastClaimDay: doc.lastClaimDay,
  wins: doc.wins,
  losses: doc.losses,
  practiceWins: doc.practiceWins,
  referredBy: doc.referredBy,
  welcomed: !!doc.welcomed,
  updatedAt: new Date(doc.updatedAt).getTime(),
});

// ---------------------------------------------------------------- routes

app.get('/healthz', (_req, res) => res.json({ ok: true, ts: Date.now() }));

/* Leaderboard: upsert this device's stats; rating recomputed server-side. */
app.post('/api/score', async (req, res) => {
  try {
    const deviceId = String(req.body.deviceId ?? '').slice(0, 64);
    if (deviceId.length < 8) return res.status(400).json({ ok: false, error: 'bad deviceId' });
    const p = {
      name: cleanName(req.body.name),
      avatarId: clampInt(req.body.avatarId, 0, 7),
      maxLevel: clampInt(req.body.maxLevel, 1, 5000),
      totalCoinsEarned: clampInt(req.body.totalCoinsEarned, 0, 100000000),
      dailyStreak: clampInt(req.body.dailyStreak, 0, 3650),
      wins: clampInt(req.body.wins, 0, 100000),
      losses: clampInt(req.body.losses, 0, 100000),
    };
    const rating = ratingOf(p);
    await Player.updateOne({ deviceId }, { $set: { ...p, rating } }, { upsert: true });
    const rank = (await Player.countDocuments({ rating: { $gt: rating } })) + 1;
    res.json({ ok: true, rating, rank });
  } catch (e) {
    res.status(500).json({ ok: false, error: 'server error' });
  }
});

/* Leaderboard: top 100 + the caller's own entry. */
app.get('/api/leaderboard', async (req, res) => {
  try {
    const top = await Player.find({}).sort({ rating: -1, updatedAt: 1 }).limit(100).lean();
    const out = top.map((p, i) => publicEntry(p, i + 1));
    let me = null;
    const deviceId = String(req.query.deviceId ?? '');
    if (deviceId.length >= 8) {
      const mine = await Player.findOne({ deviceId }).lean();
      if (mine) {
        const rank = (await Player.countDocuments({ rating: { $gt: mine.rating } })) + 1;
        me = publicEntry(mine, rank);
      }
    }
    res.json({ ok: true, top: out, me });
  } catch (e) {
    res.status(500).json({ ok: false, error: 'server error' });
  }
});

/*
 * Cloud save: upsert + return the canonical (server-clamped) snapshot.
 * The save is addressed by SHA-256(syncKey) — possession of the key IS the
 * auth; the raw key never touches the database. Absolute caps mirror the
 * client (coins 10M, gems 500k, …) and gains are rate-limited per hour.
 */
app.post('/api/save', async (req, res) => {
  try {
    const key = String(req.body.key ?? '');
    if (!/^[a-zA-Z0-9]{24,128}$/.test(key)) {
      return res.status(400).json({ ok: false, error: 'bad key' });
    }
    const saveId = sha256(key.toLowerCase());
    const body = (req.body.state && typeof req.body.state === 'object') ? req.body.state : {};
    const prev = await Save.findOne({ saveId }).lean();
    const now = Date.now();
    // first-ever save: any starting value within absolute caps is fine
    const elapsedH = prev
      ? Math.max(0.01, (now - new Date(prev.updatedAt).getTime()) / 3.6e6)
      : 1e9;
    const P = (field) => (prev ? prev[field] : null);

    const state = {
      name: cleanName(body.name),
      avatarId: clampInt(body.avatarId, 0, 7),
      coins: economyClamp(P('coins'), elapsedH, body.coins, 50000, 10000, 10000000),
      gems: economyClamp(P('gems'), elapsedH, body.gems, 300, 120, 500000),
      maxLevel: economyClamp(P('maxLevel'), elapsedH, body.maxLevel, 30, 8, 5000),
      totalCoinsEarned: economyClamp(P('totalCoinsEarned'), elapsedH, body.totalCoinsEarned, 50000, 10000, 100000000),
      dailyStreak: economyClamp(P('dailyStreak'), elapsedH, body.dailyStreak, 1 / 11, 1, 3650),
      lastClaimDay: clampInt(body.lastClaimDay, -1, 100000),
      wins: economyClamp(P('wins'), elapsedH, body.wins, 60, 12, 100000),
      losses: economyClamp(P('losses'), elapsedH, body.losses, 120, 24, 100000),
      practiceWins: economyClamp(P('practiceWins'), elapsedH, body.practiceWins, 120, 24, 100000),
      referredBy: String(body.referredBy ?? '').slice(0, 24),
      welcomed: !!body.welcomed,
    };

    const doc = await Save.findOneAndUpdate(
      { saveId },
      {
        $set: {
          ...state,
          lastDeviceId: String(req.body.deviceId ?? '').slice(0, 64),
        },
      },
      { upsert: true, new: true, setDefaultsOnInsert: true },
    ).lean();

    res.json({ ok: true, state: canonicalSave(doc) });
  } catch (e) {
    res.status(500).json({ ok: false, error: 'server error' });
  }
});

const port = process.env.PORT || 3000;
const uri = process.env.MONGODB_URI;
if (!uri) {
  console.error('MONGODB_URI is not set — add it in your Render service environment.');
  process.exit(1);
}
mongoose
  .connect(uri, { dbName: 'carjam' })
  .then(() => app.listen(port, () => console.log(`carjam api on :${port}`)))
  .catch((e) => {
    console.error('MongoDB connection failed:', e.message);
    process.exit(1);
  });
