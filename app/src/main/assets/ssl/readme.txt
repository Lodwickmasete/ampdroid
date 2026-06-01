Browser warning is normal because it's self-signed.

follow this steps to create you own.

Generate new key:
`
openssl genrsa -out server.key 2048
`
Create config file:
`
nano cert.conf
`
then modify the cert.conf (/ssl/cert.conf) in this thus directory,
or if you ran nano cert.conf
paste this 
[req]
default_bits = 2048
prompt = no
default_md = sha256
distinguished_name = dn
x509_extensions = v3_req

[dn]
C = ZA
ST = Gauteng
L = Johannesburg
O = Lodwick
OU = Dev
CN = localhost

[v3_req]
subjectAltName = @alt_names
basicConstraints = CA:FALSE
keyUsage = digitalSignature, keyEncipherment
extendedKeyUsage = serverAuth

[alt_names]
DNS.1 = localhost
IP.1 = 127.0.0.1


then save the file

and run
`
openssl req -x509 -nodes -days 365 \
-key server.key \
-out server.crt \
-config cert.conf
`
Now verify:
`
openssl x509 -in server.crt -text -noout
`
You should see:
`CA:FALSE`

NOT `CA:TRUE`

then confirm the files exist ,if so copy them so you can import them

~/ssl $ ls
cert.conf  server.crt  server.key
~/ssl $