document.getElementById('addPasskeyButton').addEventListener('click', async () => {
    const status = document.getElementById('status');

    try {
        const startResponse = await postJson('/passkeylist/add/start', {});

        const optionsJson = await startResponse.json();
        const options = PublicKeyCredential.parseCreationOptionsFromJSON(optionsJson);

        const credential = await navigator.credentials.create({publicKey: options});

        await postJson('/passkeylist/add/finish', JSON.stringify(credential.toJSON()));

        window.location.href = '/passkeylist';
    } catch (error) {
        status.textContent = 'Fehlgeschlagen: ' + error.message;
    }
});