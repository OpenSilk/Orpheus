Orpheus' second heart

Contains the index database, and scanner service

primary process is ':prvdr' with client as addon to ':ui' process and ':service' process

TODOS
    tests that actually test stuff
    smarten up the scanner, ie if not network, delay and try later,
        parallelization (though memory usage is an issue there)
    tests that verify proper interaction between client and provider and db
    error handling