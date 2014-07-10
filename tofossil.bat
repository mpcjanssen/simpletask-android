del .repo
fossil new --empty -A mpcjanssen .repo
git fast-export master| fossil import --incremental .repo

git fast-export sync-api| fossil import --incremental .repo

