# Currently

Chunking works, but appears as if breakages occur during optimization, as if FIRST point in new inputStroke is lost...
**Disabling optimization confirms hypothesis. Chunks meet up correctly.**

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
