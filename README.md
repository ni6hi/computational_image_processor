# computational_image_processor
Associated with the course CS5013: Programming with AI


A high-performance, headless-first Java computational imaging framework designed for optical research labs, specialized scientific sensors (RAW, Thermal/LWIR, SPADs, Neuromorphic Event Streams), and remote GPU cluster workflows.


## Prerequisites & Installation

### System Requirements
* **Java Development Kit (JDK):** Version 17 or higher
* **Build Tool:** Apache Maven 3.8+
* **Operating System:** Linux, macOS, or Windows

### Building from Source

1. Clone the repository:
   ```bash
   git clone [https://github.com/ni6hi/computational_image_processor](https://github.com/ni6hi/computational_image_processor)
   cd computational_image_processor
   ```

2. Compile and package the executable JAR using Maven:
   ```bash
   mvn clean package
   ```

3. Verify the generated artifact:
   ```bash
   ls -la target/image-processor-1.0.0.jar
   ```

---

### 1. Native Desktop GUI (Local Workstations)
Run directly on local machines with a display server connected:

```bash
mvn exec:java -Dexec.mainClass="com.imageapp.ImageProcessorGUI"
```

---
### 2. Local Host website for remote servers
Follow the link : http://localhost:7070/ to see the web version of the GUI

```bash
mvn compile exec:java -Dexec.mainClass="com.imageapp.WebServer"
```


## License & Attribution

Developed for scientific computational imaging labs, optical research environments, and hardware sensor preprocessing pipelines. Licensed under the [MIT License](LICENSE).

## Pipeline JSON: save, load, and automation

This project supports exporting and importing the current processing pipeline as a JSON file. Use this to persist pipeline step lists, share presets, or automate server-side startup.

- Web API (export):

   ```bash
   curl -s http://localhost:7070/api/pipeline/export -o pipeline.json
   ```

- Web API (import):

   ```bash
   curl -X POST -H "Content-Type: application/json" --data @pipeline.json http://localhost:7070/api/pipeline/import
   ```

- GUI (desktop):

   - Open the Swing GUI (`mvn exec:java -Dexec.mainClass="com.imageapp.ImageProcessorGUI"`).
   - Use the **Save Pipeline...** button to write a JSON file containing the pipeline.
   - Use the **Load Pipeline...** button to open a previously saved JSON and restore the pipeline (the preview updates automatically).

- Server auto-load:

   - If a file named `pipeline.json` exists in the server working directory when `WebServer` starts, the server will attempt to load it automatically into the in-memory pipeline.

Notes and limitations:

- Export/import is best-effort. The system maps operation display names to operation keys and attempts to preserve common parameters. Complex or custom operation parameters may not round-trip perfectly and will fall back to reasonable defaults on import.
- The JSON structure is a simple array of { "op": "<key>", "params": { ... } } entries. See exported `pipeline.json` for examples.

## Batch-run pipeline JSON over a directory (preserve tree)

You can run a saved `pipeline.json` over an entire input directory recursively. The output directory will mirror the input directory tree and preserve filenames (the file extension will be changed only if you request a different output format).

Example (via Maven exec):

```bash
mvn exec:java -Dexec.mainClass="com.imageapp.BatchRunnerMain" -Dexec.args="pipeline.json /path/to/input_dir /path/to/output_dir png"
```

Arguments:
- `pipeline.json`: path to the exported pipeline JSON
- `/path/to/input_dir`: directory containing images (subdirectories will be traversed)
- `/path/to/output_dir`: destination root directory (will be created if missing)
- `png` (optional): output image format (default: `png`). If you specify the same format as source files, filenames remain unchanged; otherwise extensions are replaced with the requested format.

The runner prints brief progress messages to stdout while processing.
