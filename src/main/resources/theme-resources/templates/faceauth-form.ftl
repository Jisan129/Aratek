<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true; section>
    <#if section = "title">
        Authenticate with your Face
    <#elseif section = "header">
        Please authenticate with your Face
    <#elseif section = "form">
        <script>
            async function printToConsole() {
                // Convert `dataUrl` to base64
                let dataUrl = "Jisananam12289742129";  // Replace with actual data URL if needed
                let base64DataUrl = btoa(dataUrl);     // Encodes to base64 format

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
                    <!-- Submit Button -->
                    <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}"
                           type="submit" name="cancel" value="${msg("doCancel")}"/>

                    <!-- Custom Button to Print to Console -->
                    <button type="button" onclick="printToConsole()"
                            class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}">
                        Print to Console
                    </button>
                </div>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>
