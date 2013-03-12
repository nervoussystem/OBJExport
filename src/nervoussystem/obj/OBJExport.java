/*
 * OBJExport - Exports obj files from processing with beginRecord and endRecord
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

import java.io.*;
import java.util.HashMap;
import processing.core.*;
import processing.opengl.*;

public class OBJExport extends PGraphics {
  File file;
  String filenameSimple;
  PrintWriter writer;
  float[][] pts;
  int[][] lines;
  int[][] faces;
  int[] colors;
  int lineCount;
  int faceCount;
  int objectMode = 0;
  HashMap<String,Integer> ptMap;
  
  static protected final int MATRIX_STACK_DEPTH = 32;
  int DEFAULT_VERTICES = 4096;
  int VERTEX_FIELD_COUNT = 3;
  float vertices[][];
  
  int numTriangles = 0;
  int numQuads = 0;
  static protected int TRIANGLE_RES = 10;
  static protected int RECT_RES = TRIANGLE_RES+4;
  
  boolean drawBegan = false;
  //make transform function work
  protected int stackDepth;
  protected float[][] matrixStack = new float[MATRIX_STACK_DEPTH][16];
  PMatrix3D transformMatrix;
  
  //color
  private boolean colorFlag = false;
  PGraphics texture;
  
  public OBJExport() {
    vertices = new float[DEFAULT_VERTICES][VERTEX_FIELD_COUNT];
	stackDepth = 0;
	transformMatrix = new PMatrix3D();
  }

  public void setPath(String path) {
    this.path = path;
    if (path != null) {
      file = new File(path);
      if (!file.isAbsolute()) file = null;
    }
    if (file == null) {
      throw new RuntimeException("OBJExport requires an absolute path " +
        "for the location of the output file.");
    }
	filenameSimple = file.getName();
	int dotPos = filenameSimple.lastIndexOf(".");
	if(dotPos > -1) {
		filenameSimple = filenameSimple.substring(0,dotPos);
	}
  }

  protected void allocate() {
  }

  public void dispose() {
    writer.flush();
    writer.close();
    writer = null;
  }

  public boolean displayable() {
    return false;  // just in case someone wants to use this on its own
  }

  public void beginDraw() {
    // have to create file object here, because the name isn't yet
    // available in allocate()
    if (writer == null) {
      try {
        writer = new PrintWriter(new FileWriter(file));
      } 
      catch (IOException e) {
        throw new RuntimeException(e);
      }
      pts = new float[4096][VERTEX_FIELD_COUNT];
      lines = new int[4096][];
      faces = new int[4096][];
      ptMap = new HashMap<String,Integer>();
    }
	if(colorFlag && colors == null) colors = new int[4096];
    lineCount = 0;
    faceCount = 0;
    vertexCount = 0;
    numTriangles = 0;
    numQuads = 0;
  }

  public void endDraw() {
    //write vertices and initialize ptMap
	if(colorFlag) {
		colorExport();
	}
    writeVertices();
    writeLines();
    writeFaces();
	drawBegan = false;
  }
  
  private void writeVertices() {
	float v[];
    for(int i=0;i<ptMap.size();++i) {
      v = pts[i];
      writer.println("v " + v[0] + " " + v[1] + " " + v[2]);
    }
  }
  
  private void writeLines() {
    for(int i=0;i<lineCount;++i) {
      int[] l = lines[i];
      String output = "l";
      for(int j=0;j<l.length;++j) {
        output += " " + l[j];
      }
      writer.println(output);
    }
  }

  private void writeFaces() {
    for(int i=0;i<faceCount;++i) {
      int[] f = faces[i];
	  if(colorFlag) {
		String output = "f";
		for(int j=0;j<f.length;++j) {
		  output += " " + f[j] + "/" + f[j];
		}
		writer.println(output);
	  } else {
		writeFace(f);
	  }
    }
  }
  
  private void writeFace(int [] f) {
	String output = "f";
      for(int j=0;j<f.length;++j) {
		output += " " + f[j];
      }
      writer.println(output);
  }
  
  private void colorExport() {
    int textureSize = PApplet.ceil(PApplet.sqrt(ptMap.size()));
	
	texture = parent.createGraphics(textureSize, textureSize, P2D);
	writer.println("mtllib " + filenameSimple + ".mtl");
	writer.println("usemtl " + filenameSimple);
	
	writeTextureCoords();
	generateTexture();
	writeMaterial();
  }
  
  private void writeTextureCoords() {
	for(int i=0;i<ptMap.size();++i) {
      writer.println("vt " + (i%texture.width)*1.0/(texture.width-1) + " " + (i/texture.width)*1.0/(texture.width-1) );
    }
  }
  
  private void writeMaterial() {
	PrintWriter matWriter = null;
	try {
		String filepath = file.getParent();
        matWriter = new PrintWriter(new FileWriter(new File(filepath + "\\" + filenameSimple + ".mtl")));
      } 
      catch (IOException e) {
        throw new RuntimeException(e);
    }
	matWriter.println("newmtl " + filenameSimple);
	matWriter.println("Ka 1.000 1.000 1.000");
	matWriter.println("Kd 1.000 1.000 1.000");
	matWriter.println("Ks 0.000 0.000 0.000");
	matWriter.println("d 1.0");
	//do texture
	matWriter.println("map_Ka " + filenameSimple + ".png");
	matWriter.println("map_Kd " + filenameSimple + ".png");
	matWriter.flush();
	matWriter.close();
  }
  
  private void generateTexture() {
    texture.beginDraw();
	texture.background(0);
	texture.loadPixels();
	int c;
	for(int i=0;i<ptMap.size();++i) {
	  c = colors[i];
	  texture.pixels[i] = c;
	}
	texture.updatePixels();
	texture.endDraw();
	texture.save(file.getParent()  + "\\" + filenameSimple + ".png");
  }
  
  public void beginShape(int kind) {
    shape = kind;
    vertexCount = 0;
  }

  public void vertex(float x, float y) {
    vertex(x,y,0);
  }
  
  private static float tempVertex[] = new float[3];

  public void vertex(float x, float y,float z) {	
    if(vertexCount >= vertices.length) {
		float newVertices[][] = new float[vertices.length*2][VERTEX_FIELD_COUNT];
		System.arraycopy(vertices,0,newVertices,0,vertices.length);
		vertices = newVertices;
    }
    float vertex[] = vertices[vertexCount];
    vertex[X] = x;
    vertex[Y] = y;
    vertex[Z] = z;
	transformMatrix.mult(vertex, tempVertex);
	x = tempVertex[0];
	y = tempVertex[1];
	z = tempVertex[2];
    if(!ptMap.containsKey(x+"_"+y+"_"+z)) {
      if(ptMap.size() >= pts.length) {
		float newPts[][] = new float[pts.length*2][];
		System.arraycopy(pts,0,newPts,0,pts.length);
		pts = newPts;
		//do the colors
		if(colorFlag) {
			int newColors[] = new int[colors.length*2];
			System.arraycopy(colors,0,newColors,0,colors.length);
			colors = newColors;
		}
      }
	  //might need to separating position and color so faces can have different colors
	  //the plan: make every call of fill add a new color, have a separate uv index for the faces
      if(colorFlag) colors[ptMap.size()] = fillColor;
	  pts[ptMap.size()] = new float[] {x,y,z};
      ptMap.put(x+"_"+y+"_"+z,new Integer(ptMap.size()+1));
    }
	//wait this is silly, I'm storing all this info twice. Should just store vertex index.
    vertex[X] = x;  // note: not mx, my, mz like PGraphics3
    vertex[Y] = y;
    vertex[Z] = z;
    vertexCount++;
  }

  public void endShape(int mode) {
    //if(stroke) endShapeStroke(mode);
    //if(fill) endShapeFill(mode);
    endShapeFill(mode);
 }

  
  public void endShapeFill(int mode) {
      switch(shape) {
      case TRIANGLES:
        {
        int stop = vertexCount-2;
          for (int i = 0; i < stop; i += 3) {
            int[] f = new int[3];
            f[0] = (ptMap.get(vertices[i][X]+"_"+vertices[i][Y]+"_"+vertices[i][Z])).intValue();
            f[1] = (ptMap.get(vertices[i+1][X]+"_"+vertices[i+1][Y]+"_"+vertices[i+1][Z])).intValue();
            f[2] = (ptMap.get(vertices[i+2][X]+"_"+vertices[i+2][Y]+"_"+vertices[i+2][Z])).intValue();
            addFace(f);
          }
        }
        break;
      case TRIANGLE_STRIP:
      {
          int stop = vertexCount - 2;
          for (int i = 0; i < stop; i++) {
            // have to switch between clockwise/counter-clockwise
            // otherwise the feller is backwards and renderer won't draw
            if ((i % 2) == 0) {
              int[] f = new int[3];
              f[0] = (ptMap.get(vertices[i][X]+"_"+vertices[i][Y]+"_"+vertices[i][Z])).intValue();
              f[1] = (ptMap.get(vertices[i+2][X]+"_"+vertices[i+2][Y]+"_"+vertices[i+2][Z])).intValue();
              f[2] = (ptMap.get(vertices[i+1][X]+"_"+vertices[i+1][Y]+"_"+vertices[i+1][Z])).intValue();
              addFace(f);
            } else {
              int[] f = new int[3];
              f[0] = (ptMap.get(vertices[i][X]+"_"+vertices[i][Y]+"_"+vertices[i][Z])).intValue();
              f[1] = (ptMap.get(vertices[i+1][X]+"_"+vertices[i+1][Y]+"_"+vertices[i+1][Z])).intValue();
              f[2] = (ptMap.get(vertices[i+2][X]+"_"+vertices[i+2][Y]+"_"+vertices[i+2][Z])).intValue();
              addFace(f);
            }
          }
      }
      break;
      case POLYGON:
      {
        int[] f;
        boolean closed = vertices[0][X]!=vertices[vertexCount-1][X] || vertices[0][Y]!=vertices[vertexCount-1][Y] || vertices[0][Z]!=vertices[vertexCount-1][Z];
        if(closed) {
         f = new int[vertexCount];
        } else {
         f = new int[vertexCount-1];
        }
        int end = vertexCount;
        if(!closed) end--;
        for(int i=0;i<end;++i) {
          f[i] = (ptMap.get(vertices[i][X]+"_"+vertices[i][Y]+"_"+vertices[i][Z])).intValue();
        }
        addFace(f);
      }
      break;
      case QUADS:
      {
        int stop = vertexCount-3;
        for (int i = 0; i < stop; i += 4) {
            int[] f = new int[4];
            f[0] = (ptMap.get(vertices[i][X]+"_"+vertices[i][Y]+"_"+vertices[i][Z])).intValue();
            f[1] = (ptMap.get(vertices[i+1][X]+"_"+vertices[i+1][Y]+"_"+vertices[i+1][Z])).intValue();
            f[2] = (ptMap.get(vertices[i+2][X]+"_"+vertices[i+2][Y]+"_"+vertices[i+2][Z])).intValue();
            f[3] = (ptMap.get(vertices[i+3][X]+"_"+vertices[i+3][Y]+"_"+vertices[i+3][Z])).intValue();
            addFace(f);
        }
      }
      break;

      case QUAD_STRIP:
      {
        int stop = vertexCount-3;
        for (int i = 0; i < stop; i += 2) {
            int[] f = new int[4];
            f[0] = (ptMap.get(vertices[i][X]+"_"+vertices[i][Y]+"_"+vertices[i][Z])).intValue();
            f[1] = (ptMap.get(vertices[i+1][X]+"_"+vertices[i+1][Y]+"_"+vertices[i+1][Z])).intValue();
            f[3] = (ptMap.get(vertices[i+2][X]+"_"+vertices[i+2][Y]+"_"+vertices[i+2][Z])).intValue();
            f[2] = (ptMap.get(vertices[i+3][X]+"_"+vertices[i+3][Y]+"_"+vertices[i+3][Z])).intValue();
            addFace(f);        }
      }
      break;
      case TRIANGLE_FAN:
      {
        int stop = vertexCount - 1;
        for (int i = 1; i < stop; i++) {
          int f[] = new int[3];
            f[0] = (ptMap.get(vertices[0][X]+"_"+vertices[0][Y]+"_"+vertices[0][Z])).intValue();
            f[1] = (ptMap.get(vertices[i][X]+"_"+vertices[i][Y]+"_"+vertices[i][Z])).intValue();
            f[2] = (ptMap.get(vertices[i+1][X]+"_"+vertices[i+1][Y]+"_"+vertices[i+1][Z])).intValue();
            addFace(f);
        }
      }
      break;
    }    
  }
  
  //unused as of now
  public void endShapeStroke(int mode) {
      switch(shape) {
      case LINES:
        {
        int stop = vertexCount-1;
        for (int i = 0; i < stop; i += 2) {
              int[] l = new int[2];
              l[0] = (ptMap.get(vertices[i][X]+"_"+vertices[i][Y]+"_"+vertices[i][Z])).intValue();
              l[1] = (ptMap.get(vertices[i+1][X]+"_"+vertices[i+1][Y]+"_"+vertices[i+1][Z])).intValue();
              addLine(l);;
          }
        }
        break;
      case TRIANGLES:
        {
        int stop = vertexCount-2;
          for (int i = 0; i < stop; i += 3) {
            int[] l = new int[4];
            l[0] = (ptMap.get(vertices[i][X]+"_"+vertices[i][Y]+"_"+vertices[i][Z])).intValue();
            l[1] = (ptMap.get(vertices[i+1][X]+"_"+vertices[i+1][Y]+"_"+vertices[i+1][Z])).intValue();
            l[2] = (ptMap.get(vertices[i+2][X]+"_"+vertices[i+2][Y]+"_"+vertices[i+2][Z])).intValue();
            l[3] = l[0];
            addLine(l);;
          }
        }
      
      break;
      case POLYGON:
      {
        int[] l;
        boolean closed = mode == CLOSE && (vertices[0][X]!=vertices[vertexCount-1][X] || vertices[0][Y]!=vertices[vertexCount-1][Y] || vertices[0][Z]!=vertices[vertexCount-1][Z]);
        if(closed) {
         l = new int[vertexCount+1];
        } else {
         l = new int[vertexCount];
        }
        for(int i=0;i<vertexCount;++i) {
          l[i] = (ptMap.get(vertices[i][X]+"_"+vertices[i][Y]+"_"+vertices[i][Z])).intValue();
        }
        if(closed) l[vertexCount] = l[0];
        addLine(l);;
      }
      break;
    }
  }
  
  private void addFace(int[] f) {
   if(faceCount >= faces.length) {
    int newfaces[][] = new int[faces.length*2][];
    System.arraycopy(faces,0,newfaces,0,faces.length);
    faces = newfaces;
   }
   if(f.length == 3) numTriangles++;
   else if(f.length == 4) numQuads++;
   faces[faceCount++] = f;
  }
  
  private void addLine(int[] l) {
   if(lineCount >= lines.length) {
    int newLines[][] = new int[lines.length*2][];
    System.arraycopy(lines,0,newLines,0,lines.length);
    lines = newLines;
   }
   lines[lineCount++] = l;
  }  
  
  //taken from PGraphicsOpenGL
  @Override
  public void pushMatrix() {
    if (stackDepth == MATRIX_STACK_DEPTH) {
      throw new RuntimeException(ERROR_PUSHMATRIX_OVERFLOW);
    }
    transformMatrix.get(matrixStack[stackDepth]);
    stackDepth++;
  }

  @Override
  public void popMatrix() {
    if (stackDepth == 0) {
      throw new RuntimeException(ERROR_PUSHMATRIX_UNDERFLOW);
    }
    stackDepth--;
    transformMatrix.set(matrixStack[stackDepth]);
  }
  
  @Override
  public void translate(float tx, float ty) {
    translateImpl(tx, ty, 0);
  }

  @Override
  public void translate(float tx, float ty, float tz) {
    translateImpl(tx, ty, tz);
  }

  protected void translateImpl(float tx, float ty, float tz) {
    transformMatrix.translate(tx, ty, tz);
  }
  
  /**
   * Two dimensional rotation. Same as rotateZ (this is identical to a 3D
   * rotation along the z-axis) but included for clarity -- it'd be weird for
   * people drawing 2D graphics to be using rotateZ. And they might kick our a--
   * for the confusion.
   */
  @Override
  public void rotate(float angle) {
    rotateImpl(angle, 0, 0, 1);
  }


  @Override
  public void rotateX(float angle) {
    rotateImpl(angle, 1, 0, 0);
  }


  @Override
  public void rotateY(float angle) {
    rotateImpl(angle, 0, 1, 0);
  }


  @Override
  public void rotateZ(float angle) {
    rotateImpl(angle, 0, 0, 1);
  }


  /**
   * Rotate around an arbitrary vector, similar to glRotate(), except that it
   * takes radians (instead of degrees).
   */
  @Override
  public void rotate(float angle, float v0, float v1, float v2) {
    rotateImpl(angle, v0, v1, v2);
  }


  protected void rotateImpl(float angle, float v0, float v1, float v2) {
    float norm2 = v0 * v0 + v1 * v1 + v2 * v2;
    if (zero(norm2)) {
      // The vector is zero, cannot apply rotation.
      return;
    }

    if (diff(norm2, 1)) {
      // The rotation vector is not normalized.
      float norm = PApplet.sqrt(norm2);
      v0 /= norm;
      v1 /= norm;
      v2 /= norm;
    }

    transformMatrix.rotate(angle, v0, v1, v2);
  }
  
  /**
   * Same as scale(s, s, s).
   */
  @Override
  public void scale(float s) {
    scaleImpl(s, s, s);
  }


  /**
   * Same as scale(sx, sy, 1).
   */
  @Override
  public void scale(float sx, float sy) {
    scaleImpl(sx, sy, 1);
  }


  /**
   * Scale in three dimensions.
   */
  @Override
  public void scale(float sx, float sy, float sz) {
    scaleImpl(sx, sy, sz);
  }

  /**
   * Scale in three dimensions.
   */
  protected void scaleImpl(float sx, float sy, float sz) {
    transformMatrix.scale(sx, sy, sz);
  }
  
  @Override
  public void shearX(float angle) {
    float t = (float) Math.tan(angle);
    applyMatrixImpl(1, t, 0, 0,
                    0, 1, 0, 0,
                    0, 0, 1, 0,
                    0, 0, 0, 1);
  }


  @Override
  public void shearY(float angle) {
    float t = (float) Math.tan(angle);
    applyMatrixImpl(1, 0, 0, 0,
                    t, 1, 0, 0,
                    0, 0, 1, 0,
                    0, 0, 0, 1);
  }


  //////////////////////////////////////////////////////////////

  // MATRIX MORE!


  @Override
  public void resetMatrix() {
    transformMatrix.reset();
  }

  @Override
  public void applyMatrix(PMatrix2D source) {
    applyMatrixImpl(source.m00, source.m01, 0, source.m02,
                    source.m10, source.m11, 0, source.m12,
                             0,          0, 1, 0,
                             0,          0, 0, 1);
  }

  @Override
  public void applyMatrix(float n00, float n01, float n02,
                          float n10, float n11, float n12) {
    applyMatrixImpl(n00, n01, 0, n02,
                    n10, n11, 0, n12,
                      0,   0, 1,   0,
                      0,   0, 0,   1);
  }


  @Override
  public void applyMatrix(PMatrix3D source) {
    applyMatrixImpl(source.m00, source.m01, source.m02, source.m03,
                    source.m10, source.m11, source.m12, source.m13,
                    source.m20, source.m21, source.m22, source.m23,
                    source.m30, source.m31, source.m32, source.m33);
  }


  /**
   * Apply a 4x4 transformation matrix to the modelview stack.
   */
  @Override
  public void applyMatrix(float n00, float n01, float n02, float n03,
                          float n10, float n11, float n12, float n13,
                          float n20, float n21, float n22, float n23,
                          float n30, float n31, float n32, float n33) {
    applyMatrixImpl(n00, n01, n02, n03,
                    n10, n11, n12, n13,
                    n20, n21, n22, n23,
                    n30, n31, n32, n33);
  }


  protected void applyMatrixImpl(float n00, float n01, float n02, float n03,
                                 float n10, float n11, float n12, float n13,
                                 float n20, float n21, float n22, float n23,
                                 float n30, float n31, float n32, float n33) {
    transformMatrix.apply(n00, n01, n02, n03,
                    n10, n11, n12, n13,
                    n20, n21, n22, n23,
                    n30, n31, n32, n33);

  }
  //CAN'T USE PGL because Java permissions are silly
   /** Machine Epsilon for float precision. **/
  protected static float FLOAT_EPS = Float.MIN_VALUE;
  // Calculation of the Machine Epsilon for float precision. From:
  // http://en.wikipedia.org/wiki/Machine_epsilon#Approximation_using_Java
  static {
    float eps = 1.0f;

    do {
      eps /= 2.0f;
    } while ((float)(1.0 + (eps / 2.0)) != 1.0);

    FLOAT_EPS = eps;
  }

  protected static boolean same(float a, float b) {
    return Math.abs(a - b) < FLOAT_EPS;
  }


  protected static boolean diff(float a, float b) {
    return FLOAT_EPS <= Math.abs(a - b);
  }


  protected static boolean zero(float a) {
    return Math.abs(a) < FLOAT_EPS;
  }


  protected static boolean nonZero(float a) {
    return FLOAT_EPS <= Math.abs(a);
  }
  
  //end Matrix stuff from PGraphisOpenGL
  
  public boolean is3D() {
	return true;
  }
  
  public boolean is2D() {
	return false;
  }
  
  public void setColor(boolean c) {
	colorFlag = c;
	//else
	// showWarning("Cannot change color mode after beginDraw()");
  }
}