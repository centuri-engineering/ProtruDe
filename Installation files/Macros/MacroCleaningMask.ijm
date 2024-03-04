radiusDilate = 5;
// raw video should be opened
title = getTitle();
run("Duplicate...", "title=copy duplicate");
// open corresponding mask image
file = File.openDialog("Open binary mask image");
open(file);
rename("mask");
run("Maximum...", "radius="+radiusDilate+" stack");
run("Make Binary", "method=Default background=Default black");
// option
run("Appearance...", "  menu=0 gui=1 16-bit=Automatic");
run("Options...", "iterations=1 count=1 black");
// convert to slices
getDimensions(width, height, channels, slices, frames);
frames = Math.max(slices, frames);
getPixelSize(unit, pixelWidth, pixelHeight);
run("Properties...", "channels="+channels+" slices=1 frames="+frames+" pixel_width="+pixelWidth+" pixel_height="+pixelHeight+" voxel_depth=1.0000");
// get ROIs
selectWindow("mask");
run("ROI Manager...");
roiManager("reset");
for (s = 1; s <= frames; s++) {
	Stack.setPosition(1, 1, s);
	run("Select None");
	run("Create Selection");
	roiManager("Add");	
}
run("Select None");
selectWindow("copy");
roiManager("Select", 0);
setBackgroundColor(0, 0, 0);
for (s = 1; s <= frames; s++) {
	Stack.setPosition(1, 1, s);
	roiManager("Select", s-1);
	run("Clear Outside", "slice");
}
rename(title+"_MaskCleaned");
roiManager("reset");
run("Select None");
