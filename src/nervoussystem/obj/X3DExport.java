/*
 * MeshExport - Exports obj and x3d files with color from processing with beginRecord and endRecord
 * by Jesse Louis-Rosenberg 
 * http://n-e-r-v-o-u-s.com
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p/>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU Lesser General
 * Public License; if not,
 * write to the Free Software Foundation, Inc., 59 Temple Place,
 * Suite 330, Boston, MA  02111-1307  USA
 */
 
package nervoussystem.obj;

import nervoussystem.obj.MeshExport;

 public class X3DExport extends MeshExport {
	private String vertexString;
	private String texString;
	
	protected void writeHeader() {
		writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		writer.println("<!DOCTYPE X3D PUBLIC \"ISO//Web3D//DTD X3D 3.1//EN\" \"http://www.web3d.org/specifications/x3d-3.1.dtd\">");
		writer.println("<X3D profile=\"Immersive\" version=\"3.1\" xsd:noNamespaceSchemaLocation=\"http://www.web3d.org/specifications/x3d-3.1.xsd\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema-instance\">");
		writer.println(" <head>");
		writer.println("  <meta content=\"" + filenameSimple + ".x3d\" name=\"title\"/>");
		writer.println("  <meta content=\"Generated from MeshExport by Nervous System\" name=\"description\"/>");
		writer.println("  <meta content=\"Processing MeshExport, http://n-e-r-v-o-u-s.com/tools/obj\" name=\"generator\"/>");
		writer.println(" </head>");
		writer.println(" <Scene>");
		writer.println("  <Shape>");
		if(colorFlag) {
			writer.println("   <Appearance>");
			writer.println("	<ImageTexture url=\"" + filenameSimple + ".png\"/>");
			writer.println("   </Appearance>");
		}
		writer.print("   <IndexedFaceSet ");
		texString = "";
		vertexString = "";
	}

	@Override
	protected void writeFaces() {
		writer.print("coordIndex=\"");
		for(int i=0;i<faceCount;++i) {
		  int[] f = faces[i];
		  for(int j=0;j<f.length;++j) {
		    //zero based indices versus 1 based of the OBJ
			writer.print((f[j]-1) + " ");
		  }
		  writer.print("-1");
		  if(i<faceCount-1) writer.print(" ");
		}
		writer.print("\"");
		if(colorFlag) {
			writer.print(" texCoordIndex=\"");
			int index = 0;
			for(int i=0;i<faceCount;++i) {
			  int[] f = faces[i];
			  for(int j=0;j<f.length;++j) {
				if(f.length <= 4)
					writer.print((index++) + " ");
				else
					writer.print("0 ");
			  }
			  writer.print("-1");
			  if(i<faceCount-1) writer.print(" ");
			}
			writer.print("\"");
		}
		writer.println(">");
	}
	
	@Override
	protected void writeFooter() {
	    writer.println("<Coordinate point=\"" + vertexString + "\"/>");
	    if(colorFlag)
			writer.println("<TextureCoordinate point=\"" + texString + "\"/>");
		writer.println("	</IndexedFaceSet>");
		writer.println("   </Shape>");
		writer.println("  </Scene>");
		writer.println("</X3D>");
	}
	
	@Override
	protected void writeTexCoord(float u, float v) {
		//writer.print("vt " + u + " " + v);
		texString += u + " " + v + " ";
	}
	
	@Override
	protected void writeVertices() {
		float v[];
		for(int i=0;i<ptMap.size();++i) {
			v = pts[i];
			vertexString += v[0] + " " + v[1] + " " + v[2] + " ";
		}
	}

}