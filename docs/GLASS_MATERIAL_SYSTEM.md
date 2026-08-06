# Vantafyn Glass Material System

## Surface Tiers

Vantafyn uses a dark luxury glass system built around smoked navy, indigo, and restrained blue/violet specular light.

- `VantafynGlassPanel`: primary containers, settings panels, large information blocks.
- `VantafynGlassCard`: secondary containers, list cards, compact content cards.
- `VantafynGlassChip`: interactive filter/category chips with pressed and selected response.
- `VantafynGlassPill`: metadata pills and compact non-interactive labels.
- `VantafynGlassTile`: tappable action tiles and segmented-style controls.
- `VantafynGlassDock`: bottom rail material; keeps the established dark navy direction.

## Color And Tint Logic

The glass palette is centralized in `VantafynGlassPalette`. Surfaces are not alpha-black blobs; they use layered navy, indigo, graphite, and violet tints with subtle cyan/blue specular light.

Passive containers stay darker and calmer. Selected/focused surfaces receive a stronger internal blue/violet lift and a clearer edge.

## Borders And Highlights

Glass borders use a low-alpha gradient hairline:

- brighter cool-white top edge
- subtle cyan/blue side light
- faint violet lower edge
- reduced opacity when disabled

Each non-dock surface also receives a restrained radial highlight and vertical top-edge sheen. This gives cards and chips depth without fake glossy reflections.

## Depth

Depth is controlled by `VantafynGlassElevation`. Larger panels and modals receive more shadow presence than chips. Shadows stay dark and compact to avoid a washed-out or floating mockup look.

## States

Default: rich smoked navy glass with subtle internal light.

Selected: brighter interior, clearer border, soft blue/violet glow.

Focused: strongest edge definition for TV/keyboard focus.

Pressed: `VantafynGlassChip` and `VantafynGlassTile` apply a small scale response and selected-level material lift.

Disabled: lower contrast while preserving readable shape and hierarchy.

## Usage

Use `VantafynGlassPanel` for large sections, `VantafynGlassCard` for repeated content, `VantafynGlassChip` for selectable filters, `VantafynGlassPill` for metadata, and `VantafynGlassTile` for action controls.

Avoid one-off alpha rectangles for app chrome or controls. If a new surface needs to look premium, route it through the shared glass system first.
