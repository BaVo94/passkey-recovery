document.getElementById('registerForm').addEventListener('submit', async (event) => {
    event.preventDefault();
    const status = document.getElementById('status');

    try {
        const startResponse = await postJson('/auth/register/start', {
            username: document.getElementById('username').value,
            displayName: document.getElementById('displayName').value
        });

        const optionsJson = await startResponse.json();
        const options = PublicKeyCredential.parseCreationOptionsFromJSON(optionsJson);

        const credential = await navigator.credentials.create({publicKey: options});

        await postJson('/auth/register/finish', JSON.stringify(credential.toJSON()));

        status.textContent = 'Registrierung erfolgreich.';
    } catch (error) {
        status.textContent = 'Fehlgeschlagen: ' + error.message;
    }
});