🚗 RIDEO – Integrated Campus Ride-Sharing Ecosystem

RIDEO is a smart Android application designed to solve commuting problems within college campuses.
It follows a Peer-to-Peer ride-sharing model, allowing students and staff to share rides safely within their campus community.

📌 Problem Statement

College campuses often face:

Limited parking space
Public transport timing mismatch
Underutilized private vehicles

RIDEO addresses these issues by using existing vehicles within the campus.

💡 Solution

RIDEO provides a secure ride-sharing platform where:

Drivers can offer rides
Passengers can search and book rides
Access is restricted to verified campus users
🚀 Features
🔐 User Management & Security
University Email / ID Verification
Profile Management (user + vehicle details)
🚖 Driver Module
Route Creation (source → destination → stops)
Seat Management
Ride Scheduling
🧍 Passenger Module
Live Ride Search
Filtering (time, destination)
Instant Ride Request
🎨 UI/UX
Material Design Interface
Real-time Notifications
🛠 Tech Stack
Component	Technology
Language	Kotlin / Java
UI	XML (Material Components)
IDE	Android Studio
Backend	Firebase
Build Tool	Gradle
Architecture	MVVM / MVC
🏗 Architecture
UI Layer → Activities & Fragments
Adapter Layer → RecyclerView Adapters
Model Layer → Ride, User, Location data classes
Firebase Backend → Authentication + Database
📂 Project Structure


rideo/
│── app/
│── gradle/
│── build.gradle.kts
│── settings.gradle.kts
│── gradlew
│── gradlew.bat
Inside app/
com.example.rideo
│── activities
│── models

res/
│── layout
│── drawable

🔮 Future Enhancements
In-app chat
Google Maps integration
Reward system
Emergency SOS feature
⚙️ Setup
git clone https://github.com/iaditya-DA/rideo.git
Run Project
Open in Android Studio
Let Gradle sync
Connect device / start emulator

Click Run
👨‍💻 Developer

Aditya Kumar Jha
GitHub: https://github.com/iaditya-DA
