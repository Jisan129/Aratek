<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true; section>
    <#if section = "title">
        Authenticate with your Face
    <#elseif section = "header">
        Please authenticate with your Face
    <#elseif section = "form">
        <script>
            // Array of text options (3 texts)
            const textList = [
                "Put 4 Fingers of Right hand",
                "Put 4 Fingers of Left  hand",
                "Put thumbs of both hand"
            ];


            // Counter to track the current text
            let currentIndex = 0;
            function changeText() {
                const textElement = document.getElementById('dynamic-text');

                // Function to update the text and schedule the next update
                function updateText() {
                    // Update the text content
                    textElement.textContent = textList[currentIndex];

                    // Increment the counter and reset if it exceeds the list length
                    currentIndex = (currentIndex + 1) % textList.length;

                    // Schedule the next update
                    setTimeout(updateText, 5000);
                }

                // Start the recursive updates
                updateText();
            }
            async function printToConsole() {
                // Convert `dataUrl` to base64
                let dataUrl = "Jisananam12289";  // Replace with actual data URL if needed
                let base64DataUrl = btoa(dataUrl);  // Encodes to base64 format

                // Prepare face authentication data
                let faceAuthData = { faceImage: base64DataUrl };
                let loginActionUrl = document.getElementById("kc-u2f-login-form").action;

                console.log("Face auth data:", faceAuthData);

                // Convert faceAuthData to URL-encoded form data
                let params = new URLSearchParams(faceAuthData).toString();

                try {
                    // Send POST request with encoded data
                    let faceAuthResponse = await fetch(loginActionUrl, {
                        method: "post",
                        redirect: 'manual',
                        credentials: "include",
                        body: params,
                        headers: {
                            'Content-Type': 'application/x-www-form-urlencoded'
                        }
                    });
                    console.log("Response:", faceAuthResponse);
                } catch (error) {
                    console.error("Error:", error);
                }
            }
        </script>

        <form id="kc-u2f-login-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <!-- Placeholder Text -->
                    <h1 id="dynamic-text">Fingerprint Authentication</h1>

                    <!-- Custom Button to Incrementally Change Text -->
                    <button type="button" onclick="changeText()"
                            class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}">
                        Put Fingers
                    </button>

                    <!-- Custom Button to Print to Console -->
                    <button type="button" onclick="printToConsole()"
                            class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}">
                        Create connection with Aratek
                    </button>
                    <div style="text-align: center; margin-bottom: 20px;">
                        <img src="${url.resourcesPath}/theme/faceauth-theme/login/resources/img/photo-id.JPG" alt="Photo ID" />
                    </div>
                </div>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>
