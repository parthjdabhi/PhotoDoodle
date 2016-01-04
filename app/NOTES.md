#Currently

Need to make a common base activity which tracks application active state, and fires events accordingly
	- sign in manager needs to listen to activate/deactivate state to connect/disconnect google api client

#TODO

Superficial shared element transitions are working, but:
- appbar region flashes white in between the two activities

redo drawing UX.
	- drop the cool popup menus :(
	- make a toolbar across the bottom with pen/pencil/erasers colorwell and camera icon
	- active tool is raised?
	
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
