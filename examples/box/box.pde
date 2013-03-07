import nervoussystem.obj.*;

boolean record;

void setup() {
  size(400,400,P3D);
  smooth();
}

void draw() {
  background(0);
  
  if (record) {
    println("being record");
    beginRecord("nervoussystem.obj.OBJExport", "filename2.obj");
  }
  
  fill(255);
  box(100,100,100);
  translate(width/2, height/2);
  box(100,100,100);
  
  if (record) {
    endRecord();
    record = false;
    println("end record");
  }
}

void keyPressed()
{
  if (key == 'r') {
    record = true;
  }
}
