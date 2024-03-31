
window.onload = function () {
    listfiles();
};
function listfiles() {
    var path = window.location.pathname;
    var index = path.indexOf("/filemanager");
    var endpoint=null; 
    let nonEmptyParts = ''; 
    var currentPath = "/filemanager";   
    if (index !== -1) {
        endpoint = path.substring(index + "/filemanager".length);
        const filepath = document.getElementById('filepath');
        const filepathValue = endpoint;
        const parts = filepathValue.split('/');
        nonEmptyParts = parts.filter(part => part !== '');
        
        for (var i in nonEmptyParts) {
            var linkElement = document.createElement('a');
            currentPath += "/" + nonEmptyParts[i];
            linkElement.href = currentPath;
            linkElement.innerText = ` / ${decodeURIComponent(nonEmptyParts[i])}`;
            linkElement.style.textDecoration = 'none';
            linkElement.style.color = 'white';
            filepath.appendChild(linkElement);
        }
    } else {
        console.log("/filemanager not found in the path");
    }
    fetch('/fetchdata' + endpoint)
        .then(response => response.json())
        .then(data => {
            const ulElement = document.getElementById('fileList');
            var valueAttribute = ulElement.getAttribute("value");
            const file = nonEmptyParts[nonEmptyParts.length - 1];
            const keys = Object.keys(data[0]);
            if (keys[0] == "content") {
                if (valueAttribute == "1") {
                    const htmlContent = `
                    <div class="container">
                        <table class="header-table">
                        <tr>
                            <th>
                            <div style="display: flex; justify-content: space-between;">
                                <div>
                                    <button>
                                        <i class="fas fa-trash-alt"></i> <!-- Font Awesome trash icon -->
                                    </button>
                                    <button onclick="uploadFile()">
                                        <i class="fas fa-upload"></i> <!-- Font Awesome upload icon -->
                                    </button>
                                </div>
                                <div>
                                    <button onclick="downloadContent('${endpoint}', '${nonEmptyParts[nonEmptyParts.length - 1]}')">
                                        <i class="fas fa-download"></i> <!-- Font Awesome download icon -->
                                    </button>
                                    <button onclick="copyContent()">
                                        <i class="far fa-copy"></i> <!-- Font Awesome copy icon -->
                                    </button>
                                </div>

                            </div>
                            </th>
                        </tr>
                        </table>
                        <div id="output"></div>
                        `;
                ulElement.innerHTML += htmlContent;
                
                if (file.endsWith(".png") || file.endsWith(".jpeg") || file.endsWith(".gif") || file.endsWith(".jpg")) {
                    const base64String = data[0].content;
                    const binaryString = atob(base64String);
                    const binaryData = new Uint8Array(binaryString.length);
                    for (let i = 0; i < binaryString.length; i++) {
                        binaryData[i] = binaryString.charCodeAt(i);
                    }
                    const imageType = file.endsWith('.png') ? 'image/png' :
                                    file.endsWith('.jpg') ? 'image/jpeg' :
                                    file.endsWith('.gif') ? 'image/gif' :
                                    'image/png';

                    const blob = new Blob([binaryData], { type: imageType });

                    const imgElement = document.createElement('img');
                    imgElement.src = URL.createObjectURL(blob);
                    imgElement.style.width = '100%';
                    imgElement.style.height = '100%';
                    imgElement.style.display = 'block';
                    imgElement.style.margin = 'auto';

                    const outputDiv = document.getElementById("output");
                    outputDiv.appendChild(imgElement);
                } else if (file.endsWith(".MOV")) {
                    var binaryData = atob(data[0].content);

                    // Create a Uint8Array from the binary data
                    var uint8Array = new Uint8Array(binaryData.length);
                    for (var i = 0; i < binaryData.length; i++) {
                        uint8Array[i] = binaryData.charCodeAt(i);
                    }

                    // Create a Blob from the Uint8Array
                    var blob = new Blob([uint8Array], { type: 'video/quicktime' });

                    // Create an object URL from the Blob
                    var objectUrl = URL.createObjectURL(blob);

                    // Create the video element dynamically
                    var videoPlayer = document.createElement('video');
                    videoPlayer.width = 640;
                    videoPlayer.height = 360;
                    videoPlayer.controls = true;
                    videoPlayer.src = objectUrl;
                    videoPlayer.type = 'video/quicktime';

                    // Append the video element to the 'output' div
                    document.getElementById('output').appendChild(videoPlayer);

                    videoPlayer.play();
                } else
                {
                    const textareaElement = document.createElement("textarea");
                    textareaElement.rows = 3; // Adjust the number of rows as needed
                    textareaElement.className = "content-textarea";
                    // Center the textarea horizontally
                    textareaElement.style.display = 'block';
                    textareaElement.style.margin = 'auto';
                    // Start
                    const encrypted_value = new TextEncoder().encode(data[0].content);
                    const hash = new TextEncoder().encode(getCookie("KeyHash"));
                    const decrypted_content = xor(encrypted_value, hash);

                    // Convert the decrypted content Uint8Array to string
                    const decrypted_string = new TextDecoder().decode(decrypted_content);
                    //END OF DECRYPTION
                    textareaElement.textContent = decrypted_string;

                    const outputDiv = document.getElementById("output");
                    outputDiv.appendChild(textareaElement);
                }
                return;
                } else {
                    const htmlContent = `
                    <div class="container">
                        <table class="header-table">
                        <tr>
                            <th>
                            <div style="display: flex; justify-content: space-between;">
                                <div>
                                    <button onclick="downloadContent('${endpoint}', '${file}')">
                                        <i class="fas fa-download"></i> <!-- Font Awesome download icon -->
                                    </button>
                                </div>
                                <div>

                                    <button onclick="copyContent()">
                                        <i class="far fa-copy"></i> <!-- Font Awesome copy icon -->
                                    </button>
                                </div>

                            </div>
                            </th>
                        </tr>
                        </table>
                        `;
                ulElement.innerHTML += htmlContent;
                return;
                }
            } else if (keys[0] == "status") {
                const ulElement = document.getElementById('fileList');
                const htmlContent = '<center><h3 style="font-family:Times New Roman;color:red;white-space:pre-wrap">ERROR FINDING FILE: 404</h3></center>';
                ulElement.innerHTML += htmlContent;
                return
            } else {
                if (valueAttribute == "1") {
                const htmlContent = `<div class='file-upload'>
                    <button id='uploadButton' onclick=upload('${currentPath}')>Upload file/folder</button>
                </div>`
                ulElement.innerHTML += htmlContent;
                }
            }
            const fileListElement = document.getElementById('fileList');
            const table = document.createElement('table');
            table.className = 'file-table';
            const tableHeader = document.createElement('thead');
            const headerRow = document.createElement('tr');

            const fileHeader = document.createElement('th');
            fileHeader.textContent = 'Contents';
            const deleteHeader = document.createElement('th');
            deleteHeader.textContent = 'Delete';
            const timestampHeader = document.createElement('th');
            timestampHeader.textContent = 'Timestamp';

            headerRow.appendChild(fileHeader);
            headerRow.appendChild(timestampHeader);
            headerRow.appendChild(deleteHeader);
            tableHeader.appendChild(headerRow);
            table.appendChild(tableHeader);

            const tableBody = document.createElement('tbody')

            if (keys[0] == "error") {
                ulElement.innerHTML += "<center><h1 style='color: red;'>You have no files</h1><center>";
                return;
            }

            let currentId = 1;
            data.forEach(entry => {
                const fileName = entry.file;
                const timestamp = entry.timestamp;

                const tableRow = document.createElement('tr');
                tableRow.id = "row_" + currentId; // Assign the unique identifier to the id attribute

                const fileNameCell = document.createElement('td');
                const fileLink = document.createElement('a');
                fileLink.href = "/filemanager" + endpoint + "/" + fileName;
                fileLink.textContent = fileName;
                fileNameCell.appendChild(fileLink);

                const deletefilecell = document.createElement('td');
                const deletefilelink = document.createElement('button');
                const icon = document.createElement('i');
                icon.classList.add("fas", "fa-trash-alt");
                deletefilelink.appendChild(icon);

                deletefilelink.onclick = function() {
                    const confirmDelete = confirm('Are you sure?');
                    if (confirmDelete) {
                        // Perform deletion logic here
                        deletetable(tableRow.id, "/deletecontent" + endpoint + "/" + fileName);
                    }
                };

                deletefilecell.appendChild(deletefilelink);

                // Timestamp Cell
                const timestampCell = document.createElement('td');
                timestampCell.textContent = timestamp;

                // Append cells to the row
                tableRow.appendChild(fileNameCell);
                tableRow.appendChild(timestampCell);
                tableRow.appendChild(deletefilecell);

                // Append row to the table body
                tableBody.appendChild(tableRow);

                // Increment the ID for the next iteration
                currentId++;
            });
            // Append the table body to the table
            table.appendChild(tableBody);


            table.appendChild(tableBody);
            fileListElement.appendChild(table);
        })
        .catch(error => console.error('Error fetching data:', error));
};

