<p align="center">
  <img src="logos/crud_jt_logo_black.png" alt="Logo Light" />
</p>

<p align="center">
  Simplifies user session. Login/Logout/Authorization
</p>

<p align="center">
  <a href="https://www.patreon.com/exwarvlad">
    <img src="logos/buy_me_a_coffee_orange.svg" alt="Buy Me a Coffee"/>
  </a>
</p>

# Installation

The compiled library is available in two ways:

## Maven

The crud_jt-java is available in Maven Central

```xml
<dependencies>
    <dependency>
      <groupId>com.github</groupId>
      <artifactId>crud_jt-java</artifactId>
      <version>1.0.0</version>
    </dependency>
</dependencies>
```

## Gradle

```txt
dependencies {
  implementation 'com.github:crud_jt-java:1.0.0'
}
```

Configure CRUD JT in your project

```java
// openssl rand -base64 48 # In your terminal
// => your_encrypted_base64/48
CRUD_JT.Config
    .encrypted_key("your_encrypted_base64/32/48/64")
    .store_jt_path("your_path_to_file_storage") // optional
    .start();
```

# C

```java
CRUD_JT.create(Map.of("user_id", 42, "role", 11))
=> "HBmKFXoXgJ46mCqer1WXyQ"
```

```java
// with ttl — token time-to-live in seconds
ttl = 3600 * 24 * 30

CRUD_JT.create(Map.of("user_id", 42, "role", 11), ttl)
=> "HBmKFXoXgJ46mCqer1WXyQ"
```

```java
// with silence_read — silently read the token a specified number of times, then delete it permanently
silence_read = 3

CRUD_JT.create(Map.of("user_id", 42, "role", 11), None, silence_read)
=> "HBmKFXoXgJ46mCqer1WXyQ"
```

```java
// with ttl and silence_read
ttl = 3600 * 24 * 30
silence_read = 3

CRUD_JT.create(Map.of("user_id", 42, "role", 11), ttl, silence_read)
=> "HBmKFXoXgJ46mCqer1WXyQ"
```

# R

```java
// ...
CRUD_JT.read("HBmKFXoXgJ46mCqer1WXyQ")
=> {data={user_id=42, role=11}}
```

```java
// with ttl
CRUD_JT.read("HBmKFXoXgJ46mCqer1WXyQ")
=> {metadata={ttl=3}, data={user_id=42, role=11}}

// after 1 second
CRUD_JT.read("HBmKFXoXgJ46mCqer1WXyQ")
=> {metadata={ttl=2}, data={user_id=42, role=11}}

// still second
CRUD_JT.read("HBmKFXoXgJ46mCqer1WXyQ")
=> {metadata={ttl=1}, data={user_id=42, role=11}}

// ups
CRUD_JT.read("HBmKFXoXgJ46mCqer1WXyQ")
=> null
```

```java
// with silence_read
CRUD_JT.read("HBmKFXoXgJ46mCqer1WXyQ")
=> {metadata={silence_read=2}, data={user_id=42, role=11}}

// after 1 read
CRUD_JT.read("HBmKFXoXgJ46mCqer1WXyQ")
=> {metadata={silence_read=1}, data={user_id=42, role=11}}

// still one read
CRUD_JT.read("HBmKFXoXgJ46mCqer1WXyQ")
=> {metadata={silence_read=0}, data={user_id=42, role=11}}

// ups
CRUD_JT.read("HBmKFXoXgJ46mCqer1WXyQ")
=> null
```

```java
// with ttl and silence_read
CRUD_JT.read("HBmKFXoXgJ46mCqer1WXyQ")
=> {metadata={ttl=88, silence_read=0}, data={user_id=42, role=11}}
// ...
```

# U

```java
CRUD_JT.update("HBmKFXoXgJ46mCqer1WXyQ", Map.of("user_id", 42, "role", 8))
=> true // {data={user_id=42, role=8}}
```

