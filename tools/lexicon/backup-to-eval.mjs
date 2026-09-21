#!/usr/bin/env node
/**
 * Vytáhne ze zálohy Heller (šifrované heslem, nebo staré plain SQLite) záznamy s kategoriemi,
 * které uživatel zařadil, a zapíše je jako vyhodnocovací sadu se štítky.
 *
 * Heslo zadáš ty do terminálu (nebo přes proměnnou HELLER_BACKUP_PASSWORD); nikam se neukládá.
 * Metadata zálohy (Fio tokeny) se zahodí hned po dešifrování — na disk jde jen dočasná SQLite
 * kopie ve složce $TMPDIR, která se po skončení smaže.
 *
 * Použití:
 *   node tools/lexicon/backup-to-eval.mjs zaloha.bin app/src/test/resources/categorize/local/real.tsv
 *   node tools/lexicon/backup-to-eval.mjs zaloha.bin pohyby.csv app/src/test/resources/categorize/local/real.tsv
 *
 * S CSV exportem z Fia (rozšířený, se sloupcem „ID operace") se štítky napojí na řádky CSV
 * podle ID transakce — sada pak pokrývá celou historii exportu a štítek má tam, kde ho appka zná.
 * Bez CSV se vypíšou přímo záznamy z DB (jen INCOME/EXPENSE; převody se přeskočí).
 */
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import crypto from 'node:crypto';
import readline from 'node:readline';
import { DatabaseSync } from 'node:sqlite';

const args = process.argv.slice(2);
if (args.length < 2) { console.error('použití: backup-to-eval.mjs <zaloha> [pohyby.csv] <vystup.tsv>'); process.exit(1); }
const backupPath = args[0];
const csvPath = args.length === 3 ? args[1] : null;
const outPath = args[args.length - 1];

const MAGIC = Buffer.from('HELLERB1', 'ascii');

async function askPassword() {
  if (process.env.HELLER_BACKUP_PASSWORD) return process.env.HELLER_BACKUP_PASSWORD;
  return new Promise(resolve => {
    const rl = readline.createInterface({ input: process.stdin, output: process.stderr, terminal: true });
    process.stderr.write('Heslo zálohy: ');
    // Skryté psaní: přepíšeme výstup readline, ať se heslo neechuje.
    rl._writeToOutput = () => {};
    rl.question('', pw => { rl.close(); process.stderr.write('\n'); resolve(pw); });
  });
}

/** Rozbalí zálohu → Buffer s SQLite DB. Metadata (tokeny) se zahodí. */
async function extractDb(blob) {
  if (blob.subarray(0, 8).equals(MAGIC)) {
    const iterations = blob.readUInt32BE(8);
    const salt = blob.subarray(12, 28);
    const iv = blob.subarray(28, 40);
    const ct = blob.subarray(40, blob.length - 16);
    const tag = blob.subarray(blob.length - 16);
    const password = await askPassword();
    const key = crypto.pbkdf2Sync(Buffer.from(password, 'utf8'), salt, iterations, 32, 'sha256');
    const decipher = crypto.createDecipheriv('aes-256-gcm', key, iv);
    decipher.setAuthTag(tag);
    let payload;
    try { payload = Buffer.concat([decipher.update(ct), decipher.final()]); }
    catch { console.error('Špatné heslo nebo poškozená záloha.'); process.exit(2); }
    const metaLen = payload.readUInt32BE(0);
    return payload.subarray(4 + metaLen); // meta (s tokeny) záměrně nečteme
  }
  if (blob.subarray(0, 15).toString('ascii') === 'SQLite format 3') return blob;
  console.error('Soubor není záloha Heller.'); process.exit(1);
}

