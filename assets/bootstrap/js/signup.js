// script.js

function getQueryParam(name) {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get(name);
}

function CheckStatus() {
    const status = getQueryParam('status');
    const statusContainer = document.getElementById('statusContainer');

    if (status === 'success') {
        statusContainer.innerHTML = '<h6>Signup Successful!</h6><center><a href="/login" ><p>login here<p></a><center>';
        statusContainer.classList.add('success');
        console.log("SUCCESS");
    } else if (status === 'taken') {
        statusContainer.innerHTML = '<h3>Signup Failed. Username has been taken</h3>';
        statusContainer.classList.add('failed');
    } else if (status === 'InvalidCharacters') {
        statusContainer.innerHTML = '<h3>Username must contain letters or numbers!</h3>';
        statusContainer.classList.add('failed');
    }
}

document.addEventListener('DOMContentLoaded', CheckStatus);
