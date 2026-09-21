#!/usr/bin/env node
/**
 * Generátor indexu provozoven (POI) pro kategorizaci z Overture Maps Places
 * (https://overturemaps.org, licence CDLA-Permissive-2.0; zdroje Meta, Microsoft, Foursquare,
 * OpenStreetMap). Vstup: GeoJSONSeq soubory stažené přes `overturemaps download --type=place`
 * pro bounding boxy CZ / SK / PL. Výstup: binární index hashů názvů → kategorie Heller
 * (`app/src/main/assets/poi_index.bin`). Do APK nejdou názvy, jen 64bitové FNV-1a hashe
 * normalizovaných názvů, takže index je malý (8 B + 1 B na provozovnu) a nic z něj nelze číst.
 *
 * Použití:
 *   node tools/lexicon/generate-poi.mjs cz.geojsonl sk.geojsonl pl.geojsonl
 *
 * Pravidla výběru: jen země CZ/SK/PL, důvěra ≥ 0.5, kategorie namapovaná níže, název po
 * normalizaci aspoň dva tokeny (nebo jeden dlouhý), bez právních forem a čísel; stejný název
 * s různými kategoriemi se bere jen při ≥ 70% shodě, jinak se vynechá. Názvy, které už zná
 * ruční slovník nebo slovník značek, se přeskočí.
 */
import fs from 'node:fs';
import path from 'node:path';
import readline from 'node:readline';
import { fileURLToPath } from 'node:url';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const OUT = path.join(ROOT, 'app/src/main/assets/poi_index.bin');
const COUNTRIES = new Set(['CZ', 'SK', 'PL']);
const MIN_CONFIDENCE = 0.5;

// Taxonomie Overture (hierarchy) → slug kategorie Heller. Nejdelší shodný prefix vyhrává.
const MAPPING = [
  ['food_and_drink/casual_eatery/delicatessen', 'food_groceries'],
  ['food_and_drink', 'food_dining'],
  ['shopping/food_and_beverage_store/vitamin_and_supplement_store', 'shopping_drugstore'],
  ['shopping/food_and_beverage_store/tobacco_shop', 'shopping_other'],
  ['shopping/food_and_beverage_store', 'food_groceries'],
  ['shopping/convenience_store', 'food_groceries'],
  ['shopping/warehouse_club_store', 'food_groceries'],
  ['shopping/market', 'food_groceries'],
  ['shopping/superstore', 'food_groceries'],
  ['shopping/specialty_store/hardware_home_and_garden_store', 'housing_home'],
  ['shopping/specialty_store/pharmacy_and_drug_store', 'health_care'],
  ['shopping/specialty_store/medical_supply_store', 'health_care'],
  ['shopping/specialty_store/flowers_and_gifts_store', 'charity'],
  ['shopping/specialty_store/souvenir_store', 'charity'],
  ['shopping/specialty_store/sporting_goods_store', 'health_sport'],
  ['shopping/specialty_store/electronics_store', 'shopping_electronics'],
  ['shopping/specialty_store/arts_crafts_and_hobby_store', 'leisure_culture'],
  ['shopping/specialty_store/books_music_and_video_store', 'leisure_culture'],
  ['shopping/specialty_store/musical_instrument_and_pro_audio_store', 'leisure_culture'],
  ['shopping/specialty_store/vehicle_parts_store', 'transport_car_other'],
  ['shopping/specialty_store/personal_care_and_beauty_store', 'shopping_drugstore'],
  ['shopping/specialty_store', 'shopping_other'],
  ['shopping/fashion_and_apparel_store/eyewear_store', 'health_care'],
  ['shopping/fashion_and_apparel_store/jewelry_store', 'shopping_other'],
  ['shopping/fashion_and_apparel_store', 'shopping_clothes'],
  ['shopping/second_hand_store', 'shopping_clothes'],
  ['shopping', 'shopping_other'],
  ['lifestyle_services/personal_or_beauty_service/shoe_repair', 'shopping_other'],
  ['lifestyle_services/personal_or_beauty_service/life_coach', 'leisure_education'],
  ['lifestyle_services/personal_or_beauty_service', 'health_sport'],
  ['lifestyle_services/wellness_service', 'health_sport'],
  ['lifestyle_services/animal_or_pet_service', 'shopping_other'],
  ['lifestyle_services/food_service', 'food_dining'],
  ['travel_and_transportation/vehicle_service', 'transport_car_other'],
  ['travel_and_transportation/fueling_station', 'transport_fuel'],
  ['travel_and_transportation/parking', 'transport_car_other'],
  ['travel_and_transportation/ground_transport_facility_or_service/taxi_or_ride_share_service', 'transport_taxi'],
  ['travel_and_transportation/ground_transport_facility_or_service', 'transport_public'],
  ['travel_and_transportation/air_transport_facility_or_service', 'transport_public'],
  ['travel_and_transportation/travel_service', 'leisure_holidays'],
  ['lodging', 'leisure_holidays'],
  ['sports_and_recreation/sport_or_fitness_facility', 'health_sport'],
  ['sports_and_recreation/sport_or_recreation_club', 'health_sport'],
  ['sports_and_recreation/recreational_equipment_rental', 'health_sport'],
  ['sports_and_recreation/park', null],
  ['sports_and_recreation/recreational_trail_or_path', null],
  ['sports_and_recreation/sport_team', null],
  ['sports_and_recreation/sport_league', null],
  ['sports_and_recreation', 'health_sport'],
  ['health_care', 'health_care'],
  ['education/library', 'leisure_culture'],
  ['education', 'leisure_education'],
  ['arts_and_entertainment/gaming_venue', 'shopping_other'],
  ['arts_and_entertainment/nightlife_venue', 'food_dining'],
  ['arts_and_entertainment', 'leisure_culture'],
  ['cultural_and_historic/historic_site', 'leisure_culture'],
  ['cultural_and_historic/memorial_site', 'leisure_culture'],
  ['cultural_and_historic/cultural_center', 'leisure_culture'],
  // Bankomaty, banky, účetní, poradci a právníci záměrně chybí: bankomat řeší typ pohybu,
  // zbytek se platí převodem a názvy jsou často jména osob.
  ['services_and_business/financial_service/insurance_agency', 'financial_insurance'],
  ['services_and_business/financial_service/investing', 'investments'],
  ['services_and_business/financial_service/installment_loans', 'financial_loans'],
  ['services_and_business/financial_service/mortgage_broker', 'financial_loans'],
  ['services_and_business/shipping_or_delivery_service', 'shopping_other'],
  ['services_and_business/home_service', 'housing_home'],
  ['services_and_business/building_or_construction_service', 'housing_home'],
  ['services_and_business/laundry_service', 'housing_home'],
  ['services_and_business/housing_or_property_service', 'housing_home'],
  ['services_and_business/storage_facility', 'housing_home'],
  ['services_and_business/technical_service', 'shopping_electronics'],
  ['services_and_business/printing_service', 'shopping_other'],
  ['services_and_business/rental_service/vehicle_rental_service', 'transport_car_other'],
  ['services_and_business/telecommunications_service', 'comm_phone_internet'],
  ['community_and_government/public_utility', 'housing_utilities'],
];

