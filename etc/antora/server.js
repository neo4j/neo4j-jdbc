const express = require('express')

const app = express()
app.use(express.static('../../docs/target/asciidoc/build/site'))

app.use('/static/assets', express.static('../../docs/target/asciidoc/build/site'))

app.get('/', (req, res) => res.redirect('/docs/'))

app.listen(8000, () => console.log('📘 http://localhost:8000'))