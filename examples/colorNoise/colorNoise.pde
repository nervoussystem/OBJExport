import nervoussystem.obj.*;

boolean record = false;
float noiseScale = .05;
void setup() {
  size(400,400,P3D);
}

void draw() {
  background(255);
  
  if(record) {
    OBJExport obj = (OBJExport) createGraphics(10,10,"nervoussystem.obj.OBJExport","colored.obj");
    obj.setColor(true);
    obj.beginDraw();
    drawNoise(obj);
    obj.endDraw();
    obj.dispose();
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
  for(int i=0;i<300;i++) {
    for(int j=0;j<300;j++) {
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
