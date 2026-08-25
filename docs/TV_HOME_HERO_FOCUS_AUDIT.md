# TV Home Hero Focus Audit

## Root Cause

The TV Home hero copy and Play/Details actions were still rendered as the first item in the `LazyColumn`.

That meant Android TV focus could ask the scroll container to keep the focused Play/Details controls visible. When those buttons gained focus, the row scroller was allowed to adjust, which made the hero title, metadata, overview and actions appear to shift vertically.

The issue was not missing padding. It was ownership: fixed hero content was inside scrollable row content.

## Fix

- The hero backdrop remains a pinned top layer.
- The hero title, metadata, overview and action buttons are now a fixed overlay sibling above the row scroller.
- The `LazyColumn` now contains only the scrolling rails and starts below the fixed hero overlay.
- Play/Details focus visuals are graphics-layer scale/border effects and do not change measured size.
- Hero action buttons use compact fixed minimum sizing so focus never causes relayout.

## Acceptance

Focusing Play or Details no longer gives the `LazyColumn` a reason to bring the hero into view, so hero content stays positionally fixed.

