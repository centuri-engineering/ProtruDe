// input image should be already processed image 
// (gaussian blur 2, subtract background 50)
radius = 5; // radius of protrusions to detect
run("Options...", "iterations=1 count=1 black do=Nothing");
run("Duplicate...", "title=Ext duplicate");
setAutoThreshold("Huang dark stack");setOption("BlackBackground", true);
run("Convert to Mask", "method=Huang background=Dark black");
run("Duplicate...", "title=Ext-1 duplicate");
run("Minimum...", "radius="+radius+" stack");// erosion
run("Maximum...", "radius="+radius+" stack");// dilation
imageCalculator("Subtract create stack", "Ext","Ext-1");
minsize = 10; // min size in pixels of the protrusion
run("Analyze Particles...", "size="+minsize+"-Infinity pixel show=Masks stack"); //FR: added pixel so it is always in pixel units
run("Grays");
// convert to slices
getDimensions(width, height, channels, slices, frames);
frames = Math.max(slices, frames);
getPixelSize(unit, pixelWidth, pixelHeight);
// set image properties
run("Properties...", "channels="+channels+" slices=1 frames="+frames+" pixel_width="+pixelWidth+" pixel_height="+pixelHeight+" voxel_depth=1.0000");
