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