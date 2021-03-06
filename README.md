eu.5ginfire.portal.api
==========

eu.5ginfire.portal.api Web Service is a RESTful service for 5GinFIRE project  (http://www.5ginfire.eu)

API Documentation
--------

Please check the [API documentation](https://5ginfire.github.io/eu.5ginfire.portal.api/doc/html2-client/)


Quick testing
--------
Clone all projects:

[eu.5ginfire.portal.api](https://github.com/5GinFIRE/eu.5ginfire.portal.api)

[eu.5ginfire.portal.web](https://github.com/5GinFIRE/eu.5ginfire.portal.web)

[eu.5ginFIRE.riftioyangschema2java](https://github.com/5GinFIRE/eu.5ginFIRE.riftioyangschema2java)

Install riftioyangschema2java project. Get into the project and run
`mvn clean install`

[nfv-requirements-extractor](https://github.com/5GinFIRE/nfv-requirements-extractor)

Install nfv-requirements-extractor project. Get into the project and run
`mvn clean install`

[eu.5ginfire.nbi.osm4java](https://github.com/5GinFIRE/eu.5ginfire.nbi.osm4java )

Install eu.5ginfire.nbi.osm4java project. Get into the project and run
`mvn clean install`


Get into the eu.5ginfire.portal.api project and run
`mvn clean -Pjetty.integration jetty:run`

by default the API is listening to port 13000. 
Open your browser to http://localhost:13000 or make a GET request to 
http://localhost:13000/5ginfireportal/services/api/repo/vxfs
or 
http://localhost:13000/5ginfireportal/services/api/repo/admin/users (needs authentication first to http://localhost:13000/5ginfireportal)

The admin user is admin, password = changeme.

Jetty can also serve the web project under http://localhost:13000/mp . See the pom.xml at the line
`<resourceBase>${project.basedir}/../eu.5ginfire.portal.web/src</resourceBase>`

login to web with same credentials. When logged in you can add some categories and the some test artefacts (e.g. users, VxF etc)

Licenses
--------

The license for this software is [Apache 2 v2.1](./src/license/header.txt).

Contact
-------

For further information on collaboration, support or alternative licensing, please contact:

* Website: https://5ginfire.eu/ 
* Email: contact@5GinFIRE.eu

