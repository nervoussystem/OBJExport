import nervoussystem.obj.*;

boolean record = false;

void setup() {
  size(400,400,P3D);
}

void draw() {
  background(255);
  
  if(record) {
    OBJExport obj = (OBJExport) createGraphics(10,10,"nervoussystem.obj.OBJExport","cube.obj");
    obj.setColor(true);
    obj.beginDraw();
    obj.noFill();
    drawColorCube(obj);
    obj.endDraw();
    obj.dispose();
    record =  false;
  }
  translate(width/2,height/2);
  scale(20);
  rotateX(PI/6.0);
  rotateY(PI/120.0*frameCount);
  drawColorCube(this.g);
}

void drawColorCube(PGraphics pg) {
  pg.beginShape(QUADS);
  //top
  pg.fill(255,0,0);
  pg.vertex(-1,-1,1);
  pg.vertex(1,-1,1);
  pg.vertex(1,1,1);
  pg.vertex(-1,1,1);
  //bottom
  pg.fill(0,255,0);
  pg.vertex(-1,-1,-1);
  pg.vertex(-1,1,-1);
  pg.vertex(1,1,-1);
  pg.vertex(1,-1,-1);
  //right
  pg.fill(0,0,255);
  pg.vertex(1,1,1);
  pg.vertex(1,-1,1);
  pg.vertex(1,-1,-1);
  pg.vertex(1,1,-1);
  //left
  pg.fill(0,255,255);
  pg.vertex(-1,1,1);
  pg.vertex(-1,1,-1);
  pg.vertex(-1,-1,-1);
  pg.vertex(-1,-1,1);
  pg.endShape();
  //front
  pg.fill(255,0,255);
  pg.vertex(1,1,1);
  pg.vertex(1,1,-1);
  pg.vertex(-1,1,-1);
  pg.vertex(-1,1,1);
  //back
  pg.fill(255,255,0);
  pg.vertex(1,-1,1);
  pg.vertex(-1,-1,1);
  pg.vertex(-1,-1,-1);
  pg.vertex(1,-1,-1);
  
  pg.endShape();
}

void keyPressed() {
  if(key == 'r') record = true;
}
