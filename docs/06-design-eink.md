# 06 — Visual style: monochrome E-Ink

Heller is designed for a **black-and-white E-Ink display**, in the style of Mudita phones. Both
the look and the interaction follow from the limits of electronic paper and from a "calm"
philosophy: quiet, simple, high contrast, nothing distracting.

## Target device
- **Mudita Kompakt** is an E-Ink Android phone, and Heller installs on it as a regular Android
  app. The Mudita Pure runs MuditaOS, not Android, so it isn't a target.
- The visual system is device-independent. It suits any E-Ink panel (Kompakt, Boox, Hisense…)
  and also works on a regular Android 8+ phone.

## 1. E-Ink constraints → design rules

| Display constraint | Design consequence |
|---|---|
| **Black and white only** (a few grays at most) | No colors. Meaning comes from shape, weight, position and icons |
| **Slow refresh, ghosting** | No animations, transitions, ripple or "live" elements |
| **Reflective, no backlight** | Maximum contrast and large, bold type |
| **Partial refresh leaves traces** | Screens switch instantly and redraw fully |
| **Low refresh rate** | Static charts, no pull-to-refresh, no spinners |

**Three core principles:**
1. **Contrast over color:** everything reads as pure black on white.
2. **Calm over effect:** no motion, few elements, lots of white space.
3. **Shape over shade:** distinguish things with icons, borders, weight and position, not gray.

## 2. Tone tokens (`core/designsystem/theme/Color.kt`)

Primarily **1-bit** (pure black and white). Grays are supplementary only.

| Token | Value | Use |
|---|---|---|
| `Ink` | `#000000` | Text, icons, borders, fills, charts |
| `Paper` | `#FFFFFF` | Background |
| `Gray700` | `#3A3A3A` | Strong secondary text |
| `Gray500` | `#7A7A7A` | Secondary text, inactive bottom-bar items |
| `Gray300` | `#BFBFBF` | Subtle dividers |
| `Gray100` | `#E6E6E6` | Chart and bar fills |

- **No semantic colors** (green, red, orange) anywhere.
- Selected and active states use **inversion** (black background, white text) or an
  **underline**, never a colored highlight.

## 3. Typography (`Type.kt`)
- The **system sans-serif** font, which the device tunes for paper.
- Meaning comes from **weight contrast** (Regular vs. Bold), not color.
- The type scale is larger than on LCD phones:

| Style | Size / line height / weight | Use |
|---|---|---|
| `displayLarge` | 36 / 42 / Bold | Main amounts, net worth, onboarding title |
| `titleLarge` | 24 / 30 / Bold | Screen titles |
| `titleMedium` | 18 / 24 / Bold | Sections, buttons |
| `bodyLarge` | 17 / 24 / Regular | Regular text, list items |
| `bodyMedium` | 15 / 22 / Regular | Secondary text, hints |
| `labelLarge` | 15 / 20 / Medium | Field labels |
| `labelMedium` | 13 / 18 / Regular | Bottom-bar labels, captions |

## 4. Layout and components
- **Flat and bordered.** Solid black 1–2 px lines take the place of shadows and elevation.
  There is no `tonalElevation` or shadow.
- **Cards** (`CalmCard`) are bordered rectangles with no shadow or colored fill, and sharp or
  slightly rounded corners (≤ 4 px).
- **Dividers** are thin black or gray lines, and list rows are always clearly separated.
- **Buttons** (`CalmButtons`): the primary button is a solid black fill with white text, and the
  secondary button is outlined with black text on white. Buttons are 52 dp tall.
- **Chips** (`CalmChip`) switch type, period and currency. The selected chip is inverted.
- **Form fields** are outlined, with a clear label.
- **Dialogs** (`CalmDialog`, `CalmConfirmSheet`) are plain bordered surfaces with text buttons.
- **Top bar** (`CalmTopBar`) is a title with a bottom line instead of a shadow.
- **Other components:** `MoneyAmount` (a signed amount), `EmptyState`, `SectionHeader`,
  `BarMeter` (budget and share bars) and `TrendChart` (a line chart).
- **Ripple is disabled** globally.

## 5. Icons
- **Monochrome Material icons** in one visual weight.
- Categories are told apart by **icon + name**, not color. Each preset category has its own
  pictogram, from a set of 72 keys (`CategoryIcons`).
- Glyphs replace status colors: `⚠` for overspent and the typographic `+` / `−` for direction.

## 6. Meaning without color

| Where Wallet uses color | Heller on E-Ink |
|---|---|
| Income green / expense red | **Sign and weight:** `+ 1 250,00 Kč` (bold) vs. `− 350,00 Kč` |
| Colored account cards | Name, type and a border |
| Colored categories | Icon and name |
| Budget status green/orange/red | **Bar fill** plus "Remaining X" / "⚠ Over by X" |
| Colored pie segments | A **horizontal ranking** with bars, amounts and % |

## 7. Charts
Charts are **static** (drawn once, never animated) and purely black-and-white. They are custom
Compose drawing, with no chart library.

- **Trends** (balance, spending, income) use `TrendChart`: one black line over a light gray
  fill, with the period total and the % change against the previous period next to it.
- **Expense structure** is a **horizontal ranking** of categories, sorted descending, with name,
  amount, % and a bar sized to the share. It replaces a colored donut.
- **Cash flow** has two bars per month: income on top, expenses below. Values are labeled
  directly.
- There are no gradients, shadows or transparency, and labels sit next to the data instead of in
  a color legend.

## 8. Motion and refresh
- **No animations or transitions.** The `NavHost` uses `EnterTransition.None` and
  `ExitTransition.None`, and screens switch instantly.
- **No spinners, skeletons or pull-to-refresh.** Loading shows plain text, for example
  "Loading…" or "Syncing…".
- **Fio sync is automatic** (on app open and daily in the background), so there is no refresh
  gesture.

## 9. Interaction
- **Touch** with large targets, allowing for the lower precision and latency of E-Ink.
- **No swipe gestures** (swipe-to-delete needs fast redraw). Actions are explicit buttons in
  detail screens: Edit, Delete, Pay now.
- Confirmations and states are shown with text and inversion, not color.
- Choosing a category opens a **separate picker screen**, not a crowded grid inside the form.

## 10. Theme
- **One theme only: white paper, black ink.** It is the natural reflective state of E-Ink and
  produces the least ghosting. There is no theme switching, no dark mode and no whole-app
  inversion.
- **Material You / dynamic color is not used.** A custom `ColorScheme` overrides Material 3 to
  black and white.

## 11. Brand
- **App name:** Heller.
- **Launcher icon:** adaptive and monochrome, with the "Horizon" mark (a sun/coin above the
  horizon) and a monochrome layer for themed icons.
- **Logo files** in `assets/logo/`: `heller-mark.svg`, `heller-mark-inverted.svg`,
  `heller-lockup.svg`.
