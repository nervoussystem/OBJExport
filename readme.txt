OBJExport 
latest v0.2.1 03/13/2013

# Description

This is a simple library to export surfaces from processing as OBJ files. It is used the same way the PDF library is used.

It works with beginRecord(), beginRaw(), and createGraphics(). The exporter as well as OBJ files work with arbitrarily sided polygon, not just triangles. This allows for the versatility to export quad or hex based meshes not just triangles. If you require triangles, you can use beginRaw() to get the tesselated output of Processing rather than the original polygons.

The library also supports color mesh export creating a .png texture and a .mtl material file to go along with the obj. Use setColor(true) on the OBJExport object before beginDraw() to turn on color export.

The library does not support texture() or normal() in the export or lines. If you're interested in that functionality, let me know or contribute!

Tested on Windows 7, Processing 2.0b8

# Installation

To install simply download the zip file, and extract the contents to your Processing libraries folder.