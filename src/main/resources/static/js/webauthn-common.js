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
