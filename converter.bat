@echo off

javac -cp .;lib/javaview.jar;lib/jvdev.jar;lib/jvdev6.jar;lib/jvx.jar Converter_starccm_to_jvx.java
java -cp .;lib/javaview.jar;lib/jvdev.jar;lib/jvdev6.jar;lib/jvx.jar Converter_starccm_to_jvx %1