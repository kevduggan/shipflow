shipflow
========

basic integration between shipworks and inflow

Configuration
-------------

This repository is public, so it does not hold the database user names or
passwords. The two connections read these keys:

- `db.shipworks.driverClassName`, `db.shipworks.url`,
  `db.shipworks.username`, `db.shipworks.password`
- `db.inflow.driverClassName`, `db.inflow.url`, `db.inflow.username`,
  `db.inflow.password`

Supply the values in one of two ways:

1. Put your own file named `shipflow.properties` in the directory you start
   shipflow from. Values in that file replace the empty values of
   `src/main/resources/shipflow.properties`. The file is ignored by git.
2. Pass the values as system properties, for example
   `-Ddb.shipworks.password=secret`.

Build and test
--------------

```
cd shipflow
mvn package
```
