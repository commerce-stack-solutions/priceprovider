#!/bin/bash

# Replace the apiBaseUrl in the JavaScript files with the value of PPS_BASEURL environment variable
# The production build has the apiBaseUrl set to 'https://api.example.com/'
# We need to replace this with the actual base URL from the environment variable

echo "Replacing API base URL with: ${PPS_BASEURL}"

# Find all JavaScript files in the htdocs directory and replace the API base URL
find /usr/local/apache2/htdocs -type f -name "*.js" -exec sed -i "s|https://api.example.com/|${PPS_BASEURL}|g" {} \;

echo "API base URL replacement complete"

# Replace the OIDC issuer URI in the JavaScript files with the value of PPS_OIDC_ISSUER_URI
# The production build has the issuer URI set to 'https://auth.example.com/realms/priceprovider'
# We need to replace this with the actual issuer URI from the environment variable

echo "Replacing OIDC issuer URI with: ${PPS_OIDC_ISSUER_URI}"

find /usr/local/apache2/htdocs -type f -name "*.js" -exec sed -i "s|https://auth.example.com/realms/priceprovider|${PPS_OIDC_ISSUER_URI}|g" {} \;

echo "OIDC issuer URI replacement complete"

# Start Apache in the foreground
exec httpd-foreground
