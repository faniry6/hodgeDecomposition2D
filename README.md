# Introduction

This repository is the main function to compute a 5 component Hodge decomposition for surface meshes. It is mainly written towards application for aorta wall shear stress analysis. By little changes, it could be easily adapted for other kind of surfaces. The official main script and severl models are published here

https://dryad.figshare.com/articles/Hodge-Morrey-Friedrich_decomposition_for_WSS_vector_field/7496762/1

The final published paper can be found here

https://royalsocietypublishing.org/doi/10.1098/rsos.181970

# The files

The two source contains a converter and a hodge computation. It is aimed for multithread use with large sized model. 

# How to use

You can download/clone the repository. Open a command line (Windows) and run the following

<code>
   converter.bat "absolute path to folder containing the *.stl and *.csv files"
 </code>
 to combine the stl file with the vector information file. It should be a face based vector information. To compute the 5 components
 Hodge decomposition, use the following command.
 <code>
   hodge.bat "absolute path to folder containing the jvx files"
  </code>
The squared L^2-norms  of all decomposition will be then saved in the same folder as the input file.
