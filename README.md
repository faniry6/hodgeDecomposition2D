# Introduction

This repository is the main function to compute a  Hodge decomposition for surface meshes (from 3 to 5 components). It is mainly written towards application for aorta wall shear stress analysis. By little changes, it could be easily adapted for other kind of surfaces. The official main script and several models are published here

https://dryad.figshare.com/articles/Hodge-Morrey-Friedrich_decomposition_for_WSS_vector_field/7496762/1

The final published paper can be found here

https://royalsocietypublishing.org/doi/10.1098/rsos.181970

# The files

The two source files contain a converter and a hodge computation. It is aimed for multithread use with large sized model. The converter is used to take an stl + csv (vector defined at the barycentric position of the faces) and convert them into jvx.

# Extern libraries

The code is using an external jar libraries (www.javaview.de) which are licenced but free for research purpose

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
The squared L^2-norms  of all decomposition will be then saved in the same folder as the input file into csv file.

# Test

There is an example test in <code>./test/fromStarrm</code> where you could find an exmple file to see how the formating of the vector looks like
# Troubleshouting

- Make sure that the correct jvx is mapped. 
- Make sure that only the stl and corresponding csv files are present in the folder. Otherwise, the other files will be also filtered out and produce errors
