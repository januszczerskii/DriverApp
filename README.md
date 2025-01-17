# DriverApp

## Overview
DriverApp is a mobile application designed to assist drivers with real-time road analysis. It utilizes advanced object detection models to ensure safer and more informed driving experiences.

### Aim of the App
DriverApp focuses on:
- **Speed Limit Detection**: Identifies road speed limit signs and updates the app interface accordingly.
- **Lane Assist**: Detects lane markings and alerts the driver when drifting out of the lane.
- **Proximity Alerts**: Recognizes vehicles and pedestrians in proximity to prevent accidents.
- **Object Identification**: Differentiates between vehicles, pedestrians, and other road features.

### Features
- **Real-Time Analysis**: Provides live feedback using a phone's camera feed.
- **Customizable Alerts**: Includes adjustable settings for proximity and lane alerts.
- **On-Device Processing**: Utilizes the YOLOv5s model for local, offline processing of camera feeds.

### User Interface (UI)
- **Intuitive Interface**:
  - Title with logo at the top.
  - Four dynamic icons changing shape and color based on detections.
  - Mute button in the bottom-left corner.
  - Settings button in the bottom-right corner.
  - Some checkboxes in the settings drawer are currently propositions and are not yet implemented.

## UI Preview

### Main Screen
![driverappmain](https://github.com/user-attachments/assets/6065258a-f6cc-4c4d-8d21-9f0efc6ae218)

### Main Screen with Active Alerts
![DRIVERAPP_ALERTS_ON](https://github.com/user-attachments/assets/8d0af1b4-dc83-4b96-963c-007ff2a0b13d)

### Settings Screen
![driverappsettings](https://github.com/user-attachments/assets/20be2577-219f-4c49-bc79-1f38910191ff)

## Contributing
We welcome contributions to improve DriverApp! Feel free to fork the repository, make changes, and submit a pull request.

## License
This project is licensed under the MIT License. Refer to the LICENSE file for more details.

