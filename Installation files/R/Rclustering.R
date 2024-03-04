library(tidyverse)
library(RColorBrewer)
library(dendextend)
library(cluster)

dir = "/home/thomas/AMU/Projects/AFMVIDEO/Data/RealData/FullTest/";
CCMatrix = read.csv(paste(dir,"CC.csv",sep=""),header=TRUE,row.names=1)
dist = as.dist(1-CCMatrix)

# dendrogram
dend = hclust(dist, method="complete")
plot(dend)

# heatmap
library(factoextra)
fviz_dist(dist, gradient = list(low = "#00AFBB", mid = "white", high = "#FC4E07"))

## compare fit for different values of clsutering
for(nbcluster in 2:10){
  clusters = cutree(dend, k=nbcluster)
  sil = silhouette(clusters, dist)
  fitsil = mean(sil[,3]) 
  print(paste("dend",nbcluster,fitsil))
}

for(nbcluster in 2:10){
  kmedoids <- pam(dist, nbcluster) 
  kclusters <- kmedoids$cluster
  sil = silhouette(kclusters, dist)
  fitsil = mean(sil[,3]) 
  print(paste("kmed",nbcluster,fitsil))
}

nbcluster = 5 ## seems best
clusters = cutree(dend, k=nbcluster)
sil = silhouette(clusters, dist)
table(clusters)
kmedoids <- pam(dist, nbcluster) 
kclusters <- kmedoids$cluster
ksil = silhouette(kclusters, dist)
table(kclusters)

df = data.frame(ksil[,1],ksil[,2],ksil[,3])
colnames(df) <- c("cluster","neigh","value")
write_csv(df,paste(dir,"clusters.csv",sep=""))
