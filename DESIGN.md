---
name: Emerald & Parchment Showcase
colors:
  surface: '#f3fcf4'
  surface-dim: '#d3dcd5'
  surface-bright: '#f3fcf4'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#edf6ee'
  surface-container: '#e7f0e9'
  surface-container-high: '#e1eae3'
  surface-container-highest: '#dce5dd'
  on-surface: '#151d19'
  on-surface-variant: '#404944'
  inverse-surface: '#2a322d'
  inverse-on-surface: '#eaf3ec'
  outline: '#707974'
  outline-variant: '#bfc9c3'
  surface-tint: '#2b6954'
  primary: '#003527'
  on-primary: '#ffffff'
  primary-container: '#064e3b'
  on-primary-container: '#80bea6'
  inverse-primary: '#95d3ba'
  secondary: '#695d46'
  on-secondary: '#ffffff'
  secondary-container: '#f1e1c3'
  on-secondary-container: '#6f634b'
  tertiary: '#003529'
  on-tertiary: '#ffffff'
  tertiary-container: '#164d3e'
  on-tertiary-container: '#87bda9'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#b0f0d6'
  primary-fixed-dim: '#95d3ba'
  on-primary-fixed: '#002117'
  on-primary-fixed-variant: '#0b513d'
  secondary-fixed: '#f1e1c3'
  secondary-fixed-dim: '#d4c5a8'
  on-secondary-fixed: '#231a08'
  on-secondary-fixed-variant: '#504530'
  tertiary-fixed: '#b7eed9'
  tertiary-fixed-dim: '#9cd2be'
  on-tertiary-fixed: '#002118'
  on-tertiary-fixed-variant: '#194f40'
  background: '#f3fcf4'
  on-background: '#151d19'
  surface-variant: '#dce5dd'
typography:
  display-lg:
    fontFamily: Roboto Flex
    fontSize: 57px
    fontWeight: '400'
    lineHeight: 64px
    letterSpacing: -0.25px
  headline-lg:
    fontFamily: Roboto Flex
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 40px
    letterSpacing: 0px
  headline-lg-mobile:
    fontFamily: Roboto Flex
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 34px
    letterSpacing: 0px
  headline-md:
    fontFamily: Roboto Flex
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 36px
    letterSpacing: 0px
  headline-sm:
    fontFamily: Roboto Flex
    fontSize: 24px
    fontWeight: '500'
    lineHeight: 32px
    letterSpacing: 0px
  title-lg:
    fontFamily: Roboto Flex
    fontSize: 22px
    fontWeight: '500'
    lineHeight: 28px
    letterSpacing: 0px
  title-md:
    fontFamily: Roboto Flex
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 24px
    letterSpacing: 0.15px
  title-sm:
    fontFamily: Roboto Flex
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.1px
  body-lg:
    fontFamily: Roboto Flex
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
    letterSpacing: 0.5px
  body-md:
    fontFamily: Roboto Flex
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: 0.25px
  body-sm:
    fontFamily: Roboto Flex
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
    letterSpacing: 0.4px
  label-lg:
    fontFamily: Roboto Flex
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.1px
  label-md:
    fontFamily: Roboto Flex
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.5px
  label-sm:
    fontFamily: Roboto Flex
    fontSize: 11px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.5px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  gutter-tablet: 1.5rem
  gutter-desktop: 1.5rem
  margin: 1rem
  margin-tablet: 1.5rem
  margin-desktop: 2rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style
The design system embodies a modern, tactile evolution of Material 3 Expressive, created specifically for developer-facing component catalogs, design token inspectors, and interactive Flutter/Kotlin widget documentation.

The visual style merges structured technical precision with warm editorial organicism:
- **Style Archetype:** Modern Material 3 Expressive, combining tonal color mapping, variable-density surfaces, and organic curves.
- **Emotional Response:** Tactile, deliberate, developer-friendly, and polished. It balances deep forest earth tones against warm parchment highlights, avoiding clinical sterile grays.
- **Key Characteristics:** 
  - Dynamic surface role mapping (`surface`, `surface-container-low`, `surface-container`, `surface-container-high`).
  - High-affordance tactile curves ranging from 28px squircles down to fully circular pills.
  - Interactive state overlays using dual tonal tinting and subtle ripple-elevation shifts.

