<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/crudjt/crudjt/refs/heads/master/logos/crudjt_logo_white_on_dark.svg">
    <source media="(prefers-color-scheme: light)" srcset="https://raw.githubusercontent.com/crudjt/crudjt/refs/heads/master/logos/crudjt_logo_dark_on_white.svg">
    <img alt="Shows a dark logo" src="https://raw.githubusercontent.com/crudjt/crudjt/refs/heads/master/logos/crudjt_logo_dark.png">
  </picture>
    </br>
    Java SDK for the fast, file-backed, scalable JSON token engine
</p>

<p align="center">
  <a href="https://www.patreon.com/crudjt">
    <img src="https://raw.githubusercontent.com/crudjt/crudjt/refs/heads/master/logos/buy_me_a_coffee_orange.svg" alt="Buy Me a Coffee"/>
  </a>
</p>

> ⚠️ Version 1.0.0-SNAPSHOT — production testing phase   
> API is stable. Feedback is welcome before the final 1.0.0 release

Fast B-tree–backed token store for stateful user sessions  
Provides authentication and authorization across multiple processes  
Optimized for vertical scaling on a single server  

# Installation

## Gradle

```txt
repositories {
    mavenCentral()

    maven {
        url = "https://central.sonatype.com/repository/maven-snapshots/"
    }
}

dependencies {
  implementation 'com.crudjt:crudjt-java:1.0.0-SNAPSHOT'
}
```

## Maven
```xml
<repositories>
    <repository>
        <id>sonatype-snapshots</id>
        <url>https://central.sonatype.com/repository/maven-snapshots/</url>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.crudjt</groupId>
        <artifactId>crudjt-java</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
</dependencies>
```

## How to use

- One process starts the master
- All other processes connect to it

## Start CRUDJT master (once)

Start the CRUDJT master when your application boots

Only **one process** can do this for a **single token storage**  

The master process manages sessions and coordination    
All functions can also be used directly from it  

