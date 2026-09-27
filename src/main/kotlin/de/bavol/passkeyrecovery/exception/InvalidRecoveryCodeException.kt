package de.bavol.passkeyrecovery.exception

import de.bavol.passkeyrecovery.model.RecoveryStatus
import java.util.UUID

sealed class InvalidRecoveryCodeException(message: String) : PasskeyRecoveryException(message)

class RecoveryCodeUnknownException :
    InvalidRecoveryCodeException("Zu dem eingegebenen Code existiert keine Wiederherstellungsanfrage.")

class RecoveryCodeExpiredException(id: UUID?) :
    InvalidRecoveryCodeException("Der Code der Anfrage $id ist abgelaufen.")

class RecoveryCodeNotApprovedException(id: UUID?, status: RecoveryStatus) :
    InvalidRecoveryCodeException("Die Anfrage $id ist nicht freigegeben, sondern im Status $status.")
