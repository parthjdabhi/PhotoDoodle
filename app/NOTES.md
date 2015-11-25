#Currently

PhotoDoodle can't save current photo via icepick - it's too big. Probably need to save to a buffer via Kryo, and can perhaps use Realm to persist that buffer.

You can draw on the photo while in PHOTO mode. I need to figure out how to delegate input appropriately. Probably want to have a isCropMode kind of flag on PhotoDoodle, in which case it lets user manipulate photo (and does NOT pass on input to super), otherwise, it passes on to super, and drawing happens. 

Photo toolbar needs better icons, and they need to show on/off state. Got to think this through. 
	- take a photo, this is an action
	- edit mode can be on or off. This is a toggle????

Toolbars: Should they be on top, right under the appbar? Should the background be @primaryColor????


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
