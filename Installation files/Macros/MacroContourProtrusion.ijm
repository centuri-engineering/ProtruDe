// Choose colour for selection
run("Colors...", "foreground=black background=black selection=cyan");

// Open aligned movie
print("Open aligned movie");
open();
dir = getDirectory("image");
title = getTitle();
run("Duplicate...", "title=Aligned-ROI.tif duplicate");

// Open protrusion movie
print("Open protrusion movie");
open();

// get all roi
roiManager("Reset");
run("Analyze Particles...", "add stack");
roiManager("Show All without labels");
selectWindow("Aligned-ROI.tif");
roiManager("Show All");
run("From ROI Manager");
run("Flatten", "stack");
//saveAs("Tiff", dir+replace(title,".tif","-ROI.tif"));
