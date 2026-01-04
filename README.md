<p align="center">
  <img src="logos/crud_jt_logo_black.png#gh-light-mode-only" alt="Logo Light" />
  <img src="logos/crud_jt_logo.png#gh-dark-mode-only" alt="Logo Dark" />
</p>

<p align="center">
  Fast, file-backed JSON token for REST APIs with multi-process support
</p>

<p align="center">
  <a href="https://www.patreon.com/crudjt">
    <img src="logos/buy_me_a_coffee_orange.svg" alt="Buy Me a Coffee"/>
  </a>
</p>

## Why?  
[Escape the JWT trap: predictable login, safe logout](https://medium.com/@CoffeeMainer/jwt-trap-login-logout-under-control-7f4495d6024d)

# Installation

The compiled library is available in two ways:

## Maven

The crudjt-java is available in Maven Central

```xml
<dependencies>
    <dependency>
      <groupId>com.github</groupId>
      <artifactId>crudjt-java</artifactId>
      <version>1.0.0</version>
    </dependency>
</dependencies>
```

## Gradle

```txt
dependencies {
  implementation 'com.github:crudjt-java:1.0.0'
}
```

Start CRUDJT master in your project  

```java
// openssl rand -base64 48 // In your terminal
// => your_encrypted_base64/48
CRUDJT.Config.startMaster(
    Map.of(
        "encrypted_key", "your_encrypted_base64/32/48/64",
        "store_jt_path", "your_path_to_file_storage", // optional
        "grpc_host", "127.0.0.1", // default
        "grpc_port", 50051 // default
    )
);
```

Or connect to master  

```java
CRUDJT.Config.connectToMaster(
    Map.of(
        "grpc_host", "127.0.0.1", // default
        "grpc_port", 50051 // default
    )
);
```

# C

```java
Map<String, Object> data = Map.of("user_id", 42, "role", 11); // required
long ttl = 3600 * 24 * 30 // optional // Dynamic time to live token in seconds

// Optional // Each read decrements silence_read by 1, when the counter reaches
// zero — the token is deleted permanently
long silence_read = 10;

CRUDJT.create(data, ttl, silence_read);
=> "HBmKFXoXgJ46mCqer1WXyQ"
```

# R

```java
CRUDJT.read("HBmKFXoXgJ46mCqer1WXyQ");
=> {metadata={ttl=101001, silence_read=9}, data={user_id=42, role=11}}
```

```java
// when expired/not found token
CRUDJT.read("HBmKFXoXgJ46mCqer1WXyQ");
=> null
```

# U

```java
CRUDJT.update("HBmKFXoXgJ46mCqer1WXyQ", Map.of("user_id", 42, "role", 8), 600, 100);
=> true // {metadata={ttl=600, silence_read=100}, data={user_id=42, role=8}}
```

```java
// when expired/not found token
CRUDJT.update("HBmKFXoXgJ46mCqer1WXyQ", Map.of("user_id", 42, "role", 8));
=> false
```

# D
```java
CRUDJT.delete("HBmKFXoXgJ46mCqer1WXyQ");
=> true
```

```java
// when expired/not found token
CRUDJT.delete("HBmKFXoXgJ46mCqer1WXyQ");
=> false
```

# Performance
**40k** requests of **256 bytes** — median over 10 runs  
ARM64 (Apple M1+), macOS 15.6/15.6.1  
Java 17.0.16/21.0.8

| Function | CRUDJT (Java) | JWT (Java) | redis-session-store (Ruby, Rails 8.0.4) |
|----------|-------|------|------|
| C        | 0.423 second | 0.207 second ⭐ | 4.057 seconds |
| R        | `0.023 second` ![Logo Favicon Light](logos/crud_jt_logo_favicon_white.png#gh-light-mode-only) ![Logo Favicon Dark](logos/crud_jt_logo_favicon_black.png#gh-dark-mode-only) | 0.297 second | 7.011 seconds |
| U        | `0.535 second` ![Logo Favicon Light](logos/crud_jt_logo_favicon_white.png#gh-light-mode-only) ![Logo Favicon Dark](logos/crud_jt_logo_favicon_black.png#gh-dark-mode-only) | X | 3.49 seconds |
| D        | `0.277 second` ![Logo Favicon Light](logos/crud_jt_logo_favicon_white.png#gh-light-mode-only) ![Logo Favicon Dark](logos/crud_jt_logo_favicon_black.png#gh-dark-mode-only) | X | 6.589 seconds |

[Full benchmark results](https://github.com/exwarvlad/benchmarks)

# Storage (File-backed)  

## Disk footprint  
**40k** tokens of **256 bytes** each — median over 10 creates  
darwin23, APFS  

`48 MB`  

[Full disk footprint results](https://github.com/Cm7B68NWsMNNYjzMDREacmpe5sI1o0g40ZC9w1y/disk_footprint)

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
- **`encrypted_key` format:** must be Base64
- **`encrypted_key` size:** must be 32, 48, or 64 bytes

# Contact & Support
<p align="center">
  <img src="logos/crud_jt_logo_favicon_black_160.png#gh-light-mode-only" alt="Visit Light" />
  <img src="logos/crud_jt_logo_favicon_white_160.png#gh-dark-mode-only" alt="Visit Dark" />
</p>

- **Custom integrations / new features / collaboration**: support@crudjt.com  
- **Library support & bug reports:** [open an issue](https://github.com/crudjt/crudjt-java/issues)


# Lincense
CRUDJT is released under the [MIT License](LICENSE.txt)

<p align="center">
  💘 Shoot your g . ? Love me out via <a href="https://www.patreon.com/crudjt">Patreon Sponsors</a>!
</p>
