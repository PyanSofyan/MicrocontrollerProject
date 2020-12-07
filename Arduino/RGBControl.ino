#include <ArduinoWebsockets.h>
#include <ESP8266WiFi.h>

const char* ssid = "My Home";
const char* password = "P@@5W0RD"; 
const char* websockets_server = "wss://codelab-api.herokuapp.com";

using namespace websockets;

WebsocketsClient client;
boolean reconnect = false;
int LED = 4;
int LED_R = 0;
int LED_G = 5;
int LED_B = 16;

int v = 255;
int red = 255;
int green = 0;
int blue = 0;

void onMessageCallback(WebsocketsMessage message) {
    Serial.print("Got Message: ");
    Serial.println(message.data());
    if(message.data() == "ON"){
      if(!digitalRead(LED)){
        digitalWrite(LED,HIGH);
        delay(1000);
      }
    }
    else if(message.data() == "OFF"){
      if(digitalRead(LED)){
        digitalWrite(LED,LOW);
        delay(1000);
      }
    }
    else if(message.data() == "STATUS"){
       sendLEDStatus();
       delay(1000);
    }
    else if(message.data().startsWith("Color")){
       Serial.println(message.data());
       String color = splitValue(message.data(),':',1);
       Serial.println(color);
       red = splitValue(color,',',0).toInt();
       green = splitValue(color,',',1).toInt();
       blue = splitValue(color,',',2).toInt();
      
    }
}

void onEventsCallback(WebsocketsEvent event, String data) {
    if(event == WebsocketsEvent::ConnectionOpened) {
        Serial.println("Connnection Opened");
    } else if(event == WebsocketsEvent::ConnectionClosed) {
        Serial.println("Connnection Closed");
    } else if(event == WebsocketsEvent::GotPing) {
        Serial.println("Got a Ping!");
    } else if(event == WebsocketsEvent::GotPong) {
        Serial.println("Got a Pong!");
    }
}

void reConnect(){
      bool connected = client.connect(websockets_server);
      if(connected) {
          Serial.println("Reconnected!");
          client.send("STATUS");
      } else {
          Serial.println("Not Connected!");
      } 
}

void sendLEDStatus(){
  if(digitalRead(LED)){
    client.send("ON");
  }
  else{
    client.send("OFF");
  }
}

void setColor()
{
   digitalWrite(LED_R, HIGH);
   analogWrite(LED_R, v - red);
   digitalWrite(LED_G, HIGH);
   analogWrite(LED_G, v - green);
   digitalWrite(LED_B, HIGH);
   analogWrite(LED_B, v - blue);
}

String splitValue(String data, char separator, int index)
{
  int found = 0;
  int strIndex[] = {0, -1};
  int maxIndex = data.length()-1;

  for(int i=0; i<=maxIndex && found<=index; i++){
    if(data.charAt(i)==separator || i==maxIndex){
        found++;
        strIndex[0] = strIndex[1]+1;
        strIndex[1] = (i == maxIndex) ? i+1 : i;
    }
  }

  return found>index ? data.substring(strIndex[0], strIndex[1]) : "";
}

void setup() {
    Serial.begin(115200);
 
    pinMode(LED, OUTPUT);
    pinMode(LED_R, OUTPUT);
    pinMode(LED_G, OUTPUT);
    pinMode(LED_B, OUTPUT);
    
    digitalWrite(LED, HIGH);
    digitalWrite(LED_R, LOW);
    digitalWrite(LED_G, HIGH);
    digitalWrite(LED_B, HIGH);

    Serial.print("Wifi connecting to ");
    Serial.println(ssid);
 
    WiFi.begin(ssid, password);

    for(int i = 0; i < 10 && WiFi.status() != WL_CONNECTED; i++) {
        Serial.print(".");
        delay(1000);
    }
    Serial.println();

    if(WiFi.status() != WL_CONNECTED) {
        Serial.println("No Wifi!");
        return;
    }
    Serial.println();
    Serial.println("Connected to Wifi, Connecting to server.");

    client.onMessage(onMessageCallback);
    client.onEvent(onEventsCallback);
    bool connected = client.connect(websockets_server);
    if(connected) {
        Serial.println("Connecetd!");
        client.send("STATUS");
    } else {
        Serial.println("Not Connected!");
    }     
    
}

void loop() {
    setColor();
   
    if(client.available()) {
        client.poll();
    }
}