run("Set Measurements...", "centroid display redirect=None decimal=3");
run("Options...", "iterations=1 count=1 black do=Nothing");
setBackgroundColor(0, 0, 0);
rename("binMovie");
getDimensions(width, height, channels, slices, frames);
getPixelSize(unit, pixelWidth, pixelHeight);
roiManager("reset");
run("Create Mask"); // roi mask image
for(f = 0; f< frames; f++){
	selectWindow("binMovie");
	roiManager("reset");
	run("Select None");
	Stack.setFrame(f+1);
	run("Analyze Particles...", "size=0-Infinity show=Nothing display clear add slice");
	for (i = 0; i < nResults; i++) {
		x = getResult("X", i) / pixelWidth;
		y = getResult("Y", i) / pixelHeight;
		selectWindow("Mask");
		if(getPixel(x, y) == 0){
			selectWindow("binMovie");
			roiManager("select", i);
			run("Clear", "frame");
		}
	}
}
roiManager("reset");
run("Select None");

