for i in {1..100}
do
curl -s --location --request POST 'http://localhost:8080/account' \
--header 'Content-Type: application/json' \
--data-raw "{
    \"id\": \"$i\",
    \"balance\" : \"100\"
}" |jq
done
