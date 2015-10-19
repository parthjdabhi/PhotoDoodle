# Currently

InputStrokeTessellator handles zig-zags poorly. They get super-thin.

- don't use tangent for cross brace. Use whatever vector is computed in the stroke tessellator - it doesn't exhibit this acute-angle artifact.
- for smoothing, instead of computing speed at a vertex, compute speed at a segment! The speed at a vertex is the speed at the two joined segments, averaged. This might be more effective. Could also use weighted average of the 2 leading and 2 following segments.


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
