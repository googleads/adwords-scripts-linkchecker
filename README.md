# AdWords Scripts Linkchecker


## Overview

This project is an App Engine application for verifying the status of URLs. The
application provides a simple API for submitting lists of URLs to be checked and
querying their progress.

## Using the application

There are several ways to use this application for checking URLs:

*   **Within AdWords Scripts**: If your goal is to check the status of URLs,
    such as landing page URLs, from within AdWords, then you may wish to
    consider the [AdWords Scripts larger-scale link
    checker](https://developers.google.com/adwords/scripts/docs/solutions/larger-scale-link-checker)
    which uses this App Engine solution directly.
*   **[Within Apps Script](#using-within-apps-script)**: If your aim is to use
    the application from within Apps Script, then this can again be achieved
    without building a copy of the application, but instead using the pre-built
    version maintained on Google Cloud Storage.
*   **[From a custom client](#using-the-application-from-a-custom-client)**:
    Even if working with your own custom client, you may wish to avoid building
    the App Engine application yourself and just deploy the existing build from
    Cloud Storage. Then, use the [linkchecker API](#the-linkchecker-api) to
    interact with the application.
*   **[Building the
    application](#building-and-deploying-your-own-version-of-the-application)**:
    If you wish to modify the App Engine application itself, you'll want to
    build it and host it yourself on Cloud Storage before working with it in one
    of the above ways.

## Using within Apps Script

One of the easiest ways to use the link checker for your own purposes, beyond
the AdWords Scripts solution, is via Apps Script: The means to deploy and
authenticate with the application, as well as examples of how to interact with
the API are already available in a sample script.

1.  Make a copy of the template spreadsheet for the [AdWords Scripts
    solution](https://docs.google.com/spreadsheets/d/1LIzzu5-pTt4Rkei5U41HjFz1EwV9f0q3uv2prpAvb5I/edit)
    by clicking **File > Make a copy**.

    The **Cloud Setup** and **App Engine Performance** sheets will be the ones
    of use, with the others not relevant outside of AdWords Scripts.

2.  Follow the instructions on the **Cloud Setup** sheet. Note that if you are
    intending to interact with other APIs in your Apps Script solution, for
    example the DoubleClick Search API, then extra scopes should be added in
    Step 2 on that sheet.

3.  Once all steps on the **Cloud Setup** sheet are complete, locate the example
    Apps Script application: Still within the spreadsheet click **Tools > Script
    editor** and locate the **Example.gs** script.

    Here you will see, within the `main` function, calls to `listOperations`,
    `createOperation`, `getOperation` and `deleteOperation` respectively. This
    is preceded by the necessary setup to get authentication up and running with
    the application.

    Using these examples, combined with [the API reference](#the-linkchecker-api),
    it is possible to quickly develop a custom script to work with the link
    checker application.

4.  Tune the settings for the application, such as the number of checks to be
    performed in parallel, by configuring the **App Engine Performance** sheet.

## Using the application from a custom client

### Deployment

If you are using a custom client outside of Apps Script, you may wish first to
use the spreadsheet described in the above sections to ease the deployment of
the application. This is highly recommended because:

1.  The deployment configuration, and location of the Cloud Storage file are all
    pre-populated.
2.  The application settings, such as the TaskQueue configuration and cron setup
    are all taken care of.

It is possible to deploy the application yourself by other means, using the [App
Engine Admin API](https://cloud.google.com/appengine/docs/admin-api/), and the
App Engine API to configure the Task Queue and cron. If you require this, then
the best option is to examine the source code of the spreadsheet.

1.  Make a copy of the [template
    spreadsheet](https://docs.google.com/spreadsheets/d/1LIzzu5-pTt4Rkei5U41HjFz1EwV9f0q3uv2prpAvb5I/edit)
    by clicking **File > Make a copy**.
2.  Click **Tools > Script editor** and locate **CloudSetup.gs**. This script
    has the details of all calls to the App Engine Admin API, and calls for
    configuring the TaskQueue and cron, which you can transpose to your language
    of choice.

### Interacting with the linkchecker API

In order to interact with the linkchecker API, it is necessary to obtain the
shared key which is required with all API calls. This approach was chosen for
its simplicity when working within AdWords Scripts.

The shared key is stored in Google Datastore, which can both be accessed by the
App Engine application, and by any client using the [Datastore
API](https://cloud.google.com/datastore/docs/reference/rest/).

The shared key should then be set in the HTTP authorization header: e.g:

`Authorization: <your_shared_key>`

### Examples of retrieving the shared key

*   **Apps Script**: As shown in the **CloudSetup.gs** file, in the
    `getSharedKey_` function.

*   **Java**: Using the [Datastore client
    library](https://cloud.google.com/datastore/docs/reference/libraries):

```java
public String getSharedKey() {
  String projectId = "<your_project_id>";
  Datastore datastore = DatastoreOptions
      .newBuilder()
      .setProjectId(projectId)
      .build()
      .getService();
  String kind = "SharedKey";
  String name = "key";
  Key taskKey = datastore.newKeyFactory().setKind(kind).newKey(name);
  Entity retrieved = datastore.get(taskKey);

  return retrieved.getString("key");
}
```

*   **Python**: Using the [Datastore client
    library](https://cloud.google.com/datastore/docs/reference/libraries):

```python
def get_shared_key():
  datastore_client = datastore.Client(project="<your_project_id>")

  kind = "SharedKey"
  name = "key"

  task_key = datastore_client.key(kind, name)
  entry = datastore_client.get(task_key)

  return entry["key"]
```

### The linkchecker API

The application provides an API with the following methods. All operational
methods are relative to the account base URL of:

```
https://<project-id>.appspot.com/_ah/api/batchLinkChecker/v1/account/<account-id>/
```

where:

*   `project-id` is the project ID taken from Google Cloud console.
*   `account-id` is a variable provided to allow a single instance of the App
    Engine application to be used by multiple sources. It can be any numeric
    value.

| Method            | HTTP request                                     | Description                                            |
| ----------------- | ------------------------------------------------ | ------------------------------------------------------ |
| [Add](#add)       | `POST [account_base_url]/batchOperation`         | Submits a batch of URLs to be processed.               |
| [List](#list)     | `GET [account_base_url]/batchOperation`          | Retrieves a list of current batches and their status.  |
| [Get](#get)       | `GET [account_base_url]/batchOperation/[id]`     | Retrieves results for a specified batch operation.     |
| [Delete](#delete) | `DELETE [account_base_url]/batchOperation/[id]`  | Deletes results for a specific operation.              |

Furthermore, the API provides methods for retrieving and modifying settings for
the linkchecker. All methods are relative to the application base URL of:

```
https://<project-id>.appspot.com/_ah/api/batchLinkChecker/v1
```

| Method                                | HTTP request                  | Description                        |
| ------------------------------------- | ----------------------------- | ---------------------------------- |
| [Get settings](#get-settings)         | `GET [app_base_url]/settings` | Retrieve user-modifiable settings. |
| [Update settings](#update-settings)   | `PUT [app_base_url]/settings` | Update user-modifiable settings.   |

#### **Add**

##### HTTP Request

```
POST https://<project-id>.appspot.com/_ah/api/batchLinkChecker/v1/account/<account-id>/batchOperation`
```

##### Authorization

The shared key must be provided in the `Authorization` header

##### Request body

The request body should be in JSON format.

| Property              | Value  | Required | Description                                                            |
| --------------------- | ------ | -------- | ---------------------------------------------------------------------- |
| `urls[]`              | `list` | Yes      | A list of URL strings for checking, with a maximum of 15000.           |                       :
| `failureMatchTexts[]` | `list` | No       | A list of strings e.g. "Out of Office" that also constitute a failure. |                     :

##### Response

```json
{
  "items": [
    string
  ]
}
```

Property  | Value  | Description
--------- | ------ | ----------------------------------------
`items[]` | `list` | A list with one entry, the ID of the job

#### **List**

##### HTTP Request

```
GET https://<project-id>.appspot.com/_ah/api/batchLinkChecker/v1/account/<account-id>/batchOperation`
```

##### Authorization

The shared key must be provided in the `Authorization` header

##### Request body

The request body should be empty

##### Response

```json
{
  "items": [
    BatchOperation
  ]
}
```

where `BatchOperation` is the following structure:

```json
{
  "createdDate": datetime,
  "batchId": string,
  "status": string
}
```

Property      | Value      | Description
------------- | ---------- | ----------------------------------------------
`createdDate` | `datetime` | The date and time of job creation (RFC 3339).
`batchId`     | `string`   | The ID of the job
`status`      | `string`   | Valid responses are `COMPLETE` or `PROCESSING`

#### **Get**

##### HTTP Request

```
GET https://<project-id>.appspot.com/_ah/api/batchLinkChecker/v1/account/<account-id>/batchOperation/<id>`
```

##### Authorization

The shared key must be provided in the `Authorization` header

##### Parameters

Parameter | Value    | Description
--------- | -------- | ------------------------------------------
`id`      | `string` | The ID of the job to retrieve results for.

##### Request body

The request body should be empty

##### Response

```json
{
  "errors": [
    BatchOperationError
  ],
  "status": string,
  "batchId": string,
  "checkedUrlCount": integer
}
```

| Property          | Value                 | Required | Description                                                                             |
| ----------------- | --------------------- | -------- | --------------------------------------------------------------------------------------- |
| `errors[]`        | `BatchOperationError` | No       | If errors were encountered, will be present as a list of `BatchOperationError` objects. |
| `batchId`         | `string`              | Yes      | The ID of the job                                                                       |
| `status`          | `string`              | Yes      | Valid responses are `COMPLETE` or `PROCESSING`.                                         |
| `checkedUrlCount` | `integer`             | Yes      | If the job is complete, contains the total number of URLs checked, otherwise is zero.   |

where `BatchOperationError` is the following structure:

```json
{
  "url": string,
  "message": string
}
```

#### **Delete**

##### HTTP Request

```
DELETE https://<project-id>.appspot.com/_ah/api/batchLinkChecker/v1/account/<account-id>/batchOperation/<id>`
```

##### Authorization

The shared key must be provided in the `Authorization` header

##### Parameters

Parameter | Value    | Description
--------- | -------- | ----------------------------
`id`      | `string` | The ID of the job to delete.

##### Request body

The request body should be empty

##### Response

The response is empty

#### **Get Settings**

##### HTTP Request

```
GET https://<project-id>.appspot.com/_ah/api/batchLinkChecker/v1/settings
```

##### Authorization

The shared key must be provided in the `Authorization` header

##### Response

```json
{
 "rateInChecksPerMinute": integer,
 "userAgentString": string
}
```

| Property                | Value     | Description                                                 |
| ----------------------- | --------- | ----------------------------------------------------------- |
| `rateInChecksPerMinute` | `integer` | The number of URLs to check per minute per parallel worker. |
| `userAgentString`       | `string`  | The User-Agent to use with each request.                    |

#### **Update Settings**

##### HTTP Request

```
PUT https://<project-id>.appspot.com/_ah/api/batchLinkChecker/v1/settings
```

##### Authorization

The shared key must be provided in the `Authorization` header

##### Request body

```json
{
 "rateInChecksPerMinute": integer,
 "userAgentString": string
}
```

| Property                | Value     | Required | Description                                                 |
| ----------------------- | --------- | -------- | ----------------------------------------------------------- |
| `rateInChecksPerMinute` | `integer` | No       | The number of URLs to check per minute per parallel worker. |
| `userAgentString`       | `string`  | No       | The User-Agent to use with each request.                    |

##### Response

The response is the new settings, if updated, as per the *Get Settings* request.

## Building and deploying your own version of the application

You will need [Maven](https://maven.apache.org/) to build this application.

### Building

To build the application, having clone the github repository:

```
mvn package
```

### Deploying

1.  Upload the generated WAR file to [Google Cloud
    Storage](https://console.cloud.google.com/storage/browser) for your project
    and enable a public link to the WAR file.
2.  Use this URL with the [App Engine Admin
    API](https://cloud.google.com/appengine/docs/admin-api/) when specifying the
    location of the application. This is best seen by examining the template
    spreadsheet as described in the [Apps Script
    section](#using-within-apps-script) above, and inspecting the
    **CloudSetup.gs** script within **Tools > Script editor**, as this
    illustrates how to use the App Engine Admin API to deploy from a package on
    Cloud Storage.

## Performance tuning

If too many parallel tasks are enabled on the App Engine application, or too
high a rate of checking per task is allowed, then where many URLs belong to the
same domain, there exists a risk that requests will be blocked, owing to the
high volume of traffic resembling a Denial of Service attack.

There are two settings that are relevant in controlling performance:

1.  **Number of parallel tasks**: URLs are checked using tasks within an App
    Engine [Task Queue](https://cloud.google.com/appengine/docs/standard/java/taskqueue/push/).
    The number of parallel tasks can be configured using the App Engine API.
    The **App Engine Performance** sheet in the template spreadsheet utilizes
    this API to allow the user to set the number of tasks.

    If you wish to modify these settings within a custom client, then the format
    of the API request can be seen in the `updateTaskQueue` function within
    **CloudSetup.gs**.
1.  **Number of requests per minute**: The [linkchecker API](#the-linkchecker-api)
    provides the means to set the request rate for each parallel task.

Using the two in conjunction allow an appropriate rate of URL checking to be
achieved.

## Miscellaneous

### Issue tracker
- https://github.com/googleads/adwords-scripts-linkchecker/issues

### Support forum
- https://groups.google.com/forum/#!forum/adwords-scripts

### Authors
- https://github.com/garanj
- https://github.com/AnashOommen
- [AdWords Scripts Team](mailto:adwords-scripts@googlegroups.com)
