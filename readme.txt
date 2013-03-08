OBJExport 
latest v0.1.2 03/07/2013

# Description

This is a simple library to export surfaces from processing as OBJ files. It is used the same way the PDF library is used.

It works with beginRecord(), beginRaw(), and createGraphics(). The exporter as well as OBJ files work with arbitrarily sided polygon, not just triangles. This allows for the versatility to export quad or hex based meshes not just triangles. If you require triangles, you can use beginRaw() to get the tesselated output of Processing rather than the original polygons. This library only supports basic mesh output, and it does not currently support advanced features such as colors, normals, textures, or materials. If that's something you're interested in, let me know or contribute!

Tested on Windows 7, Processing 2.0b8

# Installation

To install simply download the zip file, and extract the contents to your Processing libraries folder.