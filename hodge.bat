@echo off

javac -cp .;lib/javaview.jar;lib/jvdev.jar;lib/jvdev6.jar;lib/jvx.jar ComputeHodge.java
java -cp .;lib/javaview.jar;lib/jvdev.jar;lib/jvdev6.jar;lib/jvx.jar ComputeHodge %1