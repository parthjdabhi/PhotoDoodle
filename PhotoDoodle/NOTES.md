#FlyoutMenuView

FlyoutMenuTestActivity
	- Persistance of selection state no worky
	
FlyoutMenuView
	- selected menu item highlight needs to be outset by half of margin
	
Elevation:
	- need to figure out how to emulate elevation
		- blur radius as function of elevation
		- vertical offset as function of elevation
		
	Can be achieved by adding a FAB or something and setting elevation on it for comparisons


#Currently

DoodleActivity popup doesn't show if you tap it, tap outside, and tap again on toolbar

DoodleDocumentAdapter's updateAllImageBorders ALMOST works. It seems like one or two items doesn't get updated.

I'm looking into using an ItemDecorator, but this may be complex.
	might my best option be to walk the array of children and draw a border for each child explicitly???
	- need to take into account scroll position
	- only draw borders for existing items, so if there's an empty spot at end of list, don't draw decorator there
	- what about animations??? 
	

#BUGS
When deleting a bunch of items quickly - looks like the previously removed doc gets deleted. I assume snackbar is a singleton and is flushing its ondismissed queue or something to that effect.

We need to do something like detect if a delete is requested while another delete is still showing the undo button, and in that case, force that delete to run??? Or is the problem that that's happening automatically, and correctly???
 
	java.lang.IllegalStateException: Object is no longer managed by Realm. Has it been deleted?
	  at io.realm.internal.InvalidRow.getStubException(InvalidRow.java:192)
	  at io.realm.internal.InvalidRow.getString(InvalidRow.java:88)
	  at io.realm.PhotoDoodleDocumentRealmProxy.getUuid(PhotoDoodleDocumentRealmProxy.java:74)
	  at org.zakariya.photodoodle.adapters.DoodleDocumentAdapter.contains(DoodleDocumentAdapter.java:356)
	  at org.zakariya.photodoodle.fragments.DoodleDocumentGridFragment$5.onDismissed(DoodleDocumentGridFragment.java:264)
	  at android.support.design.widget.Snackbar.onViewHidden(Snackbar.java:603)

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