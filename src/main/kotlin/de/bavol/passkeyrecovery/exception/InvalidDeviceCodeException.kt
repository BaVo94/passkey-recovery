package de.bavol.passkeyrecovery.exception

sealed class InvalidDeviceCodeException(message: String) : PasskeyRecoveryException(message)

class DeviceCodeUnknownException :
    InvalidDeviceCodeException("Zu dem eingegebenen Gerätecode existiert keine Anmeldeanfrage.")

class DeviceCodeExpiredException :
    InvalidDeviceCodeException("Der Gerätecode ist abgelaufen.")

class DeviceCodeAlreadyUsedException :
    InvalidDeviceCodeException("Der Gerätecode wurde bereits verwendet.")
