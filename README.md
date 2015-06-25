
# Renjin CRAN Replacement Packages

Generally, Renjin's goal is to leverage R's existing package eco-system, which is truly an embarrassment of riches.
Renjin features even a C/Fortran transpiler that allows us to compile extensions writting with C and Fortran code.

However, this transpiler ("GCC Bridge") is not perfect, and in other situations, there are big advantages to 
replacing a C/Fortran implementation with one based on an existing JVM library, such as rjson.

This project is an umbrella project of sorts for these types of replacements.


