# collapse number of protrusions of manually classified
colProt <- function(x){
  if(x == 0) return(0)
  if(x>=1) return(1)
}

library(tidyverse)
library(randomForest)
testAFM <- read.csv("/home/thomas/AMU/Projects/AFMVIDEO/Data/CoV2-S_Examplary/classify.csv", sep =",")
manualAFM <- read.csv("/home/thomas/AMU/Projects/AFMVIDEO/Data/CoV2-S_Examplary/manual.csv", sep = "\t")
trainAFM <- inner_join(testAFM, manualAFM, by="frame")
# trainAFM <- select(trainAFM, -frame) # remove frame column
trainAFM$nbProt <- mapply(colProt, trainAFM$nbProt)
trainAFM$nbProt <- as.factor(trainAFM$nbProt)
trainrf <- randomForest(nbProt~., data = trainAFM, importance = TRUE, proximity = TRUE)
print(trainrf)
trainrf %>% importance %>% round(2)
trainrf$proximity
predict(trainrf)

library(e1071)
trainsvm = svm(nbProt ~ ., data = trainAFM, kernel = "linear", cost = 10, scale = FALSE)
