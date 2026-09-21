#!/usr/bin/env node
/**
 * Generátor lexikonu značek pro automatickou kategorizaci.
 *
 * Zdroj: Name Suggestion Index (OpenStreetMap, licence BSD-3) — kanonický seznam značek
 * obchodů, restaurací, čerpacích stanic atd. s typem provozovny a zeměmi působnosti.
 *   https://github.com/osmlab/name-suggestion-index
 *
 * Výstup: app/src/main/java/cz/heller/core/categorize/BrandLexicon.kt (generovaný, needitovat).
 *
 * Použití (z kořene repa):
 *   node tools/lexicon/generate-brands.mjs                 # stáhne aktuální NSI z CDN
 *   node tools/lexicon/generate-brands.mjs cesta/nsi.json  # použije lokální soubor
 */
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const NSI_URL = 'https://cdn.jsdelivr.net/npm/name-suggestion-index@latest/dist/nsi.min.json';
const OUT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../app/src/main/java/cz/heller/core/categorize/BrandLexicon.kt');

// Země / regiony, jejichž značky bereme. "001" = celý svět, "150" = Evropa, "eu" = EU.
const LOCATIONS = new Set(['001', '150', '151', '155', 'eu', 'cz', 'sk', 'pl', 'de', 'at', 'hu', 'hr', 'si', 'me']);

