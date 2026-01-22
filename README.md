# Interactive Code Graph Summary Tool

A Java-based tool for analyzing and visualizing code structure using JDT (Java Development Tools) Language Server. This tool builds an interactive graph representation of your Java codebase, showing relationships between classes, methods, fields, and their dependencies.

## Features

- **Code Analysis**: Automatically analyzes Java source code and builds a comprehensive code graph
- **Interactive CLI**: Command-line interface for exploring code structure
- **Multiple Export Formats**: Export graphs to JSON, Graphviz DOT, or interactive HTML
- **Dependency Tracking**: Track dependencies and dependents for any code element
- **Search Functionality**: Search for classes, methods, and other code elements
- **Package Statistics**: View statistics organized by package

## Requirements

- Java 17 or higher
- Maven 3.6 or higher
- A Java project to analyze

## Installation

1. Clone or download this project
2. Build the project using Maven:

```bash
mvn clean package
```

This will create an executable JAR file in the `target/` directory.

## Usage

### Basic Usage

Run the tool with a path to your Java project:

```bash
java -jar target/interactive-code-graph-1.0.0.jar /path/to/your/java/project
```

Or run without arguments to be prompted for the project path:

```bash
java -jar target/interactive-code-graph-1.0.0.jar
```

### Interactive Menu

Once the analysis is complete, you'll see an interactive menu:

1. **Search nodes** - Search for classes, methods, or other code elements
2. **View node details** - Get detailed information about a specific node
3. **View package statistics** - See statistics organized by package
4. **Export graph** - Export the graph to various formats
5. **View dependencies** - View dependencies for a specific node
6. **Show summary** - Display the overall code graph summary
0. **Exit** - Exit the application

### Export Formats

The tool supports three export formats:

1. **JSON** - Machine-readable format for further processing
2. **Graphviz DOT** - For generating static images using Graphviz
3. **Interactive HTML** - Web-based interactive visualization using D3.js

#### Exporting to Graphviz

After exporting to DOT format, you can generate images:

```bash
# Generate SVG
dot -Tsvg graph.dot -o graph.svg

# Generate PNG
dot -Tpng graph.dot -o graph.png

# Generate PDF
dot -Tpdf graph.dot -o graph.pdf
```

#### Interactive HTML Visualization

The HTML export creates a fully interactive graph that you can:
- Drag nodes around
- Click nodes to see details
- Zoom and pan (if implemented)
- View relationships visually

Simply open the exported HTML file in a web browser.

## Project Structure

```
interactive-code-graph/
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── codegraph/
│                   └── tool/
│                       ├── CodeGraphApplication.java    # Main entry point
│                       ├── analyzer/
│                       │   └── CodeGraphAnalyzer.java   # Code analysis engine
│                       ├── model/
│                       │   ├── CodeGraph.java           # Graph data structure
│                       │   └── CodeNode.java            # Node representation
│                       └── visualization/
│                           └── GraphExporter.java       # Export functionality
├── pom.xml
└── README.md
```

## How It Works

1. **Code Parsing**: Uses Eclipse JDT Core to parse Java source files into Abstract Syntax Trees (AST)
2. **Graph Building**: Traverses the AST to identify:
   - Classes, interfaces, enums
   - Methods and their signatures
   - Fields and their types
   - Inheritance relationships
   - Method calls and dependencies
3. **Relationship Mapping**: Builds edges between nodes based on:
   - Class inheritance
   - Interface implementations
   - Method invocations
   - Field type dependencies
4. **Visualization**: Exports the graph in various formats for visualization

## Integration with JDT Language Server

This tool uses Eclipse JDT Core for parsing, which is the same engine used by JDT Language Server. To integrate with a full LSP implementation:

1. Add LSP4J dependencies for Language Server Protocol communication
2. Implement LSP client to query JDT Language Server for:
   - Symbol information
   - References
   - Type hierarchy
   - Code completion data
3. Combine LSP data with AST analysis for richer graph information

## Example: Analyzing an Open Source Project

Here's how to analyze a popular open source project:

```bash
# Clone a project (example: Spring Framework)
git clone --depth 1 https://github.com/spring-projects/spring-framework.git

# Analyze it
java -jar target/interactive-code-graph-1.0.0.jar spring-framework/spring-core

# Export to interactive HTML
# (Use option 4 in the menu, then select option 3)
# Open the HTML file in your browser
```

## Recommended Projects for Testing

- **NestJS** (TypeScript, but has Java-like structure)
- **Spring Framework** - Large, well-organized Java project
- **Apache Commons** - Various utility libraries
- **JUnit** - Testing framework
- **Guava** - Google's core libraries

## Troubleshooting

### Out of Memory Errors

For large projects, increase heap size:

```bash
java -Xmx4g -jar target/interactive-code-graph-1.0.0.jar /path/to/project
```

### Missing Dependencies

If the analyzer can't resolve some types:
- Ensure your project has a proper build (Maven/Gradle)
- The tool will create placeholder nodes for unresolved external dependencies

### Performance Issues

For very large codebases:
- Consider analyzing specific packages only
- Use filters to exclude test code or generated code
- Increase JVM memory allocation

## Extending the Tool

### Adding New Export Formats

Extend `GraphExporter` class and add new export methods:

```java
public void exportToCustomFormat(String outputPath) {
    // Your custom export logic
}
```

### Adding New Analysis Features

Extend `CodeGraphAnalyzer` to add new analysis capabilities:

```java
// Add new visitor methods to analyze specific patterns
```

### Integrating with Other Tools

The JSON export format can be consumed by:
- Neo4j for graph database storage
- Gephi for advanced network analysis
- Custom visualization tools

## License

This is a scratch project template. Modify as needed for your use case.

## Contributing

This is a starting point for building a code graph tool. Feel free to extend it with:
- Support for more languages
- Better visualization options
- Performance optimizations
- Integration with IDEs
- Web-based UI

## Resources

- [Eclipse JDT Core Documentation](https://www.eclipse.org/jdt/core/)
- [Language Server Protocol](https://microsoft.github.io/language-server-protocol/)
- [D3.js Documentation](https://d3js.org/)
- [Graphviz Documentation](https://graphviz.org/documentation/)