const CARD = ['visa', 'mastercard', 'maestro', 'paywave', 'debit', 'credit'];
const NOISE = new Set(['dekujeme', 'dakujeme', 'dziekujemy', 'danke', 'thank you', 'thanks']);
const norm = s => String(s ?? '').normalize('NFD').replace(/\p{M}+/gu, '').replace(/[’'`´]/g, '').toLowerCase().replace(/[^a-z0-9]+/g, ' ').trim();
const looksLikeCard = n => CARD.some(w => norm(n).split(' ').includes(w));
function merchantFrom(raw) {
  if (!raw) return null;
  let s = raw.trim();
  for (const p of ['Nákup:', 'Platba kartou:', 'Výběr:', 'Kredit:', 'Nákup']) if (s.startsWith(p)) { s = s.slice(p.length).trim(); break; }
  const seg = s.split(',').map(x => x.trim()).filter(Boolean);
  if (!seg.length) return null;
  if (NOISE.has(norm(seg[0])) && seg[1]) return seg[1];
  return seg[0];
}
const clean = s => (s ?? '').toString().replace(/\t/g, ' ');

const blob = fs.readFileSync(backupPath);
const dbBytes = await extractDb(blob);
const tmp = path.join(fs.mkdtempSync(path.join(os.tmpdir(), 'heller-')), 'heller.db');
fs.writeFileSync(tmp, dbBytes);
let rows = [];
try {
  const db = new DatabaseSync(tmp, { readOnly: true });
  const cols = db.prepare('PRAGMA table_info(records)').all().map(c => c.name);
  const has = c => cols.includes(c);
  const sel = ['id', 'type', 'categoryId', 'amountMinor', 'dateTime', 'payee', 'note', 'fioTransactionId',
    has('counterAccount') ? 'counterAccount' : "NULL AS counterAccount",
    has('variableSymbol') ? 'variableSymbol' : "NULL AS variableSymbol",
    has('txType') ? 'txType' : "NULL AS txType",
    has('categoryAuto') ? 'categoryAuto' : "0 AS categoryAuto"].join(', ');
  rows = db.prepare(`SELECT ${sel} FROM records WHERE type IN ('EXPENSE','INCOME') ORDER BY dateTime ASC`).all();
  db.close();
} finally {
  fs.rmSync(path.dirname(tmp), { recursive: true, force: true });
}

// Štítek = kategorie potvrzená uživatelem (categoryAuto = 0). Ve starých DB (< v9) sloupec není,
// takže štítek je každá kategorie — včetně těch, které kdysi přiřadil starý slovník.
const labelOf = r => (r.categoryId && !r.categoryAuto) ? r.categoryId : null;
const out = ['# payee\tnote\ttxType\tcounterAccount\tvs\tamountMinor\tincome\texpected'];
let labeled = 0;

if (csvPath) {
  const byFioId = new Map(rows.filter(r => r.fioTransactionId != null).map(r => [String(r.fioTransactionId), r]));
  const text = fs.readFileSync(csvPath, 'utf8').replace(/^﻿/, '');
  const lines = text.split(/\r?\n/).filter(l => l.trim());
  const parse = l => l.slice(1, -1).split('";"');
  const h = parse(lines[0]); const col = n => h.indexOf(n);
  const iDate = col('Datum'), iAmt = col('Objem'), iAcc = col('Protiúčet'), iType = col('Typ'), iNote = col('Poznámka'), iVs = col('VS'), iName = col('Název protiúčtu'), iId = col('ID operace');
  if (iId < 0) { console.error('CSV nemá sloupec „ID operace" — použij rozšířený export z Fia.'); process.exit(1); }
  const csvRows = lines.slice(1).map(parse).map(r => { const [d, m, y] = r[iDate].split('.').map(Number); return { r, key: y * 10000 + m * 100 + d }; })
    .sort((a, b) => a.key - b.key).map(x => x.r);
  let matched = 0;
  for (const r of csvRows) {
    const amount = Number(r[iAmt].replace(',', '.'));
    const rawDesc = r[iNote] || null; const acc = r[iAcc] || null; const name = iName >= 0 ? (r[iName] || null) : null;
    const isCard = /karetn/i.test(r[iType]) || looksLikeCard(name);
    const payee = (isCard ? (merchantFrom(rawDesc) ?? name) : (name ?? merchantFrom(rawDesc) ?? acc)) ?? '';
    const note = rawDesc && rawDesc !== payee ? rawDesc : '';
    const dbRow = byFioId.get(String(r[iId]).split('.')[0]);
    if (dbRow) matched++;
    const label = dbRow ? labelOf(dbRow) : null;
    if (label) labeled++;
    out.push([clean(payee), clean(note), clean(r[iType]), clean(acc), clean(r[iVs]), Math.round(Math.abs(amount) * 100), amount >= 0 ? 1 : 0, label ?? '?'].join('\t'));
  }
  console.error(`${csvRows.length} řádků CSV, ${matched} napojeno na DB, ${labeled} se štítkem`);
} else {
  for (const r of rows) {
    // Starý import (< v1.4) ukládal u karet název karty jako plátce — obchodníka vezmi z poznámky.
    const payee = looksLikeCard(r.payee) ? (merchantFrom(r.note) ?? r.payee) : r.payee;
    const label = labelOf(r);
    if (label) labeled++;
    out.push([clean(payee), clean(r.note), clean(r.txType), clean(r.counterAccount), clean(r.variableSymbol), r.amountMinor, r.type === 'INCOME' ? 1 : 0, label ?? '?'].join('\t'));
  }
  console.error(`${rows.length} záznamů z DB, ${labeled} se štítkem`);
}
fs.mkdirSync(path.dirname(outPath), { recursive: true });
fs.writeFileSync(outPath, out.join('\n') + '\n');
console.error(`zapsáno ${outPath}`);
