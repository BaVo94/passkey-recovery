const status = document.getElementById('status');
const POLL_INTERVAL_MS = 2000;

async function poll() {
    try {
        const response = await fetch('/devicelogin/status');
        const result = await response.json();

        if (result.status === 'APPROVED') {
            await postJson('/devicelogin/complete', {});
            window.location.href = '/passkeylist';
            return;
        }

        if (result.status === 'EXPIRED') {
            status.textContent = 'Der Code ist abgelaufen. Bitte starten Sie den Vorgang erneut.';
            return;
        }

        setTimeout(poll, POLL_INTERVAL_MS);
    } catch (error) {
        status.textContent = 'Fehler bei der Statusabfrage: ' + error.message;
    }
}

poll();