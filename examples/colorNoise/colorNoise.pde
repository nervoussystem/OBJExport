import nervoussystem.obj.*;

boolean record = false;
float noiseScale = .05;
void setup() {
  size(400,400,P3D);
}

void draw() {
  background(255);
  
  if(record) {
    //export an x3d file, change to OBJExport for obj
    MeshExport x3D = (MeshExport) createGraphics(10,10,"nervoussystem.obj.X3DExport","colored.x3d");
    x3D.setColor(true);
    x3D.beginDraw();
    drawNoise(x3D);
    x3D.endDraw();
    x3D.dispose();
    record = false;
  }
  noStroke();
  //rotateX(PI/3.0);
  translate(width/2,height/2);
  rotateX(PI/6.0);
  rotateZ(frameCount*PI/360.0);
  translate(-125,-125,30);
  scale(5);
  drawNoise(this.g);
}

void drawNoise(PGraphics pg) {
  pg.beginShape(TRIANGLES);
  for(int i=0;i<50;i++) {
    for(int j=0;j<50;j++) {
      float z = noise(i*noiseScale,j*noiseScale);
      pg.fill( lerpColor( color(255,0,0),color(0,0,255),z ));
      pg.vertex(i,j,z*50);
      z = noise((i+1)*noiseScale,j*noiseScale);
      pg.fill( lerpColor( color(255,0,0),color(0,0,255),z ));
      pg.vertex(i+1,j,z*50);
      z = noise((i+1)*noiseScale,(j+1)*noiseScale);
      pg.fill( lerpColor( color(255,0,0),color(0,0,255),z ));
      pg.vertex(i+1,j+1,z*50);

      z = noise((i+1)*noiseScale,(j+1)*noiseScale);
      pg.fill( lerpColor( color(255,0,0),color(0,0,255),z ));
      pg.vertex(i+1,j+1,z*50);
      z = noise(i*noiseScale,(j+1)*noiseScale);
      pg.fill( lerpColor( color(255,0,0),color(0,0,255),z ));
      pg.vertex(i,j+1,z*50);
      z = noise(i*noiseScale,j*noiseScale);
      pg.fill( lerpColor( color(255,0,0),color(0,0,255),z ));
      pg.vertex(i,j,z*50);
    }
  }
  pg.endShape();  
}
void keyPressed()
{
  if (key == 'r') {
    record = true;
  }
}
