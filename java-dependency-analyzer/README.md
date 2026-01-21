# Java Dependency Graph Analyzer

A tool for analyzing and visualizing Java class dependencies in an interactive 3D graph. The analyzer connects to a JDT Language Server to extract class information and builds a dependency graph based on instance variables (fields). The graph is displayed in a web browser using Three.js for 3D visualization.

## Features

- **Dependency Analysis**: Analyzes Java projects via JDT LS to identify class dependencies based on instance variables
- **3D Visualization**: Interactive 3D graph rendering using Three.js
- **Force-Directed Layout**: Automatic graph layout using the Fruchterman-Reingold algorithm
- **Real-time Updates**: WebSocket-based communication for instant graph updates
- **Interactive Navigation**: Pan, zoom, and rotate the camera to explore the graph
- **Node Information**: Hover and click on nodes to view class details
- **Visual Encoding**: Node size and color indicate dependency count

## Architecture

```
┌─────────────────┐
│   JDT LS        │ Language Server Protocol
│  (Port 9999)    │◄─────────────────┐
└─────────────────┘                  │
                                     │
┌─────────────────────────────────────────┐
│  Java Backend                           │
│  ┌─────────────────────────────────┐   │
│  │  LSP Client                      │   │
│  │  ├─ Connect to JDT LS           │   │
│  │  └─ Query workspace symbols      │   │
│  └─────────────────────────────────┘   │
│  ┌─────────────────────────────────┐   │
│  │  Graph Builder                   │   │
│  │  ├─ Extract class dependencies   │   │
│  │  └─ Build graph structure        │   │
│  └─────────────────────────────────┘   │
│  ┌─────────────────────────────────┐   │
│  │  Layout Algorithm                │   │
│  │  └─ Force-directed positioning   │   │
│  └─────────────────────────────────┘   │
│  ┌─────────────────────────────────┐   │
│  │  Jetty Server (Port 8080)        │   │
│  │  ├─ WebSocket endpoint           │   │
│  │  └─ Static file serving          │   │
│  └─────────────────────────────────┘   │
└─────────────────────────────────────────┘
                  │
                  │ WebSocket
                  ▼
┌─────────────────────────────────────────┐
│  Web Frontend                            │
│  ┌─────────────────────────────────┐   │
│  │  Three.js Visualization          │   │
│  │  ├─ 3D scene rendering           │   │
│  │  ├─ Orbit controls               │   │
│  │  └─ Interactive node selection   │   │
│  └─────────────────────────────────┘   │
└─────────────────────────────────────────┘
```

## Prerequisites

- **JDK 11 or higher**: For building and running the analyzer
- **Maven 3.6+**: For dependency management and building
- **JDT Language Server**: Eclipse JDT LS must be installed on your system
  - Download from: https://download.eclipse.org/jdtls/snapshots/
  - Or install via VS Code Java extension
- **Modern Web Browser**: Chrome, Firefox, Safari, or Edge with WebSocket support

## Installation

1. **Clone or download this project**

```bash
cd java-dependency-analyzer
```

2. **Build the project**

```bash
mvn clean package
```

This will create `target/java-dependency-analyzer-1.0-SNAPSHOT-jar-with-dependencies.jar`

## Usage

### Step 1: Start JDT Language Server

Before running the analyzer, you need to start JDT LS manually. The analyzer will connect to it via socket.

#### Finding JDT LS Installation

If you have VS Code with Java extensions installed, JDT LS is typically located at:

- **macOS/Linux**: `~/.vscode/extensions/redhat.java-*/server/`
- **Windows**: `%USERPROFILE%\.vscode\extensions\redhat.java-*\server\`

#### Starting JDT LS

Navigate to your JDT LS installation directory and run:

```bash
java -jar plugins/org.eclipse.equinox.launcher_*.jar \
  -configuration config_linux \
  -data /path/to/workspace \
  -Dclient.socket.port=9999