For containerized deployments, see: [Start CRUDJT master in Docker](#start-crudjt-master-in-docker)

### Generate a new secret key (terminal)

```sh
export CRUDJT_SECRET_KEY=$(openssl rand -base64 48)
```

### Start master (java)

```java
import com.crudjt.CRUDJT;

CRUDJT.Config.startMaster(
    Map.of(
        "secret_key", System.getenv("CRUDJT_SECRET_KEY"),
        "store_jt_path", "path/to/local/storage", // optional
        "grpc_host", "127.0.0.1", // default
        "grpc_port", 50051 // default
    )
);
```

*Important: Use the same `secret_key` across all sessions. If the key changes, previously stored tokens cannot be decrypted and will return `null` or `false`*  

### Start CRUDJT master in Docker
Create a `docker-compose.yml` file:

```yml
services:
  crudjt-server:
    image: crudjt/crudjt-server:beta
    restart: unless-stopped

    ports:
      - "${CRUDJT_CLIENT_PORT:-50051}:50051"

    volumes:
      - "${STORE_JT:-./store_jt}:/app/store_jt"
      - "${CRUDJT_SECRETS:-./crudjt_secrets}:/app/secrets"

    environment:
      CRUDJT_DOCKER_HOST: 0.0.0.0
      CRUDJT_DOCKER_PORT: 50051
```
Start the server:
```bash
docker-compose up -d
```
*Ensure the secrets directory contains your secret key file at `./crudjt_secrets/secret_key.txt`*

For configuration details and image versions, see the
[CRUDJT Server on Docker Hub](https://hub.docker.com/r/crudjt/crudjt-server)

## Connect to an existing CRUDJT master

Use this in all other processes  

Typical examples:
- multiple local processes
- background jobs
- forked processes

```java
import com.crudjt.CRUDJT;

CRUDJT.Config.connectToMaster(
    Map.of(
        "grpc_host", "127.0.0.1", // default
        "grpc_port", 50051 // default
    )
);
```

### Process layout

App boot  
 ├─ Process A → start_master  
 ├─ Process B → connect_to_master  
 └─ Process C → connect_to_master  

# C

```java
Map<String, Object> data = Map.of("user_id", 42, "role", 11); // required
long ttl = 3600 * 24 * 30; // optional: token lifetime (seconds)

// Optional: read limit
// Each read decrements the counter
// When it reaches zero — the token is deleted
long silenceRead = 10;

String token = CRUDJT.create(data, ttl, silenceRead);
// token == "HBmKFXoXgJ46mCqer1WXyQ"
```

```java
// To disable token expiration or read limits, pass `null`
CRUDJT.create(Map.of("user_id", 42, "role", 11), null, null);
```

# R

```java
Map<String, Object> result = CRUDJT.read("HBmKFXoXgJ46mCqer1WXyQ");
// result == {metadata={ttl=101001, silence_read=9}, data={user_id=42, role=11}}
```

```java
// When expired or not found token
String token = CRUDJT.read("HBmKFXoXgJ46mCqer1WXyQ");
// token == null
```

# U

```java
Map<String, Object> data = Map.of("user_id", 42, "role", 8);
// `null` disables limits
long ttl = 600;
long silenceRead = 100;

boolean result = CRUDJT.update("HBmKFXoXgJ46mCqer1WXyQ", data, ttl, silenceRead);
// result == true
```

```java
// When expired or not found token
boolean result = CRUDJT.update("HBmKFXoXgJ46mCqer1WXyQ", Map.of("user_id", 42, "role", 8));
// result == false
```

# D
```java
boolean result = CRUDJT.delete("HBmKFXoXgJ46mCqer1WXyQ");
// result == true
```

```java
// When expired or not found token
boolean result = CRUDJT.delete("HBmKFXoXgJ46mCqer1WXyQ");
// result == false
```

# Performance
**40 000** requests up to **256 bytes** — median over 10 runs  
macOS 15.7.4, ARM64 (Apple M1)  
Java 17.0.18  
In-process benchmark; Redis accessed via localhost TCP  

| Function | CRUDJT (Java) | JWT (Java) | redis-session-store (Ruby, Rails 8.0.2.1) |
|----------|-------|------|------|
| C        | 0.395 second | 0.241 second ⭐ | 2.909 seconds |
| R        | `0.022 second` <picture> <source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/crudjt/crudjt/refs/heads/master/logos/crudjt_favicon_160x160_white_on_dark.svg" width=16 height=16> <source media="(prefers-color-scheme: light)" srcset="https://raw.githubusercontent.com/crudjt/crudjt/refs/heads/master/logos/crudjt_favicon_160x160_dark_on_white.svg" width=16 height=16> <img alt="Shows a favicon black on white color" src="https://raw.githubusercontent.com/crudjt/crudjt/refs/heads/master/logos/crudjt_favicon_white_on_dark.png" width=16 height=16> </picture>   | 0.972 second | 4.436 seconds |
| U        | `0.559 second` <picture> <source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/crudjt/crudjt/refs/heads/master/logos/crudjt_favicon_160x160_white_on_dark.svg" width=16 height=16> <source media="(prefers-color-scheme: light)" srcset="https://raw.githubusercontent.com/crudjt/crudjt/refs/heads/master/logos/crudjt_favicon_160x160_dark_on_white.svg" width=16 height=16> <img alt="Shows a favicon black on white color" src="https://raw.githubusercontent.com/crudjt/crudjt/refs/heads/master/logos/crudjt_favicon_white_on_dark.png" width=16 height=16> </picture>   | X | 2.124 seconds |
| D        | `0.236 second` <picture> <source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/crudjt/crudjt/refs/heads/master/logos/crudjt_favicon_160x160_white_on_dark.svg" width=16 height=16> <source media="(prefers-color-scheme: light)" srcset="https://raw.githubusercontent.com/crudjt/crudjt/refs/heads/master/logos/crudjt_favicon_160x160_dark_on_white.svg" width=16 height=16> <img alt="Shows a favicon black on white color" src="https://raw.githubusercontent.com/crudjt/crudjt/refs/heads/master/logos/crudjt_favicon_white_on_dark.png" width=16 height=16> </picture>   | X | 3.984 seconds |

[Full benchmark results](https://github.com/crudjt/benchmarks)

# Storage (File-backed)  

## Disk footprint  
**40 000** tokens of **256 bytes** each — median over 10 creates  
darwin23, APFS  

`48 MB`  

[Full disk footprint results](https://github.com/crudjt/disk_footprint)

## Path Lookup Order
Stored tokens are placed in the **file system** according to the following order

1. Explicitly set via `CRUDJT.Config.startMaster(Map.of("store_jt_path", "custom/path/to/file_system_db"));`
2. Default system location
   - **Linux**: `/var/lib/store_jt`
   - **macOS**: `/usr/local/var/store_jt`
3. Project root directory (fallback)

## Storage Characteristics
* CRUDJT **automatically removing expired tokens** after start and every 24 hours without blocking the main thread   
* **Storage automatically fsyncs every 500ms**, meanwhile tokens ​​are available from cache

# Multi-process Coordination
For multi-process scenarios, CRUDJT uses gRPC over an insecure local port for same-host communication only. It is not intended for inter-machine or internet-facing usage

# Limits
The library has the following limits and requirements

- **Java version:** tested with 17.0.16
- **Supported platforms:** Linux, macOS (x86_64 / arm64)
- **Maximum json size per token:** 256 bytes
- **`secret_key` format:** must be Base64
- **`secret_key` size:** must be 32, 48, or 64 bytes

# Contact & Support
<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/crudjt/crudjt/refs/heads/master/logos/crudjt_favicon_160x160_white_on_dark.svg" width=160 height=160>
    <source media="(prefers-color-scheme: light)" srcset="https://raw.githubusercontent.com/crudjt/crudjt/refs/heads/master/logos/crudjt_favicon_160x160_dark_on_white.svg" width=160 height=160>
    <img alt="Shows a dark favicon in light color mode and a white one in dark color mode" src="https://raw.githubusercontent.com/crudjt/crudjt/refs/heads/master/logos/crudjt_favicon_160x160_white.png" width=160 height=160>
  </picture>
</p>

- **Custom integrations / new features / collaboration**: support@crudjt.com  
- **Library support & bug reports:** [open an issue](https://github.com/crudjt/crudjt-java/issues)


# Lincense
CRUDJT is released under the [MIT License](LICENSE.txt)

<p align="center">
  💘 Shoot your g . ? Love me out via <a href="https://www.patreon.com/crudjt">Patreon Sponsors</a>!
</p>
