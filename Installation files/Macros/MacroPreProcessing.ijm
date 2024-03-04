// option
run("Appearance...", "  menu=0 gui=1 16-bit=Automatic");
run("Options...", "iterations=1 count=1 black");
// convert to slices
getDimensions(width, height, channels, slices, frames);
frames = Math.max(slices, frames);
getPixelSize(unit, pixelWidth, pixelHeight);
run("Properties...", "channels="+channels+" slices=1 frames="+frames+" pixel_width="+pixelWidth+" pixel_height="+pixelHeight+" voxel_depth=1.0000");
// pre processing
run("Median...", "radius=2 stack");
run("Subtract Background...", "rolling=50 stack");
run("LAFM"); // copy the LAFM.lut (or afm.lut) into the luts folder

info = getImageInfo();
indexTime = indexOf(info, "");