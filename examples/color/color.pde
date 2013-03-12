import nervoussystem.obj.*;

boolean record;

void setup() {
  size(400,400,P3D);
  noLoop();
}

void draw() {
  background(0);
  
  OBJExport obj = (OBJExport) createGraphics(10,10,"nervoussystem.obj.OBJExport","colored.obj");
  obj.setColor(true);
  obj.beginDraw();
  
  obj.beginShape(QUAD_STRIP);
  for(int i=0;i<20;++i) {
    obj.fill( lerpColor( color(255,0,0),color(0,0,255),i*1.0/19.0 ));
    float z = random(40);
    obj.vertex(i*10,20,z);
    obj.vertex(i*10,40,z);
  }
  obj.endShape();
  
  obj.endDraw();
  obj.dispose();
  exit();
}

void keyPressed()
{
  if (key == 'r') {
    record = true;
  }
}
