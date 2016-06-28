asciidoctor -a allow-uri-read docs/index.adoc
git checkout gh-pages
cp docs/index.html index.html
git add index.html
git commit -m"docs update"
git push
git checkout master

