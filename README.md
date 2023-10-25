# Search Engine
The search engine is a Spring application (running on any server or computer) that works with a locally installed MySQL database. It has a simple web interface and API through which you can manage and receive search results for a query
## ✨ Features
- Ability to search by query
- Indexing of sites and pages;
- Providing statistics on sites;
- Compiling an index based on keywords on pages.

## 🛠️ Technologies
- Java 17
- Spring Boot v.2.7.1
- MySQL
- Apache Lucene Morphology
- Jsoup
- Lombok

## 🔑 API Specification
### Methods
The application has several methods which are given below:
| Method    | URL                                      | Description                                         |
| --------- | ---------------------------------------- | --------------------------------------------------- |
| `GET`     | `/api/statistics`                        | Returns statistics and other service information.   |
| `GET`     | `/api/startIndexing`                     | Launches full indexing of all sites.                |
| `GET`     | `/api/stopIndexing`                      | Stops the current indexing process.                 |
| `POST`    | `/api/indexPage`                         | Adds to the index or updates a single page.         |
| `GET`     | `/api/search`                            | Searches for pages based on the passed search query.|
#### `GET` /api/statistics
The method returns statistics and other service information about the state of search indexes and the engine itself.
Example of a response:
```
{
   "result":true,
   "statistics":{
      "total":{
         "sites":10,
         "pages":436423,
         "lemmas":5127891,
         "indexing":true
      },
      "detailed":[
         {
            "url":"http://www.site.com",
            "name":"Имя сайта",
            "status":"INDEXED",
            "statusTime":1600160357,
            "error":"Indexing error: site home page is unavailable",
            "pages":5764,
            "lemmas":321115
         }, 
         ...
      ]
   }
```
#### `GET` /api/startIndexing
The method starts full indexing of all sites or full re-indexing if they are already indexed.
If indexing or reindexing is currently running, the method returns an appropriate error message.

Example of successful response:
```
{
    'result': true
}
```

Example of a response in case of error:
```
{
    'result': false,
    'error': "Indexing has already started"
}
```

#### `GET` /api/stopIndexing
This method stops the current indexing (re-indexing) process.
If indexing or reindexing is not currently occurring, the method returns an appropriate error message.

Example of successful response:
```
{
    'result': true
}
```

Example of a response in case of error:
```
{
    'result': false,
    'error': "Indexing is not running"
}
```

#### `POST` /api/indexPage
The method adds to the index or updates a separate page, address which is passed in the parameter.
If the page address is passed incorrectly, the method should return corresponding error.

Input parameters:
| Type      | Name      | Nullable | Description                                          | Example                               |
| --------- | --------- | -------- | ---------------------------------------------------- | ------------------------------------- |
| `String`    | `url`       | `false` | The address of the page that needs to be reindexed.  | https://dubaithebest.exmaple/newPage/ |

Example of successful response:
```
{
    'result': true
}
```

Example of a response in case of error:
```
{
    'result': false,
    'error': "This page is located outside the sites specified in the configuration file"
}
```

#### `GET` /api/search
The method searches for pages using the passed search query (`query` parameter).
To display results separately, you can also set the `offset` and `limit` parameters.
The response displays the total number of results `count`, independent of the values of the `offset` and `limit` parameters, and a `data` array with the search results.
Each result is an object containing properties of the search result.
If the search query is not specified or there is no ready index yet, the method should return the appropriate error.

Input parameters:
| Type      | Name      | Nullable  | Description                           | Example                       |
| --------- | --------- |---------- | ------------------------------------- | ----------------------------- |
| `String`    | `query`     | `false`   | Search query.                         | Квартиры в Дубае              |
| `String`    | `site`      | `true`    | The site to search on. May be empty.  | https://dubaithebest.example/ |
| `Integer`   | `offset`    | `true`    | Offset from 0 for paged output.       | 0                             |
| `Integer`   | `limit`     | `true`    | Number of results to display.         | 10                            |

Example of a response:
```
{
   "result":true,
   "count":574,
   "data":[
      {
         "site":"http://www.site.com",
         "siteName":"Site name",
         "uri":"/path/to/page/6784",
         "title":"Title of the page to be displayed",
         "snippet":"A fragment of text in which matches were found, <b>in bold</b>, in HTML format",
         "relevance":0.93362
      },
      ...
   ]
}
```

## 🚀 Usage
### Dashboard:
There is statistics information.
![Imgur](https://i.imgur.com/8s2BGb7.png)
### Management:
![Imgur](https://i.imgur.com/wu6QGer.png)
### Search:
Exmaple of searching.
![Imgur](https://i.imgur.com/0WcM1ee.png)

## Author
### 👤 **Denis Bezrukov**
- Email: [ds.bezrukov@icloud.com](mailto:ds.bezrukov@icloud.com)
- Telegram: [@x_bezrukov_x](https://t.me/x_bezrukov_x)
- Github: [@xbezrukovx](https://github.com/xbezrukovx)