// Mapování NSI kategorie (klíč v `nsi`) → slug kategorie Heller. Pořadí = priorita při konfliktu
// (stejná značka ve více kategoriích, např. Shell = palivo i večerka → vyhraje palivo).
const MAPPING = [
  ['brands/shop/supermarket', 'food_groceries'],
  ['brands/shop/wholesale', 'food_groceries'],
  ['brands/shop/furniture', 'housing_home'],
  ['brands/shop/doityourself', 'housing_home'],
  ['brands/amenity/fuel', 'transport_fuel'],
  ['brands/amenity/charging_station', 'transport_fuel'],
  ['brands/shop/greengrocer', 'food_groceries'],
  ['brands/shop/butcher', 'food_groceries'],
  ['brands/shop/deli', 'food_groceries'],
  ['brands/shop/frozen_food', 'food_groceries'],
  ['brands/shop/health_food', 'food_groceries'],
  ['brands/shop/seafood', 'food_groceries'],
  ['brands/shop/cheese', 'food_groceries'],
  ['brands/shop/beverages', 'food_groceries'],
  ['brands/shop/alcohol', 'food_groceries'],
  ['brands/shop/wine', 'food_groceries'],
  ['brands/shop/general', 'food_groceries'],
  ['brands/shop/convenience', 'food_groceries'],
  ['brands/amenity/fast_food', 'food_dining'],
  ['brands/amenity/restaurant', 'food_dining'],
  ['brands/amenity/cafe', 'food_dining'],
  ['brands/amenity/bar', 'food_dining'],
  ['brands/amenity/pub', 'food_dining'],
  ['brands/amenity/ice_cream', 'food_dining'],
  ['brands/shop/bakery', 'food_dining'],
  ['brands/shop/pastry', 'food_dining'],
  ['brands/shop/confectionery', 'food_dining'],
  ['brands/shop/coffee', 'food_dining'],
  ['brands/shop/tea', 'food_dining'],
  ['brands/shop/tobacco', 'food_dining'],
  ['brands/shop/e-cigarette', 'food_dining'],
  ['brands/amenity/parking', 'transport_car_other'],
  ['brands/shop/car_repair', 'transport_car_other'],
  ['brands/shop/tyres', 'transport_car_other'],
  ['brands/shop/car_parts', 'transport_car_other'],
  ['brands/shop/motorcycle', 'transport_car_other'],
  ['brands/amenity/car_wash', 'transport_car_other'],
  ['brands/amenity/vehicle_inspection', 'transport_car_other'],
  ['brands/amenity/car_rental', 'transport_car_other'],
  ['brands/amenity/car_sharing', 'transport_car_other'],
  ['brands/amenity/pharmacy', 'health_care'],
  ['brands/amenity/dentist', 'health_care'],
  ['brands/amenity/clinic', 'health_care'],
  ['brands/amenity/doctors', 'health_care'],
  ['brands/amenity/hospital', 'health_care'],
  ['brands/healthcare/laboratory', 'health_care'],
  ['brands/shop/optician', 'health_care'],
  ['brands/shop/medical_supply', 'health_care'],
  ['brands/shop/hearing_aids', 'health_care'],
  ['brands/shop/chemist', 'shopping_drugstore'],
  ['brands/shop/cosmetics', 'shopping_drugstore'],
  ['brands/shop/perfumery', 'shopping_drugstore'],
  ['brands/shop/nutrition_supplements', 'shopping_drugstore'],
  ['brands/shop/beauty', 'health_sport'],
  ['brands/shop/hairdresser', 'health_sport'],
  ['brands/shop/massage', 'health_sport'],
  ['brands/shop/tattoo', 'health_sport'],
  ['brands/shop/clothes', 'shopping_clothes'],
  ['brands/shop/shoes', 'shopping_clothes'],
  ['brands/shop/fashion_accessories', 'shopping_clothes'],
  ['brands/shop/bag', 'shopping_clothes'],
  ['brands/shop/second_hand', 'shopping_clothes'],
  ['brands/shop/jewelry', 'shopping_other'],
  ['brands/shop/watches', 'shopping_other'],
  ['brands/shop/electronics', 'shopping_electronics'],
  ['brands/shop/computer', 'shopping_electronics'],
  ['brands/shop/hifi', 'shopping_electronics'],
  ['brands/shop/video_games', 'shopping_electronics'],
  ['brands/shop/camera', 'shopping_electronics'],
  ['brands/shop/appliance', 'shopping_electronics'],
  ['brands/shop/mobile_phone', 'comm_phone_internet'],
  ['brands/shop/hardware', 'housing_home'],
  ['brands/shop/garden_centre', 'housing_home'],
  ['brands/shop/agrarian', 'housing_home'],
  ['brands/shop/houseware', 'housing_home'],
  ['brands/shop/interior_decoration', 'housing_home'],
  ['brands/shop/kitchen', 'housing_home'],
  ['brands/shop/bed', 'housing_home'],
  ['brands/shop/lighting', 'housing_home'],
  ['brands/shop/florist', 'housing_home'],
  ['brands/shop/bathroom_furnishing', 'housing_home'],
  ['brands/shop/curtain', 'housing_home'],
  ['brands/shop/paint', 'housing_home'],
  ['brands/shop/flooring', 'housing_home'],
  ['brands/shop/trade', 'housing_home'],
  ['brands/shop/toys', 'shopping_other'],
  ['brands/shop/baby_goods', 'shopping_other'],
  ['brands/amenity/kindergarten', 'shopping_other'],
  ['brands/amenity/childcare', 'shopping_other'],
  ['brands/shop/pet', 'shopping_other'],
  ['brands/amenity/veterinary', 'shopping_other'],
  ['brands/shop/books', 'leisure_culture'],
  ['brands/shop/newsagent', 'leisure_culture'],
  ['brands/shop/music', 'leisure_culture'],
  ['brands/amenity/library', 'leisure_culture'],
  ['brands/shop/stationery', 'shopping_other'],
  ['brands/shop/copyshop', 'shopping_other'],
  ['brands/shop/gift', 'charity'],
  ['brands/shop/party', 'charity'],
  ['brands/shop/variety_store', 'shopping_other'],
  ['brands/shop/department_store', 'shopping_other'],
  ['brands/shop/mall', 'shopping_other'],
  ['brands/shop/kiosk', 'shopping_other'],
  ['brands/shop/lottery', 'shopping_other'],
  ['brands/shop/bookmaker', 'shopping_other'],
  ['brands/amenity/casino', 'shopping_other'],
  ['brands/shop/erotic', 'shopping_other'],
  ['brands/shop/craft', 'leisure_culture'],
  ['brands/shop/games', 'leisure_culture'],
  ['brands/shop/art', 'leisure_culture'],
  ['brands/shop/frame', 'leisure_culture'],
  ['brands/shop/musical_instrument', 'leisure_culture'],
  ['brands/shop/fishing', 'leisure_culture'],
  ['brands/shop/photo', 'leisure_culture'],
  ['brands/shop/sports', 'health_sport'],
  ['brands/shop/outdoor', 'health_sport'],
  ['brands/shop/bicycle', 'health_sport'],
  ['brands/amenity/bicycle_rental', 'health_sport'],
  ['brands/leisure/fitness_centre', 'health_sport'],
  ['brands/leisure/sports_centre', 'health_sport'],
  ['brands/amenity/cinema', 'leisure_culture'],
  ['brands/tourism/theme_park', 'leisure_culture'],
  ['brands/leisure/bowling_alley', 'leisure_culture'],
  ['brands/leisure/amusement_arcade', 'leisure_culture'],
  ['brands/leisure/escape_game', 'leisure_culture'],
  ['brands/shop/ticket', 'leisure_culture'],
  ['brands/tourism/hotel', 'leisure_holidays'],
  ['brands/tourism/motel', 'leisure_holidays'],
  ['brands/tourism/hostel', 'leisure_holidays'],
  ['brands/shop/travel_agency', 'leisure_holidays'],
  ['brands/amenity/post_office', 'shopping_other'],
  ['brands/amenity/parcel_locker', 'shopping_other'],
  ['brands/amenity/post_depot', 'shopping_other'],
  ['brands/office/insurance', 'financial_insurance'],
  ['brands/amenity/driving_school', 'leisure_education'],
  ['brands/amenity/language_school', 'leisure_education'],
  ['brands/amenity/music_school', 'leisure_education'],
  ['brands/amenity/college', 'leisure_education'],
  ['brands/amenity/university', 'leisure_education'],
  ['brands/shop/dry_cleaning', 'housing_utilities'],
  ['brands/shop/laundry', 'housing_utilities'],
  ['brands/shop/tailor', 'housing_utilities'],
  ['brands/shop/locksmith', 'housing_utilities'],
  ['brands/shop/storage_rental', 'housing_utilities'],
  ['brands/shop/charity', 'charity'],
  ['brands/shop/funeral_directors', 'others'],
  ['brands/office/coworking', 'work_tools'],
];

