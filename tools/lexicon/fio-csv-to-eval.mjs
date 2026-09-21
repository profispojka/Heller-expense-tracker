#!/usr/bin/env node
/**
 * Převod CSV exportu z Fio internetbankingu („Pohyby na účtu", UTF-8 s BOM, středníky) do
 * formátu vyhodnocovací sady kategorizace (viz app/src/test/resources/categorize/eval.tsv).
 *
 * Plátce a poznámku odvozuje stejně jako FioRepository.importJson: u karetních plateb je
 * obchodník text mezi „Nákup:" a první čárkou (zdvořilostní „Dekujeme" se přeskočí), u převodů
 * je plátce „Název protiúčtu" (má ho jen rozšířený export), jinak číslo protiúčtu. Sloupec
 * „expected" zůstává „?" — doplň ručně, nebo nech a použij jen report pokrytí (RealDataReportTest).
 *
 *   node tools/lexicon/fio-csv-to-eval.mjs pohyby.csv app/src/test/resources/categorize/local/real.tsv
 */
import fs from 'node:fs';
import path from 'node:path';

const [,, src, dst] = process.argv;
if (!src || !dst) { console.error('použití: fio-csv-to-eval.mjs <pohyby.csv> <vystup.tsv>'); process.exit(1); }

const text = fs.readFileSync(src, 'utf8').replace(/^﻿/, '');
const lines = text.split(/\r?\n/).filter(l => l.trim());
const parse = l => l.slice(1, -1).split('";"');
const header = parse(lines[0]);
const col = name => header.indexOf(name);
const iDate = col('Datum'), iAmt = col('Objem'), iAcc = col('Protiúčet'), iType = col('Typ'), iNote = col('Poznámka'), iVs = col('VS'), iName = col('Název protiúčtu');

const NOISE = new Set(['dekujeme', 'dakujeme', 'dziekujemy', 'danke', 'thank you', 'thanks']);
const CARD = ['visa', 'mastercard', 'maestro', 'paywave', 'debit', 'credit'];
const norm = s => String(s ?? '').normalize('NFD').replace(/\p{M}+/gu, '').replace(/[’'`´]/g, '').toLowerCase().replace(/[^a-z0-9]+/g, ' ').trim();

function merchantFrom(raw) {
  if (!raw) return null;
  let s = raw.trim();
  for (const p of ['Nákup:', 'Platba kartou:', 'Výběr:', 'Kredit:', 'Nákup']) {
    if (s.startsWith(p)) { s = s.slice(p.length).trim(); break; }
  }
  const seg = s.split(',').map(x => x.trim()).filter(Boolean);
  if (!seg.length) return null;
  if (NOISE.has(norm(seg[0])) && seg[1]) return seg[1];
  return seg[0];
}
const looksLikeCard = n => CARD.some(w => norm(n).split(' ').includes(w));

const out = ['# payee\tnote\ttxType\tcounterAccount\tvs\tamountMinor\tincome\texpected'];
const rows = lines.slice(1).map(parse).map(r => {
  const [d, m, y] = r[iDate].split('.').map(Number);
  return { r, key: y * 10000 + m * 100 + d };
}).sort((a, b) => a.key - b.key).map(x => x.r);

for (const r of rows) {
  const amount = Number(r[iAmt].replace(',', '.'));
  const minor = Math.round(Math.abs(amount) * 100);
  const income = amount >= 0 ? 1 : 0;
  const rawDesc = r[iNote] || null;
  const acc = r[iAcc] || null;
  const name = iName >= 0 ? (r[iName] || null) : null;
  const isCard = /karetn/i.test(r[iType]) || looksLikeCard(name);
  const payee = (isCard ? (merchantFrom(rawDesc) ?? name) : (name ?? merchantFrom(rawDesc) ?? acc)) ?? '';
  const note = rawDesc && rawDesc !== payee ? rawDesc : '';
  const clean = s => (s ?? '').replace(/\t/g, ' ');
  out.push([clean(payee), clean(note), clean(r[iType]), clean(acc), clean(r[iVs]), minor, income, '?'].join('\t'));
}
fs.mkdirSync(path.dirname(dst), { recursive: true });
fs.writeFileSync(dst, out.join('\n') + '\n');
console.error(`${rows.length} pohybů → ${dst}`);
