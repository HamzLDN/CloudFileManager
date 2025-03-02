// script.js

function CheckStatus(callback) {
    try {
        var firstname = encodeURIComponent(document.getElementById("firstName").value);
        var lastname = encodeURIComponent(document.getElementById("lastName").value);
        var username = encodeURIComponent(document.getElementById("username").value);
        var password = encodeURIComponent(document.getElementById("password").value);

        var data = "firstname=" + firstname + "&lastname=" + lastname + "&username=" + username + "&password=" + password;

        // Make an AJAX request to the /signup endpoint
        var xhr = new XMLHttpRequest();
        xhr.open("POST", "/signup", true);
        xhr.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
        xhr.onreadystatechange = function () {
            if (xhr.readyState === XMLHttpRequest.DONE) {
                console.log("GOT DATA: " + xhr.responseText);
                callback(xhr.responseText); // Pass response to the callback
            }
        };
        xhr.send(data);
    } catch (error) {
        console.error("An error occurred:", error);
    }
}

function SignUp() {
    console.log("SIGNING UP");
    const statusContainer = document.getElementById('statusContainer');

    // Call CheckStatus with a callback function
    CheckStatus(function(status) {
        console.log(status);
        if (status === 'success') {
            console.log("SUCCESS");
            statusContainer.insertAdjacentHTML(
                'afterend',
                '<h6>Signup Successful!</h6><center><a href="/login" ><p><a href"/login"> login here </a><p></a><center>');

            statusContainer.classList.add('success');
        } else if (status === 'taken') {
            statusContainer.innerHTML = '<h3>Signup Failed. Username has been taken</h3>';
            statusContainer.classList.add('failed');
        } else if (status === 'InvalidCharacters') {
            statusContainer.innerHTML = '<h3>Username must contain letters or numbers!</h3>';
            statusContainer.classList.add('failed');
        } else {
            console.log("CANT COMPARE");
        }
    });
}