// Ruční opravy pro značky, které NSI vede ve více kategoriích nebo je mapování zavádějící.
const OVERRIDES = new Map([
  ['ikea', 'housing_home'],
  ['globus', 'food_groceries'],
  ['tesco', 'food_groceries'],
  ['geco', 'food_dining'],
  ['relay', 'food_dining'],
]);

// Značky, jejichž jméno je zároveň běžné slovo — jako samostatný token by matchovaly poznámky.
const BLACKLIST = new Set([
  'action', 'real', 'total', 'point', 'market', 'express', 'city', 'smart', 'family', 'premium',
  'super', 'mini', 'plus', 'home', 'style', 'auto', 'fresh', 'coop', 'gold', 'life', 'next', 'new',
  'prima', 'first', 'best', 'top', 'star', 'sun', 'euro', 'euro shop', 'select', 'pro', 'one',
  'simply', 'today', 'fine', 'nice', 'good', 'free', 'more', 'global', 'local', 'central', 'centrum',
  'sport', 'sports', 'fitness', 'pizza', 'burger', 'kebab', 'sushi', 'bistro', 'cafe', 'coffee', 'bar',
  'pub', 'hotel', 'hostel', 'parking', 'taxi', 'apteka', 'lekarna', 'drogerie', 'bakery', 'grill',
  'shell select', 'orlen', 'mol', 'cez', 'spar', 'ok', 'go', 'me', 'my', 'the', 'and', 'la', 'le', 'el',
  'na', 'do', 'od', 'za', 'ze', 'se', 'pod', 'nad', 'dne', 'castka', 'platba', 'nakup', 'kartou',
  'praha', 'brno', 'ostrava', 'warszawa', 'krakow', 'wien', 'berlin', 'pizzeria', 'pizzerie',
  'restaurant', 'restaurace', 'restauracja', 'kavarna', 'cukrarna', 'pekarna', 'potraviny',
  'obchod', 'sklep', 'shop', 'store', 'stores', 'market', 'supermarket', 'hypermarket', 'diskont',
  'bank', 'banka', 'post', 'posta', 'poczta', 'tabak', 'trafika', 'kiosk', 'benzina', 'benzinka',
  'motel', 'penzion', 'pension', 'camp', 'kemp', 'apartments', 'apartman', 'club', 'klub', 'gym',
  'studio', 'salon', 'school', 'skola', 'szkola', 'mall', 'galerie', 'galeria', 'outlet',
  // Značky, které jsou zároveň běžná slova nebo jména (jako samostatný token příliš riskantní).
  'orange', 'adam', 'alex', 'change', 'closed', 'delta', 'depot', 'diesel', 'expert', 'gate',
  'giant', 'hammer', 'house', 'idea', 'jumbo', 'kind', 'klara', 'legend', 'lime', 'logo', 'luna',
  'mana', 'mania', 'match', 'mobile', 'mobil', 'nova', 'nowa', 'office', 'okay', 'omega', 'only',
  'orion', 'pink', 'play', 'privat', 'profi', 'sofia', 'sprint', 'tempo', 'tiger', 'tribe', 'union',
  'vita', 'viva', 'wash', 'zero', 'smoke', 'roller', 'penta', 'natura', 'lotto', 'petrol', 'paul',
  'lotus', 'stella', 'swiss', 'sonic', 'carry', 'combi', 'outfit', 'bench', 'replay', 'roots',
  'salsa', 'zurich', 'apart', 'avanti', 'basic', 'budget', 'classic', 'comfort', 'direct', 'easy',
  'energy', 'extra', 'fashion', 'future', 'garden', 'green', 'happy', 'harmony', 'hobby', 'kids',
  'king', 'living', 'lucky', 'magic', 'maxi', 'media', 'mega', 'modern', 'nature', 'north', 'ocean',
  'optima', 'optik', 'paradise', 'park', 'partner', 'people', 'planet', 'power', 'prime', 'quick',
  'rapid', 'relax', 'royal', 'safe', 'service', 'silver', 'simple', 'south', 'space', 'spaces',
  'special', 'spot', 'standard', 'sunny', 'system', 'team', 'tech', 'time', 'trend', 'unique',
  'united', 'urban', 'value', 'vision', 'west', 'east', 'white', 'black', 'blue', 'red', 'world',
]);

