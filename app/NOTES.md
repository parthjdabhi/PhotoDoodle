# Currently

The eraser isn't erasing INTO the bitmap backing store until the stroke is saved as a static path. Need to erase, live, directly into bitmap?

Need to come up with an associated Paint (or some other object) for a given input stroke, such that users can draw in black, or erase, etc. 
- Bitmap is clear, transparent.
- Strokes are drawn in whatever color
- Eraser is drawn using erase blend mode
- See what happens rendering at partial alpha. If self overlaps aren't double-blended, I can add a circle at start and end of each stroke to improve chunking joins.
	- quick test makes it look like self-intersection does NOT result in overdraw.

# Nota Bene
Android hardware accelerated drawing does not pass the clip rect to the Canvas in onDraw when invalidate(Rect) is called. Can't use for pruning.
