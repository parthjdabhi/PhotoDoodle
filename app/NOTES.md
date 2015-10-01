# Drawing

Performance is an issue. We probably want to render strokes to a backing bitmap. 
	
# Nota Bene
Android hardware accelerated drawing does not pass the clip rect to the Canvas in onDraw when invalidate(Rect) is called. Can't use for pruning.

# Currently

The computed radius of each control point varies wildly. It should either get a max value, and/or have a smoothing applied to it
