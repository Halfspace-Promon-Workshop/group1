# Quick Start Guide

## Prerequisites Check

Before running the analyzer, ensure you have:
- [ ] JDK 11+ installed (`java -version`)
- [ ] Maven 3.6+ installed (`mvn -version`)
- [ ] JDT Language Server installed (see README.md for download link)

## Quick Setup (5 minutes)

### 1. Build the Project

```bash
cd java-dependency-analyzer
mvn clean package
```

This creates: `target/java-dependency-analyzer-1.0-SNAPSHOT-jar-with-dependencies.jar`

### 2. Start JDT LS

Find your JDT LS installation (often in `~/.vscode/extensions/redhat.java-*/server/`):

```bash
# Example for macOS
cd ~/.vscode/extensions/redhat.java-*/server/
java -jar plugins/org.eclipse.equinox.launcher_*.jar \
  -configuration config_mac \
  -data /Users/harrison/tmp/group1/nifi \
  -Dclient.socket.port=9999
```

**Wait for JDT LS to finish initializing** (watch for "ServiceReady" message)

### 3. Run the Analyzer

In a new terminal:

```bash
cd java-dependency-analyzer
java -jar target/java-dependency-analyzer-1.0-SNAPSHOT-jar-with-dependencies.jar
```

Look for the message: "Application started successfully!"

### 4. Open in Browser

Navigate to: http://localhost:8080

You should see the 3D dependency graph!

## Controls

- **Rotate**: Left-click and drag
- **Pan**: Right-click and drag
- **Zoom**: Mouse wheel
- **Hover**: See class details
- **Click**: Highlight dependencies

## Troubleshooting

### "Failed to connect to JDT LS"
- Make sure JDT LS is running first
- Check port 9999 is not in use

### "No classes found"
- Wait longer for JDT LS to initialize
- Verify the `-data` path points to a valid Java project

### WebSocket connection failed
- Check the backend is running
- Try refreshing the browser

## Configuration

Edit `src/main/resources/application.properties` to change:
- JDT LS connection settings
- Server port
- Layout algorithm parameters

## Project Structure Overview

```
Backend (Java):
├── lsp/              → JDT LS communication
├── graph/            → Dependency graph model
│   └── layout/       → Layout algorithms
└── server/           → Jetty + WebSocket

Frontend (Web):
├── index.html        → Main page
├── css/styles.css    → Styling
└── js/
    ├── main.js                → App initialization
    ├── websocket-client.js    → Server communication
    └── graph-visualizer.js    → 3D rendering
```

## Example: Analyzing NiFi

If you have the NiFi project in your workspace:

1. Start JDT LS pointing to the nifi directory
2. Run the analyzer
3. View the graph - you should see hundreds of classes and their dependencies

Expected graph size for NiFi: ~1000+ nodes, ~3000+ edges

## Next Steps

- Read the full README.md for detailed documentation
- Customize the visualization in `graph-visualizer.js`
- Implement additional layout algorithms
- Add filtering capabilities

## Support

For issues or questions, refer to the README.md troubleshooting section.