```

**Notes:**
- Replace `/path/to/workspace` with the path to the Java project you want to analyze (e.g., `../nifi`)
- Use `config_mac` on macOS, `config_win` on Windows, or `config_linux` on Linux
- The default port is 9999, but you can change it in the configuration

Example for analyzing the NiFi project:

```bash
cd ~/.vscode/extensions/redhat.java-*/server/
java -jar plugins/org.eclipse.equinox.launcher_*.jar \
  -configuration config_mac \
  -data /Users/harrison/tmp/group1/nifi \
  -Dclient.socket.port=9999
```

Wait for JDT LS to initialize and analyze the workspace. You'll see output indicating the server is ready.

### Step 2: Run the Analyzer

In a new terminal, run the analyzer:

```bash
java -jar target/java-dependency-analyzer-1.0-SNAPSHOT-jar-with-dependencies.jar
```

Or specify custom connection parameters:

```bash
java -jar target/java-dependency-analyzer-1.0-SNAPSHOT-jar-with-dependencies.jar <jdtls-host> <jdtls-port> <server-port>
```

Example:
```bash
java -jar target/java-dependency-analyzer-1.0-SNAPSHOT-jar-with-dependencies.jar localhost 9999 8080
```

### Step 3: View the Graph

Once the analyzer has built the graph, open your web browser and navigate to:

```
http://localhost:8080
```

You should see the 3D dependency graph with interactive controls.

## Configuration

You can modify the behavior by editing `src/main/resources/application.properties`:

```properties
# JDT Language Server Configuration
jdtls.host=localhost
jdtls.port=9999

# Web Server Configuration
server.port=8080

