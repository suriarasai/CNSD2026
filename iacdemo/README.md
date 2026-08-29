# IaC Architectural Challenge: Compose vs. Podman Play Kube

A hands-on comparative guide and troubleshooting reference exploring declarative infrastructure patterns with **Podman Desktop**, **Compose**, and **Kubernetes Pod Manifests**.

---

## 1. Executive Summary & Architectural Overview

Modern container infrastructure relies on declarative state management. Instead of manually clicking buttons or running isolated commands, we declare systems as code. This exercise contrasts two distinct declarative methodologies:

1. **Option A: Compose Approach (`compose.yaml`)** — Service-centric, multi-network topology modeling.
2. **Option B: Kubernetes Play Kube Approach (`kube-stack.yaml`)** — Pod-centric, shared-namespace co-location modeling.

Both approaches deploy the same conceptual 3-tier workload:
* **Edge Proxy:** Nginx reverse proxy accepting external ingress.
* **Application Core:** Node.js / Express API processing requests.
* **State / Cache:** Redis key-value store incrementing hit counts and maintaining persistence.

---

## 2. In-Depth Technical Comparison: Compose vs. Play Kube

| Architectural Dimension | Option A: Compose (`podman compose`) | Option B: Play Kube (`podman kube play`) |
| :--- | :--- | :--- |
| **Fundamental Abstraction Unit** | **Service / Container** <br> Each service represents a standalone container with its own isolated network stack and hostname. | **Pod** <br> Multiple containers share a single co-located execution sandbox, network namespace, and lifecycle. |
| **Network Boundary & DNS** | **Custom Bridged Networks (`frontend-net`, `backend-net`)** <br> Containers communicate across networks using built-in DNS (e.g., `http://backend:3000`, `cache:6379`). True network segmentation is enforced by the bridge driver. | **Shared Loopback Namespace (`localhost`)** <br> All containers in the Pod share the identical network interface. The proxy reaches the backend at `127.0.0.1:3000`, and the backend reaches Redis at `127.0.0.1:6379`. |
| **Port Exposure Model** | Ports are bound per-container at the service level (e.g., proxy exposes `8080:80`). Internal services do not expose ports to the host. | Ports are mapped at the **Pod boundary**. Container ports inside the Pod share port space—two containers in the same Pod cannot bind port `8080`. |
| **Lifecycle & Coupling** | **Loosely Coupled** <br> Services can be restarted, scaled, or upgraded independently without tearing down the entire network or adjacent containers. | **Tightly Coupled** <br> Containers in a Pod are scheduled and managed as an atomic unit. If the Pod is destroyed or restarted, all constituent containers follow. |
| **Configuration Injection** | Host-mounted volumes (`./proxy/nginx.conf:...`) or inline `environment` key-values inside service definitions. | Native Kubernetes primitives: `ConfigMap` resources mapped via volume mounts, decoupled from container code. |
| **Storage & Persistence** | Named or anonymous Docker volumes (`volumes: redis-data:...`). | Standard Kubernetes `PersistentVolumeClaim` (PVC) abstractions backed by Podman local volume drivers. |
| **Production Target Path** | Docker Swarm, Nomad, or container runtime compose utilities. | Direct parity with production Kubernetes clusters (EKS, GKE, AKS, OpenShift). |

---

## 3. Systems Thinking: Why the Difference Matters

* **Network Segmentation vs. Shared Context:**
  * In **Compose**, security boundaries exist between containers. The database can be isolated on `backend-net` so the proxy cannot physically route packets to it, enforcing the principle of least privilege at Layer 3/4.
  * In **Play Kube**, all containers inside the Pod trust each other over `localhost`. This is optimized for sidecar patterns (e.g., logging agents, proxies, service mesh sidecars) rather than distributed multi-tier segmentation.
* **Immutability & GitOps:**
  * Both approaches eliminate configuration drift. However, `kube-stack.yaml` allows developers to test production Kubernetes resource definitions locally on their laptop without needing Minikube or a remote cluster.

**When Option A (Compose) Is Preferred: Microservice Segmentation and Fast Local Iteration**

* **Scenario:** A team is building an e-commerce platform with distinct backend services (e.g., Auth, Inventory, Payments) and a shared PostgreSQL/Redis datastore. Developers need to run the entire backend on their local machines during daily feature development.
* **Why Compose Wins:**
* **Network Isolation by Default:** You can define separate bridge networks (`internal-net` vs. `public-net`) to ensure payment services can talk to the database, but edge services cannot directly query persistence layers.
* **Independent Lifecycles:** When a developer edits the Inventory service, only that specific container needs to be rebuilt and restarted (`podman compose restart inventory`) without restarting Auth, Payments, or the database.
* **Low Cognitive Overhead:** Teams don't need to write boilerplate Kubernetes YAML (ConfigMaps, Pod specs, PVCs) when they just need a simple, multi-container environment up and running quickly.

