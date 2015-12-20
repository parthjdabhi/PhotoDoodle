#Currently

When dismissing DoodleActivity, and saving the doodle data to the file, the DoodleThumbnailRenderer is notified that the doc was dirty, and needs to be updated. Trouble is, there seems to be a threading

Theme dialogs
http://www.materialdoc.com/alerts/

#TODO
use Camera2 API to integrate photo taking directly into the DoodleActivity (or more likely a PhotoView)


#Alpha Blending
Will require 3 new full-screen bitmaps. 
Since this is expensive, should only be active when alpha < 255
- livePathBitmap - is cleared and has the live stroke drawn in full alpha each live stroke change
- staticPathBitmap - gets the static paths drawn into it in full alpha
- pathCompositeBitmap - livePathBitmap and staticPathBitmap are drawn at full alpha, then, pathCompositeBitmap is drawn to screen (later to backing store when stroke is complete) at the stroke's alpha.

#Chunking

Line caps cause overlap-darkening at chunk intersections when drawing in partial alpha. this should not be a surprise.
- Possible solution: Have 2 bitmaps. A backing store, and a bitmap for the current stroke? The current stroke is rendered live on screen, static paths into the current stroke bitmap. When stroke is finished, that bitmap is rendered into the backing store.
	-- this won't fix the overlaps

# Nota Bene
Android hardware accelerated drawing does not pass the clip rect to the Canvas in onDraw when invalidate(Rect) is called. Can't use for pruning.
