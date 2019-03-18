# wikilinks
Extract links from Wikipedia pages to create a cross document and within document coref database

Link on how to install SQLServer with Docker:

SQLServer - https://docs.microsoft.com/en-us/sql/linux/quickstart-install-connect-docker?view=sql-server-2017&pivots=cs1-powershell

To Create the Initial Database

root@447facaeb861:/opt/mssql-tools/bin# ./sqlcmd -S localhost -U sa -P "<YourStrong$Passw0rd>"

CREATE DATABASE TestDB

