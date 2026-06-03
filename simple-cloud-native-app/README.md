# Simple Cloud Native Fullstack Application

This is a simple fullstack application built with a cloud-native approach, using Docker for containerization. It consists of a React frontend, a Node.js/Express backend, and a MongoDB database.

## Prerequisites

- Podman 
- Podman Compose

## Getting Started

1.  **Clone the repository (if applicable) or ensure you have the project files.**
2.  **Navigate to the project root directory:**
    ```bash
    cd simple-cloud-native-app
    ```
3.  **Build and run the application using Docker Compose:**
    ```bash
    podman compose up -d --build
    ```
    This command will:
    *   Build the Docker images for the frontend and backend services.
    *   Start the containers for the frontend, backend, and MongoDB services in detached mode (`-d`).

4.  **Access the application:**
    *   Frontend: Open your browser and go to `http://localhost:3000`
    *   Backend API (if you want to test it directly): `http://localhost:3001`

## Services

*   **Frontend:** A React application served by Nginx. It communicates with the backend service.
    *   Runs on `http://localhost:3000` (mapped from container port 80).
*   **Backend:** A Node.js/Express application providing a REST API. It connects to the MongoDB database.
    *   Runs on `http://localhost:3001` (mapped from container port 3001).
*   **MongoDB:** A NoSQL database used to store application data.
    *   Accessible within the Docker network at `mongodb://mongodb:27017/taskdb`.
    *   Host port `27017` is mapped to the container's port `27017` for direct access if needed (e.g., with MongoDB Compass), but the backend connects to it using the service name `mongodb`.

## Project Structure

```
simple-cloud-native-app/
├── compose.yaml        # Docker Compose configuration
├── backend/            # Backend service (Node.js/Express)
│   ├── Dockerfile
│   ├── package.json
│   └── server.js       # Main backend application file
├── frontend/           # Frontend service (React/Vite)
│   ├── Dockerfile
│   ├── package.json
│   └── ...             # Other frontend files (src, public, etc.)
└── README.md           # This file
```

## Technologies Used

*   **Frontend:** React, Vite, Nginx (for serving static files in Docker)
*   **Backend:** Node.js, Express.js
*   **Database:** MongoDB
*   **Containerization:** Podman, Podman compose

## Stopping the Application

To stop all running containers, navigate to the project root directory and run:

```bash
podman compose down
```

To stop and remove volumes (e.g., to clear MongoDB data):

```bash
podman compose down -v
```

## Application level logs

### 1. Log Aggregation Platforms
These platforms are designed to collect, process, and analyze logs from various sources, including containers. They are the most common solution for centralized logging in a containerized environment.

#### 1. Fluentd & Fluent Bit
These are open-source data collectors that are part of the Cloud Native Computing Foundation (CNCF). They can gather logs from running containers and forward them to a wide array of destinations. Fluent Bit is a lightweight version, making it ideal for running as a sidecar or on an edge device.

#### 2. Logstash (part of the ELK/Elastic Stack)
A powerful, server-side data processing pipeline that ingests data from a multitude of sources, transforms it, and then sends it to a "stash" like Elasticsearch.

#### 3. Promtail (part of the Loki Stack)
Promtail is the agent responsible for collecting logs and sending them to a Grafana Loki instance. This stack is optimized for cost-effective storage and easy integration with Prometheus metrics.

### 2. Logging Backends & Analytics Services
These are often managed (SaaS) or self-hosted services that provide the storage, search, and visualization capabilities for your logs.

#### 1.Elasticsearch
An open-source, distributed search and analytics engine. It's commonly paired with Logstash and Kibana (the ELK stack) for a complete log management solution.

#### 2.Grafana Loki
An open-source, multi-tenant log aggregation system inspired by Prometheus. It’s designed to be cost-effective and easy to operate.

#### 3.Splunk
A popular commercial platform for searching, monitoring, and analyzing machine-generated data.

#### 4.Datadog, New Relic, Logz.io
These are commercial, cloud-based monitoring and security platforms that offer advanced log management features, including collection, analysis, and alerting.

### Enabling Logger Service for Backend
For maintainability, we create a new file named backend/logger.js to configure Winston. This allows us to manage logging setup from a single location. 

We will then proceed to change the usual console prints to use logger instead. (In JS case its is console.out, for Java it would be stdout etc). Add appropriate ignote statement to gitignore settings. 

To view the logs for the running backend container (in real time using -f ), use the following command:
```bash
podman logs -f backend-service-container
```
[Note - if logger doesnot work, you may need to use npm build or npm install winston as necessary.]

## Contributing

Feel free to fork this project and submit pull requests.

