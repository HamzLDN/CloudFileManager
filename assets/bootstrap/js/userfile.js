
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
                console.log("WHAT");
                ulElement.innerHTML += htmlContent;
                
                if (file.endsWith(".png") || file.endsWith(".jpeg") || file.endsWith(".gif")) {
                    const base64String = data[0].content;
                    const binaryString = atob(base64String);
                    const binaryData = new Uint8Array(binaryString.length);
                    for (let i = 0; i < binaryString.length; i++) {
                        binaryData[i] = binaryString.charCodeAt(i);
                    }

                    // Determine the image type based on the file extension
                    const imageType = file.endsWith('.png') ? 'image/png' :
                                    file.endsWith('.jpg') ? 'image/jpeg' :
                                    file.endsWith('.gif') ? 'image/gif' :
                                    'image/png'; // Default to 'image/png' if the format is unknown

                    const blob = new Blob([binaryData], { type: imageType });

                    const imgElement = document.createElement('img');
                    imgElement.src = URL.createObjectURL(blob);
                    imgElement.style.width = '100%';  // Adjust the percentage as needed
                    imgElement.style.height = '100%'; // Adjust the percentage as needed
                    imgElement.style.display = 'block';
                    imgElement.style.margin = 'auto';

                    const outputDiv = document.getElementById("output");
                    outputDiv.appendChild(imgElement);
                } else {
                    const textareaElement = document.createElement("textarea");
                    textareaElement.rows = 3; // Adjust the number of rows as needed
                    textareaElement.className = "content-textarea";
                    // Center the textarea horizontally
                    textareaElement.style.display = 'block';
                    textareaElement.style.margin = 'auto';

                    textareaElement.textContent = data[0].content;

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

            const timestampHeader = document.createElement('th');
            timestampHeader.textContent = 'Timestamp';

            headerRow.appendChild(fileHeader);
            headerRow.appendChild(timestampHeader);
            tableHeader.appendChild(headerRow);
            table.appendChild(tableHeader);
            const tableBody = document.createElement('tbody');
            data.forEach(entry => {
                const fileName = entry.file;
                const timestamp = entry.timestamp;

                const tableRow = document.createElement('tr');
                const fileNameCell = document.createElement('td');
                const fileLink = document.createElement('a');
                fileLink.href = "/filemanager" + endpoint + "/" + fileName;
                fileLink.textContent = fileName;
                fileNameCell.appendChild(fileLink);

                const timestampCell = document.createElement('td');
                timestampCell.textContent = timestamp;

                tableRow.appendChild(fileNameCell);
                tableRow.appendChild(timestampCell);

                tableBody.appendChild(tableRow);
            });

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

function concatenateUint8Arrays(arr1, arr2) {
    const result = new Uint8Array(arr1.length + arr2.length);
    result.set(arr1, 0);
    result.set(arr2, arr1.length);
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
            const blob = new Blob([new Uint8Array(fileContent)]);
            const fileReader = new FileReader();

            fileReader.onloadend = function () {
                const base64String = fileReader.result.split(',')[1];
                const additionalDataUint8 = new TextEncoder().encode("filename='" + endpoint + "/" + filename + "'" + base64String);
                sendToServer(additionalDataUint8);
            };

            fileReader.readAsDataURL(blob);
        };
        reader.readAsArrayBuffer(file);
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
        return response.json();
    })
    .then(data => {
        console.log('Server response:', data);
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