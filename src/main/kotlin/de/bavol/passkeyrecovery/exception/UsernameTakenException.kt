package de.bavol.passkeyrecovery.exception

class UsernameTakenException(username: String) :
    PasskeyRecoveryException("Der Nutzername '$username' ist bereits vergeben.")
