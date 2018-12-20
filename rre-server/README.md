RRE Server
==========

This directory contains the RRE server application, along with supporting
modules. Running the RRE server provides the dashboard functionality used
for tracking metrics and versions in real-time.

Modules in this directory are:

- `rre-server-core` - the core classes required by the application and
plugins;
- `rre-server-app` - the Spring Boot application itself;
- `rre-server-plugin-elasticsearch` - a plugin which may be used to connect
the dashboard to an Elasticsearch back-end. If you're using the Elasticsearch
persistence plugin to push evaluation results into an Elasticsearch index,
you will need this.


## Building and running

To build the application, use Maven (from the top-level directory):

    mvn clean package -pl rre-server/rre-server-app -am

This will build a single jar file which can be run from the command line:

    java -jar rre-server/rre-server-app/target/rre-server-1.0.jar

The dashboard can then be accessed on http://localhost:8080.


### Building with the Elasticsearch plugin

To include the Elasticsearch plugin in the built application, use the
`elasticsearch` profile at build time:

    mvn clean package -pl rre-server/rre-server-app -am -Pelasticsearch

As above, this builds the rre-server jar file. To run in Elasticsearch mode,
pass the elasticsearch profile switch on the command line:

    java -Dspring.profiles.active=elasticsearch -jar rre-server/rre-server-app/target/rre-server-app-1.0.jar

By default, the Elasticsearch plugin will use an index called `rre` on a
local Elasticsearch instance. To change this, pass in further parameters:

- `--elasticsearch.url=http://myhost:9200` to change the Elasticsearch endpoint.
- `--elasticsearch.index=rre_test` to change the index in use.

These parameters should come after the `-jar rre-server...` command line
section - ie. at the *end* of the command.