```java
// supports for ttl and/or silence_read
ttl = 41
silence_read = 5

CRUD_JT.update("HBmKFXoXgJ46mCqer1WXyQ", Map.of("user_id", 42, "role", 8), ttl, silence_read)
=> true // {metadata={ttl=41, silence_read=5}, data={user_id=42, role=8}}
```

```java
// when expired/not found token
CRUD_JT.update("HBmKFXoXgJ46mCqer1WXyQ", Map.of("user_id", 42, "role", 8))
=> false
```

# D
```java
// when token exist
CRUD_JT.delete("HBmKFXoXgJ46mCqer1WXyQ")
=> true
```

```java
// when expired/not found token
CRUD_JT.delete("HBmKFXoXgJ46mCqer1WXyQ")
=> false
```

# Performance
**40k** requests of **256 bytes** — median over 10 runs  
ARM64 (Apple M1+), macOS 15.6/15.6.1  
Java 17.0.16/21.0.8

| Function | CRUD JT (Java) | JWT (Java) | redis-session-store (Ruby, Rails 8.0.4) |
|----------|-------|------|------|
| C        | 0.423 second | 0.207 second ⭐ | 4.057 seconds |
| R        | `0.023 second` ![Logo Favicon Light](logos/crud_jt_logo_favicon_white.png) | 0.297 second | 7.011 seconds |
| U        | `0.535 second` ![Logo Favicon Light](logos/crud_jt_logo_favicon_white.png) | X | 3.49 seconds |
| D        | `0.277 second` ![Logo Favicon Light](logos/crud_jt_logo_favicon_white.png) | X | 6.589 seconds |

[Full results](https://github.com/exwarvlad/benchmarks)

# Storage (Store JT)

## Path Lookup Order
Stored tokens are placed in the **file system** according to the following order

1. Explicitly set via `CRUD_JT.Config.store_jt_path('custom/path/to/file_system_db');`
2. Default system location
   - **Linux**: `/var/lib/store_jt`
   - **macOS**: `/usr/local/var/store_jt`
   - **Windows**: `C:\Program Files\store_jt`
3. Project root directory (fallback)

## Storage Characteristics
* Store JT **automatically removing expired tokens** every 24 hours without blocking the main thread   
* **Store JT automatically fsyncs every 500ms**, meanwhile tokens ​​are available from cache
* Store JT is available for one process to open per instance for the time being

## Configuration

You can configure the library before starting it

```java
// Required configuration
CRUD_JT.Config.encrypted_key("some_base64_key");

// Optional configuration
CRUD_JT.Config.store_jt_path("/custom/path/to/store_jt");

// Start the CRUD JT and Store JT
CRUD_JT.Config.start();
```


#### `encrypted_key(base64_key)`
Sets the encrypted key (**required**)

#### `store_jt_path(path_to_db)`
Overrides the default Store JT path (**optional**)

#### `start!`
Initializes the CRUD JT and opens the Store JT (**must be called last**)

# Limits
The library has the following limits and requirements

- **Python version:** tested with 3.12.5
- **Supported platforms:** Linux, macOS (x86_64 / arm64)
- **Maximum json size per token:** 256 bytes
- **`encrypted_key` format:** must be Base64
- **`encrypted_key` size:** must be 32, 48, or 64 bytes

# Contact & Support
<p align="center">
  <img src="logos/crud_jt_logo_favicon_black_160.png" alt="Visit Light" />
</p>

- **Custom integrations / new features / collaboration**: support@crudjt.com  
- **Library support & bug reports:** [open an issue](https://github.com/crud_jt/crud_jt-ruby/issues)


# Lincense
CRUD JT is released under the [MIT License](LICENSE.txt)

<p align="center">
  💘 Shoot your g . ? Love me out via <a href="https://www.patreon.com/exwarvlad">Github Sponsors</a>!
</p>
