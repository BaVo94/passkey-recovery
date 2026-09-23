package de.bavol.passkeyrecovery.exception

import java.util.UUID

class UserNotFoundException : PasskeyRecoveryException {
    constructor(id: UUID) : super("Kein Nutzer mit der ID '$id' gefunden.")
    constructor(username: String) : super("Kein Nutzer mit dem Namen '$username' gefunden.")
}
