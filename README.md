# Alarm Detector Monitor

Android application developed in Java for monitoring and managing Alarm Detector systems using Firebase Realtime Database and ESP32-based IoT devices.

---

## Overview

Alarm Detector Monitor is a cloud-connected monitoring solution designed to track alarm detector systems in real time.

The application communicates with Firebase Realtime Database to receive live updates from ESP32 devices, monitor sensor activity, display alarm status, visualize LED and buzzer states, and provide centralized remote monitoring capabilities.

The system is suitable for industrial monitoring, fire alarm systems, security monitoring, equipment safety systems, and IoT-based alert solutions.

---

## Features

### Real-Time Alarm Monitoring

* Live alarm status updates
* Real-time sensor monitoring
* Device activity tracking
* Instant cloud synchronization

### Firebase Realtime Database Integration

* Real-time cloud data synchronization
* Live status updates
* Centralized monitoring
* Remote device visibility

### Device Status Monitoring

* ESP32 online/offline status
* Sensor state tracking
* System health monitoring
* Device activity dashboard

### Buzzer Status Monitoring

* Buzzer ON/OFF indication
* Real-time buzzer updates
* Alarm event visualization

### LED Status Monitoring

* LED state visualization
* Active and inactive indicators
* Live device feedback

### Dashboard Interface

* Real-time monitoring dashboard
* Status indicators
* Device information display
* Instant data refresh

### Local Configuration Storage

* SharedPreferences integration
* Persistent application settings
* Automatic configuration restoration

---

## Technology Stack

### Frontend

* Android XML Layouts
* Material Design Components

### Backend Logic

* Java

### Cloud Backend

* Firebase Realtime Database

### Authentication

* Firebase Authentication (if enabled)

### Local Storage

* SharedPreferences

### IoT Device

* ESP32

---

## Architecture

```text
ESP32 Device
      │
      ▼
Firebase Realtime Database
      │
      ▼
Android Application
      │
      ▼
Live Monitoring Dashboard
```

---

## Project Structure

```text
app/
├── src/main/java
│   ├── MainActivity.java
│   ├── SettingsActivity.java
│   ├── DetectorData.java
│   ├── FirebaseManager.java
│   └── Utility Classes
│
├── src/main/res
│   ├── layout
│   ├── drawable
│   ├── anim
│   ├── values
│   └── mipmap
│
├── google-services.json
├── AndroidManifest.xml
└── build.gradle.kts
```

---

## Requirements

* Android Studio
* Android SDK 24+
* Java 8+
* Firebase Project
* Firebase Realtime Database
* ESP32 Device
* Internet Connection

---

## Installation

### Clone Repository

```bash
git clone <repository-url>
```

### Configure Firebase

1. Create a Firebase Project
2. Enable Realtime Database
3. Download google-services.json
4. Place the file inside the app module
5. Sync Gradle files

### Build Project

```bash
./gradlew build
```

### Run Application

1. Connect an Android device or emulator
2. Launch the application
3. Verify Firebase connectivity

---

## Usage

### Step 1

Launch the application.

### Step 2

Connect to the internet.

### Step 3

Allow Firebase synchronization.

### Step 4

Monitor alarm detector status.

### Step 5

Observe sensor updates in real time.

### Step 6

Monitor buzzer and LED states.

### Step 7

Review alarm events through the dashboard.

---

## Permissions Used

* INTERNET
* ACCESS_NETWORK_STATE

---

## Key Functionalities

* Firebase Realtime Database integration
* Real-time cloud synchronization
* Alarm monitoring
* Sensor status tracking
* Buzzer monitoring
* LED monitoring
* ESP32 integration
* Remote device visibility

---

## Use Cases

* Fire Alarm Monitoring
* Industrial Safety Systems
* Security Monitoring
* Smart Building Solutions
* Equipment Protection Systems
* IoT Alert Management

---

## Future Enhancements

* Push Notifications
* Firebase Cloud Messaging (FCM)
* Event History Logs
* Multi-Device Monitoring
* Analytics Dashboard
* Cloud-Based Reports

---

## Security Notes

* Never commit production Firebase credentials.
* Configure Firebase Security Rules properly.
* Restrict database access using authentication.
* Secure ESP32 device communication.

---

## Author

Developed for Alarm Detector Monitoring and Alert Management System.
