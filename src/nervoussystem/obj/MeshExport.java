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

import java.io.*;
import java.util.HashMap;
import processing.core.*;
import processing.opengl.*;

public class MeshExport extends PGraphics {
  File file;
  String filenameSimple;
  PrintWriter writer;
  float[][] pts;
  int[][] lines;
  int[][] faces;
  int[][] faceColors;
  int[] colors;
  int lineCount;
  int faceCount;
  int objectMode = 0;
  HashMap<String,Integer> ptMap;
  
  static protected final int MATRIX_STACK_DEPTH = 32;
  int DEFAULT_VERTICES = 4096;
  int VERTEX_FIELD_COUNT = 3;
  int vertices[];
  
  int numTriangles = 0;
  int numQuads = 0;
  int ptCount = 0;
  static public int TRIANGLE_RES = 7;
  static public int RECT_RES = TRIANGLE_RES+5;
  
  boolean drawBegan = false;
  //make transform function work
  protected int stackDepth;
  protected float[][] matrixStack = new float[MATRIX_STACK_DEPTH][16];
  PMatrix3D transformMatrix;
  
  //color
  boolean colorFlag = false;
  private static PGraphics textureG = null;
  private static PImage texture = null;
  
  public MeshExport() {
    vertices = new int[DEFAULT_VERTICES];
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
	ptMap.clear();
	if(texture != null) {
	}
  }

  public boolean displayable() {
    return false;  // just in case someone wants to use this on its own
  }

