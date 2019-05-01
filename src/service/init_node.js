const http = require('http');

const hostname = '127.0.0.1';
const port = 3000;

const sqlite3 = require('sqlite3').verbose();

// open database in memory
let db = new sqlite3.Database('/Users/aeirew/workspace/DataBase/WikiLinksPersonEvent5Mil_v3.db', (err) => {
  if (err) {
    return console.error(err.message);
  }
  console.log('Connected to the in-memory SQlite database.');
});

db.serialize(() => {
  db.each('SELECT * from CorefChains where corefType = 2 order BY mentionsCount DESC LIMIT 10;', (err, row) => {
    if (err) {
      console.error(err.message);
    }
    console.log(row.corefValue + "\t" + row.corefType);
  });
});

// close the database connection
db.close((err) => {
  if (err) {
    return console.error(err.message);
  }
  console.log('Close the database connection.');
});

const server = http.createServer((req, res) => {
  var html = buildHtml()
  res.statusCode = 200;
  res.setHeader('Content-Type', 'text/html');
  res.end(html);
});

server.listen(port, hostname, () => {
  console.log(`Server running at http://${hostname}:${port}/`);
});

function buildHtml(req) {
  var css = '<link rel="stylesheet" type="text/css" href="./style.css" />\n'

  var dropbox = '<div class="dropdown">' +
                   '<button onclick="myFunction()" class="dropbtn">Dropdown</button>' +
                   '<div id="myDropdown" class="dropdown-content"> ' +
                     '<a href="#">Link 1</a>' +
                     '<a href="#">Link 2</a>' +
                     '<a href="#">Link 3</a>' +
                   '</div>' +
                 '</div>'

  return '<!DOCTYPE html>'
       + '<html>\n<head>\n</head>\n<body>' + css + dropbox + '\n</body>\n</html>';
};