// Krátké klíče (< 4 znaky), které přesto chceme.
const SHORT_OK = new Set(['kfc', 'omv', 'obi', 'dm', 'cba', 'lpp', 'h m', 'c a', 'jysk', 'rwe', 'mcd']);

function normalize(s) {
  return String(s)
    .normalize('NFD').replace(/\p{M}+/gu, '')
    .replace(/[’'`´]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, ' ')
    .trim().replace(/\s+/g, ' ');
}

function acceptable(key) {
  if (!key) return false;
  if (BLACKLIST.has(key)) return false;
  const tokens = key.split(' ');
  if (tokens.length > 4) return false;
  if (/^[0-9 ]+$/.test(key)) return false;
  const letters = key.replace(/[^a-z]/g, '').length;
  if (letters < 3) return false;
  if (tokens.length === 1 && key.length < 4 && !SHORT_OK.has(key)) return false;
  if (!tokens.some(t => t.length >= 3) && !SHORT_OK.has(key)) return false;
  return true;
}

async function loadNsi(arg) {
  if (arg) return JSON.parse(fs.readFileSync(arg, 'utf8'));
  console.error(`Stahuji ${NSI_URL} …`);
  const res = await fetch(NSI_URL);
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return await res.json();
}

const data = await loadNsi(process.argv[2]);
const version = data._meta?.version ?? 'unknown';
const prio = new Map(MAPPING.map(([k], i) => [k, i]));
const catOf = new Map(MAPPING);

/** key → { category, prio, name } */
const out = new Map();
let seen = 0;
for (const [nsiKey, cat] of MAPPING) {
  const bucket = data.nsi[nsiKey];
  if (!bucket) { console.error(`  (chybí ${nsiKey})`); continue; }
  for (const item of bucket.items ?? []) {
    const inc = item.locationSet?.include ?? [];
    if (!inc.some(l => LOCATIONS.has(l))) continue;
    seen++;
    const names = new Set();
    const t = item.tags ?? {};
    for (const n of [t.brand, t.name, t['brand:en'], t['name:en'], t['brand:cs'], t['name:cs'], t['brand:pl'], t['name:pl'], t['brand:de'], t['name:de'], item.displayName]) {
      if (n) names.add(n);
    }
    for (const n of item.matchNames ?? []) names.add(n);
    for (const raw of names) {
      const key = normalize(raw);
      if (!acceptable(key)) continue;
      const p = prio.get(nsiKey);
      const prev = out.get(key);
      if (!prev || p < prev.prio) out.set(key, { category: cat, prio: p, name: raw });
    }
  }
}

for (const [key, cat] of OVERRIDES) {
  if (out.has(key)) out.set(key, { ...out.get(key), category: cat });
}

const entries = [...out.entries()].sort((a, b) => a[0].localeCompare(b[0]));
console.error(`NSI ${version}: ${seen} provozoven ve vybraných zemích → ${entries.length} klíčů`);
const byCat = {};
for (const [, v] of entries) byCat[v.category] = (byCat[v.category] ?? 0) + 1;
console.error(Object.entries(byCat).sort((a, b) => b[1] - a[1]).map(([c, n]) => `  ${c}: ${n}`).join('\n'));

// Kotlin: JVM omezuje string konstantu na 65 535 B, proto po blocích.
const lines = entries.map(([k, v]) => `${k}\t${v.category}`);
const chunks = [];
let cur = [];
let size = 0;
for (const l of lines) {
  if (size + l.length + 1 > 60000) { chunks.push(cur); cur = []; size = 0; }
  cur.push(l); size += l.length + 1;
}
if (cur.length) chunks.push(cur);

const kt = `package cz.heller.core.categorize

/**
 * GENEROVANÝ SOUBOR — needitovat ručně. Vytváří \`tools/lexicon/generate-brands.mjs\`.
 *
 * Značky obchodů, restaurací, čerpacích stanic atd. z Name Suggestion Index (OpenStreetMap,
 * licence BSD-3, verze ${version}), omezené na svět/Evropu + CZ, SK, PL, DE, AT, HU, HR, SI, ME
 * a namapované na kategorie Heller. Klíče jsou normalizované tokenové fráze (viz [MerchantText]),
 * porovnávají se po celých tokenech (ne podřetězcově). ${entries.length} klíčů.
 */
object BrandLexicon {

    /** Řádek = "klíč<TAB>categoryId". */
    val rules: List<Pair<String, String>> by lazy {
        CHUNKS.flatMap { chunk ->
            chunk.lineSequence()
                .filter { it.isNotBlank() }
                .map { line ->
                    val tab = line.indexOf('\\t')
                    line.substring(0, tab) to line.substring(tab + 1)
                }
                .toList()
        }
    }

    private val CHUNKS: List<String> = listOf(
${chunks.map((c, i) => `        CHUNK_${i},`).join('\n')}
    )
${chunks.map((c, i) => `
    private const val CHUNK_${i} = """
${c.join('\n')}
"""`).join('\n')}
}
`;
fs.writeFileSync(OUT, kt);
console.error(`Zapsáno ${OUT} (${chunks.length} bloků)`);