// Tokeny bez informace: právní formy, spojky, platební šum — stejné jako MerchantText.STOP
// plus jednopísmenné zbytky z „s.r.o.", „a.s.", „sp. z o.o.".
const STOP = new Set(['sro', 'spol', 'as', 'gmbh', 'ltd', 'inc', 'llc', 'kasa', 'pos', 'kiosk', 'www', 'com',
  'platba', 'kartou', 'nakup', 'vyber', 'kredit', 'the', 'and', 'und', 'dne', 'castka', 'czk', 'eur', 'pln', 'usd',
  'cz', 'sk', 'pl', 'de', 'at', 'nyx', 'sumup', 'gopay', 'paypal', 'zettle', 'adyen', 'payu', 'comgate', 'stripe', 'bkg', 'sq', 'sp',
  's', 'r', 'o', 'a', 'z', 'zoo', 'oo', 'ks', 'sa', 'spolka', 'akcyjna']);

const norm = s => String(s ?? '').normalize('NFD').replace(/\p{M}+/gu, '').replace(/[’'`´]/g, '').toLowerCase().replace(/[^a-z0-9]+/g, ' ').trim();
const isWord = t => t.length >= 3 && !/\d/.test(t);
// Stejný filtr jako PoiIndex.match v appce: bez stop slov, čísel a jednopísmenných tokenů („U Lípy" → „lipy").
const tokensOf = s => norm(s).split(' ').filter(t => t.length > 1 && !STOP.has(t) && !/\d/.test(t));

function categoryFor(hierarchy) {
  const h = (hierarchy ?? []).join('/');
  let best = null;
  for (const [prefix, cat] of MAPPING) {
    if ((h === prefix || h.startsWith(prefix + '/')) && (best === null || prefix.length > best[0].length)) best = [prefix, cat];
  }
  return best ? best[1] : null;
}

// Existující klíče slovníku (ruční, značky, obecná slova) — POI je nesmí duplikovat ani přebíjet.
function existingKeys() {
  const keys = new Set();
  const grab = (file, re) => { const t = fs.readFileSync(path.join(ROOT, file), 'utf8'); for (const m of t.matchAll(re)) keys.add(m[1].replace(/\*/g, '')); };
  grab('app/src/main/java/cz/heller/core/categorize/SeedRules.kt', /"([^"]+)" to "/g);
  grab('app/src/main/java/cz/heller/core/categorize/GenericLexicon.kt', /"([^"]+)" to "/g);
  const brands = fs.readFileSync(path.join(ROOT, 'app/src/main/java/cz/heller/core/categorize/BrandLexicon.kt'), 'utf8');
  for (const m of brands.matchAll(/^([a-z0-9 ]+)\t/gm)) keys.add(m[1]);
  return keys;
}

