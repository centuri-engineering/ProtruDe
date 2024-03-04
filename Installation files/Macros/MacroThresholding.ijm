minsize = 800; // minimum size in pixels
// option
run("Appearance...", "  menu=0 gui=1 16-bit=Automatic");
run("Options...", "iterations=1 count=1 black");
// convert to slices
getDimensions(width, height, channels, slices, frames);
frames = Math.max(slices, frames);
getPixelSize(unit, pixelWidth, pixelHeight);
run("Properties...", "channels="+channels+" slices=1 frames="+frames+" pixel_width="+pixelWidth+" pixel_height="+pixelHeight+" voxel_depth=1.0000");
// thresholding
title=getTitle();
run("Duplicate...", "title=maskBinary.tif duplicate");
run("Convert to Mask", "method=Li background=Dark calculate black");
run("Analyze Particles...", "size="+minsize+"-Infinity pixel show=Masks stack");
rename("FinalMaskOf"+title);
run("Properties...", "channels="+channels+" slices=1 frames="+frames+" pixel_width="+pixelWidth+" pixel_height="+pixelHeight+" voxel_depth=1.0000");
run("Grays");
