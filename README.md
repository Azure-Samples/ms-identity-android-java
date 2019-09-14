---
languages:
- java
page_type: sample
description: "Integrate Microsoft Identity Platform authentication in your Android application."
products:
- azure
- azure-active-directory
- office-ms-graph
urlFragment: msal-android-app
---

# MSAL Android app calling Microsoft graph

| [Getting Started](https://docs.microsoft.com/azure/active-directory/develop/guidedsetups/active-directory-android)| [Library](https://github.com/AzureAD/microsoft-authentication-library-for-android) | [API Reference](http://javadoc.io/doc/com.microsoft.identity.client/msal) | [Support](README.md#community-help-and-support)
| --- | --- | --- | --- |

![Build Badge](https://identitydivision.visualstudio.com/_apis/public/build/definitions/a7934fdd-dcde-4492-a406-7fad6ac00e17/500/badge)

## About the Sample

The MSAL Android library gives your app the ability to begin using the
[Microsoft identity platform](https://aka.ms/aaddev) by supporting [Microsoft Azure Active Directory](https://azure.microsoft.com/services/active-directory/) and [Microsoft Accounts](https://account.microsoft.com) in a converged experience using industry standard OAuth2 and OpenID Connect protocols. 
This sample walks you through the process of integrating Microsoft Identity Platform (formerly Azure Active Directory for developers) in your android application and demonstrates all the normal lifecycles your application should experience, including:

* Sign-in a user
* Get a token for the Microsoft Graph
* Device-wide SSO and Conditional Access support through the Auth Broker
* Select between Single user mode and Multiple user mode
* How to handle shared device mode
* Call the Microsoft Graph
* Sign out the user

## Broker Authentication

Authentication can be done through broker if "broker_redirect_uri_registered" is set to true in the configuration. This can only be done with the broker(Microsoft Authenticator or Company Portal) version that supports MSAL.

## Scenario

This app is a multi-tenant app meaning it can be used by any Azure AD tenant or Microsoft Account.  It demonstrates how a developer can build apps to connect with enterprise users and access their Azure + O365 data via the Microsoft Graph. 
During the auth flow, end users will be required to sign in and consent to the permissions of the application, and in some cases may require an admin to consent to the app.  The majority of the logic in this sample shows how to auth an end user and make a basic call to the Microsoft Graph.
This app also demonstrates Single Account and Multiple Account authentication modes. Single Account mode can also enable shared mode device which can be used for Firstline Workers scenario.

## Single account mode 

In Single account mode, only one user can sign into the application at a time. The application may have multiple accounts, but if the app only supports one signed-in user, it is recommended to use the single account mode.

Code snippet from SingleAccountModeFragment class
```
PublicClientApplication.createSingleAccountPublicClientApplication(getContext(),
                R.raw.auth_config_single_account);
```
Configuration from auth_config_single_account.json
```
"account_mode" : "SINGLE",
"broker_redirect_uri_registered": true,    
```

## Single account mode with shared device mode

In Single account mode, if 'shared-device-mode-supported' is set to true then only one user can sign-in and sign-out at the device level. SSO happens device wide

Shared Device Mode will allow you to configure Android devices to be shared by multiple employees, while providing Microsoft Identity backed management of the device. Employees will be able to sign-in to their devices and access customer information quickly. When they are finished with their shift or task, they will be able to Globally Sign-Out of the device and it will be immediately ready for the next employee to use. 

Applications that run on Shared Devices must be in Single account mode. Applications that only support Multiple account mode will not run on a Shared Device. If an employee loads an application that does not support Single account Mode, the application will not run on the Shared Device.

You can use isSharedDevice() flag to determine if an application is in Shared Device Mode. Your app can use this flag to modify UX accordingly.

Code snippet from SingleAccountModeFragment class
```
deviceModeTextView.setText(mSingleAccountApp.isSharedDevice() ?
                                "Shared" :
                                "Non-Shared");
```
Configuration from auth_config_single_account.json with shared mode                               
```
"account_mode" : "SINGLE", 
"broker_redirect_uri_registered": true,  
"shared_device_mode_supported": true,
```
#### Note: You can only put a device in to Shared Mode using the Authenticator app and with a user who is the Cloud Device Administrator role. You can reach your Organizational Roles by going to the Azure Portal and selecting: 
 
           Azure Active Directory -> Roles and Administrators -> Cloud Device Administrator  

## Multiple account mode

In this mode, the application can have multiple accounts and can switch between accounts of the user and get data from that account.

Code snippet from MultipleAccountModeFragment class
```
PublicClientApplication.createMultipleAccountPublicClientApplication(getContext(),
                R.raw.auth_config_multiple_account);
```
> If you are writing an application that will only be used for Firstline Workers on shared devices, we recommend you write your application to only support Single User Mode.
  
> If your app also support multiple accounts as well as shared device mode, you will have to perform type check and cast to the appropriate interface to perform an operation.  

For more information on the concepts used in this sample, be sure to read the documentation [link]


## How to run this sample

To run this sample, you'll need:

* Android SDK
* An internet connection

#### Note : If you have older versions of Microsoft Authenticator app or Company portal app installed in your device this sample app doesnt run on your device. Please make sure that you are installing the latest version of Microsoft Authenticator or Company Portal on your device.

## Steps to Run

##  Step 1: Register your App  

To begin registering your app, begin at the [Azure portal](https://aka.ms/MobileAppReg)

To create an app,  
1. Click `New Registration`. 

2. Name your app, select the audience you're targeting, and click `Register`. 

3. Click `Authentication` > `Add Platform` > `Android`. 
    - Enter the Package Name from your Android studio project. 
    - Generate a Signature Hash. Refer to the portal for instructions.

4. Hit the `Configured` button. Store the ***MSAL Configuration*** for the next steps. 


## Step 2: Clone the code. 

  ```
   git clone https://github.com/Azure-Samples/ms-identity-android-java.git 
  ```   
   Open Android Studio, and select *open an existing Android Studio project*. Find the cloned project and open it. 

## Step 3: Configure the sample code in `auth_config.json` and `AndroidManifest.xml`.

   - Copy and paste the ***MSAL Configuration*** JSON from the Azure portal into `auth_config.json`. 
   - Inside the `AndroidManifest.xml`, replace `android:host` and `android:path` with the same info registered above.
        - `auth_config.json` contains this information as a reference inside the `redirect_uri` field.
        - The Signature Hash should NOT be URL encoded in the `AndroidManifest.xml`. 
    Refer [Azure Active Directory Android Quickstart](https://docs.microsoft.com/en-us/azure/active-directory/develop/quickstart-v2-android) for more details
    
   In Android Studio, select *Build* > *Clean Project*. 

## Step 4: Run the sample

   In Android Studio, select *Run* > *Run 'app'*. 

## About the code

The following files have code that would be of interest to you

1. SingleAccountModeFragment.java

Contains code showing how the 'Single account' mode is implemented. The includes authentication, obtaining the token, and making a graph api call using the obtained token.

2. MultipleAccountModeFragment.java

Contains code showing how the 'Multiple account' mode is implemented. The includes authentication and obtaining the token, and making a graph api call using the obtained token.



## Feedback, Community Help, and Support

We use [Stack Overflow](http://stackoverflow.com/questions/tagged/msal) with the community to 
provide support. We highly recommend you ask your questions on Stack Overflow first and browse 
existing issues to see if someone has asked your question before. 

If you find and bug or have a feature request, please raise the issue 
on [GitHub Issues](../../issues). 

To provide a recommendation, visit 
our [User Voice page](https://feedback.azure.com/forums/169401-azure-active-directory).

## Contribute

We enthusiastically welcome contributions and feedback. You can clone the repo and start 
contributing now. Read our [Contribution Guide](Contributing.md) for more information.

This project has adopted the 
[Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/). 
For more information see 
the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or contact 
[opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments.

## Security Library

This library controls how users sign-in and access services. We recommend you always take the 
latest version of our library in your app when possible. We 
use [semantic versioning](http://semver.org) so you can control the risk associated with updating 
your app. As an example, always downloading the latest minor version number (e.g. x.*y*.x) ensures 
you get the latest security and feature enhanements but our API surface remains the same. You 
can always see the latest version and release notes under the Releases tab of GitHub.

## Security Reporting

If you find a security issue with our libraries or services please report it 
to [secure@microsoft.com](mailto:secure@microsoft.com) with as much detail as possible. Your 
submission may be eligible for a bounty through the [Microsoft Bounty](http://aka.ms/bugbounty) 
program. Please do not post security issues to GitHub Issues or any other public site. We will 
contact you shortly upon receiving the information. We encourage you to get notifications of when 
security incidents occur by 
visiting [this page](https://technet.microsoft.com/en-us/security/dd252948) and subscribing 
to Security Advisory Alerts.
