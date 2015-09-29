# Drawing

We don't want to draw the whole set of lines 60 times per second as user moves finger. We can take advantage of the fact that we only need to draw what is new.

- So we'll have an array of rendered strokes, and a current stroke which is appended to with each new incoming point.
- On touch begin, make a new current stroke
- on touch move, add point to current stroke, generate rendered variant. compute bounds of updated region of current stroke (say, segments n-1 to n)
	- render whole scenegraph but clipping to the above bounds
- on touch up, add final location to stroke (per touch move), perform render of the whole stroke, add it to rendered strokes array, null out the current stroke.
	- no need to perform another render, since rendering is up

# Currently

The computed radius of each control point varies wildly. It should either get a max value, and/or have a smoothing applied to it