function fnv1a64(str) {
  let h = 0xcbf29ce484222325n;
  for (const b of Buffer.from(str, 'utf8')) { h ^= BigInt(b); h = (h * 0x100000001b3n) & 0xffffffffffffffffn; }
  return BigInt.asIntN(64, h);
}

const files = process.argv.slice(2);
if (!files.length) { console.error('použití: generate-poi.mjs <places.geojsonl> …'); process.exit(1); }
const known = existingKeys();
const votes = new Map(); // phrase → Map<cat, count>
let seen = 0, kept = 0, byCountry = {};

for (const f of files) {
  const rl = readline.createInterface({ input: fs.createReadStream(f) });
  for await (const line of rl) {
    if (!line.trim()) continue;
    let p;
    try { p = JSON.parse(line).properties; } catch { continue; }
    const country = p.addresses?.[0]?.country;
    if (!COUNTRIES.has(country)) continue;
    if ((p.confidence ?? 0) < MIN_CONFIDENCE) continue;
    const name = p.names?.primary;
    if (!name) continue;
    seen++;
    // Nenamapovaná místa (náměstí, parky, úřady…) hlasují také — název, který je hlavně
    // náměstí a jen jednou nemocnice („Karlovo náměstí 7"), do indexu nepatří.
    const cat = categoryFor(p.taxonomy?.hierarchy) ?? '(none)';
    const tokens = tokensOf(name);
    if (!tokens.length) continue;
    const letters = tokens.join('').length;
    // Jednoslovné názvy jen dlouhé (≥ 8) — krátké kolidují s ulicemi a běžnými slovy („Černého").
    const ok = (tokens.length >= 2 && letters >= 6) || (tokens.length === 1 && tokens[0].length >= 8);
    if (!ok) continue;
    const phrase = tokens.slice(0, 6).join(' ');
    if (known.has(phrase) || (tokens.length === 1 && known.has(tokens[0]))) continue;
    const v = votes.get(phrase) ?? new Map();
    v.set(cat, (v.get(cat) ?? 0) + 1);
    votes.set(phrase, v);
    if (cat !== '(none)') { kept++; byCountry[country] = (byCountry[country] ?? 0) + 1; }
  }
  console.error(`${path.basename(f)}: zatím ${seen} kandidátů, ${kept} po filtru`);
}

const entries = [];
let conflicts = 0;
for (const [phrase, v] of votes) {
  const total = [...v.values()].reduce((a, b) => a + b, 0);
  const [cat, n] = [...v.entries()].filter(([c]) => c !== '(none)').sort((a, b) => b[1] - a[1])[0] ?? [];
  if (!cat) continue;
  if (n / total < 0.7) { conflicts++; continue; }
  // Jednoslovný název jen když se opakuje (řetězec), ne jednorázový podnik — příliš mnoho kolizí s běžnými slovy.
  if (!phrase.includes(' ') && total < 2) continue;
  entries.push([fnv1a64(phrase), cat]);
}
const cats = [...new Set(entries.map(e => e[1]))].sort();
if (cats.length > 255) throw new Error('příliš mnoho kategorií');
const catIndex = new Map(cats.map((c, i) => [c, i]));
entries.sort((a, b) => (a[0] < b[0] ? -1 : a[0] > b[0] ? 1 : 0));
// Kolize hashů (prakticky nemožné) — vynech obě.
const dedup = entries.filter((e, i) => (i === 0 || e[0] !== entries[i - 1][0]) && (i === entries.length - 1 || e[0] !== entries[i + 1][0]));

const header = Buffer.alloc(4 + 1 + 4 + 1);
header.write('HPOI', 0, 'ascii'); header.writeUInt8(1, 4); header.writeUInt32BE(dedup.length, 5); header.writeUInt8(cats.length, 9);
const catBufs = cats.map(c => { const b = Buffer.from(c, 'utf8'); return Buffer.concat([Buffer.from([b.length]), b]); });
const hashes = Buffer.alloc(dedup.length * 8);
const catBytes = Buffer.alloc(dedup.length);
dedup.forEach(([h, c], i) => { hashes.writeBigInt64BE(h, i * 8); catBytes[i] = catIndex.get(c); });
fs.mkdirSync(path.dirname(OUT), { recursive: true });
fs.writeFileSync(OUT, Buffer.concat([header, ...catBufs, hashes, catBytes]));

const perCat = {};
for (const [, c] of dedup) perCat[c] = (perCat[c] ?? 0) + 1;
console.error(`${seen} pojmenovaných míst v CZ/SK/PL, ${kept} kandidátů, ${votes.size} unikátních názvů, ${conflicts} sporných vynecháno`);
console.error(`→ ${dedup.length} položek, ${(fs.statSync(OUT).size / 1024 / 1024).toFixed(2)} MB: ${OUT}`);
console.error(Object.entries(perCat).sort((a, b) => b[1] - a[1]).map(([c, n]) => `  ${c}: ${n}`).join('\n'));
console.error('podle země:', byCountry);