  public void beginDraw() {
    // have to create file object here, because the name isn't yet
    // available in allocate()
	defaultSettings();
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
	  if(colorFlag) {
		colors = new int[4096];
		faceColors = new int[4096][];
	  }
    }
    lineCount = 0;
    faceCount = 0;
    vertexCount = 0;
	ptCount = 0;
    numTriangles = 0;
    numQuads = 0;
  }

  public void endDraw() {
    //write vertices and initialize ptMap
	writeHeader();
	if(colorFlag) {
		colorExport();
	}
    writeVertices();
    writeLines();
    writeFaces();
	writeFooter();
	drawBegan = false;
  }
    
  private void colorExport() {
	int numRects = PApplet.ceil(numTriangles/2.0f) + numQuads;
    int textureSize = PApplet.ceil(PApplet.sqrt(numRects))*RECT_RES;
	
	if(textureSize > 1024) {
		showWarning("Generating texture... this might take a while");
		textureSize = PApplet.ceil(textureSize/1024.0f)*1024;
	}
	if(texture == null) {
		texture = parent.createImage(10,10,PApplet.RGB);
		//textureG = parent.createGraphics(PApplet.min(textureSize,1024), PApplet.min(textureSize,1024), P2D);
		textureG = parent.createGraphics(1024,1024, P2D);
	}
	//need to make a separate image in case the texture is too big for openGL
	texture.resize(textureSize,textureSize);
	//textureG.setSize(PApplet.min(textureSize,1024), PApplet.min(textureSize,1024));
	//writeTextureCoords();
	generateTexture();
  }
      
  private void generateTexture() {
    textureG.beginDraw();
	textureG.background(0);
	int c;
	int currX = 0, currY = 0;
	int[] f;
	int[] fc;
	boolean upper = true;
	textureG.strokeWeight(3);
	int cIndex = 0;
	int gX = 0;
	int gY = 0;
	for(int i=0;i<faceCount;++i) {
		f = faces[i];
		fc = faceColors[i];
		if(f.length > 4) showWarning("Faces with more than 4 sides cannot be exported with color");
		else if(f.length == 3) {
			//draw triangle
			textureG.strokeWeight(3);
			if(upper) {
				textureG.beginShape();
				c = fc[0];
				textureG.fill(c);
				textureG.stroke(c);
				textureG.vertex(currX+1+.5f,currY+1+.5f);
				writeTexCoord((gX+currX+1f+.5f)/texture.width, (1.0f-(gY+currY+1f+.5f)/texture.height));
				c = fc[1];
				textureG.fill(c);
				textureG.stroke(c);
				textureG.vertex(currX+1+TRIANGLE_RES+.5f,currY+1+.5f);
				writeTexCoord((gX+currX+1f+TRIANGLE_RES+.5f)/texture.width , (1.0f-(gY+currY+1f+.5f)/texture.height));
				c = fc[2];
				textureG.fill(c);
				textureG.stroke(c);
				textureG.vertex(currX+1+.5f,currY+TRIANGLE_RES+1+.5f);
				writeTexCoord((gX+currX+1f+.5f)/texture.width , (1.0f-(gY+currY+TRIANGLE_RES+1f+.5f)/texture.height));				
				textureG.endShape(CLOSE);
			} else {
				textureG.beginShape();
				c = fc[0];
				textureG.fill(c);
				textureG.stroke(c);
				textureG.vertex(currX+3+TRIANGLE_RES+.5f,currY+3+.5f);
				writeTexCoord((gX+currX+3f+TRIANGLE_RES+.5f)/texture.width , (1.0f-(gY+currY+3f+.5f)/texture.height));
				c = fc[1];
				textureG.fill(c);
				textureG.stroke(c);
				textureG.vertex(currX+TRIANGLE_RES+3+.5f,currY+TRIANGLE_RES+3+.5f);
				writeTexCoord((gX+currX+TRIANGLE_RES+3f+.5f)/texture.width , (1.0f-(gY+currY+TRIANGLE_RES+3f+.5f)/texture.height));
				c = fc[2];
				textureG.fill(c);
				textureG.stroke(c);
				textureG.vertex(currX+3+.5f,currY+TRIANGLE_RES+3+.5f);
				writeTexCoord((gX+currX+3f+.5f)/texture.width , (1.0f-(gY+currY+TRIANGLE_RES+3f+.5f)/texture.height));
				textureG.endShape(CLOSE);
				currX += RECT_RES;
			}
			upper = !upper;
		} else if(f.length == 4) {
			textureG.strokeWeight(4);
			textureG.beginShape();
			c = fc[0];
			textureG.fill(c);
			textureG.stroke(c);
			textureG.vertex(currX+1+.5f,currY+1+.5f);
			writeTexCoord((gX+currX+1f+.5f)/texture.width , (1.0f-(gY+currY+1f+.5f)/texture.height));
			c = fc[1];
			textureG.fill(c);
			textureG.stroke(c);
			textureG.vertex(currX+1+TRIANGLE_RES+.5f,currY+1+.5f);
			writeTexCoord((gX+currX+1f+TRIANGLE_RES+.5f)/texture.width , (1.0f-(gY+currY+1f+.5f)/texture.height));
			c = fc[2];
			textureG.fill(c);
			textureG.stroke(c);
			textureG.vertex(currX+TRIANGLE_RES+1+.5f,currY+TRIANGLE_RES+1+.5f);
			writeTexCoord((gX+currX+TRIANGLE_RES+1f+.5f)/texture.width, (1.0f-(gY+currY+TRIANGLE_RES+1f+.5f)/texture.height));
			c = fc[3];
			textureG.fill(c);
			textureG.stroke(c);
			textureG.vertex(currX+1+.5f,currY+TRIANGLE_RES+1+.5f);
			writeTexCoord((gX+currX+1f+.5f)/texture.width, (1.0f-(gY+currY+TRIANGLE_RES+1f+.5f)/texture.height));				
			textureG.endShape(CLOSE);
			currX += RECT_RES;
			upper = true;
		}
		if(currX+RECT_RES > textureG.width) {
			currX = 0;
			currY += RECT_RES;
			
			if(currY+RECT_RES > textureG.height) {
				//copy to texture
				//textureG.endDraw();
				int copyW = PApplet.min(texture.width-gX, textureG.width);
				int copyH = PApplet.min(texture.height-gY, textureG.height);
				texture.copy(textureG,0,0,copyW,copyH,gX,gY,copyW,copyH);
				//textureG.beginDraw();
				textureG.background(0);
				currX = 0;
				currY = 0;
				gX += textureG.width;
				if(gX >= texture.width) {
					gY += textureG.height;
					gX = 0;
				}
			}
		}
	}
	int copyW = PApplet.min(texture.width-gX, textureG.width);
	int copyH = PApplet.min(texture.height-gY, textureG.height);
	texture.copy(textureG,0,0,copyW,copyH,gX,gY,copyW,copyH);
	
	textureG.endDraw();
	
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
  
  float[] vertex = new float[3];
  public void vertex(float x, float y,float z) {	
    if(vertexCount >= vertices.length) {
		int newVertices[] = new int[vertices.length*2];
		System.arraycopy(vertices,0,newVertices,0,vertices.length);
		vertices = newVertices;
    }
   // float vertex[] = vertices[vertexCount];
    vertex[X] = x;
    vertex[Y] = y;
    vertex[Z] = z;
	transformMatrix.mult(vertex, tempVertex);
	x = tempVertex[0];
	y = tempVertex[1];
	z = tempVertex[2];
	//does not account for floating point error or tolerance
    if(!ptMap.containsKey(x+"_"+y+"_"+z)) {
      if(ptCount >= pts.length) {
		float newPts[][] = new float[pts.length*2][];
		System.arraycopy(pts,0,newPts,0,pts.length);
		pts = newPts;
      }
	  //might need to separating position and color so faces can have different colors
	  //the plan: make every call of fill add a new color, have a separate uv index for the faces
	  pts[ptCount] = new float[] {x,y,z};
	  ptCount++;
	  ptMap.put(x+"_"+y+"_"+z,new Integer(ptMap.size()+1));
	  vertices[vertexCount] = ptCount;
    } else {
		vertices[vertexCount] = ptMap.get(x+"_"+y+"_"+z).intValue();
	}
	//color
	if(colorFlag) {
		if(vertexCount >= colors.length) {
			int newColors[] = new int[colors.length*2];
			System.arraycopy(colors,0,newColors,0,colors.length);
			colors = newColors;
		}
		colors[vertexCount] = fillColor;
	}

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
            f[0] = vertices[i];
            f[1] = vertices[i+1];
            f[2] = vertices[i+2];
            addFace(f);
			if(colorFlag) {
				int[] fc = new int[3];
				fc[0] = colors[i];
				fc[1] = colors[i+1];
				fc[2] = colors[i+2];
				addFaceColor(fc,faceCount-1);
			}
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
              f[0] = vertices[i];
              f[1] = vertices[i+2];
              f[2] = vertices[i+1];
              addFace(f);
			  if(colorFlag) {
				int[] fc = new int[3];
				fc[0] = colors[i];
				fc[1] = colors[i+2];
				fc[2] = colors[i+1];
				addFaceColor(fc,faceCount-1);
			  }
            } else {
              int[] f = new int[3];
              f[0] = vertices[i];
              f[1] = vertices[i+1];
              f[2] = vertices[i+2];
              addFace(f);
			  if(colorFlag) {
				int[] fc = new int[3];
				fc[0] = colors[i];
				fc[1] = colors[i+1];
				fc[2] = colors[i+2];
				addFaceColor(fc, faceCount-1);
			  }
            }
          }
      }
      break;
      case POLYGON:
      {
        int[] f;
        boolean closed = vertices[0]!=vertices[vertexCount-1];
        if(closed) {
         f = new int[vertexCount];
        } else {
         f = new int[vertexCount-1];
        }
        int end = vertexCount;
        if(!closed) end--;
        for(int i=0;i<end;++i) {
          f[i] = vertices[i];
        }
        addFace(f);
		if(colorFlag) {
			if(end <= 4) {
				int[] fc = new int[end];
				for(int i=0;i<end;++i) {
				  fc[i] = colors[i];
				}
				addFaceColor(fc,faceCount-1);
			} else {
				//dummy so faces and faceColors match (stupid)
				addFaceColor(new int[0],faceCount-1);
			}
		}
      }
      break;
      case QUADS:
      {
        int stop = vertexCount-3;
        for (int i = 0; i < stop; i += 4) {
            int[] f = new int[4];
            f[0] = vertices[i];
            f[1] = vertices[i+1];
            f[2] = vertices[i+2];
            f[3] = vertices[i+3];
            addFace(f);
			if(colorFlag) {
				int[] fc = new int[4];
				fc[0] = colors[i];
				fc[1] = colors[i+1];
				fc[2] = colors[i+2];
				fc[3] = colors[i+3];
				addFaceColor(fc,faceCount-1);
			}
        }
      }
      break;

      case QUAD_STRIP:
      {
        int stop = vertexCount-3;
        for (int i = 0; i < stop; i += 2) {
            int[] f = new int[4];
            f[0] = vertices[i];
            f[1] = vertices[i+1];
            f[3] = vertices[i+2];
            f[2] = vertices[i+3];
            addFace(f);        
			if(colorFlag) {
				int[] fc = new int[4];
				fc[0] = colors[i];
				fc[1] = colors[i+1];
				fc[2] = colors[i+2];
				fc[3] = colors[i+3];
				addFaceColor(fc,faceCount-1);
			}
		}
      }
      break;
      case TRIANGLE_FAN:
      {
        int stop = vertexCount - 1;
        for (int i = 1; i < stop; i++) {
			int f[] = new int[3];
			f[0] = vertices[0];
			f[1] = vertices[i];
			f[2] = vertices[i+1];
			addFace(f);
			if(colorFlag) {
				int[] fc = new int[3];
				fc[0] = colors[0];
				fc[1] = colors[i];
				fc[2] = colors[i+1];
				addFaceColor(fc,faceCount-1);
			}
		}
      }
      break;
    }
  }
  
  //unused as of this version
  public void endShapeStroke(int mode) {
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
  
  private void addFaceColor(int[] f, int pos) {
   if(pos >= faceColors.length) {
    int newfaces[][] = new int[faceColors.length*2][];
    System.arraycopy(faceColors,0,newfaces,0,faceColors.length);
    faceColors = newfaces;
   }
   faceColors[pos] = f;
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
  
  public void setTriangleRes(int res) {
	TRIANGLE_RES = res;
	RECT_RES = res+5;
  }
  
  //override me
  protected void writeTexCoord(float u, float v) {
  
  }
  
  //override me
  protected void writeHeader() {
  }

  //override me
  protected void writeFooter() {
  }
  
  //override me
  protected void writeVertices() {
  }
  
  //override me
  protected void writeLines() {
  }

  //override me
  protected void writeFaces() {
  }
    
}
