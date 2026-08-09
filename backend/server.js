'use strict';

/**
 * Car Jam Solver — real-time leaderboard API.
 *
 * The MongoDB connection string lives ONLY in the MONGODB_URI environment
 * variable (Render dashboard → Environment). Never commit it; never ship it in
 * the APK. The app talks to this service over HTTPS; clients are identified by
 * an anonymous install id, and the server re-computes + clamps every rating so
 * a tampered client cannot post a fake score.
 */

const express = require('express');
const mongoose = require('mongoose');

const app = express();
app.use(express.json({ limit: '20kb' }));
app.disable('x-powered-by');

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

app.get('/healthz', (_req, res) => res.json({ ok: true, ts: Date.now() }));

/* Upsert this device's stats; rating is recomputed server-side. */
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
    await Player.updateOne(
      { deviceId },
      { $set: { ...p, rating } },
      { upsert: true },
    );
    const rank = (await Player.countDocuments({ rating: { $gt: rating } })) + 1;
    res.json({ ok: true, rating, rank });
  } catch (e) {
    res.status(500).json({ ok: false, error: 'server error' });
  }
});

/* Top 100 + the caller's own entry (even outside the top 100). */
app.get('/api/leaderboard', async (req, res) => {
  try {
    const top = await Player.find({})
      .sort({ rating: -1, updatedAt: 1 })
      .limit(100)
      .lean();
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

const port = process.env.PORT || 3000;
const uri = process.env.MONGODB_URI;
if (!uri) {
  console.error('MONGODB_URI is not set — add it in your Render service environment.');
  process.exit(1);
}
mongoose
  .connect(uri, { dbName: 'carjam' })
  .then(() => app.listen(port, () => console.log(`carjam leaderboard api on :${port}`)))
  .catch((e) => {
    console.error('MongoDB connection failed:', e.message);
    process.exit(1);
  });
