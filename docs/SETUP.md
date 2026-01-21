# Setup Guide

## Backend Setup

1. Navigate to backend directory:
```bash
cd backend
```

2. Create virtual environment:
```bash
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
```

3. Install dependencies:
```bash
pip install -r requirements.txt
```

4. Run the backend server:
```bash
python app.py
```

The API will be available at `http://localhost:5000`

## Dashboard Setup

1. Navigate to dashboard directory:
```bash
cd dashboard
```

2. Install dependencies:
```bash
npm install
```

3. Start the development server:
```bash
npm start
```

The dashboard will be available at `http://localhost:3000`

## Android Demo App Setup

1. Open Android Studio
2. Import the `demo-app` directory as a project
3. Ensure the SDK module is included (or copy SDK code into the app)
4. Update the API endpoint in `MainActivity.kt`:
   - **For emulator**: `http://10.0.2.2:5000/api/events`
   - **For physical device (using adb reverse)**:
     - Set endpoint to: `http://localhost:5000/api/events`
     - Run: `adb reverse tcp:5000 tcp:5000` (forwards device's localhost:5000 to your computer's localhost:5000)
   - **For physical device (using network IP)**:
     - Find your computer's IP address (e.g., `ip addr` on Linux, `ipconfig` on Windows)
     - Set endpoint to: `http://YOUR_COMPUTER_IP:5000/api/events`
     - Ensure your computer's firewall allows connections on port 5000
5. Build and run the app

**Note on adb reverse**: This is the easiest method for physical devices. It forwards the device's `localhost:5000` to your computer's `localhost:5000`, so you can use `localhost` in the app configuration. Make sure your device is connected via USB and USB debugging is enabled.

**Note:** The Gradle wrapper (`gradlew`) is included. On first run, Gradle will automatically download the wrapper JAR if needed. You can also build from command line:
```bash
cd demo-app
./gradlew build  # On Linux/Mac
gradlew.bat build  # On Windows
```

## Testing the Prototype

1. Start the backend server
2. Start the dashboard
3. Run the Android demo app
4. Click "Start Monitoring" in the app
5. Click "Simulate Suspicious Activity" to generate test events
6. View the results in the dashboard

## API Endpoints

- `GET /api/health` - Health check
- `POST /api/events` - Submit security event
- `GET /api/events` - Retrieve events (with optional filters)
- `GET /api/stats` - Get aggregated statistics
