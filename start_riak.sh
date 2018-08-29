#!/bin/sh

docker run \
--rm \
-p 8087:8087 \
-v $(pwd)/riak_schemas:/etc/riak/schemas \
basho/riak-kv:ubuntu-2.2.3
