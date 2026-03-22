#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>
#include <Wire.h>
#include <Adafruit_ADXL345_U.h>

// WIFI
const char* ssid = "Linksys07520";
const char* password = "3112Anoorduyn";

// FIREBASE
String firebaseHost = "https://lm390-cd42d-default-rtdb.firebaseio.com";
String firebasePath = "/machines/machine_1.json";

// Accelerometer
Adafruit_ADXL345_Unified accel = Adafruit_ADXL345_Unified(123);

// Sampling
const int SAMPLE_PERIOD_MS = 20;
const int WINDOW_N = 100;

// Thresholds
const float STD_ON = 0.09;
const float STD_OFF = 0.06;

// Timing
const long ON_HOLD_MS = 10000;
const long OFF_HOLD_MS = 120000;

// Rolling window
float magBuf[WINDOW_N];
int wIdx = 0;
double sum = 0;
double sumsq = 0;
bool initialized = false;

// Machine state
bool machineOn = false;
long aboveOnSince = 0;
long belowOffSince = 0;

String currentState = "OFF";

// --- Magnitude ---
float magnitude(float x, float y, float z) {
  return sqrt(x*x + y*y + z*z);
}

// --- Initialize rolling buffer ---
void initWindow(float m) {

  for(int i=0;i<WINDOW_N;i++){
    magBuf[i] = m;
    sum += m;
    sumsq += m*m;
  }

  initialized = true;
}

// --- Compute STD ---
float computeSTD(float newMag){

  float old = magBuf[wIdx];
  magBuf[wIdx] = newMag;

  wIdx = (wIdx + 1) % WINDOW_N;

  sum += newMag - old;
  sumsq += newMag*newMag - old*old;

  float mean = sum / WINDOW_N;
  float var = (sumsq / WINDOW_N) - (mean*mean);

  if(var < 0) var = 0;

  return sqrt(var);
}

// --- Update machine state ---
void updateState(float std2s){

  long now = millis();

  bool aboveOn = std2s >= STD_ON;
  bool belowOff = std2s <= STD_OFF;

  if(!machineOn){

    if(aboveOn){
      if(aboveOnSince == 0) aboveOnSince = now;

      if(now - aboveOnSince >= ON_HOLD_MS){
        machineOn = true;
        currentState = "RUNNING";
        sendToFirebase();
      }
    }
    else{
      aboveOnSince = 0;
    }

  }else{

    if(belowOff){
      if(belowOffSince == 0) belowOffSince = now;

      if(now - belowOffSince >= OFF_HOLD_MS){
        machineOn = false;
        currentState = "AVAILABLE";
        sendToFirebase();
      }
    }
    else{
      belowOffSince = 0;
    }
  }
}

// --- Send state to Firebase ---
void sendToFirebase(){

  if(WiFi.status() != WL_CONNECTED) return;

  HTTPClient http;

  String url = firebaseHost + firebasePath;

  StaticJsonDocument<200> json;

  json["state"] = currentState;
  json["lastUpdated"] = millis();

  String payload;
  serializeJson(json,payload);

  http.begin(url);
  http.addHeader("Content-Type","application/json");

  int code = http.PUT(payload);

  Serial.print("Firebase response: ");
  Serial.println(code);

  http.end();
}

// --- Setup ---
void setup(){

  Serial.begin(115200);

  Wire.begin();

  if(!accel.begin()){
    Serial.println("ADXL345 not detected");
    while(1);
  }

  accel.setRange(ADXL345_RANGE_16_G);

  WiFi.begin(ssid,password);

  Serial.print("Connecting WiFi");

  while(WiFi.status()!=WL_CONNECTED){
    delay(500);
    Serial.print(".");
  }

  Serial.println("Connected!");

  currentState = "OFF";
  sendToFirebase();
}

// --- Loop ---
void loop(){

  sensors_event_t event;
  accel.getEvent(&event);

  float mag = magnitude(event.acceleration.x,
                        event.acceleration.y,
                        event.acceleration.z);

  if(!initialized) initWindow(mag);

  float std2s = computeSTD(mag);

  updateState(std2s);

  Serial.print("STD: ");
  Serial.println(std2s);

  delay(SAMPLE_PERIOD_MS);
}