function deleteFile(button) {
    var listItem = button.closest('.fileItem');
    listItem.remove();
}

function copyContent() {
  const textarea = document.querySelector('.content-textarea');
  textarea.select();

  try {
    document.execCommand('copy');
    console.log('Content copied to clipboard!');
  } catch (err) {
    console.error('Unable to copy content to clipboard', err);
  }

  window.getSelection().removeAllRanges();
}

function upload(endpoint) {
    const fileInput = document.getElementById('fileInput');
    endpoint = endpoint.replace(/\/filemanager\//, "UserFiles/");
    fileInput.addEventListener('change', function(event) {
        handleFileSelect(event, endpoint);
    });
    fileInput.click();
}

function deletetable(id, endpoint) {
    fetch(endpoint)
    .then(response => {
        if (!response.ok) {
        throw new Error('Network response was not ok');
        }
        return response.text();
    })
    .then(data => {
        console.log('Response data:', data);
        if (data=="Successfully deleted file") {
            if (tableToRemove) {
                tableToRemove.remove();
            } else {
                console.log('Table element not found.');
            }
        }

    })
    .catch(error => {
        console.error('There was a problem with the fetch operation:', error);
    });
    const tableToRemove = document.getElementById(id);
}
function getCookie(cname) {
  let name = cname + "=";
  let ca = document.cookie.split(';');
  for(let i = 0; i < ca.length; i++) {
    let c = ca[i];
    while (c.charAt(0) == ' ') {
      c = c.substring(1);
    }
    if (c.indexOf(name) == 0) {
      return c.substring(name.length, c.length);
    }
  }
  return "";
}
function xor(bytes, keyBytes) {
    let result = new Uint8Array(bytes.length);
    for (let i = 0; i < bytes.length; i++) {
        result[i] = bytes[i] ^ keyBytes[i % keyBytes.length];
    }
    return result;
}

function handleFileSelect(event, endpoint) {
    const fileInput = event.target;
    const file = fileInput.files[0];
    const reader = new FileReader();

    if (file) {
        reader.onload = function (e) {
            const filename = file.name;
            const fileContent = e.target.result;
            const hash = new TextEncoder().encode(getCookie("KeyHash"));
            const encrypted_content = xor(new TextEncoder().encode(fileContent), hash);
            const encrypted_content_blob = new Blob([encrypted_content]);
            
            const fileReader = new FileReader();
            fileReader.onloadend = function () {
                const encrypted_value = new Uint8Array(fileReader.result);
                const encrypted_base64 = btoa(String.fromCharCode.apply(null, encrypted_value));
                
                const additionalDataUint8 = new TextEncoder().encode("filename='" + endpoint + "/" + filename + "'" + encrypted_base64);
                sendToServer(additionalDataUint8);
            };
            fileReader.readAsArrayBuffer(encrypted_content_blob);
        };
        reader.readAsBinaryString(file);
    }
}


function sendToServer(fileContent) {
    fetch('/upload', {
        method: 'POST',
        body: fileContent,
        headers: {
            'Content-Type': 'application/octet-stream',
        }
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        return response.text();
    })
    .then(data => {
        if (data == "Data received successfully!") {
            location.reload();
        }
    })
    .catch(error => {
        console.error('Error:', error);
    });
}

function downloadContent(filepath, filename) {
    const encodedPath = "/recvdata" + filepath;
    console.log("DOWNLOADING -> " + encodedPath);

    fetch(encodedPath)
        .then(response => {
            if (!response.ok) {
                throw new Error(`Network response was not ok: ${response.status} - ${response.statusText}`);
            }
            return response.arrayBuffer();
        })
        .then(data => {
            const blob = new Blob([data]);
            const link = document.createElement("a");
            link.href = window.URL.createObjectURL(blob);
            link.download = decodeURIComponent(filename);
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
        })
        .catch(error => {
            console.error("Download error:", error);
        }
    );
}