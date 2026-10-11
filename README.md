# passkey-recovery

Prototyp einer vollständig passwortfreien Webanwendung mit Passkey-Authentifizierung und
Konto-Wiederherstellung nach den Empfehlungen der FIDO-Alliance.

Die Anwendung ist im Rahmen einer Bachelorarbeit an der Frankfurt University of Applied
Sciences entstanden. Sie dient als praktischer Teil der Untersuchung und ist ausdrücklich
kein produktionsreifes System. Die bewusst in Kauf genommenen Einschränkungen sind in der
Arbeit dokumentiert.

## Funktionsumfang

- Registrierung und Anmeldung ausschließlich über Passkeys, ohne nutzerdefinierte Passwörter
- Verwaltung der registrierten Passkeys mit Anzeige von Modell, Erstellungszeitpunkt,
  letzter Nutzung und Sicherungszustand
- Hinweis-Banner, das zur Einrichtung eines zweiten Authenticators auffordert
- Widerruf einzelner Passkeys
- Erstanmeldung auf einem Gerät ohne eigenen Passkey, freigegeben aus einer bestehenden,
  passkey-authentifizierten Sitzung
- Konto-Wiederherstellung nach Freigabe durch einen Administrator
- Löschung des eigenen Kontos mit vorheriger Reauthentifizierung

## Voraussetzungen

- JDK 21
- PostgreSQL 17 (ältere Versionen ab 14 sollten ebenfalls funktionieren, sind aber nicht getestet)
- Ein Browser mit WebAuthn-Unterstützung und ein Authenticator, also etwa Windows Hello,
  Touch ID, ein Passwortmanager mit Passkey-Unterstützung oder ein Smartphone über den
  Hybrid-Transport

Gradle muss nicht installiert sein, das Projekt bringt den Gradle Wrapper mit.

## Einrichtung

1. Datenbank anlegen:

   ```sql
   CREATE DATABASE passkey_recovery;
   ```

   Erwartet wird eine Verbindung auf `localhost:5432` mit dem Benutzer `postgres`. Beides
   lässt sich in `src/main/resources/application.properties` ändern. Das Schema wird beim
   ersten Start automatisch erzeugt (`spring.jpa.hibernate.ddl-auto=update`).

2. Datenbankpasswort als Umgebungsvariable setzen:

   ```bash
   export DB_PASSWORD=dein_passwort
   ```

   Unter Windows in der PowerShell:

   ```powershell
   $env:DB_PASSWORD = "dein_passwort"
   ```

3. Anwendung starten:

   ```bash
   ./gradlew bootRun
   ```

4. Im Browser `http://localhost:8080` aufrufen.

## Wichtig: nur über localhost erreichbar

WebAuthn steht nur in einem Secure Context zur Verfügung, also über HTTPS. Die einzige
Ausnahme ist `localhost`. Ein Aufruf über die Netzwerkadresse des Rechners, etwa
`http://192.168.0.10:8080`, funktioniert deshalb nicht: Die Secure-Context-Bedingung ist
nicht erfüllt, und eine IP-Adresse ist als RP-ID nicht zulässig.

Für Tests über zwei Geräte hinweg, die etwa die Erstanmeldung benötigt, gibt es zwei Wege:

- eine Portweiterleitung, durch die das zweite Gerät die Anwendung ebenfalls unter
  `localhost:8080` erreicht, oder
- der Betrieb unter HTTPS mit einem auflösbaren Domainnamen und einem vertrauenswürdigen
  Zertifikat. In diesem Fall müssen `app.webauthn.rp-id` und `app.webauthn.origin`
  angepasst werden.

Zu beachten ist, dass eine Änderung der RP-ID alle bereits registrierten Passkeys entwertet,
da sie Bestandteil der signierten Daten ist.

## Administratorrolle vergeben

Die Wiederherstellung eines Kontos setzt einen Administrator voraus, der die Identität des
Nutzers prüft und die Anfrage freigibt. Da eine Vergabe innerhalb der Anwendung bereits
einen Administrator voraussetzen würde, erfolgt sie beim ersten Mal direkt in der Datenbank:

```sql
UPDATE users SET role = 'ADMIN' WHERE username = 'dein_kontoname';
```

Nach einer erneuten Anmeldung erscheint der Menüpunkt `Admin`.

## Konfiguration

Alle anwendungsspezifischen Werte stehen in `src/main/resources/application.properties`:

| Schlüssel | Bedeutung | Vorgabe |
| --- | --- | --- |
| `app.webauthn.rp-id` | RP-ID der Relying Party | `localhost` |
| `app.webauthn.rp-name` | Anzeigename im Authenticator-Dialog | `Passkey Recovery Prototype` |
| `app.webauthn.origin` | Erwarteter Origin bei der Verifikation | `http://localhost:8080` |
| `app.recovery.code-validity` | Gültigkeit des Freischaltcodes | `7d` |
| `app.recovery.device-code-validity` | Gültigkeit des Gerätecodes | `5m` |

## Tests

```bash
./gradlew test
```

Die Tests laufen gegen eine H2-Datenbank im Arbeitsspeicher, eine laufende PostgreSQL-Instanz
ist dafür nicht nötig. Es handelt sich lediglich um den von Spring Boot erzeugten
Kontexttest; eine inhaltliche Testabdeckung besteht nicht und ist in der Arbeit als
Einschränkung benannt.

## Technologien

Kotlin 2.3, Spring Boot 4.1, Spring Security, Spring Data JPA, Thymeleaf, WebAuthn4J 0.31.10,
PostgreSQL.
