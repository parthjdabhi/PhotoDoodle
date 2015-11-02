#Persistence

Have the incremental tessellator record input strokes. When tesselating a chunk, push the inputstroke into an array such that it is not lost. To save a document, perisst the strokes and the brush used. A whole document can be a storage of these pairings Pair<Array<InputStroke>,Brush> 

# Currently

Line caps cause overlap-darkening at chunk intersections when drawing in partial alpha. this should not be a surprise.
- Possible solution: Have 2 bitmaps. A backing store, and a bitmap for the current stroke? The current stroke is rendered live on screen, static paths into the current stroke bitmap. When stroke is finished, that bitmap is rendered into the backing store.
	-- this won't fix the overlaps

# Nota Bene
Android hardware accelerated drawing does not pass the clip rect to the Canvas in onDraw when invalidate(Rect) is called. Can't use for pruning.
