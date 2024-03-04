// Localization AFM
Dialog.create("Localization AFM");
Dialog.addMessage("This is a Macro of the Bio-AFM-Lab/Scheuring-Lab\nwww.ScheuringLab.com\n");
Dialog.addSlider("Noise Tolerance (%):", 0, 100, 5)
Dialog.addNumber("Gaussian Profile:", 3);
Dialog.addCheckbox("Threshold",false);
Dialog.addMessage("Please refer to and cite:\n"+"Heath et. al. Localization AFM, Nature, 2021.");
Dialog.show();
t = Dialog.getNumber(); 
gauss = Dialog.getNumber();
thresh = Dialog.getCheckbox();
type = "Single Points";
exclude =true;
options = "";
if (exclude) options = options + " exclude";
run("Options...", "iterations=1 count=1 black");
if (thresh == true){
roiManager("reset")
run("Duplicate...", "title=NewStack duplicate");
rename("Mask_of_name");
waitForUser("Apply threshold to stack to select ROI then click ok ");
for (i=1; i<=nSlices; i++) {
setSlice(i);
run("Create Selection");
roiManager("Add");
}
}
close("Mask_of_name");
run("Duplicate...", "title=input duplicate");
input = getImageID();
selectImage(input);
run("32-bit");
setBatchMode(true);
n = nSlices();
for (i=1; i<=n; i++) {
selectImage(input);
setSlice(i);
if (thresh == true){
roiManager("Select", i-1);
}
getStatistics(array, max, min, mean, stdDev) ;
tolerance = mean*t/100;
run("Subtract...", "value="+min);
run("Find Maxima...", "noise="+ tolerance +" output=["+type+"]"+options);
if (i==1)
output = getImageID();
else if (type!="Count") {
run("Select All");
run("Copy");
close();
selectImage(output);
run("Add Slice");
run("Paste");
}
}
run("Select None");

selectImage(output);
run("32-bit");
run("Gaussian Blur...", "sigma="+gauss+" stack");
run("Divide...", "value=40.58 stack");
gmax = gauss*gauss;
run("Multiply...", "value="+gmax+" stack");

rename("Prob");


run("Duplicate...", "title=NewStack duplicate");
rename("H1");
setThreshold(0.0001,10000000);
run("Convert to Mask", "method=Default background=Dark black");
run("32-bit");
imageCalculator("Multiply 32-bit stack", "H1", input);
rename("Height");
close("H1");
close("input");
run("LAFM");
run("Duplicate...", "title=NewStack duplicate");
rename("HeightRGB");
run("RGB Color");
run("Split Channels");


imageCalculator("Multiply 32-bit stack", "HeightRGB (red)","Prob");
close("HeightRGB (red)");
rename("HeightRGB (red)");
imageCalculator("Multiply 32-bit stack", "HeightRGB (green)","Prob");
close("HeightRGB (green)");
rename("HeightRGB (green)");
imageCalculator("Multiply 32-bit stack", "HeightRGB (blue)","Prob");
close("HeightRGB (blue)");
rename("HeightRGB (blue)");
run("Merge Channels...", "c1=[HeightRGB (red)] c2=[HeightRGB (green)] c3=[HeightRGB (blue)] keep");
rename("HeightRGB");
setBatchMode(false);
nu = 10/nSlices();
run("Scale...", "x=1.0 y=1.0 z="+nu+" interpolation=Bicubic average process create");
run("Enhance Contrast", "saturated=0.5");
run("Apply LUT", "stack");
rename("HeightRGBset");
run("Grouped Z Project...", "projection=[Average Intensity]");
close("HeightRGBset");

