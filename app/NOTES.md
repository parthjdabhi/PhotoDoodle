# Currently

Chunking results in lost "bridge" between chunks.
- when clearing inputStroke after optimization, save last vertex and re-add it
- mark that vertex as "frozen" so the next update velocity pass doesn't mess with its velocity

this will still result in a small velocity error for the first new vertex added since the velocity update backtracks by 2, and the missing vertex will report a velocity of 0. This can be fixed by computing the amount to add from the n-2 vertex's velocity to the n-1 vertex such that the end result would be the same.


# Drawing

Performance is an issue. We probably want to render strokes to start backing bitmap.
 
- Have start statically-sized backing store bitmap. Say, start square enclosing the view, centered.
- When drawing, the bitmap is drawn, and the current stroke is drawn on top.
	Bitmap is clear, transparent.
	Strokes are drawn in whatever color
	Eraser is drawn using erase blend mode
	

 
 
	
# Nota Bene
Android hardware accelerated drawing does not pass the clip rect to the Canvas in onDraw when invalidate(Rect) is called. Can't use for pruning.

# Currently

The computed radius of each control point varies wildly. It should either get start max value, and/or have start smoothing applied to it
