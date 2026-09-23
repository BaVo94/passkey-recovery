function getCsrfToken() {
    return document.querySelector('meta[name="_csrf"]').content;
}

function getCsrfHeader() {
    return document.querySelector('meta[name="_csrf_header"]').content;
}

async function postJson(url, body) {
    const response = await fetch(url, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            [getCsrfHeader()]: getCsrfToken()
        },
        body: typeof body === 'string' ? body : JSON.stringify(body)
    });
    if (!response.ok) {
        throw new Error(await response.text());
    }
    return response;
}

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