## Colors
The palette leverages a signature botanical contrast: deep, authoritative emerald anchored against soft, low-luminosity parchment cream. Surface mapping adheres to dynamic luminance steps rather than harsh opacity blending:

- **Primary (`#064E3B`):** Forest emerald for key interaction anchors, selected states, elevated primary FABs, and filled CTA buttons. `on-primary` is pure `#FFFFFF`.
- **Secondary / Container Tint (`#F8E7C9`):** Warm parchment cream serving as tonal accenting, container backgrounds for interactive chips, search bars, and active segmented button pills. `on-secondary-container` is `#2B1700`.
- **Tertiary (`#2E6252`):** Medium spruce teal, employed for code syntax token highlights, badge markers, and secondary action highlights.
- **Surface Foundations:**
  - Base Surface: `#FBFDF9` (clean parchment canvas with a light warm undertone).
  - Surface Container Low: `#F5F8F3` (grouped catalog item panels).
  - Surface Container: `#F0F4EE` (default card backgrounds and segmented rail surfaces).
  - Surface Container High: `#E6ECE4` (modal bottom sheets, expandable accordion headers, floating pickers).
  - Surface Container Highest: `#DCE4DA` (hover and pressed baseline layers).
- **Outlines & Strokes:**
  - Outline: `#707973` (subtle component borders, dividers, unselected outlines at 1px).
  - Outline Variant: `#C0C9C1` (hairline splitters for tree-views and nested widget hierarchies).

## Typography
The system uses `Roboto Flex`, capturing the canonical Android & Flutter platform presence while offering modern variable axes (weight, optical size, and width).

- **Headlines & Titles:** Set with tighter letter spacing and balanced medium-to-semibold weights (`500` to `600`) to retain structural readability within dynamic multi-column property sheets and mobile inspection viewports.
- **Body:** Open counters with relaxed spacing (`0.25px` to `0.5px`) maintain high legibility when describing technical API signatures, parameter tables, and implementation snippets.
- **Labels & Badges:** Distinct, bold uppercase or medium-track titling (`label-sm` and `label-md`) delivers immediate visual scanning across nested widget trees and component tags.

## Layout & Spacing
Built on a strict 8pt base grid with an inner 4pt subgrid for precise micro-alignments (chips, icon-to-text gaps, inline badges).

- **Mobile (<600dp):** Single-column flow with a fixed 16px (`1rem`) outer canvas margin and 16px vertical component stack spacing. Floating preview docks stick to bottom safe areas.
- **Tablet / Foldables (600dp–840dp):** Dual-pane master-detail arrangement. Left pane hosts a 280dp wide widget catalog index; right pane displays the active interactive showcase canvas. Gutter expands to 24px (`1.5rem`).
- **Desktop / Large Displays (>840dp):** Three-tier layout consisting of a persistent 80dp compact navigation rail, a flexible 320dp expandable catalog hierarchy, and a broad fluid preview canvas (12-column system, 24px gutters, 32px outer margin).

## Elevation & Depth
Elevation is expressed primarily through **Tonal Layering**, supplemented by subtle ambient shadows on interactive floaters. Higher structural layers receive brighter tint-derived surface values rather than solely dark shadows.

- **Level 0 (Flat Canvas):** `#FBFDF9` — Base background canvas. Zero shadow.
- **Level 1 (Card & Group Container):** `#F0F4EE` — Outlined or filled resting cards. No shadow; separation achieved via surface tone or a 1px `#C0C9C1` hairline border.
- **Level 2 (Scrolled AppBars & Interactive Containers):** `#E6ECE4` — Shadow: `0px 2px 6px rgba(6, 78, 59, 0.04)`.
- **Level 3 (Dropdown Menus & Dialog Panels):** `#E6ECE4` — Shadow: `0px 4px 14px rgba(6, 78, 59, 0.08)`.
- **Level 4 (Floating Action Buttons & Bottom Sheets):** `#F8E7C9` or `#064E3B` — Shadow: `0px 8px 24px rgba(6, 78, 59, 0.12)`.
- **Interactive State Dynamic Uplift:** On press or drag, elevate elements by exactly one tonal tier (e.g., Level 1 resting elevates to Level 2 on drag).

