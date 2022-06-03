# High School Challenge Module of Play&Go 

This is a microservice for the management of the High School Challenge initiatives.

The microservice is implemented as Spring Boot application, relies on Mongo database,
and exposes a simple management Vue-based management frontend.

The microservice is integrated with the main Play&Go Engine.

## Configuration

Environment variables:

- ``SPRING_DATA_MONGODB_URL``: MongoDB url
- ``PLAYANDGO_ENGINE_URI``: Play&Go Engine endpoint
- ``AAC_CLIENT_ID``: Client ID of the OAuth2.0 application to generate the client credentials token 
- ``AAC_CLIENT_SECRET``: Client secret of the OAuth2.0 application to generate the client credentials token 
- ``AAC_TOKEN_URI``: OAuth2.0 token endpoint to generate the client credentials token
- ``AAC_ISSUER_URI``: OAuth2.0 issuer to validate user tokens
- ``AAC_AUTHORIZATION_URI``: OAuth2.0 authorization endpoint to authenticate the user 