**When Option B (Play Kube) Is Preferred: Production Parity and Sidecar Architecture**

* **Scenario:** A platform engineering team is preparing a service to run on Amazon EKS, Azure AKS, or OpenShift. The service uses an Envoy proxy sidecar for mTLS/rate-limiting, a logging agent sidecar, and native Kubernetes ConfigMaps/Secrets for configuration injection.
* **Why Play Kube Wins:**
* **Validating Pod & Sidecar Semantics:** Because all containers in a Pod share the `localhost` network and lifecycle, developers can test traffic interception and shared IPC locally exactly as it executes in a real cluster.
* **Direct Manifest Reuse (Shift-Left GitOps):** The exact YAML manifests tested on the laptop via `podman kube play` can be committed directly to Git and deployed via ArgoCD or Flux. Compose would require maintaining two divergent files: a `compose.yaml` for local dev and separate Helm charts/K8s manifests for production.
* **Zero Cluster Overhead:** It tests production Kubernetes declarations locally without running resource-heavy local clusters like Minikube or Kind.

---

## 4. Testing & Verifying Inside the Podman Machine

On Windows and macOS, Podman runs containers inside a lightweight Linux virtual machine (`podman-machine-default`). 

To verify that the application stack is healthy and completely eliminate host-networking bugs, query the application directly inside the virtual machine using `podman machine ssh`:

```powershell
# 1. Execute a curl request directly inside the Podman VM
podman machine ssh "curl -s http://localhost:8080/api/hits"
```

### Expected Output:
```json
{"hits": 1, "backend_pod": "app-tier-pod"}
```

* **What this proves:**
  * Nginx successfully accepted ingress on port `8080`.
  * Nginx routed to the Node.js backend.
  * Node.js connected to Redis and executed `INCR page_hits`.
  * The entire internal stack is 100% operational.

---

## 5. Accessing the Service in Your Browser (No Admin Required)

When running Podman on Windows WSL2, `http://localhost:8080` in your host browser may return `Connection Refused` if the automated Windows loopback forwarder hasn't bound the port.

You **do not need Administrator privileges** or `netsh` commands to access your application. Simply point your browser directly to the Podman VM's internal IP address.

### Step 1: Retrieve the VM IP Address

Run this command in PowerShell to extract the virtual machine's IPv4 address:

```powershell
$vmIp = (podman machine ssh "ip -4 addr show eth0 | grep -oP '(?<=inet\s)\d+(\.\d+){3}'").Trim()
Write-Host "Podman VM IP Address: $vmIp"
```

*(Alternatively, run: `podman machine ssh "hostname -I | awk '{print \$1}'"`)*

### Step 2: Open the URL in Your Browser

Copy the printed IP address (commonly `172.x.x.x` or `192.168.127.x`) and navigate directly to:

```text
http://<VM_IP>:8080/api/hits
```

For example:
```text
http://172.28.14.98:8080/api/hits
```

Refresh the browser window. You will see the hit counter increment with every request:
```json
{
  "hits": 2,
  "backend_pod": "app-tier-pod"
}
```

You can also inspect the root gateway endpoint:
```text
http://<VM_IP>:8080/
```

---

## 6. Hands-On Runbook

### Running Option A (Compose)
```bash
# Build the backend container image
podman build -t localhost/backend:latest ./backend

# Bring up the stack in detached mode
podman compose up -d

# Test inside VM
podman machine ssh "curl -s http://localhost:8080/api/hits"

# Teardown including volumes
podman compose down -v
```

### Running Option B (Kubernetes Manifests)
```bash
# Build the backend container image
podman build -t localhost/backend:latest ./backend

# Deploy the Pod and ConfigMap
podman kube play kube-stack.yaml

# Test inside VM
podman machine ssh "curl -s http://localhost:8080/api/hits"

# Deterministic teardown
podman kube play --down kube-stack.yaml
```

---

## 7. Troubleshooting Matrix

| Symptom | Probable Cause | Corrective Action |
| :--- | :--- | :--- |
| `curl : Unable to connect to the remote server` in Windows PowerShell | Windows host loopback is not forwarding port `8080` into WSL2. | Run `podman machine ssh "curl -s http://localhost:8080/api/hits"` to test the VM, then access via `http://<VM_IP>:8080` in the browser. |
| Browser shows `502 Bad Gateway` | Nginx cannot resolve or reach the backend upstream. | Check backend status: `podman logs backend-api` or verify port bindings inside `nginx.conf`. |
| Browser shows `500 Internal Server Error` | Backend API cannot establish a connection to Redis. | Verify Redis container health and ensure `REDIS_HOST` matches `cache` (Compose) or `127.0.0.1` (Kube Pod). |
