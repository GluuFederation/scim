SCIM-Client
===========

## Quick start

* [SCIM in test mode](https://www.gluu.org/docs/gluu-server/user-management/scim2/#testing-with-the-scim-client)
* [SCIM protected by OAuth2 (default)](https://www.gluu.org/docs/gluu-server/user-management/scim2/#java-client)
* [SCIM protected by UMA](https://www.gluu.org/docs/gluu-server/user-management/scim2/#testing-with-the-scim-client-uma)

## How to run tests

* Ensure you have a [working installation](https://gluu.org/docs/gluu-server/installation-guide/) of Gluu Server

* Enable and then protect your SCIM API using test mode or UMA (see [API protection](https://www.gluu.org/docs/gluu-server/user-management/scim2/#api-protection))

* Edit `profiles/default/config-scim-test.properties` or create a profile directory with your own copy of `config-scim-test.properties`.

  Supply suitable values for properties file. Use [this](https://www.gluu.org/docs/gluu-server/user-management/scim2/#testing-with-the-scim-client-uma)
   as a guide if you are under UMA protection.

* Load test data. If using Gluu 4.2 or higher, use `./setup.py -x -properties-password=<password_provided_at_installation>`

* ... and run maven. Examples:

   - `mvn test`
   - `mvn -Dcfg=<profile-name> test`
   - `mvn -Dtestmode=true test`
