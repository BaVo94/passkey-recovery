package de.bavol.passkeyrecovery.exception

import de.bavol.passkeyrecovery.model.RecoveryStatus
import java.util.UUID

class InvalidRecoveryRequestStateException(id: UUID, status: RecoveryStatus) :
    PasskeyRecoveryException("Der Status des Requests $id ist $status.")
