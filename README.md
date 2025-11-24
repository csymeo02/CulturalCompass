# CulturalCompass

A mobile app that helps travelers discover nearby cultural attractions in real time using Google Maps, Places API, Firebase, and an AI assistant.

---

## 🚀 How to Run

1. Open the project in **Android Studio**.  
2. Add your API keys in: app/src/main/res/values/strings.xml  
    - places_api_key  
    - gemini_api_key
3. Use a device or emulator running **Android 8.0+**.  
4. Wait for Gradle to finish syncing, then press **Run ▶**.

---

## 📱 How to Use

### 1. Authentication
- Create an account or log in using **Firebase Authentication**.  
- The app automatically restores your session on future launches.

### 2. Map Screen
- Displays your **current location** on Google Maps.  
- Automatically fetches **nearby attractions** using the Places API.  
- Search locations using the autocomplete bar.  
- Tap any attraction to open its details.

### 3. Description Page
- Shows image, rating, reviews, distance, and type.  
- Includes an **AI-generated description** (Gemini API).  
- Add/remove the place from **Favorites**.

### 4. Favorites Page
- Displays all attractions saved in Firestore.  
- Tap a favorite to view its full details again.

### 5. AI Assistant
- Chat-based interface for attraction explanations and general travel help.  
- Powered by the Gemini API.

### 6. Settings
- View or edit personal profile information.  
- Access the About page.  
- Logout.

---

