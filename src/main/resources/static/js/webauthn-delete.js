const confirmInput = document.getElementById('confirmUsername');
const deleteButton = document.getElementById('deleteAccountButton');

confirmInput.addEventListener('input', () => {
    deleteButton.disabled = confirmInput.value !== expectedUsername;
});

deleteButton.addEventListener('click', async () => {
    const status = document.getElementById('status');

    try {
        const startResponse = await postJson('/account/delete/start', {});
        const optionsJson = await startResponse.json();
        const options = PublicKeyCredential.parseRequestOptionsFromJSON(optionsJson);

        const credential = await navigator.credentials.get({publicKey: options});

        await postJson('/account/delete/finish', JSON.stringify(credential.toJSON()));

        window.location.href = '/';
    } catch (error) {
        status.textContent = 'Fehlgeschlagen: ' + error.message;
    }
});