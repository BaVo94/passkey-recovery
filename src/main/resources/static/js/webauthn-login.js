document.getElementById('loginButton').addEventListener('click', async () => {
    const status = document.getElementById('status');

    try {
        const startResponse = await postJson('/auth/login/start', {});
        const optionsJson = await startResponse.json();
        const options = PublicKeyCredential.parseRequestOptionsFromJSON(optionsJson);

        const credential = await navigator.credentials.get({publicKey: options});

        await postJson('/auth/login/finish', JSON.stringify(credential.toJSON()));

        status.textContent = 'Anmeldung erfolgreich.';
    } catch (error) {
        status.textContent = 'Fehlgeschlagen: ' + error.message;
    }
});