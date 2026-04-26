🚗 **RIDEO – Integrated Campus Ride-Sharing Ecosystem**

RIDEO is a smart Android application designed to solve commuting problems within college campuses. It follows a Peer-to-Peer ride-sharing model, allowing students and staff to share rides safely within their campus community.

---

## 📌 Problem Statement
College campuses often face:
- Limited parking space
- Public transport timing mismatch
- Underutilized private vehicles

RIDEO addresses these issues by utilizing existing vehicles within the campus.

## 💡 Solution
RIDEO provides a secure ride-sharing platform where:
- **Drivers** can offer rides
- **Passengers** can search and book rides
- Access is restricted to verified campus users

---

## 🚀 Features
### 🔐 User Management & Security
- University Email / ID Verification
- Profile Management (user + vehicle details)

### 🚖 Driver Module
- Route Creation (source → destination → stops)
- Seat Management
- Ride Scheduling

### 🧍 Passenger Module
- Live Ride Search
- Filtering (time, destination)
- Instant Ride Request

### 🎨 UI/UX
- Material Design Interface
- Real-time Notifications

---

## 🛠 Tech Stack
| Component    | Technology                |
|--------------|---------------------------|
| Language     | Kotlin / Java             |
| UI           | XML (Material Components) |
| IDE          | Android Studio            |
| Backend      | Firebase                  |
| Build Tool   | Gradle                    |
| Architecture | MVVM / MVC                |

---

## 🏗 Architecture
- **UI Layer**: Activities & Fragments
- **Adapter Layer**: RecyclerView Adapters
- **Model Layer**: Ride, User, Location data classes
- **Firebase Backend**: Authentication + Database

---

## 📂 Project Structure

```
rideo/
├── app/
│   └── src/
│       └── main/
│           ├── java/com/example/rideo/
│           │   ├── activities
│           │   └── models
│           └── res/
│               ├── layout/
│               └── drawable/
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew
├── gradlew.bat
```

---

## 🔮 Future Enhancements
- In-app chat
- Google Maps integration
- Reward system
- Emergency SOS feature

---

## ⚙️ Setup
1. Clone the repository:
	```sh
	git clone https://github.com/iaditya-DA/rideo.git
	```
2. Open in **Android Studio**
3. Let Gradle sync
4. Connect device / start emulator
5. Click **Run**

---

## 🤝 Contributing
Contributions are welcome! Please open issues or pull requests for suggestions and improvements.

---

## 👨‍💻 Developer
**Aditya Kumar Jha**  
[GitHub: iaditya-DA](https://github.com/iaditya-DA)
