const express = require('express')
const app = express()
const bodyParser = require('body-parser');
const sqlite3 = require('sqlite3').verbose();

app.use(express.static('public'));
app.use(bodyParser.urlencoded({ extended: true }));
app.set('view engine', 'ejs')

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

app.get('/', function (req, res) {
  res.render('index');
})

app.post('/', function (req, res) {
  res.render('index');
  console.log(req.body.city);
})

app.listen(3000, function () {
  console.log('Example app listening on port 3000!')
})