## Shapes
The shape philosophy adopts Material 3 Expressive geometry, utilizing distinct radius scales based on element scale and hierarchy:

- **Extra-Large Squircles (28px / 1.75rem):** Reserved for showcase container cards, bottom sheet dialogs, and large preview canvas wraps.
- **Medium Curved Elements (16px / 1rem):** Standard interactive showcase cards, modal dialogs, and code snippet containers.
- **Small Curved Elements (12px / 0.75rem):** Text fields, nested accordion children, inline code snippets, and button groups.
- **Full Pill (`9999px`):** Segmented button capsules, filter chips, action pills, badges, and Floating Action Buttons.

## Components

### Buttons & Floating Action Buttons (FABs)
- **Filled Button:** Pill-shaped (`9999px` radius), `#064E3B` background, pure `#FFFFFF` label (`label-lg`), height: 48px, horizontal padding: 24px. Active state shifts surface tone to `#2E6252` with an ambient 2px elevation lift.
- **Tonal Button:** Warm cream background `#F8E7C9`, deep emerald `#064E3B` text, pill shape, zero shadow.
- **Outlined Button:** Transparent background, 1px `#707973` stroke, `#064E3B` text. Pressed overlay adds 10% opacity `#064E3B` tonal ripple.
- **Floating Action Button (FAB):** Material 3 expressive rounded squircle (20px radius for standard 56px FAB, 28px radius for extended 96px showcase FAB). Background `#F8E7C9` with `#064E3B` iconography.

### Chips & Segmented Buttons
- **Filter & Assist Chips:** 32px height, fully pill-rounded. Resting state features 1px `#C0C9C1` stroke over `#FBFDF9`. Selected state swaps background to `#064E3B`, border vanishes, text and leading checkmark transition to `#FFFFFF`.
- **Segmented Buttons:** Unified 40px height container with `#E6ECE4` fill and 20px outer radius. Active segment animates via a smooth sliding pill (`#F8E7C9`) with `#064E3B` text, while unselected segments remain transparent with `#707973` text.

### Cards & Component Showcase Containers
- **Elevated Showcase Card:** 28px rounded squircle, `#F0F4EE` surface, zero outer border. Inner padding: 24px. Features an internal top preview area sitting on `#FBFDF9` with a 16px inner corner radius, creating a nested framed appearance.
- **Interactive Variant Card:** 16px radius, 1px `#C0C9C1` border. On hover/focus, border shifts to `#064E3B` at 1.5px thickness.

### Input Fields & Controls
- **Filled Text Field:** `#F0F4EE` container, 12px top-corner radius with a 2px active indicator line along the base in `#064E3B`. Placeholder in `#707973`, input text in `#1A1C19`.
- **Outlined Text Field:** 12px all-around squircle, 1px `#707973` border, advancing to 2px `#064E3B` on focus with floating label transition.
- **Checkboxes & Radios:** Selected state filled with `#064E3B` and white indicator marks. Checkbox corners rounded to 4px; radio indicators transition from a centered 8px filled emerald disc.

### Nested Expandable Accordions
- **Accordion Header:** 16px radius on `#E6ECE4`, height: 56px, horizontal padding: 20px. Trailing icon rotates 180 degrees smoothly on expansion.
- **Expanded Body Panel:** Tonal step-down to `#F5F8F3`, connected to header via a 1px `#C0C9C1` interior separator. Nested children receive 12px outer radius and 16px left indentation to visually represent the Flutter/Compose tree hierarchy.

### Badges & Status Indicators
- **Component Platform Badges (Flutter / Kotlin):** 20px height capsule pills, `#F8E7C9` background, `#064E3B` uppercase text (`label-sm`).
- **State Badges (Experimental / Stable):** Stable uses `#064E3B` tint; Experimental uses a muted amber tint (`#E5A000` text on `#FFF8E1`), styled in 8px padding pills.
