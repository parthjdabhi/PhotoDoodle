# Currently

Render static paths into a bitmap in the onStaticPathAvailable callback using a Canvas 
Render that bitmap in draw

# Drawing

Performance is an issue. Need to partition strokes as drawn, each "finished" stroke should be rendered to backing store bitmap.
 
- Have start statically-sized backing store bitmap. Say, start square enclosing the view, centered.
- When drawing, the bitmap is drawn, and the current stroke is drawn on top.
	Bitmap is clear, transparent.
	Strokes are drawn in whatever color
	Eraser is drawn using erase blend mode
	

 
 
	
# Nota Bene
Android hardware accelerated drawing does not pass the clip rect to the Canvas in onDraw when invalidate(Rect) is called. Can't use for pruning.
