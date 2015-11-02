#Rotation, and spatial persistence
I need a coordinate system centered on the current doodle size, such that on rotation, the drawing remains centered. 
This was possible when using bitmaps by making a square bitmap (size = max(width,height)) setting a translation on the bitmapCanvas, but since we're not persisting the bitmap this is no longer meaningful.

Define a scaled/offsetted coordinate system, where 0,0 is center of screen, and -1024->+1024 is the major axis (height/width). Convert user input coords to this coordinate system. When rendering generated paths, create a matrix on the bitmapCanvas that converts from unit coords back to screen space. 


#Chunking

Line caps cause overlap-darkening at chunk intersections when drawing in partial alpha. this should not be a surprise.
- Possible solution: Have 2 bitmaps. A backing store, and a bitmap for the current stroke? The current stroke is rendered live on screen, static paths into the current stroke bitmap. When stroke is finished, that bitmap is rendered into the backing store.
	-- this won't fix the overlaps

# Nota Bene
Android hardware accelerated drawing does not pass the clip rect to the Canvas in onDraw when invalidate(Rect) is called. Can't use for pruning.