# Layout Algorithm Configuration
layout.algorithm=force-directed
layout.iterations=1000
layout.temperature=100
layout.ideal-distance=150
```

After modifying, rebuild with `mvn clean package`.

## Using the Web Interface

### Navigation
- **Rotate**: Left-click and drag
- **Pan**: Right-click and drag (or Shift + left-click)
- **Zoom**: Scroll wheel or pinch gesture

### Controls
- **Reset Camera**: Return to default view
- **Toggle Labels**: Show/hide class name labels

### Node Information
- **Hover**: See basic class information
- **Click**: Select a node to highlight its dependencies

### Visual Encoding
- **Node Size**: Larger nodes have more dependencies
- **Node Color**:
  - Blue: No dependencies (leaf nodes)
  - Green: 1-2 dependencies
  - Yellow: 3-5 dependencies
  - Red: 6+ dependencies
- **Edges**: Lines connecting classes, highlighted when a node is selected

## Project Structure

```
java-dependency-analyzer/
├── pom.xml                           # Maven configuration
├── README.md                         # This file
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/analyzer/
│   │   │       ├── Main.java                      # Application entry point
│   │   │       ├── lsp/
│   │   │       │   ├── JdtLsClient.java          # JDT LS connection
│   │   │       │   └── LanguageClientImpl.java   # LSP client implementation
│   │   │       ├── graph/
│   │   │       │   ├── ClassNode.java            # Node model
│   │   │       │   ├── DependencyEdge.java       # Edge model
│   │   │       │   ├── DependencyGraph.java      # Graph data structure
│   │   │       │   ├── GraphBuilder.java         # Graph construction logic
│   │   │       │   └── layout/
│   │   │       │       ├── LayoutAlgorithm.java  # Layout interface
│   │   │       │       └── ForceDirectedLayout.java # Force-directed algorithm
│   │   │       └── server/
│   │   │           ├── JettyServer.java          # Web server
│   │   │           └── GraphWebSocketHandler.java # WebSocket handler
│   │   ├── resources/
│   │   │   ├── application.properties            # Configuration
│   │   │   └── logback.xml                       # Logging configuration
│   │   └── webapp/
│   │       ├── index.html                        # Main HTML page
│   │       ├── css/
│   │       │   └── styles.css                    # Styling
│   │       └── js/
│   │           ├── main.js                       # Application initialization
│   │           ├── graph-visualizer.js           # Three.js visualization
│   │           └── websocket-client.js           # WebSocket client
│   └── test/
│       └── java/                                 # Unit tests (if needed)
```

## How It Works

### 1. Connection to JDT LS

The analyzer connects to JDT Language Server via a socket connection using the LSP4J library. JDT LS provides rich semantic information about Java code.

### 2. Dependency Extraction

The `GraphBuilder` queries JDT LS for:
- All classes and interfaces in the workspace
- Document symbols (fields, methods, etc.) for each class
- Field type information

A dependency edge is created when:
- Class A has an instance variable (non-static field) of type Class B
- Both classes are part of the analyzed project (JDK classes are excluded)

### 3. Graph Layout

The `ForceDirectedLayout` algorithm positions nodes in 3D space:
- **Repulsive forces**: All nodes repel each other to prevent overlap
- **Attractive forces**: Connected nodes attract each other along edges
- The algorithm iterates until forces stabilize or max iterations reached

### 4. Visualization

The frontend uses Three.js to render:
- Nodes as 3D spheres with varying sizes and colors
- Edges as lines connecting dependent classes
- Interactive camera controls for navigation

### 5. Real-time Communication

WebSocket provides bidirectional communication:
- Server sends graph data to clients when they connect
- Future: Could support live updates as code changes

## Extending the Analyzer

### Adding New Layout Algorithms

1. Implement the `LayoutAlgorithm` interface:

```java
public class MyLayout implements LayoutAlgorithm {
    @Override
    public void calculateLayout(DependencyGraph graph) {
        // Your layout logic
    }
}
```

2. Update `Main.java` to instantiate your algorithm based on configuration

### Filtering Graph Nodes

Modify `GraphBuilder.shouldExclude()` to add custom filtering logic:

```java
private boolean shouldExclude(String fullyQualifiedName) {
    // Add your filtering criteria
    return fullyQualifiedName.startsWith("com.example.internal.");
}
```

### Customizing Visualization

Edit `graph-visualizer.js` to modify:
- Node appearance (color, size, shape)
- Edge styling (color, thickness, style)
- Camera behavior
- Interaction modes

## Troubleshooting

### JDT LS Connection Failed

**Problem**: "Failed to connect to JDT LS at localhost:9999"

**Solutions**:
- Ensure JDT LS is running before starting the analyzer
- Check that the port matches (default: 9999)
- Verify JDT LS is listening on the correct port (check JDT LS output)

### No Classes Found

**Problem**: "No classes found in the workspace"

**Solutions**:
- Ensure JDT LS `-data` parameter points to the correct Java project
- Wait for JDT LS to finish initializing (can take a minute for large projects)
- Check that the project is a valid Maven/Gradle Java project

### WebSocket Connection Failed

**Problem**: Frontend shows "Disconnected"

**Solutions**:
- Check that the backend is running and listening on port 8080
- Verify no firewall is blocking WebSocket connections
- Check browser console for error messages

### Graph Not Visible

**Problem**: Page loads but graph is not visible

**Solutions**:
- Check browser console for JavaScript errors
- Try clicking "Reset Camera" button
- Ensure Three.js loaded correctly (check Network tab)

## Performance Considerations

For large projects with 1000+ classes:

- **Graph Building**: May take several minutes depending on project size
- **Layout Calculation**: Increase iterations for better layout quality
- **Rendering**: Browser may slow down with 5000+ nodes
  - Consider filtering to show only specific packages
  - Reduce number of visible labels

## License

This project is provided as-is for educational and analysis purposes.

## Credits

- **Eclipse JDT LS**: https://github.com/eclipse/eclipse.jdt.ls
- **LSP4J**: https://github.com/eclipse/lsp4j
- **Three.js**: https://threejs.org/
- **Jetty**: https://www.eclipse.org/jetty/

## Future Enhancements

Potential improvements (not currently implemented):

- Multiple layout algorithms (hierarchical, circular, tree)
- Real-time graph updates as code changes
- Filter by package or class patterns
- Export graph as image or data file
- Show method call dependencies (in addition to field dependencies)
- Performance metrics and statistics
- Search functionality for finding specific classes
- Zoom to package/module level
