# LDAP and WebUntis Java Demo

**LDAP only works inside HTL network, also not with VPN!!!**

A Java project demonstrating how to connect to and query an Active Directory (AD) via LDAP using JNDI. Supports user search by `sAMAccountName` and credential authentication against AD.

## Prerequisites

- Java 11+
- Maven 3.6+
- Access to an Active Directory / LDAP server
- Access to a Webuntis Server



## Configuration

Create a `.env` file in the project root:

```env
LDAP_URL=ldap://<your-ad-server>
LDAP_BIND_ON=<username>@@SYNCHTLINN.local
LDAP_PASSWORD=<password>

UNTIS_SERVER=https://<school>.webuntis.com
UNTIS_SCHOOL=<school-name>
UNTIS_USER=<username>
UNTIS_PASSWORD=<password>

MQTT_HOST=<hivemq-cloud-host>
MQTT_PORT=8883
MQTT_TOPIC=<topic>
MQTT_CLIENT_ID_PUB=htlinn-publisher
MQTT_CLIENT_ID_SUB=htlinn-subscriber
MQTT_USERNAME=<mqtt-username>
MQTT_PASSWORD=<mqtt-password>
```

Values for HTL Anichstraße:

- UNTIS_SERVER=https://htl1-innsbruck.webuntis.com
- UNTIS_SCHOOL=htl1-innsbruck

> The `.env` file is loaded via [dotenv-java](https://github.com/cdimascio/dotenv-java). Never commit it to version control.

## Dependencies

| Dependency | Version | Purpose |
|---|---|---|
| `io.github.cdimascio:dotenv-java` | 3.0.0 | Load environment variables from `.env` |
| `com.github.untisapi:untis4j` | v1.3.6 | Untis API client (hosted on GitHub Packages) |
| `com.microsoft.playwright:playwright` | 1.46.0 | Browser automation for WebUntis scraping |
| `com.hivemq:hivemq-mqtt-client` | 1.3.3 | MQTT client for HiveMQ Cloud |

## Usage

### LDAPWrapper

```java
LDAPWrapper ldapWrapper = new LDAPWrapper(url, bindDn, password);
ldapWrapper.searchByAccountName("a.greinoecker");
ldapWrapper.close();
```

The search filter targets `objectClass=person` members of the `All-Staff` group within `DC=SYNCHTLINN,DC=local`.

### WebUntisScraper

Scrapes the teacher timetable from WebUntis using a headless Chromium browser (Playwright). Credentials and server details are read from `.env`.

```java
try (WebUntisScraper scraper = new WebUntisScraper()) {
    List<WebUntisScraper.Lesson> lessons = scraper.scrapeMyTimetable();
    for (WebUntisScraper.Lesson lesson : lessons) {
        System.out.println(lesson.day() + " | " + lesson.subject() + " | " + lesson.room()
            + (lesson.cancelled() ? " [CANCELLED]" : ""));
    }
}
```

Each `Lesson` record exposes: `day`, `klasse`, `subject`, `room`, `cancelled`.

> **Note:** `setHeadless(false)` is currently set — a browser window will open during scraping.

### MqttPublisher

Connects to a HiveMQ Cloud broker (TLS, port 8883) and publishes 5 test messages to the configured topic.

```
Simply run main() — all parameters are read directly from .env.
```

Required `.env` entries:

| Key | Description |
|---|---|
| `MQTT_HOST` | HiveMQ Cloud hostname, e.g. `abc123.s1.eu.hivemq.cloud` |
| `MQTT_PORT` | Port — always `8883` for HiveMQ Cloud |
| `MQTT_TOPIC` | Topic to publish to, e.g. `htlinn/demo` |
| `MQTT_CLIENT_ID_PUB` | Unique client ID for the publisher |
| `MQTT_USERNAME` | HiveMQ Cloud username |
| `MQTT_PASSWORD` | HiveMQ Cloud password |

### MqttSubscriber

Connects to the same broker, subscribes to the configured topic, and prints every incoming message to the console. Runs until the program is stopped manually.

```
Simply run main() — all parameters are read directly from .env.
```

Required `.env` entries:

| Key | Description |
|---|---|
| `MQTT_HOST` | HiveMQ Cloud hostname |
| `MQTT_PORT` | Port — always `8883` for HiveMQ Cloud |
| `MQTT_TOPIC` | Topic to subscribe to |
| `MQTT_CLIENT_ID_SUB` | Unique client ID for the subscriber (must differ from the publisher) |
| `MQTT_USERNAME` | HiveMQ Cloud username |
| `MQTT_PASSWORD` | HiveMQ Cloud password |

> Start `MqttSubscriber` first, then `MqttPublisher` — both programs run independently.

---

## Troubleshooting

### `PartialResultException: Unprocessed Continuation Reference(s)`

**Symptom:** All results are printed correctly, but the program crashes at the end with:

```
Exception in thread "main" javax.naming.PartialResultException: Unprocessed Continuation Reference(s);
remaining name 'DC=SYNCHTLINN,DC=local'
```

**Cause:** Active Directory appends LDAP referrals (pointers to other domain controllers) after the actual results. The JNDI implementation throws a `PartialResultException` when it encounters these referrals, even though all real data has already been delivered. Setting `Context.REFERRAL = "ignore"` alone is not always sufficient.

**Fix:** Catch `PartialResultException` inside the result iteration loop:

```java
try {
    while (results.hasMore()) {
        // process results ...
    }
} catch (PartialResultException e) {
    // AD returns referrals after real results — all data already processed
}
```

---

### Maven sync fails with 401 Unauthorized for `untis4j`

**Symptom:**

```
Could not transfer artifact org.bytedream:untis4j:pom:1.3.1
from/to github (https://maven.pkg.github.com/untisapi/untis4j):
status code: 401, reason phrase: Unauthorized (401)
```

**Cause:** GitHub Packages requires authentication even for publicly listed packages. Maven has no credentials configured for the GitHub registry.

**Fix:**

**Step 1 — Create a GitHub Personal Access Token**

Go to: GitHub → Settings → Developer Settings → Personal Access Tokens → Tokens (classic)

Create a new token with the scope:
- `read:packages`

**Step 2 — Add credentials to `~/.m2/settings.xml`**

Create the file if it does not exist:

```xml
<settings>
  <servers>
    <server>
      <id>github</id>
      <username>YOUR_GITHUB_USERNAME</username>
      <password>YOUR_TOKEN</password>
    </server>
  </servers>
</settings>
```

**Step 3 — Verify `pom.xml` repository `id` matches**

The `<id>` in `pom.xml` must exactly match the `<id>` in `settings.xml`:

```xml
<repositories>
  <repository>
    <id>github</id>
    <url>https://maven.pkg.github.com/untisapi/untis4j</url>
  </repository>
</repositories>
```
