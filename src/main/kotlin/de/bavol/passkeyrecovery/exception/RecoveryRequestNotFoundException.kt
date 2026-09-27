package de.bavol.passkeyrecovery.exception

import java.util.UUID

class RecoveryRequestNotFoundException(id: UUID) :
    PasskeyRecoveryException("Kein Request mit der ID $id gefunden.")
