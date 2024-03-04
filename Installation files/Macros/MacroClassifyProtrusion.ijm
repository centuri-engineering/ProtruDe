function getMean(column, start){
  if(nResults == 0) return 0;
  values = Table.getColumn(column,"Results");
  values = Array.slice(values,start);
  Array.getStatistics(values, min, max, mean); 
  return(mean);
}

function getSum(column, start){
  if(nResults == 0) return 0;
  values = Table.getColumn(column,"Results");
  values = Array.slice(values,start);
  Array.getStatistics(values, min, max, mean); 
  return(mean*values.length);
}

function getMax(column, start){
  if(nResults == 0) return 0;
  values = Table.getColumn(column,"Results");
  values = Array.slice(values,start);
  Array.getStatistics(values, min, max, mean); 
  return(max);
}

function setFrameInfo(column, start, value){
	for (r = start; r < nResults(); r++) {
		setResult(column, r, value);
}
updateResults();
}

// two images
// raw image = rawMovie
// bin image with protrusion = binMovie
open();
rename("rawMovie");
open();
rename("binMovie");

minsize = 10; // min size pixels of the protrusions
run("Options...", "iterations=1 count=1 black");
setBatchMode("hide");
getDimensions(width, height, channels, slices, frames);
frameInterval = Stack.getFrameInterval();
nbPro = newArray(frames);
sumArea = newArray(frames);
meaMea = newArray(frames);
sumInt = newArray(frames);
fraIdx = newArray(frames);
row = 0;
for(i = 0; i< frames; i++){
	print("Analysing "+i);
	selectWindow("rawMovie");
	setSlice(i+1);
	run("Duplicate...", "title=raw");
	run("Set Measurements...", "area mean standard fit shape feret's integrated centroid median display redirect=raw decimal=3");
	selectWindow("binMovie");
	setSlice(i+1);
	run("Duplicate...", "title=bin");
	run("Analyze Particles...", "size="+minsize+"-Infinity pixel display summarize slice"); // FR: added pixel so minsize is always in pixel units
	setFrameInfo("FrameUnit", row, i*frameInterval);
	setFrameInfo("Frame", row, (i+1));
	nbPro[i] = Table.get("Count", i,"Summary");
	sumArea[i] = getSum("Area", row);
	meaMea[i] = getMean("Mean", row);
	sumInt[i] = getSum("RawIntDen", row);
	fraIdx[i] = i+1;
	row = nResults;
	//print(nbPro[i],sumArea[i],meaMea[i],sumInt[i]);
	//wait(100);
	close("raw");
	close("bin");
}
setBatchMode("show");
print("finished");

Table.create("classify");
Table.setColumn("nb",nbPro);
Table.setColumn("area",sumArea);
Table.setColumn("mean",meaMea);
Table.setColumn("int",sumInt);
Table.setColumn("frame", fraIdx);
Table.update();

// the full results are available in the "Results" window
